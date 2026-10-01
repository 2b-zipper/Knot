package app.zipper.knot.hooks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import app.zipper.knot.Knot;
import app.zipper.knot.LineVersion;
import app.zipper.knot.Reflect;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.json.JSONArray;
import org.json.JSONObject;

final class LineSticonMediaResolver {
  private static final int EMOJI_CANVAS_SIZE = 120;
  private static final int MAX_PENDING = 64;
  private static final long PENDING_TTL_MS = TimeUnit.SECONDS.toMillis(30);
  private static final long ACTIVE_FALLBACK_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(12);
  private static final ScheduledExecutorService fallbackScheduler =
      Executors.newSingleThreadScheduledExecutor(
          runnable -> daemonThread(runnable, "Knot-SticonTimeout"));
  private static final ExecutorService fallbackExecutor =
      Executors.newFixedThreadPool(2, runnable -> daemonThread(runnable, "Knot-SticonFallback"));
  private static final ExecutorService cacheExecutor =
      Executors.newSingleThreadExecutor(runnable -> daemonThread(runnable, "Knot-SticonCache"));
  private static final Object pendingLock = new Object();
  private static final Object renderLock = new Object();
  private static final LinkedHashMap<String, PendingCache> pending = new LinkedHashMap<>();

  private LineSticonMediaResolver() {}

  private static Thread daemonThread(Runnable runnable, String name) {
    Thread thread = new Thread(runnable, name);
    thread.setDaemon(true);
    return thread;
  }

  static NotificationMediaFileStore.Attachment acquireCached(
      Context context, NotificationMediaCaptureStore.MessageData captured) {
    Lookup lookup = lookup(context, captured);
    if (lookup == null) return null;
    return acquireCached(context, lookup);
  }

  private static NotificationMediaFileStore.Attachment acquireCached(
      Context context, Lookup lookup) {
    String cacheKey = String.valueOf(lookup.key);
    NotificationMediaFileStore.Attachment stored = storedAttachment(context, cacheKey);
    if (stored != null) return stored;
    Drawable drawable = cachedDrawable(lookup);
    return drawable == null ? null : saveAttachment(context, cacheKey, drawable);
  }

  static void registerCacheReady(
      Context context,
      NotificationMediaCaptureStore.MessageData captured,
      BooleanSupplier isNeeded,
      Runnable onReady) {
    if (context == null || captured == null || onReady == null || !isNeeded.getAsBoolean()) return;
    Lookup lookup = lookup(context, captured);
    if (lookup == null) return;
    String cacheKey = String.valueOf(lookup.key);
    if (acquireCached(context, lookup) != null) {
      onReady.run();
      return;
    }

    PendingCache item;
    Object attempt = null;
    synchronized (pendingLock) {
      prunePendingLocked();
      item = pending.get(cacheKey);
      if (item == null) {
        item = new PendingCache(SystemClock.elapsedRealtime());
        pending.put(cacheKey, item);
      }
      item.callbacks.add(new CacheCallback(isNeeded, onReady));
      item.updatedAtMs = SystemClock.elapsedRealtime();
      if (item.activeAttempt == null && !item.cacheWriteScheduled) {
        attempt = new Object();
        item.activeAttempt = attempt;
      }
      trimPendingLocked();
    }

    // Recheck after registering so a concurrent LINE cache update cannot be missed.
    if (storedAttachment(context, cacheKey) != null) {
      dispatchReady(cacheKey, item);
      return;
    }
    Drawable cached = cachedDrawable(lookup);
    if (cached != null) {
      onLineCacheAvailable(lookup.key, cached);
      releaseAttempt(cacheKey, item, attempt);
      return;
    }
    if (attempt != null) startActiveFallback(context, lookup, cacheKey, item, attempt);
  }

