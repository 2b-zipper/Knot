package app.zipper.knot.hooks;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.SystemClock;
import app.zipper.knot.Knot;
import app.zipper.knot.LineVersion;
import app.zipper.knot.Reflect;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

final class NotificationMediaFileStore {
  private static final String DIRECTORY = "knot_notification_media";
  private static final long FILE_TTL_MS = TimeUnit.HOURS.toMillis(24);
  private static final long PRUNE_INTERVAL_MS = TimeUnit.HOURS.toMillis(1);
  private static final int PNG_QUALITY = 100;
  private static final int JPEG_QUALITY = 88;
  private static final int COPY_BUFFER_BYTES = 16 * 1024;
  private static final int DIGEST_BYTES = 16;

  private static long lastPruneElapsedMs;

  private NotificationMediaFileStore() {}

  static synchronized Attachment put(
      Context context, String messageId, Bitmap bitmap, String mimeType) {
    if (context == null || !hasText(messageId) || bitmap == null || !hasText(mimeType)) {
      return null;
    }

    File temporary = null;
    try {
      File root = notificationRoot(context);
      if (root == null) return null;
      boolean png = "image/png".equalsIgnoreCase(mimeType);
      File file = new File(root, digest(messageId) + (png ? ".png" : ".jpg"));
      temporary = File.createTempFile("media-", ".tmp", root);
      try (FileOutputStream out = new FileOutputStream(temporary, false)) {
        Bitmap.CompressFormat format = png ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
        if (!bitmap.compress(format, png ? PNG_QUALITY : JPEG_QUALITY, out)) return null;
        out.flush();
      }
      commitFile(temporary, file);
      return attachmentForFile(context, file, mimeType);
    } catch (Throwable t) {
      Knot.log("Knot: notification media URI unavailable: " + t.getClass().getSimpleName());
      return null;
    } finally {
      if (temporary != null) temporary.delete();
    }
  }

  static synchronized Attachment getStored(Context context, String cacheId, String mimeType) {
    if (context == null || !hasText(cacheId) || !hasText(mimeType)) return null;

    try {
      File root = notificationRoot(context);
      if (root == null) return null;
      File file = new File(root, digest(cacheId) + '.' + extensionForMime(mimeType));
      if (!file.isFile() || file.length() <= 0L) return null;
      file.setLastModified(System.currentTimeMillis());
      return attachmentForFile(context, file, mimeType);
    } catch (Throwable t) {
      Knot.log(
          "Knot: notification media stored cache unavailable: " + t.getClass().getSimpleName());
      return null;
    }
  }

  static synchronized Attachment fromExistingFile(Context context, File file, String mimeType) {
    if (context == null
        || file == null
        || !file.isFile()
        || file.length() <= 0
        || !hasText(mimeType)) {
      return null;
    }

    try {
      Attachment direct = attachmentForFile(context, file, mimeType);
      if (direct != null) return direct;
    } catch (Throwable ignored) {
    }

    try {
      File root = notificationRoot(context);
      if (root == null) return null;
      String fingerprint = file.getAbsolutePath() + ':' + file.length() + ':' + file.lastModified();
      File staged =
          new File(root, digest("line-owned:" + fingerprint) + '.' + extensionForMime(mimeType));
      if (!staged.isFile() || staged.length() != file.length()) {
        File temporary = File.createTempFile("media-", ".tmp", root);
        try {
          try (FileInputStream in = new FileInputStream(file);
              FileOutputStream out = new FileOutputStream(temporary, false)) {
            byte[] buffer = new byte[COPY_BUFFER_BYTES];
            int read;
            while ((read = in.read(buffer)) != -1) {
              out.write(buffer, 0, read);
            }
            out.flush();
          }
          if (temporary.length() != file.length()) {
            throw new IOException("LINE media file changed while copying");
          }
          commitFile(temporary, staged);
        } finally {
          temporary.delete();
        }
      }
      return attachmentForFile(context, staged, mimeType);
    } catch (Throwable t) {
      Knot.log("Knot: LINE media file is not shareable: " + t.getClass().getSimpleName());
      return null;
    }
  }

  private static void commitFile(File temporary, File destination) throws IOException {
    if (temporary.length() <= 0L) throw new IOException("Empty notification media file");
    Files.move(
        temporary.toPath(),
        destination.toPath(),
        StandardCopyOption.ATOMIC_MOVE,
        StandardCopyOption.REPLACE_EXISTING);
  }

  private static File notificationRoot(Context context) {
    File root = new File(new File(context.getCacheDir(), "external_share"), DIRECTORY);
    if (!root.isDirectory() && !root.mkdirs()) return null;
    pruneIfNeeded(root);
    return root;
  }

  private static Attachment attachmentForFile(Context context, File file, String mimeType)
      throws Exception {
    Uri uri = uriForFile(context, file);
    if (uri == null) return null;
    try {
      context.grantUriPermission(
          "com.android.systemui", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
    } catch (Throwable ignored) {
    }
    return new Attachment(mimeType, uri);
  }

  private static Uri uriForFile(Context context, File file) throws Exception {
    LineVersion.Config version = LineVersion.get();
    if (version == null) return null;
    LineVersion.Config.Notification config = version.notification;
    if (!hasText(config.fileProviderHelperClass) || !hasText(config.fileProviderUriMethod)) {
      return null;
    }
    Class<?> helper = Reflect.findClass(config.fileProviderHelperClass, context.getClassLoader());
    Object result =
        Reflect.findMethodExact(helper, config.fileProviderUriMethod, Context.class, File.class)
            .invoke(null, context, file);
    return result instanceof Uri ? (Uri) result : null;
  }

  private static void pruneIfNeeded(File root) {
    long now = SystemClock.elapsedRealtime();
    if (lastPruneElapsedMs != 0L && now - lastPruneElapsedMs < PRUNE_INTERVAL_MS) return;
    lastPruneElapsedMs = now;
    prune(root);
  }

  private static void prune(File root) {
    File[] files = root.listFiles();
    if (files == null) return;
    long cutoff = System.currentTimeMillis() - FILE_TTL_MS;
    for (File file : files) {
      if (file.isFile() && file.lastModified() < cutoff) file.delete();
    }
  }

  private static String extensionForMime(String mimeType) {
    String value = mimeType.toLowerCase(Locale.ROOT);
    if ("image/jpeg".equals(value)) return "jpg";
    if ("image/png".equals(value)) return "png";
    if ("image/gif".equals(value)) return "gif";
    if ("image/webp".equals(value)) return "webp";
    if ("image/avif".equals(value)) return "avif";
    if ("image/heif".equals(value) || "image/heic".equals(value)) return "heic";
    return "img";
  }

  private static String digest(String value) throws Exception {
    byte[] hash =
        MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    StringBuilder out = new StringBuilder(DIGEST_BYTES * 2);
    for (int i = 0; i < DIGEST_BYTES; i++) {
      out.append(Character.forDigit((hash[i] >>> 4) & 0x0F, 16));
      out.append(Character.forDigit(hash[i] & 0x0F, 16));
    }
    return out.toString();
  }

  private static boolean hasText(String value) {
    return value != null && !value.isEmpty();
  }

  static final class Attachment {
    final String mimeType;
    final Uri uri;

    Attachment(String mimeType, Uri uri) {
      this.mimeType = mimeType;
      this.uri = uri;
    }
  }
}
