package app.zipper.knot.hooks;

import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.Reflect;

public class DisableCustomRingbackHook implements BaseHook {

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    if (!config.disableCustomRingback.enabled) return;
    LineVersion.Config version = LineVersion.get();
    if (version == null || version.callTone.remoteRingbackClass.isEmpty()) return;
    LineVersion.Config.CallTone tone = version.callTone;

    // LINE plays this fallback when the callee's ringback tone fails to download.
    Knot.module
        .hook(
            Reflect.findMethodExact(
                tone.remoteRingbackClass, lpparam.classLoader, tone.methodToneUri))
        .intercept(
            chain -> {
              if (!Main.options.disableCustomRingback.enabled) return chain.proceed();
              Object ringback = chain.getThisObject();
              return Reflect.callMethod(
                  Reflect.getObjectField(ringback, tone.remoteRingbackFallbackField),
                  "invoke",
                  Reflect.getObjectField(ringback, tone.remoteRingbackContextField));
            });
  }
}
