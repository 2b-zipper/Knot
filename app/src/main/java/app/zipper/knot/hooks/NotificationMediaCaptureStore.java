package app.zipper.knot.hooks;

import android.os.SystemClock;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

final class NotificationMediaCaptureStore {
  static final int TYPE_IMAGE = 1;
  static final int TYPE_STICKER = 2;
  static final int TYPE_STICON = 3;
  static final int TYPE_VIDEO = 4;

  private static final int MAX_MESSAGES = 64;
  private static final int MAX_PENDING = 64;
  private static final long MESSAGE_TTL_MS = TimeUnit.SECONDS.toMillis(30);
  private static final long PENDING_TTL_MS = TimeUnit.SECONDS.toMillis(5);
  private static final Object lock = new Object();
  private static final LinkedHashMap<String, MessageData> messages =
      new LinkedHashMap<String, MessageData>(MAX_MESSAGES, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, MessageData> eldest) {
          return size() > MAX_MESSAGES;
        }
      };
  private static final LinkedHashMap<String, Pending> pending = new LinkedHashMap<>();

  private NotificationMediaCaptureStore() {}

  static void capture(String messageId, int type, String text, String parameter) {
    if (!hasText(messageId)) return;
    Runnable action = null;
    synchronized (lock) {
      pruneLocked();
      messages.put(
          messageId, new MessageData(type, text, parameter, SystemClock.elapsedRealtime()));
      Pending matched = pending.remove(messageId);
      if (matched != null) action = matched.onCaptured;
    }
    if (action != null) action.run();
  }

  static MessageData take(String messageId) {
    if (!hasText(messageId)) return null;
    synchronized (lock) {
      pruneLocked();
      return messages.remove(messageId);
    }
  }

  static void register(String messageId, Runnable onCaptured) {
    if (!hasText(messageId) || onCaptured == null) return;
    Runnable runNow = null;
    synchronized (lock) {
      pruneLocked();
      if (messages.containsKey(messageId)) {
        runNow = onCaptured;
      } else {
        pending.put(messageId, new Pending(SystemClock.elapsedRealtime(), onCaptured));
        while (pending.size() > MAX_PENDING) {
          Iterator<String> iterator = pending.keySet().iterator();
          if (!iterator.hasNext()) break;
          iterator.next();
          iterator.remove();
        }
      }
    }
    if (runNow != null) runNow.run();
  }

  private static void pruneLocked() {
    long now = SystemClock.elapsedRealtime();
    Iterator<Map.Entry<String, MessageData>> messageIterator = messages.entrySet().iterator();
    while (messageIterator.hasNext()) {
      if (now - messageIterator.next().getValue().capturedAtMs > MESSAGE_TTL_MS) {
        messageIterator.remove();
      }
    }
    Iterator<Map.Entry<String, Pending>> pendingIterator = pending.entrySet().iterator();
    while (pendingIterator.hasNext()) {
      if (now - pendingIterator.next().getValue().createdAtMs > PENDING_TTL_MS) {
        pendingIterator.remove();
      }
    }
  }

  private static boolean hasText(String value) {
    return value != null && !value.isEmpty();
  }

  static final class MessageData {
    final int type;
    final String text;
    final String parameter;
    final long capturedAtMs;

    MessageData(int type, String text, String parameter, long capturedAtMs) {
      this.type = type;
      this.text = text;
      this.parameter = parameter;
      this.capturedAtMs = capturedAtMs;
    }

    boolean isImage() {
      return type == TYPE_IMAGE;
    }

    boolean isSticker() {
      return type == TYPE_STICKER;
    }

    boolean isSticon() {
      return type == TYPE_STICON;
    }

    boolean isVideo() {
      return type == TYPE_VIDEO;
    }
  }

  private static final class Pending {
    final long createdAtMs;
    final Runnable onCaptured;

    Pending(long createdAtMs, Runnable onCaptured) {
      this.createdAtMs = createdAtMs;
      this.onCaptured = onCaptured;
    }
  }
}
