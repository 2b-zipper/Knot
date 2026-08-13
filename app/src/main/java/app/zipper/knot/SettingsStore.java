package app.zipper.knot;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import org.json.JSONException;
import org.json.JSONObject;

public class SettingsStore {

  private static final String SETTINGS_FILE = "knot_settings.bin";
  private static final String UNSEND_HISTORY_FILE = "knot_unsend_history.bin";
  private static final String READ_HISTORY_FILE = "knot_read_history.bin";
  private static final String EDIT_HISTORY_FILE = "knot_edit_history.bin";

  private static final byte[] UNSEND_HISTORY_MAGIC = {'K', 'U', 'H', '1'};
  private static final byte[] READ_HISTORY_MAGIC = {'K', 'R', 'H', '1'};

  private static final String READ_HISTORY_TIME_FMT = "yyyy-MM-dd HH:mm:ss";
  private static final String UNSEND_HISTORY_TIME_FMT = "yyyy/MM/dd HH:mm:ss";

  private static volatile String pointerPath = null;
  private static volatile Context appContext = null;
  private static volatile Uri cachedTreeUri = null;
  private static final Map<String, Uri> cachedDocUris = new ConcurrentHashMap<>();
  private static volatile boolean isModuleLoaded = false;
  private static volatile JSONObject cachedSettingsJson = null;

  public static void init(android.app.Activity activity) {
    appContext = activity.getApplicationContext();
    ensurePointerPath(activity);
    cachedTreeUri = null;
    cachedDocUris.clear();
    ensureTreeUri();
  }

  public static void setContext(Context context) {
    appContext = context;
    ensurePointerPath(context);
  }

  public static Context getContext() {
    return appContext;
  }

  public static boolean isLoaded() {
    return isModuleLoaded;
  }

  public static void setLoaded(boolean loaded) {
    isModuleLoaded = loaded;
  }

  public static boolean isConfigured() {
    return ensureTreeUri() != null;
  }

  public static String getSettingsDir() {
    Uri tree = ensureTreeUri();
    return tree != null ? uriToDisplayPath(tree) : null;
  }

  public static String getSettingsDirUri() {
    Uri tree = ensureTreeUri();
    return tree != null ? tree.toString() : null;
  }

  public static void setSettingsDir(String uriString) {
    ensurePointerPath(appContext);
    if (pointerPath == null) return;
    try {
      try (FileWriter w = new FileWriter(pointerPath)) {
        w.write(uriString);
      }
      cachedTreeUri = Uri.parse(uriString);
      cachedDocUris.clear();
    } catch (Throwable e) {
      android.util.Log.e("Knot", "SettingsStore.setSettingsDir failed", e);
    }
  }

  public static void load(KnotConfig config) {
    ensurePointerPath(appContext);
    try {
      JSONObject json = getSettingsJson();
      for (KnotConfig.Item item : config.items) {
        if (!json.has(item.key)) continue;
        Object val = json.get(item.key);
        if (val instanceof Boolean) item.enabled = (Boolean) val;
        else if (val instanceof String) item.value = (String) val;
      }
    } catch (Throwable ignored) {
    }
  }

  public static void save(String key, boolean value) {
    putSetting(key, value);
  }

  public static void save(String key, String value) {
    putSetting(key, value);
  }

  public static boolean get(String key, boolean defaultValue) {
    try {
      JSONObject json = getSettingsJson();
      if (json.has(key)) return json.getBoolean(key);
    } catch (Throwable ignored) {
    }
    return defaultValue;
  }

  public static String getString(String key, String defaultValue) {
    try {
      JSONObject json = readJson(SETTINGS_FILE);
      if (json.has(key)) return json.getString(key);
    } catch (Throwable ignored) {
    }
    return defaultValue;
  }

  public static JSONObject loadAll() {
    try {
      return readJson(SETTINGS_FILE);
    } catch (Throwable e) {
      return new JSONObject();
    }
  }

  public static void reset() {
    try {
      writeJson(SETTINGS_FILE, new JSONObject());
    } catch (Throwable ignored) {
    }
    try {
      ensurePointerPath(appContext);
      if (pointerPath != null) {
        File f = new File(pointerPath);
        if (f.exists()) f.delete();
      }
    } catch (Throwable ignored) {
    }
    cachedTreeUri = null;
    cachedDocUris.clear();
  }

