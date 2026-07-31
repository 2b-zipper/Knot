package app.zipper.knot.hooks;

import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.Reflect;
import app.zipper.knot.SettingsStore;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class RemoveTalkRoomAgentIToggle implements BaseHook {

  private static final String LEGACY_KEY = "remove_talkroom_agent_i_toggle";

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    if (!isEnabled(config)) return;

    LineVersion.Config cfg = LineVersion.get();
    if (cfg == null || cfg.agentIInChat.toggleComposableClass.isEmpty()) return;

    final Class<?> cls;
    try {
      cls = Reflect.findClass(cfg.agentIInChat.toggleComposableClass, lpparam.classLoader);
    } catch (Throwable t) {
      return;
    }

    XposedInterface.Hooker noOp =
        chain -> {
          if (isEnabled(Main.options)) return null;
          return chain.proceed();
        };

    int hookCount = 0;
    for (Method method : cls.getDeclaredMethods()) {
      if (!isComposeRenderMethod(cfg, method)) continue;
      Knot.module.hook(method).intercept(noOp);
      hookCount++;
    }

    if (hookCount > 0) {
      Knot.log("Knot: RemoveTalkRoomAgentIToggle hooked " + hookCount + " compose methods.");
    }
  }

  private static boolean isEnabled(KnotConfig config) {
    return config.removeSearchBarAgentIButton.enabled || SettingsStore.get(LEGACY_KEY, false);
  }

  private static boolean isComposeRenderMethod(LineVersion.Config cfg, Method method) {
    if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != Void.TYPE) {
      return false;
    }

    boolean hasComposer = false;
    boolean hasFlags = false;
    for (Class<?> type : method.getParameterTypes()) {
      if (cfg.compose.composerClass.equals(type.getName())) {
        hasComposer = true;
      } else if (type == Integer.TYPE) {
        hasFlags = true;
      }
    }
    return hasComposer && hasFlags;
  }
}
