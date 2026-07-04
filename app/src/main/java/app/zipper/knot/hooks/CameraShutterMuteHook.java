package app.zipper.knot.hooks;

import android.content.Context;
import android.hardware.Camera;
import android.media.SoundPool;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Reflect;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CameraShutterMuteHook implements BaseHook {

  private static final Set<Integer> mutedSoundIds = ConcurrentHashMap.newKeySet();
  private static final Set<Integer> targetResIds = ConcurrentHashMap.newKeySet();
  private static volatile boolean isResIdsLoaded = false;

  private static final String[] TARGET_SOUND_NAMES = {
    "camera_shutter_0613",
    "video_start_0613",
    "video_end_0613",
    "voom_countdown",
    "voom_countdown_1sec",
    "timer_shutter_jp",
    "timer_shutter_en"
  };

  @Override
  public void hook(KnotConfig options, LoadParam lpparam) throws Throwable {
    if (!options.muteCameraShutter.enabled || options.useDefaultCamera.enabled) return;

    try {
      Knot.module
          .hook(
              Reflect.findMethodExact(SoundPool.class, "load", Context.class, int.class, int.class))
          .intercept(
              chain -> {
                if (!isResIdsLoaded && chain.getArg(0) != null) {
                  Context ctx = (Context) chain.getArg(0);
                  for (String name : TARGET_SOUND_NAMES) {
                    int id = ctx.getResources().getIdentifier(name, "raw", "jp.naver.line.android");
                    if (id != 0) targetResIds.add(id);
                  }
                  isResIdsLoaded = true;
                }

                Object result = chain.proceed();
                if (result instanceof Integer && targetResIds.contains((int) chain.getArg(1))) {
                  mutedSoundIds.add((Integer) result);
                }
                return result;
              });

      Knot.module
          .hook(
              Reflect.findMethodExact(
                  SoundPool.class,
                  "play",
                  int.class,
                  float.class,
                  float.class,
                  int.class,
                  int.class,
                  float.class))
          .intercept(chain -> mutedSoundIds.contains((int) chain.getArg(0)) ? 0 : chain.proceed());

      Knot.module
          .hook(
              Reflect.findMethodExact(
                  Camera.class,
                  "takePicture",
                  Camera.ShutterCallback.class,
                  Camera.PictureCallback.class,
                  Camera.PictureCallback.class,
                  Camera.PictureCallback.class))
          .intercept(
              chain -> {
                Object[] args = chain.getArgs().toArray();
                args[0] = null;
                return chain.proceed(args);
              });

      Knot.log("Knot: Camera Shutter Mute hooked");
    } catch (Throwable t) {
      Knot.log("Knot: Failed to hook Camera Shutter Mute: " + t.getMessage());
    }
  }
}
