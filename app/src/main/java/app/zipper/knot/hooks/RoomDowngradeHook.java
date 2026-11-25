package app.zipper.knot.hooks;

import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.Reflect;
import java.lang.reflect.Constructor;
import java.util.Set;

public class RoomDowngradeHook implements BaseHook {

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    if (!config.fixDowngradeCrash.enabled) return;
    LineVersion.Config version = LineVersion.get();
    if (version == null) return;
    LineVersion.Config.Room room = version.room;

    hookConfig(room.databaseConfigClass, lpparam.classLoader, true);
    hookConfig(room.room3DatabaseConfigClass, lpparam.classLoader, false);
  }

  // Equivalent to RoomDatabase.Builder.fallbackToDestructiveMigrationOnDowngrade():
  // R8 inlines MigrationUtil.isMigrationRequired from 26.19.0, so enable the config flag directly.
  private static void hookConfig(String configClass, ClassLoader cl, boolean required)
      throws Throwable {
    if (configClass.isEmpty()) return;
    boolean hooked = false;
    for (Constructor<?> ctor : Reflect.findClass(configClass, cl).getDeclaredConstructors()) {
      Class<?>[] params = ctor.getParameterTypes();
      int downgradeIndex = findAllowDowngradeIndex(params);
      if (downgradeIndex < 0) continue;
      ctor.setAccessible(true);
      final int idx = downgradeIndex;
      Knot.module
          .hook(ctor)
          .intercept(
              chain -> {
                if (!Main.options.fixDowngradeCrash.enabled
                    || Boolean.TRUE.equals(chain.getArg(idx))) {
                  return chain.proceed();
                }
                Object[] args = chain.getArgs().toArray();
                args[idx] = Boolean.TRUE;
                return chain.proceed(args);
              });
      hooked = true;
    }
    if (!hooked && required) {
      throw new NoSuchMethodException(configClass + ".<init>");
    }
  }

  // DatabaseConfiguration lists (requireMigration, allowDestructiveMigrationOnDowngrade, Set)
  private static int findAllowDowngradeIndex(Class<?>[] params) {
    int match = -1;
    for (int i = 2; i < params.length; i++) {
      if (Set.class.isAssignableFrom(params[i])
          && params[i - 1] == boolean.class
          && params[i - 2] == boolean.class) {
        if (match >= 0) return -1;
        match = i - 1;
      }
    }
    return match;
  }
}
