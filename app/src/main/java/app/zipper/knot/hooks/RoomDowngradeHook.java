package app.zipper.knot.hooks;

import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.Reflect;
import java.lang.reflect.Method;

public class RoomDowngradeHook implements BaseHook {

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    if (!config.fixDowngradeCrash.enabled) return;
    LineVersion.Config version = LineVersion.get();
    if (version == null || version.room.migrationUtilClass.isEmpty()) return;
    LineVersion.Config.Room room = version.room;

    Method isMigrationRequired = null;
    for (Method m :
        Reflect.findClass(room.migrationUtilClass, lpparam.classLoader).getDeclaredMethods()) {
      Class<?>[] params = m.getParameterTypes();
      if (m.getName().equals(room.methodIsMigrationRequired)
          && m.getReturnType() == boolean.class
          && params.length == 3
          && params[1] == int.class
          && params[2] == int.class) {
        isMigrationRequired = m;
        break;
      }
    }
    if (isMigrationRequired == null) {
      throw new NoSuchMethodException(
          room.migrationUtilClass + "#" + room.methodIsMigrationRequired);
    }

    // Same as RoomDatabase.Builder.fallbackToDestructiveMigrationOnDowngrade(): Room drops and
    // recreates the database's tables instead of throwing.
    Knot.module
        .hook(isMigrationRequired)
        .intercept(
            chain -> {
              if (!Main.options.fixDowngradeCrash.enabled) return chain.proceed();
              int from = (int) chain.getArg(1);
              int to = (int) chain.getArg(2);
              if (from <= to) return chain.proceed();
              Knot.log(
                  "Knot: Room database downgraded from " + from + " to " + to + ", recreating");
              return false;
            });
  }
}
