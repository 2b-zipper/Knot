package app.zipper.knot.hooks;

import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Reflect;
import app.zipper.knot.SettingsStore;
import app.zipper.knot.utils.LineDBUtils;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONObject;

public class ReadReceiptHandler implements BaseHook {

  private static final String TIME_FMT = "yyyy-MM-dd HH:mm:ss";
  private static final String NOP_CHAT_ID = "KNOT_NOP";
  private static final String UNKNOWN_READER_NAME = "Unknown";

  private static volatile boolean isBulkReading = false;
  private static final Set<String> pendingManualReads = ConcurrentHashMap.newKeySet();

  @Override
  public void hook(final KnotConfig config, LoadParam lpparam) throws Throwable {
    LineVersion.Config cfg = LineVersion.get();
    ClassLoader cl = lpparam.classLoader;

    hookOperationForHistory(cfg, cl);
    hookThriftReadReceipt(cfg, cl, config);
    hookReadReceiptManager(cfg, cl, config);
  }

  private void hookOperationForHistory(LineVersion.Config cfg, ClassLoader cl) {
    try {
      Knot.hookAll(
          Reflect.findClass(cfg.unsend.notifiedReadMessageHandlerClass, cl),
          cfg.unsend.methodReadBuffer,
          chain -> {
            Object result = chain.proceed();
            if (!recordingEnabled()) return result;
            Object op = chain.getArg(1);
            if (op == null || op instanceof String) return result;
            try {
              Object type = Reflect.getObjectField(op, cfg.unsend.operationTypeField);
              if (type == null
                  || !cfg.readReceipt.operationNotifiedReadName.equals(type.toString())) {
                return result;
              }
              recordReadEvent(
                  (String) Reflect.getObjectField(op, cfg.unsend.operationParam1Field),
                  (String) Reflect.getObjectField(op, cfg.unsend.operationParam2Field),
                  (String) Reflect.getObjectField(op, cfg.unsend.operationParam3Field),
                  Reflect.getLongField(op, cfg.unsend.operationCreatedTimeField));
            } catch (Throwable ignored) {
            }
            return result;
          });
    } catch (Throwable ignored) {
    }
  }

  private boolean recordingEnabled() {
    return SettingsStore.get("record_read_history", false);
  }

  private void hookThriftReadReceipt(LineVersion.Config cfg, ClassLoader cl, KnotConfig config) {
    try {
      Knot.hookAll(
          cl.loadClass(cfg.thrift.talkServiceClientImplClass),
          cfg.thrift.v1,
          chain -> {
            List<Object> args = chain.getArgs();
            if (args.isEmpty() || args.get(0) == null) return chain.proceed();
            if (containsPendingManualRead(args) || !shouldBlockReadReceipt(config)) {
              return chain.proceed();
            }
            if (args.get(0) instanceof String) {
              Object[] newArgs = args.toArray();
              newArgs[0] = NOP_CHAT_ID;
              return chain.proceed(newArgs);
            }
            return null;
          });
    } catch (Throwable ignored) {
    }
  }

  private boolean containsPendingManualRead(List<Object> args) {
    for (Object a : args) {
      if (a instanceof String && pendingManualReads.contains(a)) return true;
    }
    return false;
  }

  private void hookReadReceiptManager(LineVersion.Config cfg, ClassLoader cl, KnotConfig config) {
    Class<?> managerCls = getManagerClass(cfg, cl);
    if (managerCls == null) return;

    hookSendReadReceipt(managerCls, cfg, config);
    hookExecuteReadReceiptAsync(managerCls, cfg, config);
    hookReadAll(managerCls, cfg, config);
  }

