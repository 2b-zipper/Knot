package app.zipper.knot.hooks;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.R;
import app.zipper.knot.Reflect;
import app.zipper.knot.utils.LineDBUtils;
import app.zipper.knot.utils.ModuleResources;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class UnhideChatHook implements BaseHook {

  // Set only while the unhide popup's message and confirm label are composed
  private static final ThreadLocal<Boolean> inUnhidePopup = ThreadLocal.withInitial(() -> false);
  private static volatile Object popupRequest;
  private static volatile boolean popupUnhides;

  @Override
  public void hook(KnotConfig options, LoadParam lpparam) throws Throwable {
    LineVersion.Config version = LineVersion.get();
    if (version == null) return;
    ClassLoader cl = lpparam.classLoader;
    LineVersion.Config.PermanentHideChat cfg = version.permanentHideChat;

    hookChatTabMenuLabel(cl, cfg);
    hookHidePopupRequest(cl, cfg);
    hookHidePopupMessage(cl, cfg);
    hookHidePopupConfirmButton(cl, cfg);
    hookStringResource(cl, cfg);

    hookChatListMenu(cl, cfg);
  }

  private static boolean isUnhideTarget(String chatId) {
    return PermanentHideChatHook.showingHidden() && LineDBUtils.isChatArchived(chatId);
  }

  private static void hookChatTabMenuLabel(
      ClassLoader cl, LineVersion.Config.PermanentHideChat cfg) {
    if (cfg.chatMenuHideItemClass.isEmpty()) return;
    try {
      Knot.module
          .hook(
              Reflect.findMethodExact(
                  cfg.chatMenuHideItemClass,
                  cl,
                  cfg.methodChatMenuItemLabel,
                  cfg.chatPreviewItemClass))
          .intercept(
              chain -> {
                Object label = chain.proceed();
                try {
                  String chatId =
                      (String) Reflect.getObjectField(chain.getArg(0), cfg.chatPreviewChatIdField);
                  int unhide = isUnhideTarget(chatId) ? lineStringId(cfg.unhideLabelRes) : 0;
                  return unhide != 0 ? unhide : label;
                } catch (Throwable t) {
                  Knot.log("Knot: UnhideChat chat tab menu error: " + t);
                  return label;
                }
              });
    } catch (Throwable t) {
      Knot.log("Knot: UnhideChat chat tab menu hook error: " + t);
    }
  }

  private static void hookHidePopupRequest(
      ClassLoader cl, LineVersion.Config.PermanentHideChat cfg) {
    if (cfg.hidePopupClass.isEmpty()) return;
    try {
      Class<?> requestCls = Reflect.findClass(cfg.hidePopupRequestClass, cl);
      for (Method m : Reflect.findMethodsByName(cfg.hidePopupClass, cl, cfg.methodHidePopup)) {
        int requestIndex = Reflect.paramIndex(m, requestCls);
        if (requestIndex < 0) continue;
        Knot.module
            .hook(m)
            .intercept(
                chain -> {
                  Object request = chain.getArg(requestIndex);
                  if (request != popupRequest) {
                    popupRequest = request;
                    popupUnhides = isUnhidePopup(request, cfg);
                  }
                  return chain.proceed();
                });
        return;
      }
      Knot.log("Knot: UnhideChat hide popup method not found");
    } catch (Throwable t) {
      Knot.log("Knot: UnhideChat hide popup hook error: " + t);
    }
  }

  private static boolean isUnhidePopup(Object request, LineVersion.Config.PermanentHideChat cfg) {
    try {
      return isUnhideTarget((String) Reflect.getObjectField(request, cfg.hidePopupChatIdField));
    } catch (Throwable t) {
      Knot.log("Knot: UnhideChat hide popup error: " + t);
      return false;
    }
  }

  private static void hookHidePopupMessage(
      ClassLoader cl, LineVersion.Config.PermanentHideChat cfg) {
    if (cfg.hidePopupMessageClass.isEmpty()) return;
    try {
      Knot.module
          .hook(
              Reflect.findMethodExact(
                  cfg.hidePopupMessageClass, cl, "invoke", Object.class, Object.class))
          .intercept(UnhideChatHook::withinUnhidePopup);
    } catch (Throwable t) {
      Knot.log("Knot: UnhideChat hide popup message hook error: " + t);
    }
  }

  // Matched by label because button lambdas are merged in some versions. The inner composable
  // recomposes alone on press.
  private static void hookHidePopupConfirmButton(
      ClassLoader cl, LineVersion.Config.PermanentHideChat cfg) {
    if (cfg.hidePopupLabelsClass.isEmpty()) return;
    try {
      Object confirmLabel =
          Reflect.getStaticObjectField(
              Reflect.findClass(cfg.hidePopupLabelsClass, cl), cfg.hidePopupConfirmLabelField);
      for (Method m : Reflect.findMethodsByName(cfg.textButtonClass, cl, cfg.methodTextButton)) {
        Knot.module
            .hook(m)
            .intercept(
                chain ->
                    popupUnhides && chain.getArgs().contains(confirmLabel)
                        ? withinUnhidePopup(chain)
                        : chain.proceed());
        return;
      }
      Knot.log("Knot: UnhideChat text button method not found");
    } catch (Throwable t) {
      Knot.log("Knot: UnhideChat hide popup button hook error: " + t);
    }
  }

  private static void hookStringResource(ClassLoader cl, LineVersion.Config.PermanentHideChat cfg) {
    if (cfg.stringResourceClass.isEmpty()) return;
    try {
      for (Method m :
          Reflect.findMethodsByName(cfg.stringResourceClass, cl, cfg.methodStringResource)) {
        // 26.14.0 and older take the resource id first
        int idIndex = Reflect.paramIndex(m, int.class);
        if (m.getParameterCount() != 2 || idIndex < 0) continue;
        Knot.module
            .hook(m)
            .intercept(
                chain -> {
                  if (!inUnhidePopup.get()) return chain.proceed();
                  String text = unhidePopupText(cfg, (int) chain.getArg(idIndex));
                  return text != null ? text : chain.proceed();
                });
        return;
      }
      Knot.log("Knot: UnhideChat string resource method not found");
    } catch (Throwable t) {
      Knot.log("Knot: UnhideChat string resource hook error: " + t);
    }
  }

  private static Object withinUnhidePopup(XposedInterface.Chain chain) throws Throwable {
    if (!popupUnhides) return chain.proceed();
    boolean outer = inUnhidePopup.get();
    inUnhidePopup.set(true);
    try {
      return chain.proceed();
    } finally {
      inUnhidePopup.set(outer);
    }
  }

  private static String unhidePopupText(LineVersion.Config.PermanentHideChat cfg, int id) {
    if (id == lineStringId(cfg.hideLabelRes)) return lineString(cfg.unhideLabelRes);
    if (id == lineStringId(cfg.hideConfirmMessageRes)
        || id == lineStringId(cfg.hideConfirmSquareMessageRes)) {
      return ModuleResources.get(R.string.unhide_chat_confirm_message);
    }
    return null;
  }

  private static void hookChatListMenu(ClassLoader cl, LineVersion.Config.PermanentHideChat cfg) {
    if (cfg.chatListHideItemClass.isEmpty()) return;
    try {
      Class<?> menuItemCls = Reflect.findClass(cfg.chatListMenuItemClass, cl);
      Class<?> actionCls = Reflect.findClass(cfg.chatListMenuActionClass, cl);
      Class<?> confirmCls = Reflect.findClass(cfg.chatListHideConfirmClass, cl);
      Knot.module
          .hook(Reflect.findMethodExact(cfg.chatListHideItemClass, cl, cfg.methodChatListMenuItem))
          .intercept(
              chain -> {
                Object menuItem = chain.proceed();
                try {
                  Object creator = chain.getThisObject();
                  Object item = Reflect.getObjectField(creator, cfg.chatListHideItemField);
                  Object listener = Reflect.getObjectField(creator, cfg.chatListHideListenerField);
                  String chatId =
                      (String)
                          Reflect.getObjectField(
                              Reflect.callMethod(item, cfg.methodChatListItemInfo),
                              cfg.chatListItemChatIdField);
                  if (!confirmCls.isInstance(listener) || !isUnhideTarget(chatId)) {
                    return menuItem;
                  }
                  int unhide = lineStringId(cfg.unhideLabelRes);
                  if (unhide == 0) return menuItem;
                  Runnable onClick = () -> showUnhideDialog(cl, cfg, listener, item);
                  return Reflect.newInstance(
                      menuItemCls, unhide, newAction(cl, actionCls, onClick));
                } catch (Throwable t) {
                  Knot.log("Knot: UnhideChat chat list menu error: " + t);
                  return menuItem;
                }
              });
    } catch (Throwable t) {
      Knot.log("Knot: UnhideChat chat list menu hook error: " + t);
    }
  }

  private static Object newAction(ClassLoader cl, Class<?> actionCls, Runnable onInvoke) {
    return Proxy.newProxyInstance(
        cl,
        new Class[] {actionCls},
        (proxy, method, args) -> {
          switch (method.getName()) {
            case "invoke":
              onInvoke.run();
              return null;
            case "equals":
              return proxy == args[0];
            case "hashCode":
              return System.identityHashCode(proxy);
            case "toString":
              return "UnhideChat";
            default:
              return null;
          }
        });
  }

  // Same builder and confirm callback as the chat list's own hide dialog
  private static void showUnhideDialog(
      ClassLoader cl, LineVersion.Config.PermanentHideChat cfg, Object listener, Object item) {
    try {
      Activity activity = SettingsUIInjector.getForegroundActivity();
      if (activity == null) {
        Knot.log("Knot: UnhideChat dialog has no foreground activity");
        return;
      }
      Object builder = Reflect.newInstance(Reflect.findClass(cfg.dialogBuilderClass, cl), activity);
      Reflect.setObjectField(
          builder,
          cfg.fieldDialogMessage,
          ModuleResources.get(R.string.unhide_chat_confirm_message));
      DialogInterface.OnClickListener onUnhide =
          (dialog, which) -> {
            try {
              Reflect.callMethod(listener, cfg.methodConfirmHide, Knot.currentApplication(), item);
            } catch (Throwable t) {
              Knot.log("Knot: UnhideChat unhide failed: " + t);
            }
          };
      Reflect.callMethod(
          builder, cfg.methodDialogPositive, lineStringId(cfg.unhideLabelRes), onUnhide);
      Reflect.callMethod(builder, cfg.methodDialogNegative, lineStringId(cfg.cancelLabelRes), null);
      Reflect.callMethod(builder, cfg.methodDialogShow);
    } catch (Throwable t) {
      Knot.log("Knot: UnhideChat dialog error: " + t);
    }
  }

  private static int lineStringId(String name) {
    Context ctx = Knot.currentApplication();
    if (ctx == null || name.isEmpty()) return 0;
    return ctx.getResources().getIdentifier(name, "string", ctx.getPackageName());
  }

  private static String lineString(String name) {
    int id = lineStringId(name);
    return id == 0 ? null : Knot.currentApplication().getString(id);
  }
}
