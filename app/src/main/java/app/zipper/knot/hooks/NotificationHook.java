package app.zipper.knot.hooks;

import android.app.Application;
import android.app.Notification;
import android.os.Bundle;
import android.provider.Settings;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Reflect;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class NotificationHook implements BaseHook {
  private static final String PADDING_EXTRA = "knot.notification_padding";
  private static final String HUAWEI_HEALTH_LISTENER = "com.huawei.health/";
  private static final Set<Notification.Builder> muteRemoved =
      Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    Knot.module
        .hook(
            Reflect.findMethodExact(
                Notification.Builder.class, "addAction", Notification.Action.class))
        .intercept(
            chain -> {
              if (!config.removeNotificationMuteButton.enabled) return chain.proceed();
              Notification.Action action = (Notification.Action) chain.getArg(0);
              if (action == null || !isMuteLabel(action.title)) return chain.proceed();
              muteRemoved.add((Notification.Builder) chain.getThisObject());
              return chain.getThisObject();
            });

    Knot.module
        .hook(Reflect.findMethodExact(Notification.Builder.class, "build"))
        .intercept(
            chain -> {
              Object result = chain.proceed();
              if (muteRemoved.remove(chain.getThisObject()) && result instanceof Notification) {
                padSingleAction((Notification) result);
              }
              return result;
            });
  }

  private static boolean isMuteLabel(CharSequence title) {
    Application app = Knot.currentApplication();
    if (title == null || app == null) return false;
    int resId =
        app.getResources()
            .getIdentifier("notification_button_mute", "string", app.getPackageName());
    return resId != 0 && app.getString(resId).contentEquals(title);
  }

  // Huawei Health drops LINE notifications that have exactly one action
  private static void padSingleAction(Notification notification) {
    Notification.Action[] actions = notification.actions;
    if (actions == null || actions.length != 1 || !isHuaweiHealthListening()) return;
    Bundle extras = new Bundle();
    extras.putBoolean(PADDING_EXTRA, true);
    // A null intent renders as a disabled button, and the empty title keeps it invisible on AOSP
    Notification.Action blank =
        new Notification.Action.Builder(actions[0].getIcon(), "", null).addExtras(extras).build();
    notification.actions = new Notification.Action[] {actions[0], blank};
  }

  private static boolean isHuaweiHealthListening() {
    Application app = Knot.currentApplication();
    if (app == null) return false;
    try {
      String listeners =
          Settings.Secure.getString(app.getContentResolver(), "enabled_notification_listeners");
      return listeners != null && listeners.contains(HUAWEI_HEALTH_LISTENER);
    } catch (Throwable t) {
      Knot.log("Knot: notification listener check failed: " + t);
      return false;
    }
  }

  static boolean isPadding(Notification.Action action) {
    return action != null && action.getExtras().getBoolean(PADDING_EXTRA, false);
  }
}