  private static JSONObject getSettingsJson() throws Throwable {
    if (cachedSettingsJson != null) return cachedSettingsJson;
    synchronized (SettingsStore.class) {
      if (cachedSettingsJson != null) return cachedSettingsJson;
      byte[] data = readDeflated(SETTINGS_FILE);
      if (data == null) {
        return new JSONObject();
      }
      String text = new String(data, StandardCharsets.UTF_8).trim();
      cachedSettingsJson = text.isEmpty() ? new JSONObject() : new JSONObject(text);
      return cachedSettingsJson;
    }
  }

  private static void putSetting(String key, Object value) {
    try {
      synchronized (SettingsStore.class) {
        JSONObject json = getSettingsJson();
        json.put(key, value);
        writeJson(SETTINGS_FILE, json);
        cachedSettingsJson = json;
      }
    } catch (Throwable e) {
      android.util.Log.e("Knot", "SettingsStore.save failed", e);
    }
  }

  public static JSONObject loadEditHistory() {
    try {
      return readJson(EDIT_HISTORY_FILE);
    } catch (Throwable e) {
      return new JSONObject();
    }
  }

  public static void saveEditHistory(JSONObject json) {
    try {
      writeJson(EDIT_HISTORY_FILE, json);
    } catch (Throwable ignored) {
    }
  }

  public static JSONObject loadUnsendHistory() {
    try {
      return readUnsendHistory();
    } catch (Throwable e) {
      return new JSONObject();
    }
  }

  public static void saveUnsendHistory(JSONObject json) {
    try {
      writeUnsendHistory(json);
    } catch (Throwable ignored) {
    }
  }

  private static JSONObject readUnsendHistory() throws Throwable {
    DataInputStream in = openBinary(UNSEND_HISTORY_FILE, UNSEND_HISTORY_MAGIC);
    if (in == null) return new JSONObject();

    SimpleDateFormat fmt = new SimpleDateFormat(UNSEND_HISTORY_TIME_FMT, Locale.getDefault());
    JSONObject json = new JSONObject();
    int count = readVarInt(in);
    for (int i = 0; i < count; i++) {
      long msgId = readVarLong(in);
      long ts = readVarLong(in);
      json.put(String.valueOf(msgId), formatTime(fmt, ts));
    }
    return json;
  }

  private static void writeUnsendHistory(JSONObject json) throws Throwable {
    SimpleDateFormat fmt = new SimpleDateFormat(UNSEND_HISTORY_TIME_FMT, Locale.getDefault());

    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(buf);
    out.write(UNSEND_HISTORY_MAGIC);

    List<String> keys = keysOf(json);
    writeVarInt(out, keys.size());
    for (String k : keys) {
      writeVarLong(out, parseLongSafe(k));
      writeVarLong(out, parseTimeSafe(fmt, json.optString(k, "")));
    }
    out.flush();
    writeDeflated(UNSEND_HISTORY_FILE, buf.toByteArray());
  }

  public static JSONObject loadReadHistory() {
    try {
      return readReadHistory();
    } catch (Throwable e) {
      return new JSONObject();
    }
  }

  public static void saveReadHistory(JSONObject json) {
    try {
      writeReadHistory(json);
    } catch (Throwable ignored) {
    }
  }

  private static JSONObject readReadHistory() throws Throwable {
    DataInputStream in = openBinary(READ_HISTORY_FILE, READ_HISTORY_MAGIC);
    if (in == null) return new JSONObject();

    String[] strings = readStringTable(in);
    SimpleDateFormat fmt = new SimpleDateFormat(READ_HISTORY_TIME_FMT, Locale.getDefault());

    JSONObject root = new JSONObject();
    JSONObject chats = new JSONObject();
    root.put("c", chats);

    int chatCount = readVarInt(in);
    for (int i = 0; i < chatCount; i++) {
      String chatId = strings[readVarInt(in)];
      chats.put(chatId, readChat(in, strings, fmt));
    }
    return root;
  }

