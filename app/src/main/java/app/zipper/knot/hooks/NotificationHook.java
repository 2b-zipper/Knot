package app.zipper.knot.hooks;

import android.app.Notification;
import android.os.Build;
import android.os.Bundle;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Reflect;

public class NotificationHook implements BaseHook {
  private static final String MUTE_MARKER_EXTRA = "knot.contextual_mute";
  private static final String HIDE_SMART_REPLIES_EXTRA = "android.hideSmartReplies";

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

              // Huawei Health drops LINE notifications that have exactly one action, so keep it
              Notification.Action contextual = contextual(action);
              if (contextual == null) return chain.getThisObject();
              return chain.proceed(new Object[] {contextual});
            });

    // LINE's compat builder replaces the extras after adding actions
    Knot.module
        .hook(Reflect.findMethodExact(Notification.Builder.class, "build"))
        .intercept(
            chain -> {
              Object result = chain.proceed();
              if (!config.removeNotificationMuteButton.enabled) return result;
              if (result instanceof Notification) hideSmartActions((Notification) result);
              return result;
            });
  }

  private static boolean isMuteLabel(CharSequence title) {
    if (title == null) return false;

    android.app.Application app = Knot.currentApplication();
    if (app == null) return false;

    int resId =
        app.getResources()
            .getIdentifier("notification_button_mute", "string", app.getPackageName());
    if (resId == 0) return false;

    return app.getString(resId).equals(title.toString());
  }

  private static Notification.Action contextual(Notification.Action action) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null;
    if (action.getIcon() == null || action.actionIntent == null) return null;

    Bundle marker = new Bundle();
    marker.putBoolean(MUTE_MARKER_EXTRA, true);
    return new Notification.Action.Builder(action).setContextual(true).addExtras(marker).build();
  }

  private static void hideSmartActions(Notification notification) {
    if (notification.actions == null || notification.extras == null) return;
    for (Notification.Action action : notification.actions) {
      if (action != null && action.getExtras().getBoolean(MUTE_MARKER_EXTRA, false)) {
        notification.extras.putBoolean(HIDE_SMART_REPLIES_EXTRA, true);
        return;
      }
    }
  }
}
