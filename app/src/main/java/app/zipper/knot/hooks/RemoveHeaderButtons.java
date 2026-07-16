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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

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
      hookMiniTabAgentButton(cfg, lpparam.classLoader);
      hookHome26AgentButton(cfg, lpparam.classLoader);
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
          cfg.talkTabHeader.subDeviceOpenChatButtonClass,
          lpparam.classLoader,
          () -> Main.options.removeOpenChatButton.enabled);
    }

    if (!cfg.talkTabHeader.subDeviceAlbumButtonClass.isEmpty()) {
      hookSubDeviceHeaderButton(
          cfg.talkTabHeader.subDeviceAlbumButtonClass,
          lpparam.classLoader,
          () -> Main.options.removeAlbumButton.enabled);
    }

    Knot.hookAllCtors(
        cls,
        chain -> {
          Object result = chain.proceed();
          if (Main.options.removeAiFriendsButton.enabled
              || Main.options.removeOpenChatButton.enabled
              || Main.options.removeCalendarButton.enabled) {
            try {
              patchState(
                  chain.getThisObject(), cfg, hiddenTypes(aiFriend, album, openChat, calendar));
            } catch (Exception e) {
              Knot.log("Knot: RemoveHeaderButtons constructor error: " + e);
            }
          }
          return result;
        });
  }

  private static void patchState(Object instance, LineVersion.Config cfg, Set<Object> hiddenTypes) {
    Object iconState = Reflect.getObjectField(instance, cfg.talkTabHeader.iconListStateField);
    List<?> icons = (List<?>) Reflect.callMethod(iconState, "getValue");
    if (icons != null) {
      Reflect.callMethod(iconState, "setValue", filterByType(icons, icon -> icon, hiddenTypes));
    }

    Object btnState = Reflect.getObjectField(instance, cfg.talkTabHeader.buttonListStateField);
    List<?> buttons = (List<?>) Reflect.callMethod(btnState, "getValue");
    if (buttons != null) {
      Function<Object, Object> typeOfButton =
          btn -> Reflect.getObjectField(btn, cfg.talkTabHeader.iconTypeFieldInButton);
      Reflect.callMethod(btnState, "setValue", filterByType(buttons, typeOfButton, hiddenTypes));
    }
  }

  private static Set<Object> hiddenTypes(
      Object aiFriend, Object album, Object openChat, Object calendar) {
    Set<Object> hidden = new HashSet<>();
    if (Main.options.removeAiFriendsButton.enabled) {
      addIfPresent(hidden, aiFriend);
      addIfPresent(hidden, album);
    }
    if (Main.options.removeOpenChatButton.enabled) addIfPresent(hidden, openChat);
    if (Main.options.removeCalendarButton.enabled) addIfPresent(hidden, calendar);
    return hidden;
  }

  private static void addIfPresent(Set<Object> set, Object value) {
    if (value != null) set.add(value);
  }

  private static List<Object> filterByType(
      List<?> items, Function<Object, Object> typeOf, Set<Object> hiddenTypes) {
    List<Object> out = new ArrayList<>();
    for (Object item : items) {
      if (!hiddenTypes.contains(typeOf.apply(item))) out.add(item);
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
    Method visible = findZeroArgMethod(cls, cfg.searchBarAgentI.talkVisibleMethod, boolean.class);
    Method click = findZeroArgMethod(cls, cfg.searchBarAgentI.talkClickMethod, void.class);

    if (visible == null) {
      Knot.log("Knot: RemoveHeaderButtons could not find search bar AI visibility method.");
    }
    if (click == null) {
      Knot.log("Knot: RemoveHeaderButtons could not find search bar AI click method.");
    }

    hookWhenSearchBarAiDisabled(visible, false);
    hookWhenSearchBarAiDisabled(click, null);

    if (visible != null || click != null) {
      Knot.log("Knot: RemoveHeaderButtons hooked Talk search bar Agent i button.");
    }
  }

  private static void hookWhenSearchBarAiDisabled(Method method, Object disabledResult) {
    if (method == null) return;
    Knot.module
        .hook(method)
        .intercept(
            chain -> {
              if (Main.options.removeSearchBarAgentIButton.enabled) return disabledResult;
              return chain.proceed();
            });
  }

  private static void hookSubDeviceHeaderButton(
      String className, ClassLoader classLoader, BooleanSupplier shouldRemove) {
    try {
      Class<?> subCls = Reflect.findClass(className, classLoader);
      Knot.hookAll(
          subCls,
          "getVisibility",
          chain -> shouldRemove.getAsBoolean() ? View.GONE : chain.proceed());
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

  private static void hookMiniTabAgentButton(LineVersion.Config cfg, ClassLoader classLoader) {
    if (cfg.searchBarAgentI.miniTabHeaderClass.isEmpty()
        || cfg.searchBarAgentI.miniTabAgentMethod.isEmpty()) return;

    try {
      Class<?> cls = Reflect.findClass(cfg.searchBarAgentI.miniTabHeaderClass, classLoader);
      Knot.hookAll(
          cls,
          cfg.searchBarAgentI.miniTabAgentMethod,
          chain -> {
            if (Main.options.removeSearchBarAgentIButton.enabled) return null;
            return chain.proceed();
          });
      Knot.log("Knot: RemoveHeaderButtons hooked mini-app tab Agent i button.");
    } catch (Throwable t) {
      Knot.log("Knot: RemoveHeaderButtons could not hook mini-app tab Agent i button: " + t);
    }
  }

  private static void hookHome26AgentButton(LineVersion.Config cfg, ClassLoader classLoader) {
    if (cfg.searchBarAgentI.home26NavIconClass.isEmpty()
        || cfg.searchBarAgentI.home26NavIconMethod.isEmpty()
        || cfg.searchBarAgentI.home26AgentDrawableId == 0) return;

    final int agentDrawableId = cfg.searchBarAgentI.home26AgentDrawableId;
    try {
      Class<?> cls = Reflect.findClass(cfg.searchBarAgentI.home26NavIconClass, classLoader);
      Knot.hookAll(
          cls,
          cfg.searchBarAgentI.home26NavIconMethod,
          chain -> {
            if (Main.options.removeSearchBarAgentIButton.enabled
                && chain.getArgs().contains(agentDrawableId)) {
              return null;
            }
            return chain.proceed();
          });
      Knot.log("Knot: RemoveHeaderButtons hooked HOME26 Agent i button.");
    } catch (Throwable t) {
      Knot.log("Knot: RemoveHeaderButtons could not hook HOME26 Agent i button: " + t);
    }
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
        || name.equals(cfg.searchBarAgentI.chatTabName)
        || name.equals(cfg.searchBarAgentI.newsTabName);
  }

  private static int dpToPx(Context context, int dp) {
    return Math.round(dp * context.getResources().getDisplayMetrics().density);
  }
}
