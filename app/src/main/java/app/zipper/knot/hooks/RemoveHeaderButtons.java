package app.zipper.knot.hooks;

import android.content.Context;
import android.view.View;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.Reflect;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class RemoveHeaderButtons implements BaseHook {

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    LineVersion.Config cfg = LineVersion.get();
    if (cfg == null) return;

    if (!config.removeAiFriendsButton.enabled
        && !config.removeSearchBarAgentIButton.enabled
        && !config.removeOpenChatButton.enabled
        && !config.removeAlbumButton.enabled
        && !config.removeCalendarButton.enabled) return;

    if (config.removeSearchBarAgentIButton.enabled) {
      hookHomeSearchBarAiButton(cfg, lpparam.classLoader);
    }

    if (cfg.talkTabHeader.chatTabHeaderStateClass.isEmpty()) return;

    Class<?> cls =
        Reflect.findClass(cfg.talkTabHeader.chatTabHeaderStateClass, lpparam.classLoader);
    Class<?> iconTypeCls = Reflect.findClass(cfg.talkTabHeader.iconTypeClass, lpparam.classLoader);

    final Object aiFriend = firstAvailableValueOf(iconTypeCls, "AI_FRIEND", "AI_FRIENDS");
    Object album = safeValueOf(iconTypeCls, "ALBUM");
    Object openChat = safeValueOf(iconTypeCls, "OPEN_CHAT");
    Object calendar = safeValueOf(iconTypeCls, "CALENDAR");

    if (config.removeSearchBarAgentIButton.enabled) {
      hookSearchBarAiButton(cfg, cls);
    }

    if (!cfg.talkTabHeader.subDeviceOpenChatButtonClass.isEmpty()) {
      hookSubDeviceHeaderButton(
          cfg.talkTabHeader.subDeviceOpenChatButtonClass, lpparam.classLoader, "OPEN_CHAT");
    }

    if (!cfg.talkTabHeader.subDeviceAlbumButtonClass.isEmpty()) {
      hookSubDeviceHeaderButton(
          cfg.talkTabHeader.subDeviceAlbumButtonClass, lpparam.classLoader, "ALBUM");
    }

    Knot.hookAllCtors(
        cls,
        chain -> {
          Object result = chain.proceed();
          if (Main.options.removeAiFriendsButton.enabled
              || Main.options.removeOpenChatButton.enabled
              || Main.options.removeCalendarButton.enabled) {
            try {
              patchState(chain.getThisObject(), cfg, aiFriend, album, openChat, calendar);
            } catch (Exception e) {
              Knot.log("Knot: RemoveHeaderButtons constructor error: " + e);
            }
          }
          return result;
        });

    Knot.hookAll(
        cls,
        "createEndButtons",
        chain -> {
          Object result = chain.proceed();
          if (!Main.options.removeAiFriendsButton.enabled
              && !Main.options.removeOpenChatButton.enabled
              && !Main.options.removeCalendarButton.enabled) return result;
          List<?> list = (List<?>) result;
          if (list == null || list.isEmpty()) return result;
          try {
            return filterButtons(list, cfg, aiFriend, album, openChat, calendar);
          } catch (Exception e) {
            Knot.log("Knot: RemoveHeaderButtons createEndButtons error: " + e);
            return result;
          }
        });
  }

  private static void patchState(
      Object instance,
      LineVersion.Config cfg,
      Object aiFriend,
      Object album,
      Object openChat,
      Object calendar)
      throws Exception {
    Object iconState = Reflect.getObjectField(instance, cfg.talkTabHeader.iconListStateField);
    List<?> icons = (List<?>) Reflect.callMethod(iconState, "getValue");
    if (icons != null)
      Reflect.callMethod(
          iconState, "setValue", filterIcons(icons, aiFriend, album, openChat, calendar));

    Object btnState = Reflect.getObjectField(instance, cfg.talkTabHeader.buttonListStateField);
    List<?> buttons = (List<?>) Reflect.callMethod(btnState, "getValue");
    if (buttons != null)
      Reflect.callMethod(
          btnState, "setValue", filterButtons(buttons, cfg, aiFriend, album, openChat, calendar));
  }

  private static List<Object> filterIcons(
      List<?> icons, Object aiFriend, Object album, Object openChat, Object calendar) {
    boolean removeAi = Main.options.removeAiFriendsButton.enabled;
    boolean removeOc = Main.options.removeOpenChatButton.enabled;
    boolean removeCal = Main.options.removeCalendarButton.enabled;
    List<Object> out = new ArrayList<>();
    for (Object icon : icons) {
      if (removeAi && (icon == aiFriend || icon == album)) continue;
      if (removeOc && icon == openChat) continue;
      if (removeCal && icon == calendar) continue;
      out.add(icon);
    }
    return out;
  }

  private static List<Object> filterButtons(
      List<?> buttons,
      LineVersion.Config cfg,
      Object aiFriend,
      Object album,
      Object openChat,
      Object calendar)
      throws Exception {
    boolean removeAi = Main.options.removeAiFriendsButton.enabled;
    boolean removeOc = Main.options.removeOpenChatButton.enabled;
    boolean removeCal = Main.options.removeCalendarButton.enabled;
    List<Object> out = new ArrayList<>();
    for (Object btn : buttons) {
      Object type = Reflect.getObjectField(btn, cfg.talkTabHeader.iconTypeFieldInButton);
      if (removeAi && (type == aiFriend || type == album)) continue;
      if (removeOc && type == openChat) continue;
      if (removeCal && type == calendar) continue;
      out.add(btn);
    }
    return out;
  }

  private static Object safeValueOf(Class<?> cls, String name) {
    try {
      return Reflect.callStaticMethod(cls, "valueOf", name);
    } catch (Exception e) {
      return null;
    }
  }

  private static Object firstAvailableValueOf(Class<?> cls, String... names) {
    for (String name : names) {
      Object value = safeValueOf(cls, name);
      if (value != null) return value;
    }
    return null;
  }

  private static void hookSearchBarAiButton(LineVersion.Config cfg, Class<?> cls) {
    Method searchBarAiVisible =
        findZeroArgMethod(cls, cfg.searchBarAgentI.talkVisibleMethod, boolean.class);
    Method searchBarAiClick =
        findZeroArgMethod(cls, cfg.searchBarAgentI.talkClickMethod, void.class);

    if (searchBarAiVisible != null) {
      Knot.module
          .hook(searchBarAiVisible)
          .intercept(
              chain -> {
                if (Main.options.removeSearchBarAgentIButton.enabled) return false;
                return chain.proceed();
              });
    } else {
      Knot.log("Knot: RemoveHeaderButtons could not find search bar AI visibility method.");
    }

    if (searchBarAiClick != null) {
      Knot.module
          .hook(searchBarAiClick)
          .intercept(
              chain -> {
                if (Main.options.removeSearchBarAgentIButton.enabled) return null;
                return chain.proceed();
              });
    }

    if (searchBarAiVisible != null || searchBarAiClick != null) {
      Knot.log("Knot: RemoveHeaderButtons hooked Talk search bar Agent i button.");
    }
  }

  private static void hookSubDeviceHeaderButton(
      String className, ClassLoader classLoader, String type) {
    try {
      Class<?> subCls = Reflect.findClass(className, classLoader);
      Knot.hookAll(
          subCls,
          "getVisibility",
          chain -> {
            boolean shouldRemove =
                type.equals("OPEN_CHAT")
                    ? Main.options.removeOpenChatButton.enabled
                    : Main.options.removeAlbumButton.enabled;
            if (shouldRemove) return View.GONE;
            return chain.proceed();
          });
      Knot.log("Knot: RemoveHeaderButtons hooked sub-device button " + className);
    } catch (Throwable t) {
      Knot.log(
          "Knot: RemoveHeaderButtons could not hook sub-device button " + className + ": " + t);
    }
  }

  private static Method findZeroArgMethod(Class<?> cls, String methodName, Class<?> returnType) {
    if (methodName == null || methodName.isEmpty()) return null;
    try {
      Method method = cls.getDeclaredMethod(methodName);
      if (method.getReturnType() == returnType) {
        method.setAccessible(true);
        return method;
      }
    } catch (NoSuchMethodException e) {
    }
    return null;
  }

  private static void hookHomeSearchBarAiButton(LineVersion.Config cfg, ClassLoader classLoader) {
    if (cfg.searchBarAgentI.homeSearchBarClass.isEmpty()
        || cfg.searchBarAgentI.homeRefreshMethod.isEmpty()) return;

    Class<?> cls;
    try {
      cls = Reflect.findClass(cfg.searchBarAgentI.homeSearchBarClass, classLoader);
    } catch (Throwable t) {
      Knot.log("Knot: RemoveHeaderButtons could not find Home search bar class.");
      return;
    }

    XposedInterface.Hooker patchHook =
        chain -> {
          Object result = chain.proceed();
          if (Main.options.removeSearchBarAgentIButton.enabled) {
            try {
              patchHomeSearchBarAiButton(cfg, chain.getThisObject());
            } catch (Exception e) {
              Knot.log("Knot: RemoveHeaderButtons Home search bar error: " + e);
            }
          }
          return result;
        };

    Knot.hookAllCtors(cls, patchHook);
    Knot.hookAll(cls, cfg.searchBarAgentI.homeRefreshMethod, patchHook);
    Knot.log("Knot: RemoveHeaderButtons hooked Home search bar Agent i button.");
  }

  private static void patchHomeSearchBarAiButton(LineVersion.Config cfg, Object instance) {
    if (!isHomeSearchBar(cfg, instance)) return;

    View rootView = (View) Reflect.getObjectField(instance, cfg.searchBarAgentI.homeRootViewField);
    if (rootView == null) return;

    Context context = rootView.getContext();
    int aiContainerId = cfg.searchBarAgentI.homeAiContainerId;
    if (aiContainerId == 0) return;

    View aiContainer = rootView.findViewById(aiContainerId);
    if (aiContainer == null) return;

    aiContainer.setOnClickListener(null);
    aiContainer.setClickable(false);
    aiContainer.setVisibility(View.GONE);

    int guidelineId = cfg.searchBarAgentI.homeGuidelineId;
    View guidelineView = guidelineId != 0 ? rootView.findViewById(guidelineId) : null;
    if (guidelineView != null && cfg.searchBarAgentI.homeGuidelineEndDp > 0) {
      if (cfg.searchBarAgentI.homeGuidelineClass.equals(guidelineView.getClass().getName())) {
        try {
          Reflect.callMethod(
              guidelineView,
              "setGuidelineEnd",
              dpToPx(context, cfg.searchBarAgentI.homeGuidelineEndDp));
        } catch (Throwable t) {
          Knot.log("Knot: RemoveHeaderButtons guideline error: " + t);
        }
      }
    }
  }

  private static boolean isHomeSearchBar(LineVersion.Config cfg, Object instance) {
    if (cfg.searchBarAgentI.homeTabTypeField.isEmpty()) return false;

    Object tabType = Reflect.getObjectField(instance, cfg.searchBarAgentI.homeTabTypeField);
    if (!(tabType instanceof Enum<?>)) return false;
    String name = ((Enum<?>) tabType).name();
    return name.equals(cfg.searchBarAgentI.homeTabName)
        || name.equals(cfg.searchBarAgentI.homeTabV2Name)
        || name.equals("CHAT");
  }

  private static int dpToPx(Context context, int dp) {
    return Math.round(dp * context.getResources().getDisplayMetrics().density);
  }
}
