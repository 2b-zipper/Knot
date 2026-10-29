package app.zipper.knot.hooks;

import android.net.Uri;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Reflect;
import io.github.libxposed.api.XposedInterface;
import java.util.Map;

public class LongVideoHook implements BaseHook {

  @Override
  public void hook(KnotConfig options, LoadParam lpparam) throws Throwable {
    if (!options.longVideo.enabled) return;

    LineVersion.Config v = LineVersion.get();
    if (v == null) return;

    hookDurationCheck(lpparam.classLoader, v);
    hookMediaPickerParams(lpparam.classLoader, v);
    hookGalleryController(lpparam.classLoader, v);
    hookProfileTrimmer(lpparam.classLoader, v);
    hookSmartDurationBypass(lpparam.classLoader, v);
    hookDroppedMediaPreprocessor(lpparam.classLoader, v);
    hookPickerSelectionValidator(lpparam.classLoader, v);
  }

  private void hookDurationCheck(ClassLoader cl, LineVersion.Config v) {
    if (v.media.videoDurationCheckClass.isEmpty()) return;
    try {
      Class<?> checkClass = Reflect.findClass(v.media.videoDurationCheckClass, cl);
      Class<?> resultClass = Reflect.findClass(v.media.videoDurationSuccessClass, cl);
      final Object success =
          Reflect.getStaticObjectField(resultClass, v.media.fieldVideoDurationSuccess);

      XposedInterface.Hooker bypassHook = chain -> success;

      Knot.module
          .hook(
              Reflect.findMethodExact(
                  checkClass,
                  v.media.videoDurationCheckMethod,
                  Uri.class,
                  Map.class,
                  boolean.class))
          .intercept(bypassHook);
      Knot.module
          .hook(
              Reflect.findMethodExact(
                  checkClass, v.media.videoDurationCheckWithSizeMethod, Uri.class))
          .intercept(bypassHook);
      Knot.module
          .hook(
              Reflect.findMethodExact(
                  checkClass, v.media.videoDurationCheckWithoutSizeMethod, Uri.class))
          .intercept(bypassHook);
    } catch (Throwable ignored) {
    }
  }

  private void hookMediaPickerParams(ClassLoader cl, LineVersion.Config v) {
    if (v.media.mediaPickerParamsClass.isEmpty()) return;
    try {
      Knot.hookAllCtors(
          Reflect.findClass(v.media.mediaPickerParamsClass, cl),
          chain -> {
            Object result = chain.proceed();
            Reflect.setLongField(
                chain.getThisObject(),
                v.media.fieldMediaPickerMaxVideoDuration,
                (long) Integer.MAX_VALUE);
            return result;
          });
    } catch (Throwable ignored) {
    }
  }

  private void hookGalleryController(ClassLoader cl, LineVersion.Config v) {
    if (v.media.galleryViewClass.isEmpty()) return;
    try {
      Class<?> galleryViewClass = Reflect.findClass(v.media.galleryViewClass, cl);
      // Field.set widens the Integer for the long field before 26.17.0
      Reflect.setStaticObjectField(
          galleryViewClass, v.media.fieldGalleryDurationLimit, Integer.MAX_VALUE);
    } catch (Throwable ignored) {
    }
  }

  private void hookSmartDurationBypass(ClassLoader cl, LineVersion.Config v) {
    try {
      Knot.module
          .hook(Reflect.findMethodExact(android.media.MediaPlayer.class, "getDuration"))
          .intercept(
              chain -> {
                Object result = chain.proceed();
                return applySpoof(result, v);
              });

      Knot.module
          .hook(
              Reflect.findMethodExact(
                  android.media.MediaMetadataRetriever.class, "extractMetadata", int.class))
          .intercept(
              chain -> {
                Object result = chain.proceed();
                if ((int) chain.getArg(0) == 9) {
                  return applySpoof(result, v);
                }
                return result;
              });
    } catch (Throwable ignored) {
    }
  }

  private Object applySpoof(Object result, LineVersion.Config v) {
    long duration;
    if (result instanceof Integer) {
      duration = (int) result;
    } else if (result instanceof String) {
      try {
        duration = Long.parseLong((String) result);
      } catch (Exception e) {
        return result;
      }
    } else {
      return result;
    }

    if (duration > 300000 && shouldSpoof(v)) {
      if (result instanceof Integer) {
        return 1000;
      } else {
        return "1000";
      }
    }
    return result;
  }

  private boolean shouldSpoof(LineVersion.Config v) {
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    for (StackTraceElement element : stackTrace) {
      String className = element.getClassName();

      if (className.startsWith(v.media.videoDurationCheckClass)
          || className.startsWith(v.media.droppedMediaPreprocessorClass)
          || className.contains("VideoValidator")
          || className.contains("MediaLimitCheck")) {
        return true;
      }

      if (className.startsWith("kc1.")
          || className.startsWith("lc1.")
          || className.startsWith("com.linecorp.line.media.picker")
          || className.contains("ViewHolder")
          || className.contains("ViewData")
          || className.contains("Adapter")
          || className.contains("Formatter")
          || className.contains("Thumbnail")) {
        return false;
      }
    }
    for (StackTraceElement element : stackTrace) {
      if (element.getClassName().startsWith("jp.naver.line.android")
          || element.getClassName().startsWith("com.linecorp.line")) {
        return true;
      }
    }
    return false;
  }

  private void hookDroppedMediaPreprocessor(ClassLoader cl, LineVersion.Config v) {
    if (v.media.droppedMediaPreprocessorClass.isEmpty()) return;
    try {
      Knot.module
          .hook(
              Reflect.findMethodExact(
                  v.media.droppedMediaPreprocessorClass, cl, "invokeSuspend", Object.class))
          .intercept(
              chain -> {
                Object result = chain.proceed();
                if (result != null && result.toString().equals("EXCEEDS_LIMIT")) {
                  return null;
                }
                return result;
              });
    } catch (Throwable ignored) {
    }
  }

  private void hookPickerSelectionValidator(ClassLoader cl, LineVersion.Config v) {
    if (v.media.selectionValidatorClass.isEmpty()) return;
    try {
      Knot.module
          .hook(
              Reflect.findMethodExact(
                  v.media.selectionValidatorClass,
                  cl,
                  v.media.selectionValidatorMethod,
                  v.media.selectionValidatorParamClass))
          .intercept(chain -> false);
    } catch (Throwable ignored) {
    }
  }

  private void hookProfileTrimmer(ClassLoader cl, LineVersion.Config v) {
    if (v.media.videoProfileTrimmerActivityClass.isEmpty()) return;
    try {
      Knot.module
          .hook(
              Reflect.findMethodExact(
                  v.media.videoProfileTrimmerActivityClass,
                  cl,
                  "onCreate",
                  android.os.Bundle.class))
          .intercept(
              chain -> {
                Object result = chain.proceed();
                Reflect.setIntField(
                    chain.getThisObject(),
                    v.media.fieldVideoProfileTrimmerLimit,
                    Integer.MAX_VALUE);
                return result;
              });
    } catch (Throwable ignored) {
    }
  }
}
