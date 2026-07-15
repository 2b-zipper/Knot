package app.zipper.knot.hooks;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Reflect;
import app.zipper.knot.utils.LineDBUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SearchMin1CharHook implements BaseHook {

  private static boolean isIgnoredKeywordCodePoint(int codePoint) {
    return Character.isWhitespace(codePoint)
        || Character.isSpaceChar(codePoint)
        || Character.isISOControl(codePoint)
        || Character.getType(codePoint) == Character.FORMAT;
  }

  private static boolean hasMeaningfulKeyword(String str) {
    if (str == null) return false;

    for (int i = 0; i < str.length(); ) {
      int codePoint = str.codePointAt(i);
      i += Character.charCount(codePoint);

      if (isIgnoredKeywordCodePoint(codePoint)) continue;
      return true;
    }
    return false;
  }

  private static String normalizeOneCharacterKeyword(String str) {
    if (str == null) return null;

    StringBuilder normalized = new StringBuilder();
    int meaningfulCodePoints = 0;
    for (int i = 0; i < str.length(); ) {
      int codePoint = str.codePointAt(i);
      i += Character.charCount(codePoint);

      if (isIgnoredKeywordCodePoint(codePoint)) continue;
      meaningfulCodePoints++;
      if (meaningfulCodePoints > 1) return null;
      normalized.appendCodePoint(codePoint);
    }
    return meaningfulCodePoints == 1 ? normalized.toString() : null;
  }

  @Override
  public void hook(KnotConfig options, LoadParam lpparam) throws Throwable {
    if (!options.searchMin1Char.enabled) return;

    LineVersion.Config config = LineVersion.get();
    if (config == null
        || config.chat.searchKeywordTypeClass.isEmpty()
        || config.chat.searchKeywordTypeMethod.isEmpty()) return;

    try {
      Knot.module
          .hook(
              Reflect.findMethodExact(
                  config.chat.searchKeywordTypeClass,
                  lpparam.classLoader,
                  config.chat.searchKeywordTypeMethod,
                  String.class))
          .intercept(
              chain ->
                  hasMeaningfulKeyword((String) chain.getArg(0)) ? Boolean.TRUE : chain.proceed());
    } catch (Throwable t) {
      Knot.log("Knot: SearchMin1CharHook error: " + t);
    }

    if (!config.chat.searchResultClass.isEmpty())
      hookOneCharacterResults(config, lpparam.classLoader);
  }

  private void hookOneCharacterResults(LineVersion.Config config, ClassLoader classLoader) {
    try {
      Knot.module
          .hook(
              Reflect.findConstructorExact(
                  config.chat.searchResultClass,
                  classLoader,
                  String.class,
                  int.class,
                  String.class,
                  List.class))
          .intercept(
              chain -> {
                Object[] args = chain.getArgs().toArray();
                replaceOneCharacterResults(args);
                return chain.proceed(args);
              });
    } catch (Throwable t) {
      Knot.log("Knot: SearchMin1CharHook result hook error: " + t);
    }
  }

  private static void replaceOneCharacterResults(Object[] args) {
    try {
      String chatId = (String) args[0];
      String keyword = normalizeOneCharacterKeyword((String) args[2]);
      if (chatId == null || keyword == null) return;

      List<Long> localIds = fetchExactOneCharacterLocalIds(chatId, keyword);
      if (localIds == null) return;

      args[1] = localIds.size();
      args[3] = localIds;
    } catch (Throwable t) {
      Knot.log("Knot: SearchMin1CharHook replace results error: " + t);
    }
  }

  private static List<Long> fetchExactOneCharacterLocalIds(String chatId, String keyword) {
    try {
      Context context = Knot.currentApplication();
      if (context == null) return null;

      File dbFile = context.getDatabasePath("naver_line");
      if (!dbFile.exists()) return null;

      String senderMid = SearchByMemberHook.getActiveFilterMid(chatId);
      StringBuilder sql =
          new StringBuilder(
              "SELECT id FROM chat_history"
                  + " WHERE chat_id = ?"
                  + " AND content IS NOT NULL"
                  + " AND content = ?");
      List<String> args = new ArrayList<>();
      args.add(chatId);
      args.add(keyword);

      if (senderMid != null && !senderMid.isEmpty()) {
        String myMid = LineDBUtils.getMyMid();
        if (senderMid.equals(myMid)) {
          sql.append(" AND (from_mid = ? OR from_mid IS NULL)");
        } else {
          sql.append(" AND from_mid = ?");
        }
        args.add(senderMid);
      }

      sql.append(" ORDER BY id DESC");

      try (SQLiteDatabase db =
              SQLiteDatabase.openDatabase(
                  dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
          Cursor cursor = db.rawQuery(sql.toString(), args.toArray(new String[0]))) {
        List<Long> result = new ArrayList<>();
        while (cursor.moveToNext()) result.add(cursor.getLong(0));
        return result;
      }
    } catch (Throwable t) {
      Knot.log("Knot: SearchMin1CharHook fetch local ids error: " + t);
      return null;
    }
  }
}
