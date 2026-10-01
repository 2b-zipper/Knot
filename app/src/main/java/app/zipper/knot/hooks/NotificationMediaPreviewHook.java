package app.zipper.knot.hooks;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.Reflect;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationMediaPreviewHook implements BaseHook {
  static final String REPOST_MARKER = "knot.notification_media_preview_repost";
  static final Object NOTIFICATION_POST_LOCK = new Object();

  private static final String SESSION_MARKER = "knot.notification_media_session";
  private static final int MEDIA_RESOLVER_THREADS = 4;
  private static final int MAX_NOTIFICATION_SESSIONS = 64;
  private static final Map<String, String> notificationSessions =
      new LinkedHashMap<String, String>(MAX_NOTIFICATION_SESSIONS, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
          return size() > MAX_NOTIFICATION_SESSIONS;
        }
      };
  private static final ExecutorService mediaExecutor =
      Executors.newFixedThreadPool(
          MEDIA_RESOLVER_THREADS,
          runnable -> {
            Thread thread = new Thread(runnable, "Knot-NotificationMedia");
            thread.setDaemon(true);
            return thread;
          });

  // Combination sticker downloads can block; keep them off the shared preview workers.
  private static final ExecutorService combinationExecutor =
      Executors.newFixedThreadPool(
          2,
          runnable -> {
            Thread thread = new Thread(runnable, "Knot-CombinationSticker");
            thread.setDaemon(true);
            return thread;
          });

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    LineVersion.Config version = LineVersion.get();
    if (version == null || !hasText(version.notification.messageClass)) return;

    Knot.module
        .hook(
            Reflect.findMethodExact(
                NotificationManager.class, "notify", String.class, int.class, Notification.class))
        .intercept(
            chain -> {
              if (!config.notificationMediaPreview.enabled) return chain.proceed();

              synchronized (NOTIFICATION_POST_LOCK) {
                String tag = (String) chain.getArg(0);
                int id = (int) chain.getArg(1);
                Notification notification = (Notification) chain.getArg(2);
                if (!isCandidate(tag, notification)) return chain.proceed();

                LineVersion.Config currentVersion = LineVersion.get();
                if (currentVersion == null) return chain.proceed();
                String messageId =
                    stringExtra(notification.extras, currentVersion.notification.messageIdExtra);
                if (!hasText(messageId)) return chain.proceed();

                Notification active =
                    StackMessageNotificationsHook.activeNotification(
                        Knot.currentApplication(), tag, id);
                String key = sessionKey(tag, id);
                String session = notificationSessions.get(key);
                if (active == null
                    || !sameNonEmpty(session, stringExtra(active.extras, SESSION_MARKER))) {
                  session = UUID.randomUUID().toString();
                }
                notificationSessions.put(key, session);
                Notification source = notification.clone();
                source.extras.putString(SESSION_MARKER, session);
                NotificationMediaCaptureStore.MessageData captured =
                    NotificationMediaCaptureStore.take(messageId);

                Object result = chain.proceed(new Object[] {tag, id, source});
                if (captured != null) {
                  dispatchCaptured(tag, id, source, messageId, captured);
                  return result;
                }

                NotificationMediaCaptureStore.register(
                    messageId, () -> resumeAfterCapture(tag, id, source, messageId));
                return result;
              }
            });

    hookCancellation(String.class, int.class);
    hookCancellation(int.class);
    Knot.module
        .hook(Reflect.findMethodExact(NotificationManager.class, "cancelAll"))
        .intercept(
            chain -> {
              synchronized (NOTIFICATION_POST_LOCK) {
                notificationSessions.clear();
                return chain.proceed();
              }
            });
  }

  private static void hookCancellation(Class<?>... parameterTypes) {
    Knot.module
        .hook(
            Reflect.findMethodExact(NotificationManager.class, "cancel", (Object[]) parameterTypes))
        .intercept(
            chain -> {
              synchronized (NOTIFICATION_POST_LOCK) {
                String tag = parameterTypes.length == 2 ? (String) chain.getArg(0) : null;
                int id = (int) chain.getArg(parameterTypes.length - 1);
                notificationSessions.remove(sessionKey(tag, id));
                return chain.proceed();
              }
            });
  }

  private static void resumeAfterCapture(
      String tag, int id, Notification notification, String messageId) {
    NotificationMediaCaptureStore.MessageData captured =
        NotificationMediaCaptureStore.take(messageId);

    if (captured != null) dispatchCaptured(tag, id, notification, messageId, captured);
  }

  private static void dispatchCaptured(
      String tag,
      int id,
      Notification notification,
      String messageId,
      NotificationMediaCaptureStore.MessageData captured) {
    if (captured.isImage() || captured.isVideo()) {
      resolveChatThumbnail(tag, id, notification, messageId, captured.isVideo());
      return;
    }

    if (captured.isSticker()) {
      LineStickerMediaResolver.StickerMetadata sticker =
          LineStickerMediaResolver.parse(captured.parameter);
      if (sticker != null) {
        mediaExecutor.execute(() -> updateSticker(tag, id, notification, messageId, sticker));
      } else {
        logFailure("sticker", messageId, "sticker metadata parse failed");
      }
      return;
    }

    if (captured.isSticon()) {
      mediaExecutor.execute(() -> updateSticon(tag, id, notification, messageId, captured));
    }
  }

  private static void resolveChatThumbnail(
      String tag, int id, Notification original, String messageId, boolean video) {
    NotificationMediaThumbnailKeyStore.Key key =
        NotificationMediaThumbnailKeyStore.getKey(messageId);
    if (key != null) {
      mediaExecutor.execute(() -> updateChatThumbnail(tag, id, original, messageId, video, key));
      return;
    }

    NotificationMediaThumbnailKeyStore.registerKey(
        messageId, () -> resolveChatThumbnail(tag, id, original, messageId, video));
  }

  private static void updateChatThumbnail(
      String tag,
      int id,
      Notification original,
      String messageId,
      boolean video,
      NotificationMediaThumbnailKeyStore.Key key) {
    if (!isNotificationCurrent(tag, id, original, messageId)) return;
    Context context = Knot.currentApplication();
    String mediaType = video ? "video-thumbnail" : "image";
    if (context == null) {
      logFailure(mediaType, messageId, "application context unavailable");
      return;
    }

    NotificationMediaFileStore.Attachment attachment =
        LineChatThumbnailResolver.acquireOrRequest(
            context, key, () -> isNotificationCurrent(tag, id, original, messageId));
    if (attachment == null) {
      if (!isNotificationCurrent(tag, id, original, messageId)) return;
      logFailure(mediaType, messageId, "LINE thumbnail acquisition failed");
      return;
    }

    postMedia(context, tag, id, original, messageId, attachment);
  }

  private static void updateSticker(
      String tag,
      int id,
      Notification original,
      String messageId,
      LineStickerMediaResolver.StickerMetadata sticker) {
    if (!isNotificationCurrent(tag, id, original, messageId)) return;
    Context context = Knot.currentApplication();
    if (context == null) {
      logFailure(
          sticker.isCombinationSticker() ? "combination-sticker" : "sticker",
          messageId,
          "application context unavailable");
      return;
    }

    if (sticker.isCombinationSticker()) {
      LineCombinationStickerMediaResolver.acquireAsync(
          context,
          sticker.combinationStickerId,
          combinationExecutor,
          () -> isNotificationCurrent(tag, id, original, messageId),
          attachment -> {
            if (attachment != null) {
              postMedia(context, tag, id, original, messageId, attachment);
            } else {
              logFailure(
                  "combination-sticker",
                  messageId,
                  "LINE combination sticker cache was incomplete");
            }
          });
      return;
    }

    NotificationMediaFileStore.Attachment attachment =
        LineStickerMediaResolver.acquireCached(context, sticker);
    if (attachment != null) {
      postMedia(context, tag, id, original, messageId, attachment);
      return;
    }

    LineStickerMediaResolver.registerCacheReady(
        context,
        sticker,
        () ->
            mediaExecutor.execute(
                () -> updateStickerFromLineCache(tag, id, original, messageId, sticker)));
  }

  private static void updateStickerFromLineCache(
      String tag,
      int id,
      Notification original,
      String messageId,
      LineStickerMediaResolver.StickerMetadata sticker) {
    if (!isNotificationCurrent(tag, id, original, messageId)) return;
    Context context = Knot.currentApplication();
    if (context == null) return;

    NotificationMediaFileStore.Attachment attachment =
        LineStickerMediaResolver.acquireCached(context, sticker);
    if (attachment == null) {
      logFailure("sticker", messageId, "LINE sticker fetch completed without cache file");
      return;
    }
    postMedia(context, tag, id, original, messageId, attachment);
  }

  private static void updateSticon(
      String tag,
      int id,
      Notification original,
      String messageId,
      NotificationMediaCaptureStore.MessageData captured) {
    if (!isNotificationCurrent(tag, id, original, messageId)) return;
    Context context = Knot.currentApplication();
    if (context == null) {
      logFailure("sticon", messageId, "application context unavailable");
      return;
    }

    NotificationMediaFileStore.Attachment attachment =
        LineSticonMediaResolver.acquireCached(context, captured);
    if (attachment != null) {
      postMedia(context, tag, id, original, messageId, attachment);
      return;
    }

    LineSticonMediaResolver.registerCacheReady(
        context,
        captured,
        () -> isNotificationCurrent(tag, id, original, messageId),
        () ->
            mediaExecutor.execute(
                () -> updateSticonFromLineCache(tag, id, original, messageId, captured)));
  }

  private static void updateSticonFromLineCache(
      String tag,
      int id,
      Notification original,
      String messageId,
      NotificationMediaCaptureStore.MessageData captured) {
    if (!isNotificationCurrent(tag, id, original, messageId)) return;
    Context context = Knot.currentApplication();
    if (context == null) return;

    NotificationMediaFileStore.Attachment attachment =
        LineSticonMediaResolver.acquireCached(context, captured);
    if (attachment == null) {
      logFailure("sticon", messageId, "LINE sticon fetch completed without cache drawable");
      return;
    }
    postMedia(context, tag, id, original, messageId, attachment);
  }

  private static void postMedia(
      Context context,
      String tag,
      int id,
      Notification original,
      String messageId,
      NotificationMediaFileStore.Attachment attachment) {
    try {
      synchronized (NOTIFICATION_POST_LOCK) {
        buildAndRepost(context, tag, id, original, messageId, attachment);
      }
    } catch (Throwable t) {
      Knot.log(
          "Knot: notification media preview: notification repost failed (messageId="
              + messageId
              + ")",
          t);
    }
  }

  private static void buildAndRepost(
      Context context,
      String tag,
      int id,
      Notification original,
      String messageId,
      NotificationMediaFileStore.Attachment attachment) {
    Notification active = currentNotification(context, tag, id, original, messageId);
    if (active == null) return;
    boolean stackEnabled = Main.options.stackMessageNotifications.enabled;
    Notification enriched =
        StackMessageNotificationsHook.buildMediaMessageNotification(
            context, tag, id, active, original, messageId, attachment, stackEnabled);
    if (enriched == null) {
      logFailure("notification", messageId, "MessagingStyle rebuild returned null");
      return;
    }

    Notification.Builder builder = Notification.Builder.recoverBuilder(context, enriched);
    Bundle marker = new Bundle();
    marker.putBoolean(REPOST_MARKER, true);
    builder.addExtras(marker);
    builder.setOnlyAlertOnce(true);
    repost(context, tag, id, builder.build());
  }

  private static boolean isNotificationCurrent(
      String tag, int id, Notification original, String messageId) {
    synchronized (NOTIFICATION_POST_LOCK) {
      Context context = Knot.currentApplication();
      return context != null && currentNotification(context, tag, id, original, messageId) != null;
    }
  }

  // Called with NOTIFICATION_POST_LOCK held.
  private static Notification currentNotification(
      Context context, String tag, int id, Notification original, String messageId) {
    if (!Main.options.notificationMediaPreview.enabled) return null;
    Notification active = StackMessageNotificationsHook.activeNotification(context, tag, id);
    if (active == null || active.extras == null) {
      return null;
    }
    if (!sameNonEmpty(
        notificationSessions.get(sessionKey(tag, id)),
        stringExtra(active.extras, SESSION_MARKER))) {
      return null;
    }

    boolean stackEnabled = Main.options.stackMessageNotifications.enabled;
    if (!sameSession(active, original)) return null;
    if (!stackEnabled) {
      LineVersion.Config currentVersion = LineVersion.get();
      if (currentVersion == null) return null;
      String activeMessageId =
          stringExtra(active.extras, currentVersion.notification.messageIdExtra);
      if (!messageId.equals(activeMessageId)) return null;
    }
    return active;
  }

  private static void logFailure(String mediaType, String messageId, String detail) {
    Knot.log(
        "Knot: notification media preview failed ["
            + mediaType
            + "] (messageId="
            + messageId
            + "): "
            + detail);
  }

  private static String sessionKey(String tag, int id) {
    return tag + ":" + id;
  }

  private static boolean sameSession(Notification active, Notification original) {
    return sameNonEmpty(
        stringExtra(active.extras, SESSION_MARKER), stringExtra(original.extras, SESSION_MARKER));
  }

  private static boolean isCandidate(String tag, Notification notification) {
    if (notification == null || notification.extras == null) return false;
    if (notification.extras.getBoolean(REPOST_MARKER, false)) return false;
    if ((notification.flags & Notification.FLAG_ONGOING_EVENT) != 0) return false;
    if ((notification.flags & Notification.FLAG_FOREGROUND_SERVICE) != 0) return false;
    if ((notification.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return false;
    if (Notification.CATEGORY_CALL.equals(notification.category)
        || Notification.CATEGORY_SERVICE.equals(notification.category)) return false;

    LineVersion.Config version = LineVersion.get();
    if (version == null) return false;
    LineVersion.Config.Notification notificationConfig = version.notification;
    return sameNonEmpty(tag, notificationConfig.messageNotificationTag)
        || sameNonEmpty(tag, notificationConfig.chatNotificationTag);
  }

  private static void repost(Context context, String tag, int id, Notification notification) {
    NotificationManager manager =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    if (manager == null) return;
    if (tag == null) manager.notify(id, notification);
    else manager.notify(tag, id, notification);
  }

  private static String stringExtra(Bundle extras, String key) {
    if (extras == null || key == null) return null;
    Object value = extras.get(key);
    return value == null ? null : String.valueOf(value);
  }

  private static boolean sameNonEmpty(String a, String b) {
    return hasText(a) && hasText(b) && a.equals(b);
  }

  private static boolean hasText(String value) {
    return value != null && !value.isEmpty();
  }
}
