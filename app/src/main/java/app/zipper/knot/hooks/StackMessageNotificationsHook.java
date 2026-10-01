package app.zipper.knot.hooks;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.R;
import app.zipper.knot.Reflect;
import app.zipper.knot.utils.ModuleResources;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class StackMessageNotificationsHook implements BaseHook {
  private static final String MESSAGE_TEXT_KEY = "text";
  private static final String MESSAGE_TIME_KEY = "time";
  private static final String MESSAGE_SENDER_KEY = "sender";
  private static final String MESSAGE_SENDER_PERSON_KEY = "sender_person";
  private static final String MESSAGE_DATA_MIME_TYPE_KEY = "type";
  private static final String MESSAGE_DATA_URI_KEY = "uri";
  private static final String MEDIA_MESSAGE_ID_EXTRA = "knot.media_message_id";
  private static final String KNOT_REACTION_CHANNEL_ID = "knot_reaction";
  private static final int MAX_STACKED_LINES = 7;
  private static final int MAX_CACHE_KEYS = 32;
  private static final long MEDIA_HANDOFF_GRACE_MS = TimeUnit.SECONDS.toMillis(5);
  private static final LinkedHashMap<NotificationKey, StackState> stackedLines =
      new LinkedHashMap<NotificationKey, StackState>(MAX_CACHE_KEYS, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<NotificationKey, StackState> eldest) {
          return size() > MAX_CACHE_KEYS;
        }
      };

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    Knot.module
        .hook(
            Reflect.findMethodExact(
                NotificationManager.class, "notify", String.class, int.class, Notification.class))
        .intercept(
            chain -> {
              if (!config.stackMessageNotifications.enabled) return chain.proceed();

              synchronized (NotificationMediaPreviewHook.NOTIFICATION_POST_LOCK) {
                String tag = (String) chain.getArg(0);
                int id = (int) chain.getArg(1);
                Notification notification = (Notification) chain.getArg(2);
                if (notification.extras != null
                    && notification.extras.getBoolean(
                        NotificationMediaPreviewHook.REPOST_MARKER, false)) {
                  return chain.proceed();
                }
                Notification stacked =
                    mergeNotification(Knot.currentApplication(), tag, id, notification);
                if (stacked == notification) return chain.proceed();
                return chain.proceed(new Object[] {tag, id, stacked});
              }
            });

    Knot.module
        .hook(Reflect.findMethodExact(NotificationManager.class, "cancel", String.class, int.class))
        .intercept(
            chain -> {
              clearStack((String) chain.getArg(0), (int) chain.getArg(1));
              return chain.proceed();
            });

    Knot.module
        .hook(Reflect.findMethodExact(NotificationManager.class, "cancel", int.class))
        .intercept(
            chain -> {
              clearStack(null, (int) chain.getArg(0));
              return chain.proceed();
            });

    Knot.module
        .hook(Reflect.findMethodExact(NotificationManager.class, "cancelAll"))
        .intercept(
            chain -> {
              clearAllStacks();
              return chain.proceed();
            });
  }

  static Notification mergeNotification(
      Context context, String tag, int id, Notification notification) {
    LineVersion.Config version = LineVersion.get();
    if (version == null) return notification;
    LineVersion.Config.Notification notificationConfig = version.notification;
    if (!isLineMessageNotificationTag(notificationConfig, tag)) return notification;
    if (!isLineMessageNotification(notification)) return notification;
    if (context == null) return notification;

    Bundle extras = notification.extras;
    String messageId = stringExtra(extras, MEDIA_MESSAGE_ID_EXTRA);
    if (!hasText(messageId)) {
      messageId = stringExtra(extras, notificationConfig.messageIdExtra);
    }
    CharSequence text = firstText(extras);
    if (!hasText(messageId) || !hasText(text)) return notification;

    NotificationKey key = new NotificationKey(tag, id);
    MessageLine incomingLine = incomingLine(extras, messageId, text);
    List<MessageLine> lines = mergedLines(context, key, incomingLine);
    if (lines.size() <= 1) return notification;

    try {
      Notification.Builder builder = Notification.Builder.recoverBuilder(context, notification);
      builder.setContentText(text);
      if (usesMessagingStyle(lines)) {
        builder.setStyle(messagingStyle(lines, extras));
      } else {
        builder.setStyle(inboxStyle(lines, extras));
      }
      return builder.build();
    } catch (Throwable t) {
      Knot.log("Knot: Failed to stack message notification: " + t);
      return notification;
    }
  }

  private static boolean isLineMessageNotification(Notification notification) {
    if (notification == null) return false;
    if ((notification.flags & Notification.FLAG_ONGOING_EVENT) != 0) return false;
    if ((notification.flags & Notification.FLAG_FOREGROUND_SERVICE) != 0) return false;
    if ((notification.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return false;
    if (KNOT_REACTION_CHANNEL_ID.equals(notification.getChannelId())) return false;

    String category = notification.category;
    if (Notification.CATEGORY_SERVICE.equals(category)
        || Notification.CATEGORY_CALL.equals(category)
        || Notification.CATEGORY_TRANSPORT.equals(category)
        || Notification.CATEGORY_STATUS.equals(category)) {
      return false;
    }

    return notification.extras != null;
  }

  private static synchronized List<MessageLine> mergedLines(
      Context context, NotificationKey key, MessageLine incomingLine) {
    List<MessageLine> lines = new ArrayList<>();
    Notification active = findActiveNotification(context, key);
    StackState cachedState = stackedLines.get(key);
    long now = SystemClock.elapsedRealtime();
    boolean recentMediaHandoff =
        active == null
            && incomingLine.hasData()
            && cachedState != null
            && now - cachedState.updatedAtMs <= MEDIA_HANDOFF_GRACE_MS;
    if (active == null && !recentMediaHandoff) {
      stackedLines.remove(key);
      cachedState = null;
    }

    if (cachedState != null) {
      for (MessageLine line : cachedState.lines) {
        addLine(lines, line);
      }
    }

    if (active != null && active.extras != null) {
      addMessagingLines(lines, active.extras);

      CharSequence[] activeLines =
          active.extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
      if (activeLines != null) {
        for (CharSequence line : activeLines) {
          addLine(lines, new MessageLine(null, line, null, null, null, 0L));
        }
      }
    }

    addLine(lines, incomingLine);

    while (lines.size() > MAX_STACKED_LINES) {
      lines.remove(0);
    }
    stackedLines.put(key, new StackState(new ArrayList<>(lines), now));
    return lines;
  }

  private static Notification.MessagingStyle messagingStyle(
      List<MessageLine> lines, Bundle extras) {
    Notification.MessagingStyle style = new Notification.MessagingStyle("");
    CharSequence conversationTitle = conversationTitle(lines, extras);
    if (hasText(conversationTitle)) style.setConversationTitle(conversationTitle);
    for (MessageLine line : lines) {
      style.addMessage(message(line));
    }
    return style;
  }

  static Notification buildMediaMessageNotification(
      Context context,
      String tag,
      int id,
      Notification base,
      Notification mediaSource,
      String messageId,
      NotificationMediaFileStore.Attachment attachment,
      boolean stackEnabled) {
    if (context == null
        || base == null
        || base.extras == null
        || mediaSource == null
        || mediaSource.extras == null
        || attachment == null) {
      return null;
    }

    try {
      CharSequence mediaText = firstText(mediaSource.extras);
      if (!hasText(mediaText)) return null;
      MessageLine line =
          incomingLine(mediaSource.extras, messageId, mediaText)
              .withData(attachment.mimeType, attachment.uri);
      Notification.Builder builder = Notification.Builder.recoverBuilder(context, base);
      CharSequence currentText = firstText(base.extras);
      builder.setContentText(hasText(currentText) ? currentText : mediaText);
      builder.setOnlyAlertOnce(true);
      List<MessageLine> lines = new ArrayList<>();
      lines.add(line);
      builder.setStyle(messagingStyle(lines, mediaSource.extras));
      Bundle mediaExtras = new Bundle();
      mediaExtras.putString(MEDIA_MESSAGE_ID_EXTRA, messageId);
      builder.addExtras(mediaExtras);
      Notification built = builder.build();
      return stackEnabled ? mergeNotification(context, tag, id, built) : built;
    } catch (Throwable t) {
      Knot.log("Knot: media MessagingStyle build failed: " + t.getClass().getSimpleName());
      return null;
    }
  }

  private static Notification.InboxStyle inboxStyle(List<MessageLine> lines, Bundle extras) {
    Notification.InboxStyle style = new Notification.InboxStyle();
    CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
    if (hasText(title)) style.setBigContentTitle(title);
    style.setSummaryText(
        ModuleResources.getQuantity(
            R.plurals.notification_message_count, lines.size(), lines.size()));
    for (MessageLine line : lines) {
      style.addLine(line.text);
    }
    return style;
  }

  private static synchronized void clearStack(String tag, int id) {
    if (tag == null) {
      List<NotificationKey> keys = new ArrayList<>();
      for (NotificationKey key : stackedLines.keySet()) {
        if (key.id == id) keys.add(key);
      }
      for (NotificationKey key : keys) {
        stackedLines.remove(key);
      }
      return;
    }
    LineVersion.Config version = LineVersion.get();
    if (version != null && isLineMessageNotificationTag(version.notification, tag)) {
      stackedLines.remove(new NotificationKey(tag, id));
    }
  }

  private static synchronized void clearAllStacks() {
    stackedLines.clear();
  }

  private static Notification findActiveNotification(Context context, NotificationKey key) {
    try {
      NotificationManager nm =
          (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
      if (nm == null) return null;
      for (StatusBarNotification sbn : nm.getActiveNotifications()) {
        if (sbn.getId() == key.id && sameTag(sbn.getTag(), key.tag)) {
          return sbn.getNotification();
        }
      }
    } catch (Throwable ignored) {
    }
    return null;
  }

  static Notification activeNotification(Context context, String tag, int id) {
    if (context == null) return null;
    return findActiveNotification(context, new NotificationKey(tag, id));
  }

  private static void addLine(List<MessageLine> lines, MessageLine next) {
    if (next == null || (!hasText(next.text) && !next.hasData())) return;
    for (int i = 0; i < lines.size(); i++) {
      MessageLine current = lines.get(i);
      if (hasText(next.messageId) && next.messageId.equals(current.messageId)) {
        lines.set(i, next);
        return;
      }
      if (!hasText(next.messageId)
          && sameText(next.text, current.text)
          && sameText(next.sender, current.sender)) {
        return;
      }
    }
    lines.add(next);
  }

  private static MessageLine incomingLine(Bundle extras, String messageId, CharSequence text) {
    MessageLine messagingLine = latestMessagingLine(extras, messageId);
    if (messagingLine != null) return messagingLine;
    return new MessageLine(
        messageId, messageLineText(extras, text), null, null, null, System.currentTimeMillis());
  }

  private static void addMessagingLines(List<MessageLine> lines, Bundle extras) {
    Parcelable[] messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES);
    if (messages == null) return;

    CharSequence conversationTitle = conversationTitle(extras);
    for (Parcelable message : messages) {
      if (message instanceof Bundle) {
        MessageLine line = messagingLine((Bundle) message, null, conversationTitle);
        if (line != null) addLine(lines, line);
      }
    }
  }

  private static MessageLine latestMessagingLine(Bundle extras, String messageId) {
    Parcelable[] messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES);
    if (messages == null) return null;

    CharSequence conversationTitle = conversationTitle(extras);
    for (int i = messages.length - 1; i >= 0; i--) {
      if (messages[i] instanceof Bundle) {
        MessageLine line = messagingLine((Bundle) messages[i], messageId, conversationTitle);
        if (line != null) return line;
      }
    }
    return null;
  }

  private static MessageLine messagingLine(
      Bundle message, String messageId, CharSequence conversationTitle) {
    CharSequence text = message.getCharSequence(MESSAGE_TEXT_KEY);
    String dataMimeType = message.getString(MESSAGE_DATA_MIME_TYPE_KEY);
    Parcelable dataValue = message.getParcelable(MESSAGE_DATA_URI_KEY);
    Uri dataUri = dataValue instanceof Uri ? (Uri) dataValue : null;
    if (!hasText(text) && (!hasText(dataMimeType) || dataUri == null)) return null;

    Parcelable senderPerson = message.getParcelable(MESSAGE_SENDER_PERSON_KEY);
    CharSequence sender = messageSender(message, senderPerson);
    long timestamp = message.getLong(MESSAGE_TIME_KEY, System.currentTimeMillis());
    return new MessageLine(
        messageId, text, sender, senderPerson, conversationTitle, timestamp, dataMimeType, dataUri);
  }

  private static Notification.MessagingStyle.Message message(MessageLine line) {
    Notification.MessagingStyle.Message message = null;
    try {
      Class<?> personClass = Class.forName("android.app.Person");
      if (personClass.isInstance(line.senderPerson)) {
        Object value =
            Notification.MessagingStyle.Message.class
                .getConstructor(CharSequence.class, long.class, personClass)
                .newInstance(line.text, line.timestamp, line.senderPerson);
        if (value instanceof Notification.MessagingStyle.Message) {
          message = (Notification.MessagingStyle.Message) value;
        }
      }
    } catch (Throwable ignored) {
    }
    if (message == null) {
      message = new Notification.MessagingStyle.Message(line.text, line.timestamp, line.sender);
    }
    if (line.hasData()) message.setData(line.dataMimeType, line.dataUri);
    return message;
  }

  private static CharSequence messageSender(Bundle message, Parcelable senderPerson) {
    CharSequence sender = message.getCharSequence(MESSAGE_SENDER_KEY);
    if (hasText(sender)) return sender;
    if (senderPerson == null) return null;
    try {
      Object name = senderPerson.getClass().getMethod("getName").invoke(senderPerson);
      if (name instanceof CharSequence) return (CharSequence) name;
    } catch (Throwable ignored) {
    }
    return null;
  }

  private static CharSequence firstText(Bundle extras) {
    if (extras == null) return null;
    CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);
    if (hasText(text)) return text;
    return extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
  }

  private static CharSequence messageLineText(Bundle extras, CharSequence text) {
    if (extras == null) return text;

    CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
    CharSequence subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
    if (!hasText(title) || !hasText(subText)) return text;
    if (sameText(title, subText)) return text;
    if (startsWithSenderPrefix(text, title)) return text;

    return title + ": " + text;
  }

  private static CharSequence conversationTitle(List<MessageLine> lines, Bundle extras) {
    for (int i = lines.size() - 1; i >= 0; i--) {
      CharSequence title = lines.get(i).conversationTitle;
      if (hasText(title)) return title;
    }
    return conversationTitle(extras);
  }

  private static CharSequence conversationTitle(Bundle extras) {
    if (extras == null) return null;
    CharSequence conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE);
    if (hasText(conversationTitle)) return conversationTitle;

    CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
    CharSequence subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
    if (hasText(title) && hasText(subText) && !sameText(title, subText)) return subText;
    return null;
  }

  private static boolean usesMessagingStyle(List<MessageLine> lines) {
    for (MessageLine line : lines) {
      if (hasText(line.sender) || line.senderPerson != null || line.hasData()) return true;
    }
    return false;
  }

  private static String stringExtra(Bundle extras, String key) {
    if (extras == null) return null;
    Object value = extras.get(key);
    return value == null ? null : value.toString();
  }

  private static boolean hasText(CharSequence text) {
    return text != null && text.length() > 0;
  }

  private static boolean sameTag(String a, String b) {
    return a == null ? b == null : a.equals(b);
  }

  private static boolean sameText(CharSequence a, CharSequence b) {
    return a == null ? b == null : b != null && a.toString().contentEquals(b);
  }

  private static boolean startsWithSenderPrefix(CharSequence text, CharSequence sender) {
    if (!hasText(text) || !hasText(sender)) return false;
    return text.toString().startsWith(sender + ": ") || text.toString().startsWith(sender + " : ");
  }

  private static boolean isLineMessageNotificationTag(
      LineVersion.Config.Notification notificationConfig, String tag) {
    return sameNonEmptyText(tag, notificationConfig.messageNotificationTag)
        || sameNonEmptyText(tag, notificationConfig.chatNotificationTag);
  }

  private static boolean sameNonEmptyText(String a, String b) {
    return hasText(a) && a.equals(b);
  }

  private static final class NotificationKey {
    final String tag;
    final int id;

    NotificationKey(String tag, int id) {
      this.tag = tag;
      this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof NotificationKey)) return false;
      NotificationKey other = (NotificationKey) obj;
      return id == other.id && sameTag(tag, other.tag);
    }

    @Override
    public int hashCode() {
      return 31 * id + (tag == null ? 0 : tag.hashCode());
    }
  }

  private static final class StackState {
    final List<MessageLine> lines;
    final long updatedAtMs;

    StackState(List<MessageLine> lines, long updatedAtMs) {
      this.lines = lines;
      this.updatedAtMs = updatedAtMs;
    }
  }

  private static final class MessageLine {
    final String messageId;
    final CharSequence text;
    final CharSequence sender;
    final Parcelable senderPerson;
    final CharSequence conversationTitle;
    final long timestamp;
    final String dataMimeType;
    final Uri dataUri;

    MessageLine(
        String messageId,
        CharSequence text,
        CharSequence sender,
        Parcelable senderPerson,
        CharSequence conversationTitle,
        long timestamp) {
      this(messageId, text, sender, senderPerson, conversationTitle, timestamp, null, null);
    }

    MessageLine(
        String messageId,
        CharSequence text,
        CharSequence sender,
        Parcelable senderPerson,
        CharSequence conversationTitle,
        long timestamp,
        String dataMimeType,
        Uri dataUri) {
      this.messageId = messageId;
      this.text = text;
      this.sender = sender;
      this.senderPerson = senderPerson;
      this.conversationTitle = conversationTitle;
      this.timestamp = timestamp;
      this.dataMimeType = dataMimeType;
      this.dataUri = dataUri;
    }

    MessageLine withData(String mimeType, Uri uri) {
      return new MessageLine(
          messageId, text, sender, senderPerson, conversationTitle, timestamp, mimeType, uri);
    }

    boolean hasData() {
      return hasText(dataMimeType) && dataUri != null;
    }
  }
}
