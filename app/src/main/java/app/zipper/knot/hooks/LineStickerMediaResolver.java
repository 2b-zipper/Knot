package app.zipper.knot.hooks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import app.zipper.knot.Knot;
import app.zipper.knot.LineVersion;
import app.zipper.knot.Reflect;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.json.JSONObject;

final class LineStickerMediaResolver {
  private static final float MAX_STICKER_WIDTH_DP = 158.4f;
  private static final int MAX_PENDING = 64;
  private static final long PENDING_TTL_MS = TimeUnit.SECONDS.toMillis(30);

  private static volatile Object stickerFileManager;
  private static final Object pendingLock = new Object();
  private static final LinkedHashMap<String, PendingCache> pending = new LinkedHashMap<>();

  private LineStickerMediaResolver() {}

  static NotificationMediaFileStore.Attachment acquireCached(
      Context context, StickerMetadata metadata) {
    if (context == null || metadata == null) return null;
    return attachmentFromFile(
        context, lineStickerCacheFile(context, metadata.packageId, metadata.stickerId));
  }

  static void registerCacheReady(Context context, StickerMetadata metadata, Runnable onReady) {
    if (context == null || metadata == null || onReady == null) return;
    registerFileReady(
        lineStickerCacheFile(context, metadata.packageId, metadata.stickerId), onReady);
  }

  static File cachedStickerFile(Context context, StickerPart sticker) {
    if (context == null || sticker == null) return null;
    File file = lineStickerCacheFile(context, sticker.packageId, sticker.stickerId);
    return isUsableImage(file) ? file : null;
  }

  static boolean requestMissingPart(
      Context context, StickerPart sticker, BooleanSupplier isNeeded) {
    if (context == null || sticker == null || !isNeeded.getAsBoolean()) return false;
    String key = sticker.packageId + ":" + sticker.stickerId;
    if (cachedStickerFile(context, sticker) != null) return true;
    try {
      Object model = createStaticPartModel(context, sticker);
      if (model == null) return false;
      LineGlideRequest.requestFile(context, model, isNeeded);
    } catch (Throwable t) {
      Knot.log("Knot: combination sticker part request failed (part=" + key + ")", t);
    }
    return cachedStickerFile(context, sticker) != null;
  }

  private static Object createStaticPartModel(Context context, StickerPart sticker)
      throws Exception {
    LineVersion.Config version = LineVersion.get();
    if (version == null) return null;
    LineVersion.Config.Notification config = version.notification;
    if (!hasText(config.stickerRequestFactoryClass)) return null;
    ClassLoader loader = context.getClassLoader();
    Class<?> factoryClass = Reflect.findClass(config.stickerRequestFactoryClass, loader);
    Object provider = Reflect.getStaticObjectField(factoryClass, config.stickerRequestFactoryField);
    Object factory =
        Reflect.callStaticMethod(
            Reflect.findClass(config.stickerServiceLocatorClass, loader),
            config.stickerServiceLocatorMethod,
            context,
            provider);
    Class<?> secretClass = Reflect.findClass(config.stickerSecretClass, loader);
    Object secret =
        sticker.hash == null
            ? null
            : Reflect.findConstructorExact(secretClass, String.class, String.class)
                .newInstance(sticker.hash, null);
    Class<?> resourceClass = Reflect.findClass(config.stickerResourceClass, loader);
    Object resource =
        Reflect.findConstructorExact(
                resourceClass, long.class, long.class, long.class, secretClass, String.class)
            .newInstance(sticker.packageId, sticker.version, sticker.stickerId, secret, null);
    Object option =
        Reflect.getStaticObjectField(
            Reflect.findClass(config.stickerOptionClass, loader), config.stickerStaticOptionField);
    // A notification composes still images from LINE's main sticker cache. STATIC selects that same
    // main image cache even for an animated sticker; true persists it in LINE.
    return Reflect.callMethod(factory, config.stickerMainRequestMethod, resource, option, true);
  }

  static void onLineStickerFetchCompleted(File file) {
    if (!isUsableImage(file)) return;
    List<Runnable> callbacks = null;
    synchronized (pendingLock) {
      prunePendingLocked();
      PendingCache item = pending.remove(file.getAbsolutePath());
      if (item != null && !item.callbacks.isEmpty()) {
        callbacks = new ArrayList<>(item.callbacks);
      }
    }
    if (callbacks != null) {
      for (Runnable callback : callbacks) callback.run();
    }
  }

  static StickerMetadata parse(String parameter) {
    String combinationStickerId = null;
    long stickerId = -1L;
    long packageId = -1L;
    if (hasText(parameter)) {
      try {
        JSONObject json = new JSONObject(parameter);
        combinationStickerId = stringValue(json, "CSSTKID");
        stickerId = longValue(json, "STKID");
        packageId = longValue(json, "STKPKGID");
      } catch (Throwable ignored) {
      }
    }
    return new StickerMetadata(combinationStickerId, stickerId, packageId);
  }

  private static void registerFileReady(File cache, Runnable onReady) {
    if (cache == null || onReady == null) return;
    if (isUsableImage(cache)) {
      onReady.run();
      return;
    }

    List<Runnable> readyCallbacks = null;
    synchronized (pendingLock) {
      prunePendingLocked();
      String path = cache.getAbsolutePath();
      PendingCache item = pending.get(path);
      if (item == null) {
        item = new PendingCache(SystemClock.elapsedRealtime());
        pending.put(path, item);
      }
      item.callbacks.add(onReady);
      item.updatedAtMs = SystemClock.elapsedRealtime();
      trimPendingLocked();

      if (isUsableImage(cache)) {
        pending.remove(path);
        readyCallbacks = new ArrayList<>(item.callbacks);
      }
    }

    if (readyCallbacks != null) {
      for (Runnable callback : readyCallbacks) callback.run();
    }
  }

