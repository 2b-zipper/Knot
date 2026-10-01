package app.zipper.knot.hooks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import app.zipper.knot.Knot;
import app.zipper.knot.LineVersion;
import app.zipper.knot.Reflect;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import org.json.JSONArray;
import org.json.JSONObject;

final class LineCombinationStickerMediaResolver {
  private static final long MAX_METADATA_BYTES = 1024 * 1024;
  private static final int MAX_LAYOUTS = 16;
  private static final int STICKER_CANVAS_WIDTH = 370;
  private static final int STICKER_CANVAS_HEIGHT = 320;
  private static final int MAX_PENDING_METADATA = 64;
  private static final long PENDING_TTL_MS = TimeUnit.SECONDS.toMillis(30);

  private static final Object metadataLock = new Object();
  private static final LinkedHashMap<String, PendingMetadata> pendingMetadata =
      new LinkedHashMap<>();

  private LineCombinationStickerMediaResolver() {}

  interface ResultCallback {
    void onResult(NotificationMediaFileStore.Attachment attachment);
  }

  static void acquireAsync(
      Context context,
      String combinationStickerId,
      Executor executor,
      BooleanSupplier isNeeded,
      ResultCallback callback) {
    if (context == null || !hasText(combinationStickerId) || executor == null || callback == null) {
      if (callback != null) callback.onResult(null);
      return;
    }
    new Request(context, combinationStickerId, executor, isNeeded, callback).schedule();
  }

  static void onLineMetadataCached(File file) {
    boolean usable = isUsableMetadata(file);

    if (!usable) return;
    List<Runnable> callbacks = null;
    synchronized (metadataLock) {
      pruneMetadataLocked();
      PendingMetadata pending = pendingMetadata.remove(file.getAbsolutePath());
      if (pending != null && !pending.callbacks.isEmpty()) {
        callbacks = new ArrayList<>(pending.callbacks);
      }
    }

    if (callbacks != null) {
      for (Runnable callback : callbacks) callback.run();
    }
  }

  private static void attempt(Request request) {
    if (request.completed.get()) return;
    if (!request.isNeeded.getAsBoolean()) {
      request.completed.set(true);
      return;
    }

    CombinationSpec spec = loadCachedSpec(request.context, request.combinationId);
    if (spec == null) {
      if (!request.metadataRegistered) {
        request.metadataRegistered = true;
        registerMetadataReady(request.context, request.combinationId, request::schedule);
      }
      return;
    }

    for (PartSpec part : spec.parts) {
      if (!request.isNeeded.getAsBoolean()) {
        request.completed.set(true);
        return;
      }
      LineStickerMediaResolver.StickerPart sticker =
          new LineStickerMediaResolver.StickerPart(
              part.stickerId, part.productId, part.version, part.hash);
      if (!LineStickerMediaResolver.requestMissingPart(
          request.context, sticker, request.isNeeded)) {
        if (!request.isNeeded.getAsBoolean()) {
          request.completed.set(true);
          return;
        }
        Knot.log(
            "Knot: combination part acquisition failed id="
                + request.combinationId
                + " part="
                + part.productId
                + ":"
                + part.stickerId);
        request.finish(null);
        return;
      }
    }

    if (!request.isNeeded.getAsBoolean()) {
      request.completed.set(true);
      return;
    }
    request.finish(compose(request.context, request.combinationId, spec));
  }

  private static void registerMetadataReady(
      Context context, String combinationId, Runnable onReady) {
    File file = metadataFile(context, combinationId);
    if (file == null) {
      return;
    }
    if (isUsableMetadata(file)) {
      onReady.run();
      return;
    }

    List<Runnable> readyCallbacks = null;
    synchronized (metadataLock) {
      pruneMetadataLocked();
      String path = file.getAbsolutePath();
      PendingMetadata pending = pendingMetadata.get(path);
      if (pending == null) {
        pending = new PendingMetadata(SystemClock.elapsedRealtime());
        pendingMetadata.put(path, pending);
      }
      pending.callbacks.add(onReady);
      pending.updatedAtMs = SystemClock.elapsedRealtime();
      trimMetadataLocked();

      if (isUsableMetadata(file)) {
        pendingMetadata.remove(path);
        readyCallbacks = new ArrayList<>(pending.callbacks);
      }
    }

    if (readyCallbacks != null) {
      for (Runnable callback : readyCallbacks) callback.run();
    }
  }

