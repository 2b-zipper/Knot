package app.zipper.knot.hooks;

import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.Reflect;
import java.lang.reflect.Method;

public class ScrollPreviewHook implements BaseHook {

  private static final String LABS_PLAN = "LABS";

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    if (!config.enableScrollPreview.enabled) return;
    LineVersion.Config version = LineVersion.get();
    if (version == null || version.scrollPreview.availabilityClass.isEmpty()) return;

    ClassLoader cl = lpparam.classLoader;
    hookAvailability(cl, version.scrollPreview);
    hookPlan(cl, version.scrollPreview);
  }

  private static void hookAvailability(ClassLoader cl, LineVersion.Config.ScrollPreview cfg) {
    try {
      Knot.module
          .hook(Reflect.findMethodExact(cfg.availabilityClass, cl, cfg.methodIsAvailable))
          .intercept(chain -> enabled() ? Boolean.TRUE : chain.proceed());
      Knot.log("Knot: scroll preview availability hooked on " + cfg.availabilityClass);
    } catch (Throwable t) {
      Knot.log("Knot: scroll preview availability hook failed: " + t);
    }
  }

  // Non-member plans can't scroll; resolvePlan suspends, so LABS is returned without proceeding
  private static void hookPlan(ClassLoader cl, LineVersion.Config.ScrollPreview cfg) {
    try {
      Object labsPlan = enumConstant(Reflect.findClass(cfg.planEnumClass, cl), LABS_PLAN);
      if (labsPlan == null) {
        Knot.log("Knot: scroll preview LABS plan not found on " + cfg.planEnumClass);
        return;
      }

      for (Method m : Reflect.findMethodsByName(cfg.availabilityClass, cl, cfg.methodResolvePlan)) {
        if (m.getReturnType() != Enum.class) continue;
        Knot.module.hook(m).intercept(chain -> enabled() ? labsPlan : chain.proceed());
        Knot.log("Knot: scroll preview plan forced to LABS on " + cfg.availabilityClass);
        return;
      }
    } catch (Throwable t) {
      Knot.log("Knot: scroll preview plan hook failed: " + t);
    }
  }

  private static boolean enabled() {
    return Main.options.enableScrollPreview.enabled;
  }

  private static Object enumConstant(Class<?> enumClass, String name) {
    for (Object constant : enumClass.getEnumConstants()) {
      if (((Enum<?>) constant).name().equals(name)) return constant;
    }
    return null;
  }
}
