package app.zipper.knot.hooks;

import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.Reflect;
import java.lang.reflect.Method;

public class SilentCallRecordingHook implements BaseHook {

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    LineVersion.Config version = LineVersion.get();
    if (version == null) return;
    LineVersion.Config.CallRecording rec = version.callRecording;

    // The shared "record" state is what makes the other side show the indicator and play its guide
    if (!rec.methodSetState.isEmpty()) {
      for (Method m :
          Reflect.findMethodsByName(rec.stateShareClass, lpparam.classLoader, rec.methodSetState)) {
        if (m.getParameterCount() != 1) continue;
        Knot.module
            .hook(m)
            .intercept(
                chain ->
                    Main.options.silentCallRecording.enabled && isRecordState(chain.getArg(0))
                        ? null
                        : chain.proceed());
      }
    }

    if (rec.voiceGuideClass.isEmpty()) return;
    Class<?> guide = Reflect.findClass(rec.voiceGuideClass, lpparam.classLoader);
    for (String method : new String[] {rec.methodGuideA, rec.methodGuideB}) {
      if (method.isEmpty()) continue;
      Knot.module
          .hook(Reflect.findMethodExact(guide, method))
          .intercept(chain -> Main.options.silentCallRecording.enabled ? null : chain.proceed());
    }
  }

  private static boolean isRecordState(Object state) {
    if (state == null) return false;
    try {
      return "record".equals(Reflect.callMethod(state, "getName"));
    } catch (Throwable t) {
      Knot.log("Knot: failed to read the shared state name: " + t);
      return false;
    }
  }
}
