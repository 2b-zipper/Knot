package app.zipper.knot.hooks;

import android.app.Application;
import android.app.Notification;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Reflect;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class NotificationHook implements BaseHook {
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
    if (actions == null || actions.length != 1) return;
    // A null intent renders as a disabled button, and the empty title keeps it invisible
    Notification.Action blank =
        new Notification.Action.Builder(actions[0].getIcon(), "", null).build();
    notification.actions = new Notification.Action[] {actions[0], blank};
  }
}
