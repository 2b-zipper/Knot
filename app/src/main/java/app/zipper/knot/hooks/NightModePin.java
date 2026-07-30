package app.zipper.knot.hooks;

import app.zipper.knot.Knot;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Reflect;
import java.lang.reflect.Field;
import java.util.function.BooleanSupplier;

final class NightModePin {

  private NightModePin() {}

  static void install(LoadParam lpparam, BooleanSupplier active, String tag) {
    LineVersion.Config cfg = LineVersion.get();
    if (cfg == null
        || cfg.nightMode.nightModeConfiguratorClass.isEmpty()
        || cfg.nightMode.methodApplyNightMode.isEmpty()) {
      return;
    }

    Class<?> configurator =
        Reflect.findClass(cfg.nightMode.nightModeConfiguratorClass, lpparam.classLoader);
    final Field systemDarkMode = booleanField(configurator, cfg.nightMode.fieldSystemDarkMode, tag);

    Knot.module
        .hook(
            Reflect.findMethodExact(
                configurator, cfg.nightMode.methodApplyNightMode, boolean.class))
        .intercept(
            chain -> {
              if (!active.getAsBoolean()) return chain.proceed();
              if (systemDarkMode == null) return chain.proceed(new Object[] {Boolean.TRUE});

              boolean systemIsDark = systemDarkMode.getBoolean(null);
              try {
                systemDarkMode.setBoolean(null, true);
                return chain.proceed(new Object[] {Boolean.TRUE});
              } finally {
                systemDarkMode.setBoolean(null, systemIsDark);
              }
            });

    Knot.log(
        tag
            + ": pinned night mode via "
            + cfg.nightMode.nightModeConfiguratorClass
            + "."
            + cfg.nightMode.methodApplyNightMode
            + (systemDarkMode == null ? " (only while the OS is in dark mode)" : ""));
  }

  private static Field booleanField(Class<?> owner, String name, String tag) {
    if (name == null || name.isEmpty()) return null;
    try {
      Field f = owner.getDeclaredField(name);
      if (f.getType() != boolean.class) return null;
      f.setAccessible(true);
      return f;
    } catch (Throwable t) {
      Knot.log(tag + ": dark mode flag " + name + " not found: " + t);
      return null;
    }
  }
}