  private void hookSendReadReceipt(Class<?> managerCls, LineVersion.Config cfg, KnotConfig config) {
    try {
      Knot.hookAll(
          managerCls,
          cfg.readReceipt.methodSendReadReceipt,
          chain -> {
            Class<?>[] params = ((Method) chain.getExecutable()).getParameterTypes();
            String chatId = null;
            if (params.length == 3 && params[0] == long.class && chain.getArgs().size() > 1) {
              Object arg = chain.getArg(1);
              if (arg instanceof String) chatId = (String) arg;
            }

            boolean isManualRead = chatId != null && pendingManualReads.contains(chatId);
            boolean skip = chatId != null && !isManualRead && shouldBlockReadReceipt(config);

            Object result = skip ? null : chain.proceed();
            if (chatId != null) pendingManualReads.remove(chatId);
            return result;
          });
    } catch (Throwable ignored) {
    }
  }

  private void hookExecuteReadReceiptAsync(
      Class<?> managerCls, LineVersion.Config cfg, KnotConfig config) {
    try {
      Knot.hookAll(
          managerCls,
          cfg.readReceipt.methodExecuteReadReceiptAsync,
          chain -> {
            if (isPreventActive(config)) {
              Class<?>[] params = ((Method) chain.getExecutable()).getParameterTypes();
              if (params.length == 1
                  && params[0] == String.class
                  && shouldRememberManualRead(cfg)) {
                pendingManualReads.add((String) chain.getArg(0));
              }
            }
            return chain.proceed();
          });
    } catch (Throwable ignored) {
    }
  }

  private boolean shouldRememberManualRead(LineVersion.Config cfg) {
    String manualClass = cfg.readReceipt.longPressReadClass;
    boolean manual = manualClass != null && !manualClass.isEmpty() && isFromClass(manualClass);
    return manual || SettingsStore.get("send_mark_state", false);
  }

  private void hookReadAll(Class<?> managerCls, LineVersion.Config cfg, KnotConfig config) {
    try {
      Knot.hookAll(
          managerCls,
          cfg.readReceipt.methodReadAll,
          chain -> {
            boolean isNoArg = ((Method) chain.getExecutable()).getParameterCount() == 0;
            if (isPreventActive(config) && isNoArg) isBulkReading = true;
            try {
              return chain.proceed();
            } finally {
              if (isNoArg) isBulkReading = false;
            }
          });
    } catch (Throwable ignored) {
    }
  }

  private boolean shouldBlockReadReceipt(KnotConfig config) {
    return isPreventActive(config) && !isBulkReading;
  }

  private boolean isPreventActive(KnotConfig config) {
    return SettingsStore.get("prevent_mark_as_read", false)
        && SettingsStore.get("prevent_read_state", true);
  }

  private boolean isFromClass(String prefix) {
    for (StackTraceElement el : Thread.currentThread().getStackTrace()) {
      String n = el.getClassName();
      if (n.equals(prefix) || n.startsWith(prefix + "$") || n.startsWith(prefix + ".")) return true;
    }
    return false;
  }

  private boolean isLocalReadContext() {
    StackTraceElement[] stack = Thread.currentThread().getStackTrace();
    for (StackTraceElement element : stack) {
      String name = element.getClassName();
      if (name.contains("ChatHistoryActivity")
          || name.contains("MessageList")
          || name.contains("ChatList")) return true;
    }
    return false;
  }

  private Class<?> getManagerClass(LineVersion.Config cfg, ClassLoader cl) {
    if (cfg.readReceipt.readReceiptManagerClass.isEmpty()) return null;
    try {
      return Reflect.findClass(cfg.readReceipt.readReceiptManagerClass, cl);
    } catch (Throwable ignored) {
      return null;
    }
  }