  private static CombinationSpec loadCachedSpec(Context context, String combinationId) {
    File file = metadataFile(context, combinationId);
    if (!isUsableMetadata(file)) return null;

    try {
      return parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
    } catch (Throwable t) {
      Knot.log(
          "Knot: combination metadata READ failed id="
              + combinationId
              + " error="
              + t.getClass().getSimpleName());
      return null;
    }
  }

  private static File metadataFile(Context context, String combinationId) {
    if (context == null || !hasText(combinationId)) return null;
    try {
      LineVersion.Config version = LineVersion.get();
      if (version == null) return null;
      LineVersion.Config.Notification config = version.notification;
      if (!hasText(config.combinationStickerMetadataFileManagerClass)
          || !hasText(config.combinationStickerMetadataFileMethod)) {
        return null;
      }

      Class<?> managerClass =
          Reflect.findClass(
              config.combinationStickerMetadataFileManagerClass, context.getClassLoader());
      Object manager =
          Reflect.findConstructorExact(managerClass, Context.class).newInstance(context);
      Object result =
          Reflect.findMethodExact(
                  managerClass, config.combinationStickerMetadataFileMethod, String.class)
              .invoke(manager, combinationId);
      return result instanceof File ? (File) result : null;
    } catch (Throwable t) {
      Knot.log(
          "Knot: combination metadata FILE lookup failed id="
              + combinationId
              + " error="
              + t.getClass().getSimpleName());
      return null;
    }
  }

  private static boolean isUsableMetadata(File file) {
    return file != null
        && file.isFile()
        && file.length() > 0L
        && file.length() <= MAX_METADATA_BYTES;
  }

  private static void pruneMetadataLocked() {
    long now = SystemClock.elapsedRealtime();
    Iterator<Map.Entry<String, PendingMetadata>> iterator = pendingMetadata.entrySet().iterator();
    while (iterator.hasNext()) {
      if (now - iterator.next().getValue().updatedAtMs > PENDING_TTL_MS) {
        iterator.remove();
      }
    }
  }

  private static void trimMetadataLocked() {
    while (pendingMetadata.size() > MAX_PENDING_METADATA) {
      Iterator<Map.Entry<String, PendingMetadata>> iterator = pendingMetadata.entrySet().iterator();
      if (!iterator.hasNext()) break;
      iterator.next();
      iterator.remove();
    }
  }

