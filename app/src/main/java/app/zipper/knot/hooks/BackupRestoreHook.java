package app.zipper.knot.hooks;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.documentfile.provider.DocumentFile;
import app.zipper.knot.R;
import app.zipper.knot.SettingsStore;
import app.zipper.knot.utils.LineTheme;
import app.zipper.knot.utils.ModuleResources;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class BackupRestoreHook {

  private static final String LOG_TAG = "KnotSync";
  private static final ExecutorService syncExecutor = Executors.newSingleThreadExecutor();
  private static final Handler uiHandler = new Handler(Looper.getMainLooper());

  private static final List<String> DB_NAMES = Arrays.asList("naver_line", "contact", "square");
  private static final String[] DB_SUFFIXES = {"", "-wal"};
  private static final byte[] ZIP_MAGIC = {0x50, 0x4B, 0x03, 0x04};
  private static final String MARKER_ENTRY = "knot-backup-v1";
  private static final int COPY_BUFFER_SIZE = 64 * 1024;

  public static void runBackup(Context context) {
    final ProgressDialog pd =
        createSyncProgress(context, ModuleResources.get(R.string.restore_preparing));
    pd.show();
    LineTheme.applyDialogColors(pd, context);

    syncExecutor.execute(
        () -> {
          final boolean result = executeKnotBackup(context);
          uiHandler.post(
              () -> {
                pd.dismiss();
                notifySyncResult(
                    context,
                    result,
                    ModuleResources.get(R.string.backup_success),
                    ModuleResources.get(R.string.backup_error));
              });
        });
  }

  public static void runRestore(Context context, File backupFile) {
    final ProgressDialog pd =
        createSyncProgress(context, ModuleResources.get(R.string.restore_processing));
    pd.show();
    LineTheme.applyDialogColors(pd, context);

    syncExecutor.execute(
        () -> {
          final boolean result = executeFullRestore(context, backupFile);
          uiHandler.post(
              () -> {
                pd.dismiss();
                if (result) {
                  LineTheme.applyDialogColors(
                      new AlertDialog.Builder(context, LineTheme.dialogTheme(context))
                          .setTitle(ModuleResources.get(R.string.restore_success))
                          .setMessage(ModuleResources.get(R.string.manager_restart_required))
                          .setPositiveButton(
                              ModuleResources.get(R.string.restart_ok),
                              (d, w) -> android.os.Process.killProcess(android.os.Process.myPid()))
                          .setCancelable(false)
                          .show(),
                      context);
                } else {
                  notifySyncResult(
                      context,
                      false,
                      ModuleResources.get(R.string.restore_success),
                      ModuleResources.get(R.string.restore_error));
                }
                if (backupFile.getName().startsWith("knot_restore_")) {
                  backupFile.delete();
                }
              });
        });
  }

  private static boolean executeKnotBackup(Context context) {
    DocumentFile outFile = null;
    try {
      String dirUriStr = SettingsStore.getSettingsDirUri();
      if (dirUriStr == null) return false;

      DocumentFile root = DocumentFile.fromTreeUri(context, Uri.parse(dirUriStr));
      if (root == null || !root.canWrite()) {
        Log.e(LOG_TAG, "Cannot access backup directory: " + dirUriStr);
        return false;
      }

      File mainDb = context.getDatabasePath("naver_line");
      if (!mainDb.exists()) {
        Log.e(LOG_TAG, "Source database not found");
        return false;
      }

      String stamp =
          new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
      DocumentFile knotFolder = ensureSubDir(root, "KnotBackup");
      if (knotFolder == null) return false;

      outFile = knotFolder.createFile("application/octet-stream", "Knot_" + stamp + ".knotbak");
      if (outFile == null) return false;

      writeBackupZip(context, outFile.getUri());
      return true;
    } catch (Exception e) {
      Log.e(LOG_TAG, "Backup failed: " + e.getMessage());
      if (outFile != null) safeDelete(outFile);
      return false;
    }
  }

  private static void writeBackupZip(Context context, Uri dst) throws IOException {
    try (OutputStream raw = context.getContentResolver().openOutputStream(dst);
        ZipOutputStream zip = new ZipOutputStream(raw)) {

      zip.putNextEntry(new ZipEntry(MARKER_ENTRY));
      zip.closeEntry();

      for (String dbName : DB_NAMES) {
        File baseDb = context.getDatabasePath(dbName);
        for (String suffix : DB_SUFFIXES) {
          File f = new File(baseDb.getPath() + suffix);
          if (f.isFile()) {
            writeFileEntry(zip, dbName + ".db" + suffix, f);
          }
        }
      }
    }
  }

  private static void writeFileEntry(ZipOutputStream zip, String name, File file)
      throws IOException {
    zip.putNextEntry(new ZipEntry(name));
    try (FileInputStream in = new FileInputStream(file)) {
      copyStream(in, zip);
    }
    zip.closeEntry();
  }

  private static boolean executeFullRestore(Context context, File srcFile) {
    return isZipFile(srcFile) ? restoreFromZip(context, srcFile) : restoreLegacy(context, srcFile);
  }

  private static boolean restoreFromZip(Context context, File srcFile) {
    try (ZipFile zip = new ZipFile(srcFile)) {
      if (zip.getEntry(MARKER_ENTRY) == null) {
        Log.e(LOG_TAG, "Restore failed: not a Knot backup");
        return false;
      }

      Set<String> dbsToReplace = new HashSet<>();
      Enumeration<? extends ZipEntry> entries = zip.entries();
      while (entries.hasMoreElements()) {
        String name = entries.nextElement().getName();
        String dbName = matchedDbName(name);
        if (dbName != null && name.equals(dbName + ".db")) {
          dbsToReplace.add(dbName);
        }
      }

      for (String dbName : dbsToReplace) wipeDbFamily(context.getDatabasePath(dbName));

      entries = zip.entries();
      while (entries.hasMoreElements()) {
        ZipEntry e = entries.nextElement();
        if (e.isDirectory() || MARKER_ENTRY.equals(e.getName())) continue;

        String dbName = matchedDbName(e.getName());
        if (dbName == null) {
          Log.w(LOG_TAG, "Skipping unknown entry: " + e.getName());
          continue;
        }

        File baseDb = context.getDatabasePath(dbName);
        File target = new File(baseDb.getPath() + e.getName().substring((dbName + ".db").length()));
        try (InputStream in = zip.getInputStream(e);
            OutputStream out = new FileOutputStream(target)) {
          copyStream(in, out);
        }
      }
      return true;
    } catch (Exception e) {
      Log.e(LOG_TAG, "Zip restore failed: " + e.getMessage());
      return false;
    }
  }

  private static boolean restoreLegacy(Context context, File srcFile) {
    try (SQLiteDatabase check =
        SQLiteDatabase.openDatabase(
            srcFile.getAbsolutePath(),
            null,
            SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS)) {
    } catch (Exception e) {
      Log.e(LOG_TAG, "Restore failed: Invalid database file");
      return false;
    }

    File localDb = context.getDatabasePath("naver_line");
    try {
      wipeDbFamily(localDb);
      try (InputStream in = new FileInputStream(srcFile);
          OutputStream out = new FileOutputStream(localDb)) {
        copyStream(in, out);
      }
      return true;
    } catch (Exception e) {
      Log.e(LOG_TAG, "Legacy restore failed: " + e.getMessage());
      return false;
    }
  }

  private static String matchedDbName(String entryName) {
    for (String name : DB_NAMES) {
      for (String suffix : DB_SUFFIXES) {
        if (entryName.equals(name + ".db" + suffix)) return name;
      }
    }
    return null;
  }

  private static void wipeDbFamily(File baseDb) {
    baseDb.delete();
    new File(baseDb.getPath() + "-wal").delete();
    new File(baseDb.getPath() + "-shm").delete();
  }

  private static boolean isZipFile(File file) {
    if (file == null || !file.exists() || file.length() < ZIP_MAGIC.length) return false;
    try (FileInputStream in = new FileInputStream(file)) {
      byte[] header = new byte[ZIP_MAGIC.length];
      return in.read(header) == ZIP_MAGIC.length && Arrays.equals(header, ZIP_MAGIC);
    } catch (Throwable t) {
      return false;
    }
  }

  private static void copyStream(InputStream in, OutputStream out) throws IOException {
    byte[] buf = new byte[COPY_BUFFER_SIZE];
    int n;
    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
  }

  private static DocumentFile ensureSubDir(DocumentFile parent, String name) {
    DocumentFile dir = parent.findFile(name);
    return (dir != null && dir.isDirectory()) ? dir : parent.createDirectory(name);
  }

  private static void safeDelete(DocumentFile file) {
    try {
      file.delete();
    } catch (Throwable ignored) {
    }
  }

  private static ProgressDialog createSyncProgress(Context context, String text) {
    ProgressDialog pd = new ProgressDialog(context, LineTheme.dialogTheme(context));
    pd.setMessage(text);
    pd.setCancelable(false);
    return pd;
  }

  private static void notifySyncResult(Context context, boolean success, String sMsg, String eMsg) {
    Toast.makeText(context, success ? sMsg : eMsg, Toast.LENGTH_LONG).show();
  }
}
