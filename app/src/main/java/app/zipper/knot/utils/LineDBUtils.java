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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LineDBUtils {

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
        SQLiteDatabase db =
            SQLiteDatabase.openDatabase(
                dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        Cursor cursor =
            db.rawQuery(
                "SELECT chat_name FROM chat WHERE chat_id = ? LIMIT 1", new String[] {chatId});
        if (cursor.moveToFirst()) {
          String name = cursor.getString(0);
          cursor.close();
          db.close();
          return name;
        }
        cursor.close();
        db.close();
      }
    } catch (Throwable ignored) {
    }
    return null;
  }

  public static String resolveMessageContent(String serverId) {
    if (serverId == null) return null;
    try {
      Context context = Knot.currentApplication();
      if (context == null) return null;
      File dbFile = context.getDatabasePath("naver_line");
      if (dbFile.exists()) {
        SQLiteDatabase db =
            SQLiteDatabase.openDatabase(
                dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        Cursor cursor =
            db.rawQuery(
                "SELECT content, parameter FROM chat_history WHERE server_id = ?",
                new String[] {serverId});
        if (cursor.moveToFirst()) {
          String content = cursor.getString(0);
          String parameter = cursor.getString(1);
          cursor.close();
          db.close();
          return resolveMessageText(content, parameter);
        }
        cursor.close();
        db.close();
      }
    } catch (Throwable ignored) {
    }
    return null;
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

  public static String resolveMessageText(String content, String parameter) {
    if (content != null && !content.isEmpty() && !"null".equals(content)) {
      return content;
    }

    if (parameter != null) {
      if (parameter.contains("STKPKGID")) {
        return ModuleResources.get(R.string.msg_sticker);
      } else if (parameter.contains("IMAGE") || parameter.contains("image")) {
        return ModuleResources.get(R.string.msg_image);
      } else if (parameter.contains("VIDEO") || parameter.contains("video")) {
        return ModuleResources.get(R.string.msg_video);
      } else if (parameter.contains("FILE") || parameter.contains("file")) {
        return ModuleResources.get(R.string.msg_file);
      } else if (parameter.contains("LOCATION") || parameter.contains("location")) {
        return ModuleResources.get(R.string.msg_location);
      }
    }
    return null;
  }

  public static String getMyMid() {
    try {
      Context context = Knot.currentApplication();
      if (context == null) return null;

      LineVersion.Config cfg = LineVersion.get();
      if (cfg == null || cfg.profile.g50fClass.isEmpty()) return null;

      try {
        ClassLoader cl = context.getClassLoader();
        Class<?> g50f = cl.loadClass(cfg.profile.g50fClass);
        Class<?> h13ba = cl.loadClass(cfg.profile.h13baClass);

        java.lang.reflect.Field h3Field = h13ba.getDeclaredField(cfg.profile.fieldH3);
        h3Field.setAccessible(true);
        Object h3 = h3Field.get(null);

        if (h3 != null) {
          java.lang.reflect.Method aMethod =
              g50f.getMethod("a", Context.class, cl.loadClass(cfg.profile.g50aClass));
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
              if (mid != null && mid.startsWith("u")) return mid;
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
              "SELECT server_id, content, parameter, created_time FROM chat_history"
                  + " WHERE chat_id = ? AND from_mid = ? ORDER BY created_time DESC LIMIT 200";
          args = new String[] {chatId, senderMid};
        } else {
          sql =
              "SELECT server_id, content, parameter, created_time FROM chat_history"
                  + " WHERE chat_id = ? AND from_mid = ? AND content LIKE ?"
                  + " ORDER BY created_time DESC LIMIT 100";
          args = new String[] {chatId, senderMid, "%" + keyword + "%"};
        }
        Cursor cursor = db.rawQuery(sql, args);
        try {
          while (cursor.moveToNext()) {
            String text = resolveMessageText(cursor.getString(1), cursor.getString(2));
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
        String currentChatName = resolveChatName(targetChatId);
        SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        String sql =
            "SELECT server_id, content, parameter, from_mid, created_time "
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
          while (cursor.moveToNext()) {
            String mId = cursor.getString(0);
            String rawContent = cursor.getString(1);
            String rawParam = cursor.getString(2);
            String fromMid = cursor.getString(3);
            long timeLong = cursor.getLong(4);

            if (fromMid == null) fromMid = myMid;
            if (!fromMid.equals(myMid)) continue;

            String resolvedText = resolveMessageText(rawContent, rawParam);
            String senderName = resolveMemberName(fromMid);
            String formattedTime = dateFormat.format(new Date(timeLong));

            results.add(
                new MessageRecord(
                    mId,
                    resolvedText != null ? resolvedText : "",
                    fromMid,
                    senderName != null ? senderName : "Unknown",
                    targetChatId,
                    currentChatName != null ? currentChatName : "Unknown",
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