  private void recordReadEvent(
      String chatId, String readerMid, String lastMsgIdStr, long readTime) {
    if (chatId == null || readerMid == null || lastMsgIdStr == null) return;

    long lastMsgId = parseLong(lastMsgIdStr, -1L);
    if (lastMsgId < 0) return;

    String myMid = LineDBUtils.getMyMid();
    if (myMid == null || readerMid.equals(myMid)) return;

    try {
      JSONObject history = SettingsStore.loadReadHistory();
      JSONObject chat = ensureChat(history, chatId);

      long prevHwm = getReaderHwm(chat, readerMid);
      if (lastMsgId <= prevHwm) return;

      String readerName = LineDBUtils.resolveMemberName(readerMid);
      String timeStr =
          new SimpleDateFormat(TIME_FMT, Locale.getDefault()).format(new Date(readTime));

      long lowerBound = prevHwm > 0 ? prevHwm : lastMsgId - 1;
      List<LineDBUtils.MessageRecord> records =
          LineDBUtils.fetchMyMessagesUpTo(chatId, lowerBound, lastMsgId, myMid);

      ensureMessageEntries(chat, records);
      markReaderOnMessagesUpTo(chat, readerMid, readerName, timeStr, lastMsgId);
      setReaderHwm(chat, readerMid, lastMsgId);
      SettingsStore.saveReadHistory(history);
    } catch (Throwable ignored) {
    }
  }

  private JSONObject ensureChat(JSONObject history, String chatId) throws Exception {
    JSONObject chats = history.optJSONObject("c");
    if (chats == null) history.put("c", chats = new JSONObject());
    JSONObject chat = chats.optJSONObject(chatId);
    if (chat == null) chats.put(chatId, chat = new JSONObject());
    return chat;
  }

  private long getReaderHwm(JSONObject chat, String readerMid) {
    JSONObject hwm = chat.optJSONObject("rh");
    return hwm == null ? 0L : parseLong(hwm.optString(readerMid, ""), 0L);
  }

  private void setReaderHwm(JSONObject chat, String readerMid, long msgId) throws Exception {
    JSONObject hwm = chat.optJSONObject("rh");
    if (hwm == null) chat.put("rh", hwm = new JSONObject());
    hwm.put(readerMid, String.valueOf(msgId));
  }

  private void ensureMessageEntries(JSONObject chat, List<LineDBUtils.MessageRecord> records)
      throws Exception {
    if (records.isEmpty()) return;
    JSONObject messages = chat.optJSONObject("m");
    if (messages == null) chat.put("m", messages = new JSONObject());

    if (!chat.has("n")) {
      String chatName = records.get(0).chatName;
      if (chatName != null) chat.put("n", chatName);
    }

    for (LineDBUtils.MessageRecord record : records) {
      if (messages.has(record.id)) continue;
      JSONObject msg = new JSONObject();
      msg.put("c", record.text);
      msg.put("sn", record.senderName);
      msg.put("ct", record.timestamp);
      msg.put("r", new JSONObject());
      messages.put(record.id, msg);
    }
  }

  private void markReaderOnMessagesUpTo(
      JSONObject chat, String readerMid, String readerName, String timeStr, long lastMsgId)
      throws Exception {
    JSONObject messages = chat.optJSONObject("m");
    if (messages == null) return;

    Iterator<String> it = messages.keys();
    while (it.hasNext()) {
      String key = it.next();
      if (parseLong(key, Long.MAX_VALUE) > lastMsgId) continue;
      JSONObject msg = messages.optJSONObject(key);
      if (msg != null) markReader(msg, readerMid, readerName, timeStr);
    }
  }

  private void markReader(JSONObject msg, String readerMid, String readerName, String timeStr)
      throws Exception {
    JSONObject readers = msg.optJSONObject("r");
    if (readers == null) {
      readers = new JSONObject();
      msg.put("r", readers);
    }
    if (readers.has(readerMid)) return;
    JSONObject info = new JSONObject();
    info.put("n", readerName != null ? readerName : UNKNOWN_READER_NAME);
    info.put("t", timeStr);
    readers.put(readerMid, info);
  }

  private static long parseLong(String s, long fallback) {
    if (s == null || s.isEmpty()) return fallback;
    try {
      return Long.parseLong(s);
    } catch (NumberFormatException e) {
      return fallback;
    }
  }
}
