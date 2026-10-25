package app.zipper.knot.hooks;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.Reflect;
import app.zipper.knot.utils.LineDBUtils;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class PermanentHideChatHook implements BaseHook {

  private static final Pattern NOT_ARCHIVED =
      Pattern.compile("\\bis_archived\\s*=\\s*0\\b", Pattern.CASE_INSENSITIVE);

  private static ClassLoader classLoader;
  private static LineVersion.Config config;

  private static final ThreadLocal<Boolean> openingChat = ThreadLocal.withInitial(() -> false);

  private static volatile Set<String> hiddenChats;

  @Override
  public void hook(KnotConfig options, LoadParam lpparam) throws Throwable {
    LineVersion.Config version = LineVersion.get();
    if (version == null) return;
    ClassLoader cl = lpparam.classLoader;
    LineVersion.Config.PermanentHideChat cfg = version.permanentHideChat;
    classLoader = cl;
    config = version;

    hookResurface(cl, cfg);
    hookArchiveUpdate(cl, cfg, version.message.dataManagerClass);
    hookChatOpen(cl, cfg);
    hookOpenChatResurface(cl, cfg);
    hookOpenChatUnarchive(cl, cfg);
    hookArchivedFilter();
  }

  static boolean showingHidden() {
    return ReadToggle.SHOW_HIDDEN_CHATS.isActive();
  }

  static boolean isHidden(String chatId) {
    if (chatId == null || !showingHidden()) return false;
    Set<String> ids = hiddenChats;
    if (ids == null) hiddenChats = ids = LineDBUtils.getArchivedChatIds();
    return ids.contains(chatId);
  }

  // The last argument resets is_archived to 0 for an incoming message
  private static void hookResurface(ClassLoader cl, LineVersion.Config.PermanentHideChat cfg) {
    if (cfg.chatDaoClass.isEmpty()) return;
    try {
      for (Method m :
          Reflect.findMethodsByName(cfg.chatDaoClass, cl, cfg.methodUpdateChatOnMessage)) {
        int unarchiveIndex = m.getParameterCount() - 1;
        if (unarchiveIndex < 0 || m.getParameterTypes()[unarchiveIndex] != boolean.class) continue;
        Knot.module
            .hook(m)
            .intercept(
                chain -> {
                  if (!Main.options.permanentHideChat.enabled) return chain.proceed();
                  Object[] args = chain.getArgs().toArray();
                  args[unarchiveIndex] = false;
                  return chain.proceed(args);
                });
        return;
      }
      Knot.log("Knot: PermanentHideChat chat update method not found");
    } catch (Throwable t) {
      Knot.log("Knot: PermanentHideChat chat update hook error: " + t);
    }
  }

  // Menu unhides and the hidden list sync with the server; automatic unhides (sending, BGM) don't
  private static void hookArchiveUpdate(
      ClassLoader cl, LineVersion.Config.PermanentHideChat cfg, String dataManagerClass) {
    if (dataManagerClass.isEmpty()) return;
    try {
      for (Method m : Reflect.findMethodsByName(dataManagerClass, cl, cfg.methodUpdateArchived)) {
        // 26.13.0 takes the continuation first, later versions the chat id
        int chatIdIndex = Reflect.paramIndex(m, String.class);
        int archivedIndex = Reflect.paramIndex(m, boolean.class);
        int syncIndex = m.getParameterCount() - 1;
        if (m.getParameterCount() != 4
            || chatIdIndex < 0
            || archivedIndex < 0
            || archivedIndex == syncIndex) {
          continue;
        }
        Knot.module
            .hook(m)
            .intercept(
                chain -> {
                  String chatId = (String) chain.getArg(chatIdIndex);
                  boolean archive = Boolean.TRUE.equals(chain.getArg(archivedIndex));
                  boolean sync = Boolean.TRUE.equals(chain.getArg(syncIndex));
                  boolean turnAround = archive && sync && showingHidden();
                  // Archiving an archived chat is a no-op (Unchanged)
                  boolean keepHidden = !archive && !sync && Main.options.permanentHideChat.enabled;
                  if ((!turnAround && !keepHidden) || !LineDBUtils.isChatArchived(chatId)) {
                    return chain.proceed();
                  }
                  Object[] args = chain.getArgs().toArray();
                  args[archivedIndex] = keepHidden;
                  return chain.proceed(args);
                });
        return;
      }
      Knot.log("Knot: PermanentHideChat archive update method not found");
    } catch (Throwable t) {
      Knot.log("Knot: PermanentHideChat archive update hook error: " + t);
    }
  }

  // The chat screen unhides the chat on load and rejects it if it is still archived
  private static void hookChatOpen(ClassLoader cl, LineVersion.Config.PermanentHideChat cfg) {
    if (cfg.chatOpenLoaderClass.isEmpty()) return;
    try {
      Class<?> chatData = Reflect.findClass(cfg.chatDataClass, cl);
      for (Class<?> type : chatData.getDeclaredClasses()) {
        if (!chatData.isAssignableFrom(type) || Modifier.isAbstract(type.getModifiers())) continue;
        try {
          Knot.module
              .hook(type.getDeclaredMethod(cfg.methodChatDataArchived))
              .intercept(chain -> openingChat.get() ? Boolean.FALSE : chain.proceed());
        } catch (Throwable t) {
          Knot.log("Knot: PermanentHideChat archived getter hook error (" + type + "): " + t);
        }
      }
      for (Method m :
          Reflect.findMethodsByName(cfg.chatOpenLoaderClass, cl, cfg.methodChatOpenLoad)) {
        Knot.module
            .hook(m)
            .intercept(
                chain -> {
                  if (!Main.options.permanentHideChat.enabled) return chain.proceed();
                  openingChat.set(true);
                  try {
                    return chain.proceed();
                  } finally {
                    openingChat.set(false);
                  }
                });
        return;
      }
      Knot.log("Knot: PermanentHideChat chat open method not found");
    } catch (Throwable t) {
      Knot.log("Knot: PermanentHideChat chat open hook error: " + t);
    }
  }

  // New messages write ARCHIVED_TYPE with other columns; hide and unhide write it alone
  private static void hookOpenChatResurface(
      ClassLoader cl, LineVersion.Config.PermanentHideChat cfg) {
    if (cfg.squareChatDaoClass.isEmpty()) return;
    try {
      Object archivedColumn =
          Reflect.getStaticObjectField(
              Reflect.findClass(cfg.squareChatColumnClass, cl), cfg.squareArchivedColumn);
      Object archivedType = squareArchiveType(cl, cfg, cfg.squareArchivedType);
      for (Method m :
          Reflect.findMethodsByName(cfg.squareChatDaoClass, cl, cfg.methodUpdateSquareChat)) {
        int columnsIndex = Reflect.paramIndex(m, Set.class);
        if (m.getParameterCount() != 2 || columnsIndex < 0) continue;
        int chatIndex = 1 - columnsIndex;
        Knot.module
            .hook(m)
            .intercept(
                chain -> {
                  Set<?> columns = (Set<?>) chain.getArg(columnsIndex);
                  if (!Main.options.permanentHideChat.enabled
                      || columns == null
                      || columns.size() < 2
                      || !columns.contains(archivedColumn)
                      || !isHiddenOpenChat(
                          chain.getThisObject(),
                          chatMid(chain.getArg(chatIndex), cfg),
                          archivedType,
                          cfg)) {
                    return chain.proceed();
                  }
                  Set<Object> kept = new LinkedHashSet<>(columns);
                  kept.remove(archivedColumn);
                  Object[] args = chain.getArgs().toArray();
                  args[columnsIndex] = kept;
                  return chain.proceed(args);
                });
        return;
      }
      Knot.log("Knot: PermanentHideChat OpenChat update method not found");
    } catch (Throwable t) {
      Knot.log("Knot: PermanentHideChat OpenChat update hook error: " + t);
    }
  }

  // Thread replies unarchive OpenChats through this setter
  private static void hookOpenChatUnarchive(
      ClassLoader cl, LineVersion.Config.PermanentHideChat cfg) {
    if (cfg.methodSetSquareArchiveType.isEmpty()) return;
    try {
      Object archivedType = squareArchiveType(cl, cfg, cfg.squareArchivedType);
      Object notArchivedType = squareArchiveType(cl, cfg, cfg.squareNotArchivedType);
      Knot.module
          .hook(
              Reflect.findMethodExact(
                  cfg.squareChatDaoClass,
                  cl,
                  cfg.methodSetSquareArchiveType,
                  String.class,
                  cfg.squareArchiveTypeClass))
          .intercept(
              chain -> {
                if (Main.options.permanentHideChat.enabled
                    && chain.getArg(1) == notArchivedType
                    && isHiddenOpenChat(
                        chain.getThisObject(), (String) chain.getArg(0), archivedType, cfg)) {
                  return null;
                }
                return chain.proceed();
              });
    } catch (Throwable t) {
      Knot.log("Knot: PermanentHideChat OpenChat unarchive hook error: " + t);
    }
  }

  private static Object squareArchiveType(
      ClassLoader cl, LineVersion.Config.PermanentHideChat cfg, String name) {
    return Reflect.getStaticObjectField(Reflect.findClass(cfg.squareArchiveTypeClass, cl), name);
  }

  private static String chatMid(Object chat, LineVersion.Config.PermanentHideChat cfg) {
    try {
      return (String) Reflect.getObjectField(chat, cfg.squareChatIdField);
    } catch (Throwable t) {
      Knot.log("Knot: PermanentHideChat OpenChat id error: " + t);
      return null;
    }
  }

  // Uses LINE's DAO to avoid opening the database for every message
  private static boolean isHiddenOpenChat(
      Object dao, String chatMid, Object archivedType, LineVersion.Config.PermanentHideChat cfg) {
    if (chatMid == null) return false;
    try {
      Object chat = Reflect.callMethod(dao, cfg.methodGetSquareChat, chatMid);
      return chat != null
          && Reflect.getObjectField(chat, cfg.squareChatArchiveTypeField) == archivedType;
    } catch (Throwable t) {
      Knot.log("Knot: PermanentHideChat OpenChat lookup error: " + t);
      return false;
    }
  }

  // LINE's chat list queries use rawQuery, which reaches rawQueryWithFactory without its hook
  private static void hookArchivedFilter() {
    try {
      Knot.module
          .hook(
              Reflect.findMethodExact(
                  SQLiteDatabase.class, "rawQuery", String.class, String[].class))
          .intercept(
              chain -> {
                String sql = (String) chain.getArg(0);
                if (sql == null || !sql.contains("is_archived") || !showingHidden()) {
                  return chain.proceed();
                }
                String rewritten = NOT_ARCHIVED.matcher(sql).replaceAll("is_archived IN (0, 1)");
                if (rewritten.equals(sql)) return chain.proceed();
                // Reloaded with the list so hides and unhides are picked up
                hiddenChats = null;
                Object[] args = chain.getArgs().toArray();
                args[0] = rewritten;
                return chain.proceed(args);
              });
    } catch (Throwable t) {
      Knot.log("Knot: PermanentHideChat query hook error: " + t);
    }
  }

  // Same events LINE posts after hiding a chat
  public static void refreshChatList() {
    LineVersion.Config version = config;
    Context ctx = Knot.currentApplication();
    if (version == null || ctx == null || version.eventBus.busClass.isEmpty()) return;
    LineVersion.Config.PermanentHideChat cfg = version.permanentHideChat;
    try {
      ClassLoader cl = classLoader;
      Object provider =
          Reflect.getStaticObjectField(
              Reflect.findClass(version.eventBus.busClass, cl), version.eventBus.providerField);
      Object eventBus =
          Reflect.callStaticMethod(
              Reflect.findClass(version.services.locatorClass, cl),
              version.services.methodLocate,
              ctx,
              provider);
      Reflect.callMethod(
          eventBus,
          version.eventBus.methodPost,
          Reflect.getStaticObjectField(
              Reflect.findClass(cfg.updateViewEventClass, cl), cfg.updateViewEventChatList));
      Reflect.callMethod(
          eventBus,
          version.eventBus.methodPost,
          Reflect.getStaticObjectField(
              Reflect.findClass(cfg.chatTabBadgeEventClass, cl), cfg.chatTabBadgeEventField));
    } catch (Throwable t) {
      Knot.log("Knot: PermanentHideChat refresh error: " + t);
    }
  }
}
