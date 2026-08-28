package app.zipper.knot.hooks;

import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.Reflect;
import java.lang.reflect.Method;

public class MuteMessageHook implements BaseHook {

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    if (!config.enableMuteMessage.enabled) return;
    LineVersion.Config version = LineVersion.get();
    if (version == null || version.muteMessage.labFeatureClass.isEmpty()) return;

    LineVersion.Config.MuteMessage cfg = version.muteMessage;
    Class<?> silentMessage = Reflect.findClass(cfg.silentMessageFeatureClass, lpparam.classLoader);
    Method gate =
        findGate(
            Reflect.findClass(cfg.labFeatureClass, lpparam.classLoader),
            cfg.methodIsFeatureEnabled);
    if (gate == null) {
      Knot.log("Knot: mute message gate not found on " + cfg.labFeatureClass);
      return;
    }

    Knot.module
        .hook(gate)
        .intercept(
            chain ->
                Main.options.enableMuteMessage.enabled
                        && silentMessage.isInstance(chain.getThisObject())
                    ? Boolean.TRUE
                    : chain.proceed());
  }

  // 26.10.x declares the gate without arguments, 26.11.0 and later take a Context.
  private static Method findGate(Class<?> owner, String name) {
    for (Method method : owner.getDeclaredMethods()) {
      if (method.getName().equals(name) && method.getReturnType() == boolean.class) {
        method.setAccessible(true);
        return method;
      }
    }
    return null;
  }
}