  private static JSONObject readChat(DataInputStream in, String[] strings, SimpleDateFormat fmt)
      throws IOException, JSONException {
    JSONObject chat = new JSONObject();
    String chatName = strings[readVarInt(in)];
    if (!chatName.isEmpty()) chat.put("n", chatName);

    int hwmCount = readVarInt(in);
    if (hwmCount > 0) {
      JSONObject hwm = new JSONObject();
      for (int j = 0; j < hwmCount; j++) {
        hwm.put(strings[readVarInt(in)], String.valueOf(readVarLong(in)));
      }
      chat.put("rh", hwm);
    }

    int msgCount = readVarInt(in);
    if (msgCount > 0) {
      JSONObject messages = new JSONObject();
      for (int j = 0; j < msgCount; j++) {
        long msgId = readVarLong(in);
        messages.put(String.valueOf(msgId), readMessage(in, strings, fmt));
      }
      chat.put("m", messages);
    }
    return chat;
  }

  private static JSONObject readMessage(DataInputStream in, String[] strings, SimpleDateFormat fmt)
      throws IOException, JSONException {
    JSONObject msg = new JSONObject();
    msg.put("sn", strings[readVarInt(in)]);
    msg.put("ct", formatTime(fmt, readVarLong(in)));
    msg.put("c", readUtf8(in));

    JSONObject readers = new JSONObject();
    int readerCount = readVarInt(in);
    for (int k = 0; k < readerCount; k++) {
      String mid = strings[readVarInt(in)];
      JSONObject info = new JSONObject();
      info.put("n", strings[readVarInt(in)]);
      info.put("t", formatTime(fmt, readVarLong(in)));
      readers.put(mid, info);
    }
    msg.put("r", readers);
    return msg;
  }

  private static void writeReadHistory(JSONObject json) throws Throwable {
    SimpleDateFormat fmt = new SimpleDateFormat(READ_HISTORY_TIME_FMT, Locale.getDefault());

    LinkedHashMap<String, Integer> table = new LinkedHashMap<>();
    table.put("", 0);

    ByteArrayOutputStream payloadBuf = new ByteArrayOutputStream();
    DataOutputStream payload = new DataOutputStream(payloadBuf);

    JSONObject chats = optObject(json, "c");
    List<String> chatIds = keysOf(chats);
    writeVarInt(payload, chatIds.size());
    for (String chatId : chatIds) {
      writeVarInt(payload, internString(table, chatId));
      writeChat(payload, table, fmt, optObject(chats, chatId));
    }
    payload.flush();

    ByteArrayOutputStream finalBuf = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(finalBuf);
    out.write(READ_HISTORY_MAGIC);
    writeStringTable(out, table);
    out.write(payloadBuf.toByteArray());
    out.flush();
    writeDeflated(READ_HISTORY_FILE, finalBuf.toByteArray());
  }

  private static void writeChat(
      DataOutputStream out,
      LinkedHashMap<String, Integer> table,
      SimpleDateFormat fmt,
      JSONObject chat)
      throws IOException {
    writeVarInt(out, internString(table, chat.optString("n", "")));

    JSONObject hwm = optObject(chat, "rh");
    List<String> hwmKeys = keysOf(hwm);
    writeVarInt(out, hwmKeys.size());
    for (String mid : hwmKeys) {
      writeVarInt(out, internString(table, mid));
      writeVarLong(out, parseLongSafe(hwm.optString(mid, "0")));
    }

    JSONObject messages = optObject(chat, "m");
    List<String> msgIds = keysOf(messages);
    writeVarInt(out, msgIds.size());
    for (String msgIdStr : msgIds) {
      writeVarLong(out, parseLongSafe(msgIdStr));
      writeMessage(out, table, fmt, optObject(messages, msgIdStr));
    }
  }

  private static void writeMessage(
      DataOutputStream out,
      LinkedHashMap<String, Integer> table,
      SimpleDateFormat fmt,
      JSONObject msg)
      throws IOException {
    writeVarInt(out, internString(table, msg.optString("sn", "")));
    writeVarLong(out, parseTimeSafe(fmt, msg.optString("ct", "")));
    writeUtf8(out, msg.optString("c", ""));

    JSONObject readers = optObject(msg, "r");
    List<String> readerMids = keysOf(readers);
    writeVarInt(out, readerMids.size());
    for (String mid : readerMids) {
      JSONObject info = optObject(readers, mid);
      writeVarInt(out, internString(table, mid));
      writeVarInt(out, internString(table, info.optString("n", "")));
      writeVarLong(out, parseTimeSafe(fmt, info.optString("t", "")));
    }
  }