  private static File lineStickerCacheFile(Context context, long packageId, long stickerId) {
    if (context == null || packageId <= 0L || stickerId <= 0L) return null;
    LineVersion.Config version = LineVersion.get();
    if (version == null) return null;
    LineVersion.Config.Notification config = version.notification;
    if (!hasText(config.stickerFileManagerClass)
        || !hasText(config.stickerInitializeMethod)
        || !hasText(config.stickerMainCacheFileMethod)) {
      return null;
    }

    try {
      ClassLoader loader = context.getClassLoader();
      Class<?> managerClass = Reflect.findClass(config.stickerFileManagerClass, loader);
      Object manager = stickerFileManager;
      if (manager == null || !managerClass.isInstance(manager)) {
        synchronized (LineStickerMediaResolver.class) {
          manager = stickerFileManager;
          if (manager == null || !managerClass.isInstance(manager)) {
            manager = Reflect.findConstructorExact(managerClass).newInstance();
            Reflect.findMethodExact(managerClass, config.stickerInitializeMethod, Context.class)
                .invoke(manager, context);
            stickerFileManager = manager;
          }
        }
      }

      Object result =
          Reflect.findMethodExact(
                  managerClass, config.stickerMainCacheFileMethod, long.class, long.class)
              .invoke(manager, packageId, stickerId);
      return result instanceof File ? (File) result : null;
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static NotificationMediaFileStore.Attachment attachmentFromFile(
      Context context, File file) {
    if (!isUsableImage(file)) return null;
    String mimeType = MediaFileUtils.sniffMime(file);
    int maxWidth =
        Math.max(
            1,
            Math.round(MAX_STICKER_WIDTH_DP * context.getResources().getDisplayMetrics().density));
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    if (options.outWidth <= 0 || options.outHeight <= 0) return null;
    if (options.outWidth <= maxWidth) {
      return NotificationMediaFileStore.fromExistingFile(context, file, mimeType);
    }

    String cacheId =
        "sticker-width:"
            + maxWidth
            + ':'
            + file.getAbsolutePath()
            + ':'
            + file.length()
            + ':'
            + file.lastModified();
    NotificationMediaFileStore.Attachment stored =
        NotificationMediaFileStore.getStored(context, cacheId, "image/png");
    if (stored != null) return stored;

    int height = Math.max(1, Math.round((float) options.outHeight * maxWidth / options.outWidth));
    options.inJustDecodeBounds = false;
    options.inSampleSize = 1;
    while (options.outWidth / (options.inSampleSize * 2) >= maxWidth) {
      options.inSampleSize *= 2;
    }
    Bitmap decoded = null;
    Bitmap resized = null;
    try {
      decoded = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
      if (decoded == null) return null;
      resized = Bitmap.createScaledBitmap(decoded, maxWidth, height, true);
      return NotificationMediaFileStore.put(context, cacheId, resized, "image/png");
    } catch (Throwable t) {
      Knot.log("Knot: notification sticker resizing failed", t);
      return null;
    } finally {
      if (resized != null && resized != decoded) resized.recycle();
      if (decoded != null) decoded.recycle();
    }
  }

  private static boolean isUsableImage(File file) {
    if (file == null || !file.isFile() || file.length() <= 0L) return false;
    return MediaFileUtils.isImage(MediaFileUtils.sniffMime(file));
  }

  private static void prunePendingLocked() {
    long now = SystemClock.elapsedRealtime();
    Iterator<Map.Entry<String, PendingCache>> iterator = pending.entrySet().iterator();
    while (iterator.hasNext()) {
      if (now - iterator.next().getValue().updatedAtMs > PENDING_TTL_MS) {
        iterator.remove();
      }
    }
  }

  private static void trimPendingLocked() {
    while (pending.size() > MAX_PENDING) {
      Iterator<Map.Entry<String, PendingCache>> iterator = pending.entrySet().iterator();
      if (!iterator.hasNext()) break;
      iterator.next();
      iterator.remove();
    }
  }

  private static String stringValue(JSONObject json, String key) {
    Object value = json.opt(key);
    if (value == null || value == JSONObject.NULL) return null;
    String text = String.valueOf(value);
    return hasText(text) ? text : null;
  }

  private static long longValue(JSONObject json, String key) {
    String value = stringValue(json, key);
    if (!hasText(value)) return -1L;
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ignored) {
      return -1L;
    }
  }

  private static boolean hasText(String value) {
    return value != null && !value.isEmpty();
  }

  static final class StickerMetadata {
    final String combinationStickerId;
    final long stickerId;
    final long packageId;

    StickerMetadata(String combinationStickerId, long stickerId, long packageId) {
      this.combinationStickerId = combinationStickerId;
      this.stickerId = stickerId;
      this.packageId = packageId;
    }

    boolean isCombinationSticker() {
      return hasText(combinationStickerId);
    }
  }

  static final class StickerPart {
    final long stickerId;
    final long packageId;
    final long version;
    final String hash;

    StickerPart(long stickerId, long packageId) {
      this(stickerId, packageId, 1L, null);
    }

    StickerPart(long stickerId, long packageId, long version, String hash) {
      this.stickerId = stickerId;
      this.packageId = packageId;
      this.version = Math.max(version, 1L);
      this.hash = hash;
    }
  }

  private static final class PendingCache {
    final List<Runnable> callbacks = new ArrayList<>();
    long updatedAtMs;

    PendingCache(long updatedAtMs) {
      this.updatedAtMs = updatedAtMs;
    }
  }
}
