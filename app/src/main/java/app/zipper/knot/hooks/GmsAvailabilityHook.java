package app.zipper.knot.hooks;

import android.content.Context;
import android.content.pm.PackageManager;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Reflect;

public class GmsAvailabilityHook implements BaseHook {

  private static final String GMS_PACKAGE = "com.google.android.gms";

  private static volatile String gmsPackage;

  // NPatch answers lookups of GMS with the microG package, whose signature never passes the check
  private static boolean isRedirected(Context context) {
    String target = gmsPackage;
    if (target == null) {
      try {
        target = context.getPackageManager().getPackageInfo(GMS_PACKAGE, 0).packageName;
      } catch (PackageManager.NameNotFoundException e) {
        target = GMS_PACKAGE;
      }
      if (!GMS_PACKAGE.equals(target)) {
        Knot.log("Knot: GMS is redirected to " + target + ", reporting it as available");
      }
      gmsPackage = target;
    }
    return !GMS_PACKAGE.equals(target);
  }

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    LineVersion.Config version = LineVersion.get();
    if (version == null || version.notificationFix.gmsAvailabilityClass.isEmpty()) return;
    LineVersion.Config.NotificationFix cfg = version.notificationFix;

    Knot.module
        .hook(
            Reflect.findMethodExact(
                cfg.gmsAvailabilityClass,
                lpparam.classLoader,
                cfg.gmsAvailabilityMethod,
                Context.class,
                int.class))
        .intercept(
            chain -> {
              Object result = chain.proceed();
              if (Integer.valueOf(0).equals(result)) return result;
              Context context = (Context) chain.getArg(0);
              return context != null && isRedirected(context) ? Integer.valueOf(0) : result;
            });
  }
}
