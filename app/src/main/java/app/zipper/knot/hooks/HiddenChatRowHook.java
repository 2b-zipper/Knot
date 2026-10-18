package app.zipper.knot.hooks;

import android.view.ViewGroup;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Reflect;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class HiddenChatRowHook implements BaseHook {

  private static final float HIDDEN_ALPHA = 0.4f;

  // Invisible. Without it a row's data stays equal when its chat is hidden or unhidden in place,
  // so the list doesn't redraw it.
  private static final String HIDDEN_MARK = "\u2060";

  private static final ThreadLocal<Boolean> inHiddenRow = ThreadLocal.withInitial(() -> false);

  @Override
  public void hook(KnotConfig options, LoadParam lpparam) throws Throwable {
    LineVersion.Config version = LineVersion.get();
    if (version == null) return;
    ClassLoader cl = lpparam.classLoader;
    LineVersion.Config.PermanentHideChat cfg = version.permanentHideChat;

    hookChatTabRows(cl, cfg);
    hookChatListRows(cl, cfg);
  }

  private static void hookRowData(Class<?> dataCls) {
    try {
      for (Constructor<?> ctor : dataCls.getDeclaredConstructors()) {
        Class<?>[] params = ctor.getParameterTypes();
        if (params.length < 2 || params[0] != String.class || params[1] != String.class) continue;
        Knot.module
            .hook(ctor)
            .intercept(
                chain -> {
                  String name = (String) chain.getArg(1);
                  if (name == null || name.isEmpty()) return chain.proceed();
                  String marked = mark(name, (String) chain.getArg(0));
                  if (marked.equals(name)) return chain.proceed();
                  Object[] args = chain.getArgs().toArray();
                  args[1] = marked;
                  return chain.proceed(args);
                });
      }
    } catch (Throwable t) {
      Knot.log("Knot: HiddenChatRow row data hook error (" + dataCls.getName() + "): " + t);
    }
  }

  // Copies of a row pass the marked name back in
  private static String mark(String name, String chatId) {
    String plain =
        name.endsWith(HIDDEN_MARK) ? name.substring(0, name.length() - HIDDEN_MARK.length()) : name;
    return PermanentHideChatHook.isHidden(chatId) ? plain + HIDDEN_MARK : plain;
  }

  // Rows ignore their modifier argument, so the alpha goes on the row layout they share
  private static void hookChatTabRows(ClassLoader cl, LineVersion.Config.PermanentHideChat cfg) {
    if (cfg.chatTabItemClass.isEmpty()) return;
    try {
      hookRowData(Reflect.findClass(cfg.chatTabRowDataClass, cl));
      Method alphaModifier = alphaModifier(cl, cfg);
      Class<?> modifierCls = alphaModifier.getParameterTypes()[0];
      Class<?> rowCls = Reflect.findClass(cfg.chatTabRowClass, cl);
      Method rowKey = Reflect.findMethodExact(rowCls, cfg.methodChatTabRowKey);

      for (Method m : Reflect.findMethodsByName(cfg.chatTabItemClass, cl, cfg.methodChatTabItem)) {
        int rowIndex = Reflect.paramIndex(m, rowCls);
        if (rowIndex < 0) continue;
        Knot.module
            .hook(m)
            .intercept(
                chain -> {
                  if (!isHiddenRow(rowKey, chain.getArg(rowIndex))) return chain.proceed();
                  inHiddenRow.set(true);
                  try {
                    return chain.proceed();
                  } finally {
                    inHiddenRow.set(false);
                  }
                });
        break;
      }

      for (Method m :
          Reflect.findMethodsByName(cfg.chatTabRowLayoutClass, cl, cfg.methodChatTabRowLayout)) {
        int modifierIndex = Reflect.paramIndex(m, modifierCls);
        if (modifierIndex < 0) continue;
        Knot.module
            .hook(m)
            .intercept(
                chain -> {
                  Object dimmed =
                      inHiddenRow.get() ? dim(alphaModifier, chain.getArg(modifierIndex)) : null;
                  if (dimmed == null) return chain.proceed();
                  Object[] args = chain.getArgs().toArray();
                  args[modifierIndex] = dimmed;
                  return chain.proceed(args);
                });
        break;
      }
    } catch (Throwable t) {
      Knot.log("Knot: HiddenChatRow chat tab hook error: " + t);
    }
  }

  private static Method alphaModifier(ClassLoader cl, LineVersion.Config.PermanentHideChat cfg) {
    for (Method m :
        Reflect.findMethodsByName(cfg.alphaModifierClass, cl, cfg.methodAlphaModifier)) {
      if (m.getParameterCount() == 2 && m.getParameterTypes()[1] == float.class) return m;
    }
    throw new NoSuchMethodError(cfg.alphaModifierClass + "#" + cfg.methodAlphaModifier);
  }

  private static boolean isHiddenRow(Method rowKey, Object row) {
    if (row == null || !PermanentHideChatHook.showingHidden()) return false;
    try {
      return PermanentHideChatHook.isHidden((String) rowKey.invoke(row));
    } catch (Throwable t) {
      Knot.log("Knot: HiddenChatRow row key error: " + t);
      return false;
    }
  }

  private static Object dim(Method alphaModifier, Object modifier) {
    if (modifier == null) return null;
    try {
      return alphaModifier.invoke(null, modifier, HIDDEN_ALPHA);
    } catch (Throwable t) {
      Knot.log("Knot: HiddenChatRow alpha error: " + t);
      return null;
    }
  }

  private static void hookChatListRows(ClassLoader cl, LineVersion.Config.PermanentHideChat cfg) {
    if (cfg.chatListRowBinderClass.isEmpty()) return;
    try {
      Class<?> itemCls =
          Reflect.findClass(cfg.chatListHideItemClass, cl)
              .getDeclaredField(cfg.chatListHideItemField)
              .getType();
      hookRowData(Reflect.findMethodExact(itemCls, cfg.methodChatListItemInfo).getReturnType());
      Class<?> holderCls = Reflect.findClass(cfg.viewHolderClass, cl);
      for (Method m :
          Reflect.findMethodsByName(cfg.chatListRowBinderClass, cl, cfg.methodChatListRowBind)) {
        int itemIndex = Reflect.paramIndex(m, itemCls);
        int holderIndex = Reflect.paramIndex(m, holderCls);
        if (itemIndex < 0 || holderIndex < 0) continue;
        Knot.module
            .hook(m)
            .intercept(
                chain -> {
                  Object result = chain.proceed();
                  dimChatListRow(cfg, chain.getArg(itemIndex), chain.getArg(holderIndex));
                  return result;
                });
        return;
      }
      Knot.log("Knot: HiddenChatRow chat list bind method not found");
    } catch (Throwable t) {
      Knot.log("Knot: HiddenChatRow chat list hook error: " + t);
    }
  }

  // The item animator fades the row view itself, so the children carry the alpha
  private static void dimChatListRow(
      LineVersion.Config.PermanentHideChat cfg, Object item, Object holder) {
    try {
      ViewGroup row = (ViewGroup) Reflect.getObjectField(holder, cfg.viewHolderViewField);
      String chatId =
          (String)
              Reflect.getObjectField(
                  Reflect.callMethod(item, cfg.methodChatListItemInfo),
                  cfg.chatListItemChatIdField);
      float alpha = PermanentHideChatHook.isHidden(chatId) ? HIDDEN_ALPHA : 1f;
      for (int i = 0; i < row.getChildCount(); i++) row.getChildAt(i).setAlpha(alpha);
    } catch (Throwable t) {
      Knot.log("Knot: HiddenChatRow chat list row error: " + t);
    }
  }
}