  private static void startActiveFallback(
      Context context, Lookup lookup, String cacheKey, PendingCache item, Object attempt) {
    if (!hasText(lookup.config.sticonCoroutineRequestClass)
        || !hasText(lookup.config.sticonCoroutineBuildersClass)
        || !hasText(lookup.config.sticonCoroutineRunBlockingMethod)
        || !hasText(lookup.config.sticonCoroutineFunctionClass)) {
      releaseAttempt(cacheKey, item, attempt);
      return;
    }
    FutureTask<Void> future =
        new FutureTask<Void>(
            () -> {
              try {
                synchronized (pendingLock) {
                  if (pending.get(cacheKey) != item || item.activeAttempt != attempt) return;
                }
                if (!hasCurrentRequester(item)) return;
                requestCoroutine(lookup);
                if (storedAttachment(context, cacheKey) != null) {
                  dispatchReady(cacheKey, item);
                } else {
                  Drawable drawable = cachedDrawable(lookup);
                  if (drawable != null) {
                    onLineCacheAvailable(lookup.key, drawable);
                  } else {
                    Knot.log("Knot: sticon fetch completed without cache (key=" + cacheKey + ")");
                  }
                }
              } catch (Throwable t) {
                Knot.log("Knot: sticon active fetch failed (key=" + cacheKey + ")", t);
              } finally {
                releaseAttempt(cacheKey, item, attempt);
              }
            },
            null) {
          @Override
          public void run() {
            // Queue time does not consume the request's timeout budget.
            ScheduledFuture<?> timeout =
                fallbackScheduler.schedule(
                    () -> {
                      if (cancel(true)) {
                        releaseAttempt(cacheKey, item, attempt);
                        Knot.log("Knot: sticon active fetch timed out (key=" + cacheKey + ")");
                      }
                    },
                    ACTIVE_FALLBACK_TIMEOUT_MS,
                    TimeUnit.MILLISECONDS);
            try {
              super.run();
            } finally {
              timeout.cancel(false);
            }
          }
        };
    try {
      fallbackExecutor.execute(future);
    } catch (RuntimeException t) {
      releaseAttempt(cacheKey, item, attempt);
      Knot.log("Knot: sticon active fetch rejected", t);
    }
  }

  private static void releaseAttempt(String cacheKey, PendingCache item, Object attempt) {
    synchronized (pendingLock) {
      if (attempt != null && pending.get(cacheKey) == item && item.activeAttempt == attempt) {
        item.activeAttempt = null;
      }
    }
  }

  static void onLineCacheAvailable(Object key, Drawable drawable) {
    if (key == null || drawable == null) return;
    String cacheKey = String.valueOf(key);
    PendingCache item;
    synchronized (pendingLock) {
      prunePendingLocked();
      item = pending.get(cacheKey);
      if (item == null || item.cacheWriteScheduled) return;
      item.cacheWriteScheduled = true;
    }
    // The LINE hook only hands off a drawable; drawing and file I/O run off its thread.
    try {
      cacheExecutor.execute(
          () -> {
            try {
              synchronized (pendingLock) {
                if (pending.get(cacheKey) != item) return;
              }
              if (!hasCurrentRequester(item)) return;
              Context context = Knot.currentApplication();
              if (context != null && saveAttachment(context, cacheKey, drawable) != null) {
                dispatchReady(cacheKey, item);
              }
            } catch (Throwable t) {
              Knot.log("Knot: sticon notification cache write failed", t);
            } finally {
              synchronized (pendingLock) {
                if (pending.get(cacheKey) == item) item.cacheWriteScheduled = false;
              }
            }
          });
    } catch (RuntimeException t) {
      synchronized (pendingLock) {
        if (pending.get(cacheKey) == item) item.cacheWriteScheduled = false;
      }
      Knot.log("Knot: sticon notification cache write rejected", t);
    }
  }

  private static void dispatchReady(String cacheKey, PendingCache expected) {
    List<CacheCallback> callbacks;
    synchronized (pendingLock) {
      if (pending.get(cacheKey) != expected) return;
      pending.remove(cacheKey);
      callbacks = new ArrayList<>(expected.callbacks);
    }
    for (CacheCallback callback : callbacks) {
      if (callback.isNeeded.getAsBoolean()) callback.onReady.run();
    }
  }

  private static NotificationMediaFileStore.Attachment storedAttachment(
      Context context, String cacheKey) {
    return NotificationMediaFileStore.getStored(context, persistentCacheId(cacheKey), "image/png");
  }

  private static NotificationMediaFileStore.Attachment saveAttachment(
      Context context, String cacheKey, Drawable drawable) {
    synchronized (renderLock) {
      NotificationMediaFileStore.Attachment stored = storedAttachment(context, cacheKey);
      if (stored != null) return stored;
      Bitmap bitmap = renderDrawable(drawable);
      if (bitmap == null) return null;
      try {
        return NotificationMediaFileStore.put(
            context, persistentCacheId(cacheKey), bitmap, "image/png");
      } finally {
        bitmap.recycle();
      }
    }
  }