  private static CombinationSpec parse(String json) {
    if (!hasText(json)) return null;
    try {
      JSONObject root = new JSONObject(json);
      float canvasWidth = (float) root.optDouble("canvasWidth", 0.0d);
      float canvasHeight = (float) root.optDouble("canvasHeight", 0.0d);
      JSONArray layouts = root.optJSONArray("stickerLayouts");
      if (canvasWidth <= 0.0f
          || canvasHeight <= 0.0f
          || layouts == null
          || layouts.length() == 0
          || layouts.length() > MAX_LAYOUTS) {
        return null;
      }

      List<PartSpec> parts = new ArrayList<>(layouts.length());
      for (int i = 0; i < layouts.length(); i++) {
        JSONObject layout = layouts.optJSONObject(i);
        if (layout == null) return null;
        JSONObject sticker = layout.optJSONObject("stickerInfo");
        JSONObject position = layout.optJSONObject("layoutInfo");
        if (sticker == null || position == null) return null;

        long productId = longValue(sticker.opt("productId"));
        long stickerId = longValue(sticker.opt("stickerId"));
        float x = (float) position.optDouble("x", Float.NaN);
        float y = (float) position.optDouble("y", Float.NaN);
        float width = (float) position.optDouble("width", Float.NaN);
        float height = (float) position.optDouble("height", Float.NaN);
        float rotation = (float) position.optDouble("rotation", 0.0d);
        if (productId <= 0L
            || stickerId <= 0L
            || !Float.isFinite(x)
            || !Float.isFinite(y)
            || !Float.isFinite(width)
            || !Float.isFinite(height)
            || !Float.isFinite(rotation)
            || width <= 0.0f
            || height <= 0.0f) {
          return null;
        }

        parts.add(
            new PartSpec(
                productId,
                stickerId,
                longValue(sticker.opt("stickerVersion")),
                sticker.isNull("stickerHash") ? null : sticker.optString("stickerHash", null),
                x,
                y,
                width,
                height,
                rotation));
      }
      return new CombinationSpec(canvasWidth, canvasHeight, parts);
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static NotificationMediaFileStore.Attachment compose(
      Context context, String combinationId, CombinationSpec spec) {
    float scale =
        Math.min(
            (float) STICKER_CANVAS_WIDTH / spec.canvasWidth,
            (float) STICKER_CANVAS_HEIGHT / spec.canvasHeight);
    float offsetX = (STICKER_CANVAS_WIDTH - spec.canvasWidth * scale) / 2.0f;
    float offsetY = (STICKER_CANVAS_HEIGHT - spec.canvasHeight * scale) / 2.0f;
    Bitmap output = null;

    try {
      output =
          Bitmap.createBitmap(STICKER_CANVAS_WIDTH, STICKER_CANVAS_HEIGHT, Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(output);
      Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

      for (PartSpec part : spec.parts) {
        File file =
            LineStickerMediaResolver.cachedStickerFile(
                context, new LineStickerMediaResolver.StickerPart(part.stickerId, part.productId));
        if (file == null) return null;

        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (bitmap == null) return null;
        try {
          RectF target =
              new RectF(
                  offsetX + part.x * scale,
                  offsetY + part.y * scale,
                  offsetX + (part.x + part.width) * scale,
                  offsetY + (part.y + part.height) * scale);
          Matrix matrix = new Matrix();
          matrix.setRectToRect(
              new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()),
              target,
              Matrix.ScaleToFit.FILL);
          if (part.rotation != 0.0f) {
            matrix.postRotate(part.rotation, target.centerX(), target.centerY());
          }
          canvas.drawBitmap(bitmap, matrix, paint);
        } finally {
          bitmap.recycle();
        }
      }

      return NotificationMediaFileStore.put(
          context, "combination-sticker:" + combinationId, output, "image/png");
    } catch (Throwable t) {
      Knot.log(
          "Knot: notification media preview: combination sticker composition failed (id="
              + combinationId
              + ")",
          t);
      return null;
    } finally {
      if (output != null) output.recycle();
    }
  }

  private static long longValue(Object value) {
    if (value instanceof Number) return ((Number) value).longValue();
    try {
      return value == null ? -1L : Long.parseLong(String.valueOf(value));
    } catch (NumberFormatException ignored) {
      return -1L;
    }
  }

  private static boolean hasText(String value) {
    return value != null && !value.isEmpty();
  }

  private static final class Request {
    final Context context;
    final String combinationId;
    final Executor executor;
    final ResultCallback callback;
    final BooleanSupplier isNeeded;
    final AtomicBoolean scheduled = new AtomicBoolean();
    final AtomicBoolean completed = new AtomicBoolean();
    volatile boolean metadataRegistered;

    Request(
        Context context,
        String combinationId,
        Executor executor,
        BooleanSupplier isNeeded,
        ResultCallback callback) {
      this.context = context;
      this.combinationId = combinationId;
      this.executor = executor;
      this.callback = callback;
      this.isNeeded = isNeeded;
    }

    void schedule() {
      if (completed.get() || !scheduled.compareAndSet(false, true)) return;
      executor.execute(
          () -> {
            synchronized (this) {
              scheduled.set(false);
              attempt(this);
            }
          });
    }

    void finish(NotificationMediaFileStore.Attachment attachment) {
      if (completed.compareAndSet(false, true)) {
        callback.onResult(attachment);
      }
    }
  }

  private static final class PendingMetadata {
    final List<Runnable> callbacks = new ArrayList<>();
    long updatedAtMs;

    PendingMetadata(long updatedAtMs) {
      this.updatedAtMs = updatedAtMs;
    }
  }

  private static final class CombinationSpec {
    final float canvasWidth;
    final float canvasHeight;
    final List<PartSpec> parts;

    CombinationSpec(float canvasWidth, float canvasHeight, List<PartSpec> parts) {
      this.canvasWidth = canvasWidth;
      this.canvasHeight = canvasHeight;
      this.parts = parts;
    }
  }

  private static final class PartSpec {
    final long productId;
    final long stickerId;
    final long version;
    final String hash;
    final float x;
    final float y;
    final float width;
    final float height;
    final float rotation;

    PartSpec(
        long productId,
        long stickerId,
        long version,
        String hash,
        float x,
        float y,
        float width,
        float height,
        float rotation) {
      this.productId = productId;
      this.stickerId = stickerId;
      this.version = version;
      this.hash = hash;
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      this.rotation = rotation;
    }
  }
}
