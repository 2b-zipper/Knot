package app.zipper.knot.hooks;

import android.content.Context;
import app.zipper.knot.Knot;
import app.zipper.knot.LineVersion;
import app.zipper.knot.Reflect;
import java.io.File;
import java.util.function.BooleanSupplier;

final class LineChatThumbnailResolver {
  private static volatile Object lineContentCache;

  private LineChatThumbnailResolver() {}

  static NotificationMediaFileStore.Attachment acquireCached(
      Context context, NotificationMediaThumbnailKeyStore.Key key) {
    if (context == null || key == null) return null;
    LineVersion.Config version = LineVersion.get();
    if (version == null) return null;
    try {
      return attachmentFromFile(
          context, lineThumbnailCacheFile(context, key, version.notification));
    } catch (Throwable t) {
      logAcquisitionFailure(key, t);
      return null;
    }
  }

  static NotificationMediaFileStore.Attachment acquireOrRequest(
      Context context, NotificationMediaThumbnailKeyStore.Key key, BooleanSupplier isNeeded) {
    if (!isNeeded.getAsBoolean()) return null;
    NotificationMediaFileStore.Attachment cached = acquireCached(context, key);
    if (cached != null) return cached;
    if (context == null || key == null) return null;
    LineVersion.Config version = LineVersion.get();
    if (version == null) return null;
    try {
      Object request = buildThumbnailRequest(context, key, version.notification);
      return attachmentFromFile(context, LineGlideRequest.requestFile(context, request, isNeeded));
    } catch (Throwable t) {
      logAcquisitionFailure(key, t);
      return null;
    }
  }

  private static NotificationMediaFileStore.Attachment attachmentFromFile(
      Context context, File file) {
    if (file == null || !file.isFile() || file.length() <= 0L) return null;
    String mimeType = MediaFileUtils.sniffMime(file);
    if (!MediaFileUtils.isImage(mimeType)) return null;
    return NotificationMediaFileStore.fromExistingFile(context, file, mimeType);
  }

  private static void logAcquisitionFailure(
      NotificationMediaThumbnailKeyStore.Key key, Throwable t) {
    Knot.log(
        "Knot: notification media thumbnail acquisition failed (messageId="
            + key.serverMessageId
            + ")",
        t);
  }

  private static Object buildThumbnailRequest(
      Context context,
      NotificationMediaThumbnailKeyStore.Key key,
      LineVersion.Config.Notification config)
      throws Exception {
    ClassLoader loader = context.getClassLoader();
    Class<?> requestClass = Reflect.findClass(config.messageThumbnailRequestClass, loader);
    Class<?> encryptionClass = Reflect.findClass(config.messageObsEncryptionDataClass, loader);
    return Reflect.findConstructorExact(
            requestClass,
            String.class,
            String.class,
            long.class,
            String.class,
            encryptionClass,
            String.class,
            boolean.class)
        .newInstance(
            key.chatId,
            key.serverMessageId,
            key.localMessageId,
            null,
            key.obsEncryptionData,
            key.obsPopInfo,
            key.isSquare);
  }

  private static File lineThumbnailCacheFile(
      Context context,
      NotificationMediaThumbnailKeyStore.Key key,
      LineVersion.Config.Notification config)
      throws Exception {
    ClassLoader loader = context.getClassLoader();
    Class<?> cacheClass = Reflect.findClass(config.messageContentCacheClass, loader);
    Object cache = lineContentCache;
    if (cache == null || !cacheClass.isInstance(cache)) {
      synchronized (LineChatThumbnailResolver.class) {
        cache = lineContentCache;
        if (cache == null || !cacheClass.isInstance(cache)) {
          cache = Reflect.findConstructorExact(cacheClass).newInstance();
          Reflect.findMethodExact(
                  cacheClass, config.messageContentCacheInitializeMethod, Context.class)
              .invoke(cache, context);
          lineContentCache = cache;
        }
      }
    }

    Class<?> keyClass = Reflect.findClass(config.messageContentCacheKeyClass, loader);
    Object contentKey =
        Reflect.findConstructorExact(keyClass, String.class, long.class)
            .newInstance(key.chatId, key.localMessageId);
    Object result =
        Reflect.findMethodExact(cacheClass, config.messageThumbnailCacheFileMethod, keyClass)
            .invoke(cache, contentKey);
    return result instanceof File ? (File) result : null;
  }
}
