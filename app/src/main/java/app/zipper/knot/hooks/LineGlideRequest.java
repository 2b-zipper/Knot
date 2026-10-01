package app.zipper.knot.hooks;

import android.content.Context;
import app.zipper.knot.Knot;
import app.zipper.knot.LineVersion;
import app.zipper.knot.Reflect;
import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

final class LineGlideRequest {
  private static final long REQUEST_TIMEOUT_MS = 3500L;

  private LineGlideRequest() {}

  static File requestFile(Context context, Object model, BooleanSupplier isNeeded) {
    if (context == null || model == null || !isNeeded.getAsBoolean()) return null;
    LineVersion.Config version = LineVersion.get();
    if (version == null) return null;
    LineVersion.Config.Notification config = version.notification;
    if (!hasText(config.glideClass)) return null;

    Object requestManager = null;
    Object target = null;
    try {
      Class<?> glide = Reflect.findClass(config.glideClass, context.getClassLoader());
      requestManager = Reflect.callStaticMethod(glide, config.glideWithContextMethod, context);
      if (requestManager == null) {
        Object retriever = Reflect.callStaticMethod(glide, config.glideRetrieverMethod, context);
        requestManager = Reflect.callMethod(retriever, config.glideRetrieverGetMethod, context);
      }
      if (requestManager == null) return null;
      Object builder = Reflect.callMethod(requestManager, config.glideAsFileMethod);
      builder = Reflect.callMethod(builder, config.glideLoadMethod, model);
      if (!isNeeded.getAsBoolean()) return null;
      target = Reflect.callMethod(builder, config.glideSubmitMethod);
      Object result = Reflect.callMethod(target, "get", REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
      return result instanceof File ? (File) result : null;
    } catch (Throwable t) {
      Knot.log(
          "Knot: notification media Glide request failed"
              + " (model="
              + model.getClass().getName()
              + "): "
              + t.getClass().getSimpleName()
              + (t.getMessage() == null ? "" : ": " + t.getMessage()),
          t);
      return null;
    } finally {
      if (requestManager != null && target != null) {
        try {
          Reflect.callMethod(requestManager, config.glideClearMethod, target);
        } catch (Throwable ignored) {
        }
      }
    }
  }

  private static boolean hasText(String value) {
    return value != null && !value.isEmpty();
  }
}
