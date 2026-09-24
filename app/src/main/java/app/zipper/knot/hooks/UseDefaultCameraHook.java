package app.zipper.knot.hooks;

import android.app.Activity;
import android.net.Uri;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.Reflect;

public class UseDefaultCameraHook implements BaseHook {

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    if (!config.useDefaultCamera.enabled) return;
    LineVersion.Config version = LineVersion.get();
    if (version == null || version.camera.cameraModuleClass.isEmpty()) return;
    LineVersion.Config.Camera camera = version.camera;
    ClassLoader cl = lpparam.classLoader;

    Knot.module
        .hook(Reflect.findMethodExact(camera.cameraModuleClass, cl, camera.methodUseExternalCamera))
        .intercept(chain -> Main.options.useDefaultCamera.enabled ? Boolean.TRUE : chain.proceed());

    // LINE refuses the external camera for line:// link launches; open its usual chooser instead.
    // Skipped for the scheme service activity, which finishes on return and would dismiss it.
    Class<?> chooserClass = Reflect.findClass(camera.captureChooserClass, cl);
    Class<?> choiceClass = Reflect.findClass(camera.captureChoiceClass, cl);
    Knot.module
        .hook(
            Reflect.findMethodExact(
                camera.cameraLauncherClass,
                cl,
                camera.methodLaunchCamera,
                Activity.class,
                camera.launchModeClass,
                camera.launchSourceClass,
                camera.launchCallbackClass,
                Uri.class))
        .intercept(
            chain -> {
              Activity activity = (Activity) chain.getArg(0);
              Enum<?> source = (Enum<?>) chain.getArg(2);
              if (!Main.options.useDefaultCamera.enabled
                  || source == null
                  || !source.name().equals("URL_SCHEME")
                  || activity.getClass().getName().equals(camera.schemeServiceActivity)) {
                return chain.proceed();
              }
              Reflect.callStaticMethod(
                  chooserClass,
                  camera.methodShowCaptureChooser,
                  activity,
                  Reflect.newInstance(choiceClass, activity, chain.getArg(3), source));
              return Boolean.FALSE;
            });
  }
}