  private static void requestCoroutine(Lookup lookup) throws Exception {
    ClassLoader loader = lookup.repository.getClass().getClassLoader();
    LineVersion.Config.Notification config = lookup.config;
    Class<?> requestClass = Reflect.findClass(config.sticonCoroutineRequestClass, loader);
    Class<?> continuationClass = Reflect.findClass("kotlin.coroutines.Continuation", loader);
    // Reuse LINE's suspend lambda: it downloads, decodes, and fills the native cache.
    // Native runBlocking also cancels its coroutine when the worker is interrupted.
    Object request =
        Reflect.findConstructorExact(
                requestClass,
                lookup.repository.getClass(),
                lookup.key.getClass(),
                continuationClass)
            .newInstance(lookup.repository, lookup.key, null);
    Class<?> buildersClass = Reflect.findClass(config.sticonCoroutineBuildersClass, loader);
    Class<?> functionClass = Reflect.findClass(config.sticonCoroutineFunctionClass, loader);
    Reflect.findMethodExact(buildersClass, config.sticonCoroutineRunBlockingMethod, functionClass)
        .invoke(null, request);
  }

  private static Lookup lookup(
      Context context, NotificationMediaCaptureStore.MessageData captured) {
    if (context == null || captured == null) return null;
    SticonSpec spec = parseSingleSticon(captured.text, captured.parameter);
    if (spec == null) return null;

    try {
      LineVersion.Config version = LineVersion.get();
      if (version == null) return null;
      LineVersion.Config.Notification config = version.notification;
      if (!hasText(config.sticonImageRepositoryClass)
          || !hasText(config.sticonImageRepositoryFactoryField)
          || !hasText(config.sticonImageRepositoryFactoryMethod)
          || !hasText(config.sticonImageRepositoryCacheMethod)
          || !hasText(config.sticonImageKeyClass)
          || !hasText(config.sticonPaidProductClass)
          || !hasText(config.sticonPaidClass)
          || !hasText(config.sticonOptionTypeClass)) {
        return null;
      }

      ClassLoader loader = context.getClassLoader();
      Class<?> repositoryClass = Reflect.findClass(config.sticonImageRepositoryClass, loader);
      Object factory =
          Reflect.getStaticObjectField(repositoryClass, config.sticonImageRepositoryFactoryField);
      if (factory == null) return null;

      Object repository =
          Reflect.callMethod(factory, config.sticonImageRepositoryFactoryMethod, context);
      if (repository == null) return null;

      Class<?> keyClass = Reflect.findClass(config.sticonImageKeyClass, loader);
      Object key = createImageKey(loader, config, spec, keyClass);
      return key == null ? null : new Lookup(repository, key, config);
    } catch (Throwable t) {
      Knot.log("Knot: sticon lookup failed: " + t.getClass().getSimpleName());
      return null;
    }
  }

  private static Drawable cachedDrawable(Lookup lookup) {
    try {
      return asDrawable(
          Reflect.callMethod(
              lookup.repository, lookup.config.sticonImageRepositoryCacheMethod, lookup.key));
    } catch (Throwable t) {
      Knot.log(
          "Knot: sticon cache lookup failed key="
              + String.valueOf(lookup.key)
              + " error="
              + t.getClass().getSimpleName());
      return null;
    }
  }

  private static Object createImageKey(
      ClassLoader loader,
      LineVersion.Config.Notification config,
      SticonSpec spec,
      Class<?> keyClass)
      throws Exception {
    Class<?> paidProductClass = Reflect.findClass(config.sticonPaidProductClass, loader);
    Object paidProduct = Reflect.newInstance(paidProductClass, spec.productId);

    Class<?> paidClass = Reflect.findClass(config.sticonPaidClass, loader);
    Object paid = Reflect.newInstance(paidClass, paidProduct, spec.sticonId);

    Class<?> optionTypeClass = Reflect.findClass(config.sticonOptionTypeClass, loader);
    String optionName =
        spec.resourceType.toUpperCase(Locale.ROOT).contains("ANIMATION") ? "ANIMATION" : "STATIC";
    Object optionType = Reflect.getStaticObjectField(optionTypeClass, optionName);
    if (optionType == null) return null;

    Class<?> sticonBaseClass = paidClass.getSuperclass();
    if (sticonBaseClass == null) return null;
    return Reflect.findConstructorExact(
            keyClass, sticonBaseClass, int.class, optionTypeClass, boolean.class)
        .newInstance(paid, spec.version, optionType, false);
  }

  private static Drawable asDrawable(Object value) {
    return value instanceof Drawable ? (Drawable) value : null;
  }

