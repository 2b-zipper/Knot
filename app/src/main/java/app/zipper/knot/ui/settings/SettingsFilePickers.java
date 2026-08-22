package app.zipper.knot.ui.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import androidx.documentfile.provider.DocumentFile;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.Main;
import app.zipper.knot.SettingsStore;
import app.zipper.knot.hooks.BackupRestoreHook;
import app.zipper.knot.utils.FontFileUtil;
import app.zipper.knot.utils.LineTheme;
import app.zipper.knot.utils.ModuleStrings;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public final class SettingsFilePickers {

  private static final int PICK_DIRECTORY_CODE = 0x4C58;
  private static final int PICK_FONT_CODE = 0x4C59;
  private static final int PICK_RESTORE_DB_CODE = 0x4C5A;

  private static final String FONT_PATH_KEY = "custom_font_path";
  private static final String FONT_NAME_KEY = "custom_font_name";

  private SettingsFilePickers() {}

  public static boolean consumeResult(Activity host, int requestCode, int resultCode, Intent data) {
    if (requestCode != PICK_DIRECTORY_CODE
        && requestCode != PICK_FONT_CODE
        && requestCode != PICK_RESTORE_DB_CODE) {
      return false;
    }
    Uri uri = resultCode == Activity.RESULT_OK && data != null ? data.getData() : null;
    if (uri == null) return true;

    if (requestCode == PICK_DIRECTORY_CODE) {
      onDirectoryPicked(host, uri);
    } else if (requestCode == PICK_FONT_CODE) {
      onFontPicked(host, uri);
    } else {
      new Thread(() -> prepareRestoreDb(host, uri)).start();
    }
    return true;
  }

  public static void openFolderPicker(Context ctx) {
    Activity host = SettingsViews.activityOf(ctx);
    if (host == null) return;
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    host.startActivityForResult(intent, PICK_DIRECTORY_CODE);
  }

  public static void openFontPicker(Context ctx) {
    Activity host = SettingsViews.activityOf(ctx);
    if (host == null) return;
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("*/*");
    intent.putExtra(
        Intent.EXTRA_MIME_TYPES,
        new String[] {"font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-otf"});
    host.startActivityForResult(intent, PICK_FONT_CODE);
  }

  public static void openRestorePicker(Context ctx) {
    Activity host = SettingsViews.activityOf(ctx);
    if (host == null) return;
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("*/*");
    intent.putExtra(Intent.EXTRA_TITLE, "Knot_*.knotbak");
    Uri initialUri = backupFolderUri(ctx);
    if (initialUri != null) intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
    host.startActivityForResult(intent, PICK_RESTORE_DB_CODE);
  }

  private static Uri backupFolderUri(Context ctx) {
    try {
      String dirUriStr = SettingsStore.getSettingsDirUri();
      if (dirUriStr == null) return null;

      Uri treeUri = Uri.parse(dirUriStr);
      String treeId = DocumentsContract.getTreeDocumentId(treeUri);
      DocumentFile root = DocumentFile.fromTreeUri(ctx, treeUri);
      DocumentFile backupDir = root == null ? null : root.findFile("KnotBackup");

      String targetId = treeId;
      if (backupDir != null && backupDir.isDirectory()) {
        targetId = treeId + (treeId.endsWith(":") ? "" : "/") + "KnotBackup";
      }
      return DocumentsContract.buildDocumentUriUsingTree(treeUri, targetId);
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static void onDirectoryPicked(Activity host, Uri treeUri) {
    try {
      host.getContentResolver()
          .takePersistableUriPermission(
              treeUri,
              Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    } catch (Throwable ignored) {
    }
    SettingsStore.setSettingsDir(treeUri.toString());
    SettingsStore.load(Main.options);
    KnotSettingsDialog.notifyConfigChanged();
  }

  private static void onFontPicked(Context ctx, Uri fontUri) {
    try {
      File out = new File(ctx.getFilesDir(), "knot_custom_font.ttf");
      try (InputStream is = ctx.getContentResolver().openInputStream(fontUri);
          OutputStream os = new FileOutputStream(out)) {
        copy(is, os);
      }

      String localPath = out.getAbsolutePath();
      SettingsStore.save(FONT_PATH_KEY, localPath);
      SettingsStore.save(FONT_NAME_KEY, resolveFontName(ctx, out, fontUri));
      KnotConfig.Item item = Main.options.find(FONT_PATH_KEY);
      if (item != null) item.value = localPath;
      KnotSettingsDialog.notifyConfigChanged();
    } catch (Throwable t) {
      Knot.log("Knot: Failed to copy font file: " + t.getMessage());
    }
  }

  public static String currentFontName() {
    String stored = SettingsStore.getString(FONT_NAME_KEY, "");
    if (!stored.isEmpty()) return stored;

    String path = SettingsStore.getString(FONT_PATH_KEY, "");
    String name = path.isEmpty() ? null : FontFileUtil.readFontName(new File(path));
    if (name == null) return "";

    SettingsStore.save(FONT_NAME_KEY, name);
    return name;
  }

  private static String resolveFontName(Context ctx, File fontFile, Uri fontUri) {
    String name = FontFileUtil.readFontName(fontFile);
    if (name == null) name = queryDisplayName(ctx, fontUri);
    return name == null ? "" : name;
  }

  private static String queryDisplayName(Context ctx, Uri uri) {
    String[] columns = {OpenableColumns.DISPLAY_NAME};
    try (Cursor cursor = ctx.getContentResolver().query(uri, columns, null, null, null)) {
      if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) return cursor.getString(0);
    } catch (Throwable ignored) {
    }
    return null;
  }

  private static void prepareRestoreDb(Context ctx, Uri dbUri) {
    File tempFile = null;
    try {
      tempFile = File.createTempFile("knot_restore_", ".db", ctx.getCacheDir());
      try (InputStream is = ctx.getContentResolver().openInputStream(dbUri);
          OutputStream os = new FileOutputStream(tempFile)) {
        copy(is, os);
      }
      final File finalFile = tempFile;
      new Handler(Looper.getMainLooper()).post(() -> confirmRestore(ctx, finalFile));
    } catch (Throwable t) {
      Knot.log("Knot: Failed to prepare restore DB: " + t.getMessage());
      if (tempFile != null) tempFile.delete();
    }
  }

  private static void confirmRestore(Context ctx, File file) {
    LineTheme.applyDialogColors(
        new AlertDialog.Builder(ctx, LineTheme.dialogTheme(ctx))
            .setTitle(ModuleStrings.RESTORE_CONFIRM_TITLE)
            .setMessage(ModuleStrings.RESTORE_CONFIRM_MSG)
            .setPositiveButton(
                ModuleStrings.SETTINGS_YES, (d, w) -> BackupRestoreHook.runRestore(ctx, file))
            .setNegativeButton(ModuleStrings.SETTINGS_CANCEL, (d, w) -> file.delete())
            .show(),
        ctx);
  }

  private static void copy(InputStream is, OutputStream os) throws Exception {
    byte[] buffer = new byte[8192];
    int len;
    while ((len = is.read(buffer)) != -1) os.write(buffer, 0, len);
  }
}