  private static void ensurePointerPath(Context ctx) {
    if (pointerPath != null || ctx == null) return;
    try {
      File extDir = ctx.getExternalFilesDir(null);
      if (extDir != null) {
        extDir.mkdirs();
        pointerPath = extDir.getAbsolutePath() + "/knot_ptr";
      }
    } catch (Throwable ignored) {
    }
  }

  private static String readPointer() {
    ensurePointerPath(appContext);
    if (pointerPath == null) return null;
    try {
      File f = new File(pointerPath);
      if (!f.exists()) return null;
      try (BufferedReader r = new BufferedReader(new FileReader(f))) {
        String line = r.readLine();
        return line != null ? line.trim() : null;
      }
    } catch (Throwable ignored) {
    }
    return null;
  }

  private static Uri ensureTreeUri() {
    if (cachedTreeUri != null) return cachedTreeUri;
    String saved = readPointer();
    if (saved == null || !saved.startsWith("content://")) return null;
    cachedTreeUri = Uri.parse(saved);
    return cachedTreeUri;
  }

  private static Uri getDocUri(String fileName, boolean createIfMissing) throws Throwable {
    Uri cached = cachedDocUris.get(fileName);
    if (cached != null) return cached;

    Uri treeUri = ensureTreeUri();
    if (treeUri == null || appContext == null) return null;

    ContentResolver resolver = appContext.getContentResolver();
    String treeDocId = DocumentsContract.getTreeDocumentId(treeUri);
    Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId);

