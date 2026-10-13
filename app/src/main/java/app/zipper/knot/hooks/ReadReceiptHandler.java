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
  private static Object reactionReadManager;

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    LineVersion.Config cfg = LineVersion.get();
    ClassLoader cl = lpparam.classLoader;

    hookOperationForHistory(cfg, cl);
    hookThriftReadReceipt(cfg, cl);
    hookReadReceiptManager(cfg, cl);
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

  // Lets one read that the user explicitly asked Knot for pass read avoidance
  static void addPendingManualRead(String chatId) {
    pendingManualReads.add(chatId);
  }

  private boolean recordingEnabled() {
    return SettingsStore.get("record_read_history", false);
  }

  private void hookThriftReadReceipt(LineVersion.Config cfg, ClassLoader cl) {
    try {
      Knot.hookAll(
          cl.loadClass(cfg.thrift.talkServiceClientImplClass),
          cfg.thrift.v1,
          chain -> {
            List<Object> args = chain.getArgs();
            if (args.isEmpty() || args.get(0) == null) return chain.proceed();
            if (containsPendingManualRead(args) || !shouldBlockReadReceipt()) {
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

  private void hookReadReceiptManager(LineVersion.Config cfg, ClassLoader cl) {
    Class<?> managerCls = getManagerClass(cfg, cl);
    if (managerCls == null) return;

    hookSendReadReceipt(managerCls, cfg);
    hookExecuteReadReceiptAsync(managerCls, cfg);
    hookResolveReadTarget(managerCls, cfg);
    hookReadAll(managerCls, cfg);
    hookReactionMarkRead(managerCls, cfg, cl);
  }

  private void hookResolveReadTarget(Class<?> managerCls, LineVersion.Config cfg) {
    String method = cfg.readReceipt.methodResolveReadTarget;
    if (method == null || method.isEmpty()) return;
    try {
      Knot.hookAll(
          managerCls,
          method,
          chain -> {
            Object result = chain.proceed();
            if (result instanceof Long
                && (Long) result == 0L
                && !chain.getArgs().isEmpty()
                && chain.getArg(0) instanceof String) {
              pendingManualReads.remove(chain.getArg(0));
            }
            return result;
          });
    } catch (Throwable ignored) {
    }
  }

  private void hookSendReadReceipt(Class<?> managerCls, LineVersion.Config cfg) {
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
            boolean skip = chatId != null && !isManualRead && shouldBlockReadReceipt();

            Object result = skip ? null : chain.proceed();
            if (chatId != null) pendingManualReads.remove(chatId);
            return result;
          });
    } catch (Throwable ignored) {
    }
  }

  private void hookExecuteReadReceiptAsync(Class<?> managerCls, LineVersion.Config cfg) {
    try {
      Knot.hookAll(
          managerCls,
          cfg.readReceipt.methodExecuteReadReceiptAsync,
          chain -> {
            if (ReadToggle.PREVENT_READ.isActive()) {
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
    return manual || ReadToggle.SEND_MARK_READ.isOn();
  }

  private void hookReadAll(Class<?> managerCls, LineVersion.Config cfg) {
    try {
      Knot.hookAll(
          managerCls,
          cfg.readReceipt.methodReadAll,
          chain -> {
            boolean isNoArg = ((Method) chain.getExecutable()).getParameterCount() == 0;
            if (ReadToggle.PREVENT_READ.isActive() && isNoArg) isBulkReading = true;
            try {
              return chain.proceed();
            } finally {
              if (isNoArg) isBulkReading = false;
            }
          });
    } catch (Throwable ignored) {
    }
  }

  private void hookReactionMarkRead(Class<?> managerCls, LineVersion.Config cfg, ClassLoader cl) {
    String method = cfg.readReceipt.methodReact;
    if (method == null || method.isEmpty()) return;
    try {
      Class<?> successCls = Reflect.findClass(cfg.readReceipt.reactSuccessResultClass, cl);
      Knot.hookAll(
          Reflect.findClass(cfg.readReceipt.reactClientClass, cl),
          method,
          chain -> {
            Object result = chain.proceed();
            if (!successCls.isInstance(result) || !ReadToggle.REACTION_MARK_READ.isActive()) {
              return result;
            }
            try {
              long messageId =
                  Reflect.getLongField(chain.getArg(0), cfg.readReceipt.reactRequestMessageIdField);
              new Thread(() -> markReadAfterReaction(managerCls, cfg, messageId)).start();
            } catch (Throwable t) {
              Knot.log("Knot: reaction message id read failed: " + t);
            }
            return result;
          });
    } catch (Throwable ignored) {
    }
  }

  private void markReadAfterReaction(Class<?> managerCls, LineVersion.Config cfg, long messageId) {
    String chatId = LineDBUtils.resolveChatIdByServerId(String.valueOf(messageId));
    if (chatId == null) return;
    pendingManualReads.add(chatId);
    try {
      Object manager = obtainReactionReadManager(managerCls);
      long target =
          (long) Reflect.callMethod(manager, cfg.readReceipt.methodResolveReadTarget, chatId);
      if (target != 0L) {
        Reflect.callMethod(manager, cfg.readReceipt.methodSendReadReceipt, target, chatId, true);
      }
    } catch (Throwable t) {
      Knot.log("Knot: mark read after reaction failed: " + t);
    } finally {
      pendingManualReads.remove(chatId);
    }
  }

  private static synchronized Object obtainReactionReadManager(Class<?> managerCls) {
    if (reactionReadManager == null) {
      reactionReadManager = Reflect.newInstance(managerCls, Knot.currentApplication());
    }
    return reactionReadManager;
  }

  private boolean shouldBlockReadReceipt() {
    return ReadToggle.PREVENT_READ.isActive() && !isBulkReading;
  }

  private boolean isFromClass(String prefix) {
    for (StackTraceElement el : Thread.currentThread().getStackTrace()) {
      String n = el.getClassName();
      if (n.equals(prefix) || n.startsWith(prefix + "$") || n.startsWith(prefix + ".")) return true;
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