  private static Bitmap renderDrawable(Drawable source) {
    Bitmap bitmap = null;
    try {
      Drawable.ConstantState state = source.getConstantState();
      if (state == null) return null;
      Drawable drawable = state.newDrawable().mutate();
      if (drawable == source) return null;
      bitmap = Bitmap.createBitmap(EMOJI_CANVAS_SIZE, EMOJI_CANVAS_SIZE, Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(bitmap);

      int intrinsicWidth = drawable.getIntrinsicWidth();
      int intrinsicHeight = drawable.getIntrinsicHeight();
      int width = EMOJI_CANVAS_SIZE;
      int height = EMOJI_CANVAS_SIZE;
      if (intrinsicWidth > 0 && intrinsicHeight > 0) {
        float scale =
            Math.min(
                (float) EMOJI_CANVAS_SIZE / intrinsicWidth,
                (float) EMOJI_CANVAS_SIZE / intrinsicHeight);
        width = Math.max(1, Math.round(intrinsicWidth * scale));
        height = Math.max(1, Math.round(intrinsicHeight * scale));
      }
      int left = (EMOJI_CANVAS_SIZE - width) / 2;
      int top = (EMOJI_CANVAS_SIZE - height) / 2;

      drawable.setBounds(left, top, left + width, top + height);
      drawable.draw(canvas);
      return bitmap;
    } catch (Throwable ignored) {
      if (bitmap != null) bitmap.recycle();
      return null;
    }
  }

  private static SticonSpec parseSingleSticon(String text, String parameter) {
    if (!hasText(text) || !hasText(parameter)) return null;
    try {
      Object replaceValue = new JSONObject(parameter).opt("REPLACE");
      if (replaceValue == null || replaceValue == JSONObject.NULL) return null;
      String replaceJson = String.valueOf(replaceValue);
      if (!hasText(replaceJson)) return null;

      JSONObject sticon = new JSONObject(replaceJson).optJSONObject("sticon");
      if (sticon == null) return null;
      JSONArray resources = sticon.optJSONArray("resources");
      if (resources == null || resources.length() != 1) return null;
      JSONObject resource = resources.optJSONObject(0);
      if (resource == null
          || resource.optInt("S", -1) != 0
          || resource.optInt("E", -1) != text.length()) return null;

      String productId = resource.optString("productId", null);
      String sticonId = resource.optString("sticonId", null);
      if (!hasText(productId) || !hasText(sticonId)) return null;
      return new SticonSpec(
          productId,
          sticonId,
          resource.optInt("version", 0),
          resource.optString("resourceType", "STATIC"));
    } catch (Throwable ignored) {
      return null;
    }
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

  private static String persistentCacheId(String cacheKey) {
    return "sticon-line-key:" + cacheKey;
  }

  private static boolean hasText(String value) {
    return value != null && !value.isEmpty();
  }

  private static final class Lookup {
    final Object repository;
    final Object key;
    final LineVersion.Config.Notification config;

    Lookup(Object repository, Object key, LineVersion.Config.Notification config) {
      this.repository = repository;
      this.key = key;
      this.config = config;
    }
  }

  private static boolean hasCurrentRequester(PendingCache item) {
    List<CacheCallback> callbacks;
    synchronized (pendingLock) {
      callbacks = new ArrayList<>(item.callbacks);
    }
    // Notification validation takes the post lock; never hold pendingLock here.
    for (CacheCallback callback : callbacks) {
      if (callback.isNeeded.getAsBoolean()) return true;
    }
    return false;
  }

  private static final class CacheCallback {
    final BooleanSupplier isNeeded;
    final Runnable onReady;

    CacheCallback(BooleanSupplier isNeeded, Runnable onReady) {
      this.isNeeded = isNeeded;
      this.onReady = onReady;
    }
  }

  private static final class PendingCache {
    final List<CacheCallback> callbacks = new ArrayList<>();
    long updatedAtMs;
    Object activeAttempt;
    boolean cacheWriteScheduled;

    PendingCache(long updatedAtMs) {
      this.updatedAtMs = updatedAtMs;
    }
  }

  private static final class SticonSpec {
    final String productId;
    final String sticonId;
    final int version;
    final String resourceType;

    SticonSpec(String productId, String sticonId, int version, String resourceType) {
      this.productId = productId;
      this.sticonId = sticonId;
      this.version = version;
      this.resourceType = resourceType;
    }
  }
}
