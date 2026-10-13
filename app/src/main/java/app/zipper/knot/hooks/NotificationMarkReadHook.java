package app.zipper.knot.hooks;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Reflect;

public class NotificationMarkReadHook implements BaseHook {
  private static final String MARKER_EXTRA = "knot.notification_mark_as_read";
  private static final String SQUARE_EXTRA = "knot.notification_square";

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    LineVersion.Config version = LineVersion.get();
    if (version == null) return;
    LineVersion.Config.Notification cfg = version.notification;
    if (!hasText(cfg.accessServiceClass)
        || !hasText(cfg.accessServiceCallbackClass)
        || !hasText(cfg.accessServiceCallbackMethod)
        || !hasText(cfg.accessServiceChatIdExtra)
        || !hasText(cfg.markAsReadAction)
        || !hasText(cfg.chatIdExtra)) {
      return;
    }

    ClassLoader cl = lpparam.classLoader;
    Class<?> serviceClass = Reflect.findClass(cfg.accessServiceClass, cl);
    Class<?> callbackClass = Reflect.findClass(cfg.accessServiceCallbackClass, cl);

    Knot.module
        .hook(
            Reflect.findMethodExact(
                NotificationManager.class, "notify", String.class, int.class, Notification.class))
        .intercept(
            chain -> {
              if (!config.notificationMarkAsRead.enabled) return chain.proceed();
              String tag = (String) chain.getArg(0);
              int id = (int) chain.getArg(1);
              Notification notification = (Notification) chain.getArg(2);
              Notification withAction = withMarkReadAction(cfg, tag, notification);
              if (withAction == notification) return chain.proceed();
              return chain.proceed(new Object[] {tag, id, withAction});
            });

    Knot.module
        .hook(
            Reflect.findMethodExact(
                serviceClass, "onStartCommand", Intent.class, int.class, int.class))
        .intercept(
            chain -> {
              Intent intent = (Intent) chain.getArg(0);
              if (intent == null || !intent.getBooleanExtra(MARKER_EXTRA, false)) {
                return chain.proceed();
              }
              String chatId = intent.getStringExtra(cfg.accessServiceChatIdExtra);
              if (!hasText(chatId)) return Service.START_NOT_STICKY;
              boolean square = intent.getBooleanExtra(SQUARE_EXTRA, false);
              // Regular chats are read through the manager that read avoidance blocks
              if (!square) ReadReceiptHandler.addPendingManualRead(chatId);
              try {
                // LINE's post-reply callback marks the chat read (OpenChat included) and clears its
                // notifications, matching "Mark as read" in the chat list.
                Object callback = Reflect.newInstance(callbackClass, chain.getThisObject(), chatId);
                Reflect.callMethod(callback, cfg.accessServiceCallbackMethod);
                return Service.START_NOT_STICKY;
              } catch (Throwable t) {
                Knot.log("Knot: notification mark-as-read failed: " + t);
                // LINE's plain mark-as-read only handles regular chats and keeps the notification
                if (square) return Service.START_NOT_STICKY;
                return chain.proceed();
              }
            });
  }

  private static Notification withMarkReadAction(
      LineVersion.Config.Notification cfg, String tag, Notification notification) {
    if (!isCandidate(cfg, tag, notification)) return notification;
    Context context = Knot.currentApplication();
    if (context == null) return notification;

    try {
      // LINE's own chat-list label keeps the wording and translations identical to its menu
      String label = lineString(context, cfg.markAsReadLabelRes);
      if (label == null) return notification;
      String chatId = stringExtra(notification.extras, cfg.chatIdExtra);
      boolean square =
          hasText(cfg.squareNotificationExtra)
              && notification.extras.getBoolean(cfg.squareNotificationExtra, false);
      Intent intent =
          new Intent(cfg.markAsReadAction)
              .setClassName(context.getPackageName(), cfg.accessServiceClass)
              // LINE's hidden mark-as-read action targets the same service and action, so distinct
              // data keeps the two PendingIntents from replacing each other.
              .setData(
                  new Uri.Builder()
                      .scheme("knot")
                      .authority("mark-as-read")
                      .appendPath(chatId)
                      .build())
              .putExtra(cfg.accessServiceChatIdExtra, chatId)
              .putExtra(MARKER_EXTRA, true)
              .putExtra(SQUARE_EXTRA, square);
      PendingIntent pendingIntent =
          PendingIntent.getService(
              context,
              chatId.hashCode(),
              intent,
              PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

      Bundle actionExtras = new Bundle();
      actionExtras.putBoolean(MARKER_EXTRA, true);
      Notification.Action action =
          new Notification.Action.Builder(notification.getSmallIcon(), label, pendingIntent)
              .addExtras(actionExtras)
              .build();

      Notification copy = notification.clone();
      Notification.Action[] current =
          copy.actions == null ? new Notification.Action[0] : copy.actions;
      // Sit just left of reply; without a reply action, append after LINE's buttons
      int index = replyActionIndex(current);
      if (index < 0) index = current.length;
      Notification.Action[] actions = new Notification.Action[current.length + 1];
      System.arraycopy(current, 0, actions, 0, index);
      actions[index] = action;
      System.arraycopy(current, index, actions, index + 1, current.length - index);
      copy.actions = actions;
      return copy;
    } catch (Throwable t) {
      Knot.log("Knot: notification mark-as-read button failed: " + t);
      return notification;
    }
  }

  // LINE's reply is the only action with a text input
  private static int replyActionIndex(Notification.Action[] actions) {
    for (int i = 0; i < actions.length; i++) {
      Notification.Action action = actions[i];
      if (action == null) continue;
      RemoteInput[] inputs = action.getRemoteInputs();
      if (inputs != null && inputs.length > 0) return i;
    }
    return -1;
  }

  private static boolean isCandidate(
      LineVersion.Config.Notification cfg, String tag, Notification notification) {
    if (notification == null || notification.extras == null) return false;
    if (!hasText(tag) || !tag.equals(cfg.messageNotificationTag)) return false;
    int excluded =
        Notification.FLAG_GROUP_SUMMARY
            | Notification.FLAG_ONGOING_EVENT
            | Notification.FLAG_FOREGROUND_SERVICE;
    if ((notification.flags & excluded) != 0) return false;
    if (!hasText(stringExtra(notification.extras, cfg.chatIdExtra))) return false;
    return !hasMarkReadAction(notification);
  }

  // Stacked and media-preview reposts recover the builder, which keeps our action
  private static boolean hasMarkReadAction(Notification notification) {
    if (notification.actions == null) return false;
    for (Notification.Action action : notification.actions) {
      if (action != null && action.getExtras().getBoolean(MARKER_EXTRA, false)) return true;
    }
    return false;
  }

  private static String lineString(Context context, String name) {
    if (!hasText(name)) return null;
    int id = context.getResources().getIdentifier(name, "string", context.getPackageName());
    return id == 0 ? null : context.getString(id);
  }

  private static String stringExtra(Bundle extras, String key) {
    if (extras == null || key == null) return null;
    Object value = extras.get(key);
    return value == null ? null : value.toString();
  }

  private static boolean hasText(String value) {
    return value != null && !value.isEmpty();
  }
}
