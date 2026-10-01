package app.zipper.knot.hooks;

import android.os.SystemClock;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

final class NotificationMediaThumbnailKeyStore {
  private static final int MAX_ENTRIES = 64;
  private static final long ENTRY_TTL_MS = TimeUnit.SECONDS.toMillis(30);
  private static final Object lock = new Object();
  private static final LinkedHashMap<String, Entry> entries = new LinkedHashMap<>();

  private NotificationMediaThumbnailKeyStore() {}

  static void captureKey(
      String messageId,
      String chatId,
      long localMessageId,
      String obsPopInfo,
      Object obsEncryptionData,
      boolean isSquare) {
    if (!hasText(messageId) || !hasText(chatId)) return;
    Runnable action = null;
    synchronized (lock) {
      pruneLocked();
      Entry entry = entries.get(messageId);
      if (entry == null) {
        entry = new Entry(SystemClock.elapsedRealtime());
        entries.put(messageId, entry);
      }
      entry.key =
          new Key(chatId, messageId, localMessageId, obsPopInfo, obsEncryptionData, isSquare);
      entry.updatedAtMs = SystemClock.elapsedRealtime();
      action = entry.onKeyAvailable;
      entry.onKeyAvailable = null;
      trimLocked();
    }
    if (action != null) action.run();
  }

  static Key getKey(String messageId) {
    if (!hasText(messageId)) return null;
    synchronized (lock) {
      pruneLocked();
      Entry entry = entries.get(messageId);
      return entry == null ? null : entry.key;
    }
  }

  static void registerKey(String messageId, Runnable action) {
    if (!hasText(messageId) || action == null) return;
    boolean runNow = false;
    synchronized (lock) {
      pruneLocked();
      Entry entry = entries.get(messageId);
      if (entry == null) {
        entry = new Entry(SystemClock.elapsedRealtime());
        entries.put(messageId, entry);
      }
      if (entry.key != null) runNow = true;
      else entry.onKeyAvailable = action;
      trimLocked();
    }
    if (runNow) action.run();
  }

  private static void trimLocked() {
    while (entries.size() > MAX_ENTRIES) {
      Iterator<Map.Entry<String, Entry>> iterator = entries.entrySet().iterator();
      if (!iterator.hasNext()) break;
      iterator.next();
      iterator.remove();
    }
  }

  private static void pruneLocked() {
    long now = SystemClock.elapsedRealtime();
    Iterator<Map.Entry<String, Entry>> iterator = entries.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, Entry> item = iterator.next();
      if (now - item.getValue().updatedAtMs > ENTRY_TTL_MS) iterator.remove();
    }
  }

  private static boolean hasText(String value) {
    return value != null && !value.isEmpty();
  }

  static final class Key {
    final String chatId;
    final String serverMessageId;
    final long localMessageId;
    final String obsPopInfo;
    final Object obsEncryptionData;
    final boolean isSquare;

    Key(
        String chatId,
        String serverMessageId,
        long localMessageId,
        String obsPopInfo,
        Object obsEncryptionData,
        boolean isSquare) {
      this.chatId = chatId;
      this.serverMessageId = serverMessageId;
      this.localMessageId = localMessageId;
      this.obsPopInfo = obsPopInfo;
      this.obsEncryptionData = obsEncryptionData;
      this.isSquare = isSquare;
    }
  }

  private static final class Entry {
    Key key;
    long updatedAtMs;
    Runnable onKeyAvailable;

    Entry(long updatedAtMs) {
      this.updatedAtMs = updatedAtMs;
    }
  }
}
