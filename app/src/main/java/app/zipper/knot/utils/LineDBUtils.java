package app.zipper.knot.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import app.zipper.knot.Knot;
import app.zipper.knot.LineVersion;
import app.zipper.knot.R;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class LineDBUtils {

  private static final int SQLITE_IN_BATCH_SIZE = 500;
  private static final int ATTACHMENT_IMAGE = 1;
  private static final int ATTACHMENT_VIDEO = 2;
  private static final int ATTACHMENT_AUDIO = 3;
  private static final int ATTACHMENT_STICKER = 7;
  private static final int ATTACHMENT_CONTACT = 13;
  private static final int ATTACHMENT_FILE = 14;
  private static final int ATTACHMENT_LOCATION = 15;
  private static final int ATTACHMENT_EXTIMAGE = 21;

  private static volatile String cachedMyMid;

  public static String resolveMemberName(String mid) {
    if (mid == null) return null;
    try {
      Context context = Knot.currentApplication();
      if (context == null) return null;
      File dbFile = context.getDatabasePath("contact");
      if (dbFile.exists()) {
        SQLiteDatabase db =
            SQLiteDatabase.openDatabase(
                dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        Cursor cursor =
            db.rawQuery(
                "SELECT coalesce(overridden_name, address_book_name, profile_name) FROM contacts WHERE mid = ?",
                new String[] {mid});
        if (cursor.moveToFirst()) {
          String name = cursor.getString(0);
          cursor.close();
          db.close();
          return name;
        }
        cursor.close();
        db.close();
      }
    } catch (Throwable t) {
      Knot.log("Knot: Member name resolution failed: " + t);
    }
    return null;
  }

  public static class ContactTimes {
    public Long friendCreated;
    public Long favorite;
    public Long profileUpdated;

    public boolean isEmpty() {
      return friendCreated == null && favorite == null && profileUpdated == null;
    }
  }

  public static ContactTimes getContactTimes(String mid) {
    if (mid == null) return null;
    try {
      Context context = Knot.currentApplication();
      if (context == null) return null;
      File dbFile = context.getDatabasePath("contact");
      if (!dbFile.exists()) return null;
      SQLiteDatabase db =
          SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
      try {
        Cursor cursor =
            db.rawQuery(
                "SELECT friend_created_time_millis, favorite_time_millis,"
                    + " profile_updated_time_millis FROM contacts WHERE mid = ?",
                new String[] {mid});
        try {
          if (!cursor.moveToFirst()) return null;
          ContactTimes t = new ContactTimes();
          t.friendCreated = getNullableLong(cursor, 0);
          t.favorite = getNullableLong(cursor, 1);
          t.profileUpdated = getNullableLong(cursor, 2);
          return t;
        } finally {
          cursor.close();
        }
      } finally {
        db.close();
      }
    } catch (Throwable t) {
      Knot.log("Knot: getContactTimes failed: " + t);
    }
    return null;
  }

  private static Long getNullableLong(Cursor cursor, int index) {
    if (cursor.isNull(index)) return null;
    long value = cursor.getLong(index);
    return value > 0 ? value : null;
  }

  public static String resolveChatName(String chatId) {
    if (chatId == null) return null;
    try {
      Context context = Knot.currentApplication();
      if (context == null) return null;
      File dbFile = context.getDatabasePath("naver_line");
      if (dbFile.exists()) {
        try (SQLiteDatabase db =
            SQLiteDatabase.openDatabase(
                dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY)) {
          return resolveChatName(db, chatId);
        }
      }
    } catch (Throwable ignored) {
    }
    return null;
  }

  private static String resolveChatName(SQLiteDatabase db, String chatId) {
    if (db == null || chatId == null) return null;
    try (Cursor cursor =
        db.rawQuery(
            "SELECT chat_name FROM chat WHERE chat_id = ? LIMIT 1", new String[] {chatId})) {
      if (cursor.moveToFirst()) return cursor.getString(0);
    } catch (Throwable ignored) {
    }
    return null;
  }

  public static boolean isChatArchived(String chatId) {
    if (chatId == null) return false;
    Context context = Knot.currentApplication();
    if (context == null) return false;
    Boolean archived = queryArchived(context, "naver_line", "chat", "chat_id", chatId);
    if (archived == null) archived = queryOpenChatArchived(context, chatId);
    return archived != null && archived;
  }

  private static Boolean queryOpenChatArchived(Context context, String chatMid) {
    return queryArchived(context, "square", "square_chat", "chat_mid", chatMid);
  }

  public static Set<String> getArchivedChatIds() {
    Set<String> ids = new HashSet<>();
    Context context = Knot.currentApplication();
    if (context == null) return ids;
    addArchivedIds(context, "naver_line", "chat", "chat_id", ids);
    addArchivedIds(context, "square", "square_chat", "chat_mid", ids);
    return ids;
  }

  private static void addArchivedIds(
      Context context, String dbName, String table, String idColumn, Set<String> ids) {
    try {
      File dbFile = context.getDatabasePath(dbName);
      if (!dbFile.exists()) return;
      SQLiteDatabase db =
          SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
      try {
        Cursor cursor =
            db.rawQuery("SELECT " + idColumn + " FROM " + table + " WHERE is_archived = 1", null);
        try {
          while (cursor.moveToNext()) ids.add(cursor.getString(0));
        } finally {
          cursor.close();
        }
      } finally {
        db.close();
      }
    } catch (Throwable t) {
      Knot.log("Knot: getArchivedChatIds failed (" + dbName + "): " + t);
    }
  }

  private static Boolean queryArchived(
      Context context, String dbName, String table, String idColumn, String chatId) {
    try {
      File dbFile = context.getDatabasePath(dbName);
      if (!dbFile.exists()) return null;
      SQLiteDatabase db =
          SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
      try {
        Cursor cursor =
            db.rawQuery(
                "SELECT is_archived FROM " + table + " WHERE " + idColumn + " = ? LIMIT 1",
                new String[] {chatId});
        try {
          // OpenChat also uses 2 (DELETED_ARCHIVED) and 3 (CREATED_ARCHIVED)
          return cursor.moveToFirst() ? cursor.getInt(0) == 1 : null;
        } finally {
          cursor.close();
        }
      } finally {
        db.close();
      }
    } catch (Throwable t) {
      Knot.log("Knot: queryArchived failed (" + dbName + "): " + t);
    }
    return null;
  }

  public static String resolveMessageContent(String serverId) {
    if (serverId == null || serverId.isEmpty()) return null;
    return resolveMessageContents(Collections.singletonList(serverId)).get(serverId);
  }

  public static String resolveMessageContentByLocalId(long localId) {
    try (SQLiteDatabase db = openReadOnlyDatabase("naver_line")) {
      if (db == null) return null;
      try (Cursor cursor =
          db.rawQuery(
              "SELECT content, parameter, attachement_type FROM chat_history WHERE id = ? LIMIT 1",
              new String[] {String.valueOf(localId)})) {
        if (cursor.moveToFirst()) return resolveMessageText(cursor, 0, 1, 2);
      }
    } catch (Throwable ignored) {
    }
    return null;
  }

  public static Map<String, String> resolveMessageContents(Collection<String> serverIds) {
    Map<String, String> results = new HashMap<>();
    List<String> ids = collectNonEmptyIds(serverIds);
    if (ids.isEmpty()) return results;
    try (SQLiteDatabase db = openReadOnlyDatabase("naver_line")) {
      if (db == null) return results;
      for (int start = 0; start < ids.size(); start += SQLITE_IN_BATCH_SIZE) {
        List<String> batch = ids.subList(start, Math.min(start + SQLITE_IN_BATCH_SIZE, ids.size()));
        String placeholders = String.join(",", Collections.nCopies(batch.size(), "?"));
        String sql =
            "SELECT server_id, content, parameter, attachement_type FROM chat_history"
                + " WHERE server_id IN ("
                + placeholders
                + ")";
        try (Cursor cursor = db.rawQuery(sql, batch.toArray(new String[0]))) {
          while (cursor.moveToNext()) {
            String serverId = cursor.getString(0);
            String text = resolveMessageText(cursor, 1, 2, 3);
            if (serverId != null && text != null && !text.isEmpty()) {
              results.put(serverId, text);
            }
          }
        }
      }
    } catch (Throwable ignored) {
    }
    return results;
  }

  private static SQLiteDatabase openReadOnlyDatabase(String dbName) {
    Context context = Knot.currentApplication();
    if (context == null) return null;
    File dbFile = context.getDatabasePath(dbName);
    if (!dbFile.exists()) return null;
    return SQLiteDatabase.openDatabase(
        dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
  }

  private static List<String> collectNonEmptyIds(Collection<String> serverIds) {
    if (serverIds == null || serverIds.isEmpty()) return Collections.emptyList();
    Set<String> unique = new LinkedHashSet<>();
    for (String id : serverIds) {
      if (id != null && !id.isEmpty()) unique.add(id);
    }
    return unique.isEmpty() ? Collections.emptyList() : new ArrayList<>(unique);
  }

  public static long resolveMessageCreatedTime(String serverId) {
    if (serverId == null) return 0L;
    try {
      Context context = Knot.currentApplication();
      if (context == null) return 0L;
      File dbFile = context.getDatabasePath("naver_line");
      if (dbFile.exists()) {
        try (SQLiteDatabase db =
                SQLiteDatabase.openDatabase(
                    dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            Cursor cursor =
                db.rawQuery(
                    "SELECT created_time FROM chat_history WHERE server_id = ? LIMIT 1",
                    new String[] {serverId})) {
          if (cursor.moveToFirst()) return cursor.getLong(0);
        }
      }
    } catch (Throwable ignored) {
    }
    return 0L;
  }

  public static long resolveLocalMessageId(String serverId) {
    if (serverId == null) return -1L;
    try {
      Context context = Knot.currentApplication();
      if (context == null) return -1L;
      File dbFile = context.getDatabasePath("naver_line");
      if (dbFile.exists()) {
        SQLiteDatabase db =
            SQLiteDatabase.openDatabase(
                dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        Cursor cursor =
            db.rawQuery("SELECT id FROM chat_history WHERE server_id = ?", new String[] {serverId});
        long localId = -1L;
        if (cursor.moveToFirst()) {
          localId = cursor.getLong(0);
        }
        cursor.close();
        db.close();
        return localId;
      }
    } catch (Throwable ignored) {
    }
    return -1L;
  }

  public static String resolveChatIdByServerId(String serverId) {
    if (serverId == null) return null;
    try {
      Context context = Knot.currentApplication();
      if (context == null) return null;
      File dbFile = context.getDatabasePath("naver_line");
      if (!dbFile.exists()) return null;
      SQLiteDatabase db =
          SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
      try {
        Cursor cursor =
            db.rawQuery(
                "SELECT chat_id FROM chat_history WHERE server_id = ? LIMIT 1",
                new String[] {serverId});
        try {
          return cursor.moveToFirst() ? cursor.getString(0) : null;
        } finally {
          cursor.close();
        }
      } finally {
        db.close();
      }
    } catch (Throwable t) {
      Knot.log("Knot: resolveChatIdByServerId failed: " + t);
    }
    return null;
  }

  public static boolean isOwnMessage(String serverId) {
    if (serverId == null) return false;
    try {
      Context context = Knot.currentApplication();
      if (context == null) return false;
      String myMid = getMyMid();
      File dbFile = context.getDatabasePath("naver_line");
      if (!dbFile.exists()) return false;
      SQLiteDatabase db =
          SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
      try {
        Cursor cursor =
            db.rawQuery(
                "SELECT from_mid FROM chat_history WHERE server_id = ? LIMIT 1",
                new String[] {serverId});
        try {
          // Messages you sent yourself have a NULL from_mid
          if (!cursor.moveToFirst()) return false;
          if (cursor.isNull(0)) return true;
          String fromMid = cursor.getString(0);
          return fromMid.isEmpty() || fromMid.equals(myMid);
        } finally {
          cursor.close();
        }
      } finally {
        db.close();
      }
    } catch (Throwable t) {
      Knot.log("Knot: isOwnMessage failed: " + t);
    }
    return false;
  }

  private static String resolveMessageText(
      Cursor cursor, int contentCol, int paramCol, int attachmentTypeCol) {
    int attachmentType = cursor.isNull(attachmentTypeCol) ? 0 : cursor.getInt(attachmentTypeCol);
    return resolveMessageText(
        cursor.getString(contentCol), cursor.getString(paramCol), attachmentType);
  }

  public static String resolveMessageText(String content, String parameter) {
    return resolveMessageText(content, parameter, 0);
  }

  public static String resolveMessageText(String content, String parameter, int attachmentType) {
    if (content != null && !content.isEmpty() && !"null".equals(content)) {
      return content;
    }
    String byType = resolveAttachmentLabel(attachmentType, parameter);
    if (byType != null) return byType;
    return resolveParameterLabel(parameter);
  }

  private static String resolveAttachmentLabel(int attachmentType, String parameter) {
    switch (attachmentType) {
      case ATTACHMENT_IMAGE:
      case ATTACHMENT_EXTIMAGE:
        return ModuleResources.get(R.string.msg_image);
      case ATTACHMENT_VIDEO:
        return ModuleResources.get(R.string.msg_video);
      case ATTACHMENT_AUDIO:
        return ModuleResources.get(R.string.msg_audio);
      case ATTACHMENT_STICKER:
        return parameter != null && parameter.contains("CSSTKID")
            ? ModuleResources.get(R.string.msg_combination_sticker)
            : ModuleResources.get(R.string.msg_sticker);
      case ATTACHMENT_CONTACT:
        return ModuleResources.get(R.string.msg_contact);
      case ATTACHMENT_FILE:
        return ModuleResources.get(R.string.msg_file);
      case ATTACHMENT_LOCATION:
        return ModuleResources.get(R.string.msg_location);
      default:
        return null;
    }
  }

  private static String resolveParameterLabel(String parameter) {
    if (parameter == null) return null;
    if (parameter.contains("CSSTKID")) {
      return ModuleResources.get(R.string.msg_combination_sticker);
    }
    if (parameter.contains("STKPKGID") || parameter.contains("STKID")) {
      return ModuleResources.get(R.string.msg_sticker);
    }
    if (parameter.contains("IMAGE") || parameter.contains("image")) {
      return ModuleResources.get(R.string.msg_image);
    }
    if (parameter.contains("VIDEO") || parameter.contains("video")) {
      return ModuleResources.get(R.string.msg_video);
    }
    if (parameter.contains("FILE") || parameter.contains("file")) {
      return ModuleResources.get(R.string.msg_file);
    }
    if (parameter.contains("LOCATION") || parameter.contains("location")) {
      return ModuleResources.get(R.string.msg_location);
    }
    return extractParameterValue(parameter, "ALT_TEXT");
  }

  private static String extractParameterValue(String parameter, String key) {
    if (parameter == null || parameter.isEmpty()) return null;
    String[] parts = parameter.split("\t");
    for (int i = 0; i + 1 < parts.length; i += 2) {
      if (key.equals(parts[i]) && !parts[i + 1].isEmpty()) {
        return parts[i + 1];
      }
    }
    return null;
  }

  public static String getMyMid() {
    if (cachedMyMid != null) return cachedMyMid;
    try {
      Context context = Knot.currentApplication();
      if (context == null) return null;

      LineVersion.Config cfg = LineVersion.get();
      if (cfg == null || cfg.services.locatorClass.isEmpty()) return null;

      try {
        ClassLoader cl = context.getClassLoader();
        Class<?> locator = cl.loadClass(cfg.services.locatorClass);
        Class<?> h13ba = cl.loadClass(cfg.profile.h13baClass);

        java.lang.reflect.Field h3Field = h13ba.getDeclaredField(cfg.profile.fieldH3);
        h3Field.setAccessible(true);
        Object h3 = h3Field.get(null);

        if (h3 != null) {
          java.lang.reflect.Method aMethod =
              locator.getMethod(
                  cfg.services.methodLocate, Context.class, cl.loadClass(cfg.services.keyClass));
          Object profileManager = aMethod.invoke(null, context, h3);

          if (profileManager != null) {
            Object profile =
                profileManager
                    .getClass()
                    .getMethod(cfg.profile.methodGetProfile)
                    .invoke(profileManager);
            if (profile != null) {
              java.lang.reflect.Field midField =
                  profile.getClass().getDeclaredField(cfg.profile.fieldMid);
              midField.setAccessible(true);
              String mid = (String) midField.get(profile);
              if (mid != null && mid.startsWith("u")) {
                cachedMyMid = mid;
                return mid;
              }
            }
          }
        }
      } catch (Throwable ignored) {
      }
    } catch (Throwable t) {
      Knot.log("Knot: Error in getMyMid: " + t.getMessage());
    }
    return null;
  }

  public static class MemberInfo {
    public final String mid;
    public final String name;

    public MemberInfo(String mid, String name) {
      this.mid = mid;
      this.name = name;
    }
  }

  public static List<MessageRecord> searchMessagesByMember(
      String chatId, String senderMid, String keyword) {
    List<MessageRecord> results = new ArrayList<>();
    try {
      Context context = Knot.currentApplication();
      if (context == null) return results;
      File dbFile = context.getDatabasePath("naver_line");
      if (!dbFile.exists()) return results;
      SQLiteDatabase db =
          SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
      try {
        SimpleDateFormat fmt = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
        String sql;
        String[] args;
        if (keyword == null || keyword.isEmpty()) {
          sql =
              "SELECT server_id, content, parameter, created_time, attachement_type"
                  + " FROM chat_history WHERE chat_id = ? AND from_mid = ?"
                  + " ORDER BY created_time DESC LIMIT 200";
          args = new String[] {chatId, senderMid};
        } else {
          sql =
              "SELECT server_id, content, parameter, created_time, attachement_type"
                  + " FROM chat_history WHERE chat_id = ? AND from_mid = ? AND content LIKE ?"
                  + " ORDER BY created_time DESC LIMIT 100";
          args = new String[] {chatId, senderMid, "%" + keyword + "%"};
        }
        Cursor cursor = db.rawQuery(sql, args);
        try {
          while (cursor.moveToNext()) {
            String text = resolveMessageText(cursor, 1, 2, 4);
            if (text == null || text.isEmpty()) continue;
            String ts = fmt.format(new Date(cursor.getLong(3)));
            results.add(
                new MessageRecord(cursor.getString(0), text, senderMid, null, chatId, null, ts));
          }
        } finally {
          cursor.close();
        }
      } finally {
        db.close();
      }
    } catch (Throwable t) {
      Knot.log("Knot: searchMessagesByMember failed: " + t);
    }
    return results;
  }

  public static List<MemberInfo> getChatMembers(String chatId) {
    List<MemberInfo> results = new ArrayList<>();
    if (chatId == null) return results;
    try {
      Context context = Knot.currentApplication();
      if (context == null) return results;
      File dbFile = context.getDatabasePath("naver_line");
      if (!dbFile.exists()) return results;
      SQLiteDatabase db =
          SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
      try {
        Cursor cursor =
            db.rawQuery(
                "SELECT from_mid FROM chat_history"
                    + " WHERE chat_id = ? AND from_mid IS NOT NULL"
                    + " GROUP BY from_mid ORDER BY MAX(created_time) DESC",
                new String[] {chatId});
        try {
          while (cursor.moveToNext()) {
            String mid = cursor.getString(0);
            String name = resolveMemberName(mid);
            results.add(new MemberInfo(mid, name != null ? name : mid));
          }
        } finally {
          cursor.close();
        }
        String myMid = getMyMid();
        if (myMid != null) {
          boolean alreadyPresent = false;
          for (MemberInfo mi : results) {
            if (myMid.equals(mi.mid)) {
              alreadyPresent = true;
              break;
            }
          }
          if (!alreadyPresent) {
            Cursor selfCursor =
                db.rawQuery(
                    "SELECT COUNT(*) FROM chat_history WHERE chat_id = ? AND from_mid IS NULL",
                    new String[] {chatId});
            try {
              if (selfCursor.moveToFirst() && selfCursor.getLong(0) > 0) {
                String myName = resolveMemberName(myMid);
                results.add(0, new MemberInfo(myMid, myName != null ? myName : myMid));
              }
            } finally {
              selfCursor.close();
            }
          }
        }
      } finally {
        db.close();
      }
    } catch (Throwable t) {
      Knot.log("Knot: getChatMembers failed: " + t);
    }
    return results;
  }

  public static class MessageRecord {
    public final String id;
    public final String text;
    public final String senderMid;
    public final String senderName;
    public final String chatId;
    public final String chatName;
    public final String timestamp;

    public MessageRecord(
        String id,
        String text,
        String senderMid,
        String senderName,
        String chatId,
        String chatName,
        String timestamp) {
      this.id = id;
      this.text = text;
      this.senderMid = senderMid;
      this.senderName = senderName;
      this.chatId = chatId;
      this.chatName = chatName;
      this.timestamp = timestamp;
    }
  }

  public static List<MessageRecord> fetchMyMessagesUpTo(
      String targetChatId, long minExclusiveId, long maxInclusiveId, String myMid) {
    List<MessageRecord> results = new ArrayList<>();
    if (targetChatId == null || myMid == null) return results;
    if (maxInclusiveId <= minExclusiveId) return results;

    try {
      Context context = Knot.currentApplication();
      if (context == null) return results;

      File dbFile = context.getDatabasePath("naver_line");
      if (!dbFile.exists()) return results;

      SQLiteDatabase db =
          SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);

      try {
        String sql =
            "SELECT server_id, content, parameter, from_mid, created_time, attachement_type "
                + "FROM chat_history WHERE chat_id = ? "
                + "AND CAST(server_id AS INTEGER) > ? "
                + "AND CAST(server_id AS INTEGER) <= ? "
                + "AND (from_mid = ? OR from_mid IS NULL) "
                + "ORDER BY CAST(server_id AS INTEGER) DESC";

        String[] args =
            new String[] {
              targetChatId, String.valueOf(minExclusiveId), String.valueOf(maxInclusiveId), myMid
            };

        Cursor cursor = db.rawQuery(sql, args);
        try {
          String currentChatName = null;
          String senderName = null;
          SimpleDateFormat dateFormat = null;

          while (cursor.moveToNext()) {
            String mId = cursor.getString(0);
            String fromMid = cursor.getString(3);
            long timeLong = cursor.getLong(4);

            if (fromMid == null) fromMid = myMid;
            if (!fromMid.equals(myMid)) continue;

            if (dateFormat == null) {
              dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
              String resolvedChatName = resolveChatName(db, targetChatId);
              currentChatName = resolvedChatName != null ? resolvedChatName : "Unknown";
              String resolvedSenderName = resolveMemberName(myMid);
              senderName = resolvedSenderName != null ? resolvedSenderName : "Unknown";
            }

            String resolvedText = resolveMessageText(cursor, 1, 2, 5);
            String formattedTime = dateFormat.format(new Date(timeLong));

            results.add(
                new MessageRecord(
                    mId,
                    resolvedText != null ? resolvedText : "",
                    fromMid,
                    senderName,
                    targetChatId,
                    currentChatName,
                    formattedTime));
          }
        } finally {
          cursor.close();
        }
      } finally {
        db.close();
      }
    } catch (Throwable t) {
      Knot.log("Knot: Message resolution error: " + t.getMessage());
    }
    return results;
  }
}