    try (Cursor cursor =
        resolver.query(
            childrenUri,
            new String[] {
              DocumentsContract.Document.COLUMN_DOCUMENT_ID,
              DocumentsContract.Document.COLUMN_DISPLAY_NAME
            },
            null,
            null,
            null)) {
      if (cursor != null) {
        while (cursor.moveToNext()) {
          if (fileName.equals(cursor.getString(1))) {
            Uri uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, cursor.getString(0));
            cachedDocUris.put(fileName, uri);
            return uri;
          }
        }
      }
    }

    if (!createIfMissing) return null;

    Uri parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId);
    Uri newUri =
        DocumentsContract.createDocument(
            resolver, parentDocUri, "application/octet-stream", fileName);
    if (newUri != null) cachedDocUris.put(fileName, newUri);
    return newUri;
  }

  private static JSONObject readJson(String fileName) throws Throwable {
    byte[] data = readDeflated(fileName);
    if (data == null) return new JSONObject();
    String text = new String(data, StandardCharsets.UTF_8).trim();
    return text.isEmpty() ? new JSONObject() : new JSONObject(text);
  }

  private static void writeJson(String fileName, JSONObject json) throws Throwable {
    writeDeflated(fileName, json.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static byte[] readDeflated(String fileName) throws Throwable {
    Uri docUri = getDocUri(fileName, false);
    if (docUri == null || appContext == null) return null;
    try (InputStream is = appContext.getContentResolver().openInputStream(docUri);
        InflaterInputStream iis = new InflaterInputStream(is)) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      byte[] buf = new byte[8192];
      int n;
      while ((n = iis.read(buf)) != -1) bos.write(buf, 0, n);
      return bos.toByteArray();
    } catch (Throwable t) {
      return null;
    }
  }

  private static void writeDeflated(String fileName, byte[] data) throws Throwable {
    Uri docUri = getDocUri(fileName, true);
    if (docUri == null || appContext == null) throw new IllegalStateException("Not configured");
    try (OutputStream os = appContext.getContentResolver().openOutputStream(docUri, "wt")) {
      Deflater def = new Deflater(Deflater.BEST_COMPRESSION);
      try (DeflaterOutputStream dos = new DeflaterOutputStream(os, def)) {
        dos.write(data);
      } finally {
        def.end();
      }
    }
  }

  private static DataInputStream openBinary(String fileName, byte[] magic) throws Throwable {
    byte[] raw = readDeflated(fileName);
    if (raw == null || raw.length < magic.length) return null;
    for (int i = 0; i < magic.length; i++) if (raw[i] != magic[i]) return null;
    return new DataInputStream(
        new ByteArrayInputStream(raw, magic.length, raw.length - magic.length));
  }

  private static void writeVarInt(DataOutputStream out, int v) throws IOException {
    while ((v & ~0x7F) != 0) {
      out.writeByte((v & 0x7F) | 0x80);
      v >>>= 7;
    }
    out.writeByte(v);
  }

  private static void writeVarLong(DataOutputStream out, long v) throws IOException {
    while ((v & ~0x7FL) != 0) {
      out.writeByte((int) ((v & 0x7F) | 0x80));
      v >>>= 7;
    }
    out.writeByte((int) v);
  }

  private static int readVarInt(DataInputStream in) throws IOException {
    int result = 0;
    for (int shift = 0; ; shift += 7) {
      int b = in.readUnsignedByte();
      result |= (b & 0x7F) << shift;
      if ((b & 0x80) == 0) return result;
    }
  }

  private static long readVarLong(DataInputStream in) throws IOException {
    long result = 0;
    for (int shift = 0; ; shift += 7) {
      int b = in.readUnsignedByte();
      result |= ((long) (b & 0x7F)) << shift;
      if ((b & 0x80) == 0) return result;
    }
  }

  private static void writeUtf8(DataOutputStream out, String s) throws IOException {
    byte[] b = s.getBytes(StandardCharsets.UTF_8);
    writeVarInt(out, b.length);
    out.write(b);
  }

  private static String readUtf8(DataInputStream in) throws IOException {
    int len = readVarInt(in);
    byte[] b = new byte[len];
    in.readFully(b);
    return new String(b, StandardCharsets.UTF_8);
  }

  private static String[] readStringTable(DataInputStream in) throws IOException {
    int count = readVarInt(in);
    String[] strings = new String[count];
    for (int i = 0; i < count; i++) strings[i] = readUtf8(in);
    return strings;
  }

  private static void writeStringTable(DataOutputStream out, LinkedHashMap<String, Integer> table)
      throws IOException {
    writeVarInt(out, table.size());
    for (String s : table.keySet()) writeUtf8(out, s);
  }

  private static int internString(LinkedHashMap<String, Integer> table, String s) {
    if (s == null) s = "";
    Integer idx = table.get(s);
    if (idx != null) return idx;
    int newIdx = table.size();
    table.put(s, newIdx);
    return newIdx;
  }

  private static List<String> keysOf(JSONObject json) {
    List<String> keys = new ArrayList<>();
    if (json == null) return keys;
    Iterator<String> it = json.keys();
    while (it.hasNext()) keys.add(it.next());
    return keys;
  }

  private static JSONObject optObject(JSONObject parent, String key) {
    JSONObject child = parent != null ? parent.optJSONObject(key) : null;
    return child != null ? child : new JSONObject();
  }

  private static long parseLongSafe(String s) {
    if (s == null || s.isEmpty()) return 0L;
    try {
      return Long.parseLong(s);
    } catch (NumberFormatException e) {
      return 0L;
    }
  }

  private static long parseTimeSafe(SimpleDateFormat fmt, String s) {
    if (s == null || s.isEmpty()) return 0L;
    try {
      Date d = fmt.parse(s);
      return d != null ? d.getTime() : 0L;
    } catch (Throwable t) {
      return 0L;
    }
  }

  private static String formatTime(SimpleDateFormat fmt, long ms) {
    return ms > 0 ? fmt.format(new Date(ms)) : "";
  }

  private static String uriToDisplayPath(Uri uri) {
    if (uri == null) return null;
    try {
      String decoded = Uri.decode(uri.toString());
      if (decoded.contains("/tree/primary:")) {
        String path =
            decoded.substring(decoded.indexOf("/tree/primary:") + "/tree/primary:".length());
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        return "/sdcard/" + path;
      }
      String docId = DocumentsContract.getTreeDocumentId(uri);
      if (docId != null && docId.contains(":")) {
        return "Internal:/" + docId.substring(docId.indexOf(":") + 1);
      }
      return Uri.decode(uri.getLastPathSegment());
    } catch (Throwable ignored) {
    }
    return uri.getLastPathSegment();
  }
}
