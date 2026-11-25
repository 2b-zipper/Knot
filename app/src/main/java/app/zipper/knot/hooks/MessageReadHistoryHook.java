package app.zipper.knot.hooks;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.R;
import app.zipper.knot.Reflect;
import app.zipper.knot.SettingsStore;
import app.zipper.knot.ui.MessageReadSheet;
import app.zipper.knot.utils.ModuleResources;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public class MessageReadHistoryHook implements BaseHook {

  // Repurposed only for non-Square own messages; Square chats keep the original item intact.
  private static final String PLACEHOLDER_ITEM = "UNSEND_SQUARE_MESSAGE";
  private static final String ICON_DRAWABLE = "checks";
  private static final int ICON_ID = 0x64000011;
  private static final String APPCOMPAT_IMAGE_VIEW = "androidx.appcompat.widget.AppCompatImageView";

  private static LineVersion.Config.MessageEditHistory cfg;
  private static volatile boolean activeMenuInjected = false;
  private static volatile Object menuItem;
  private static volatile Object menuAction;

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    if (!config.recordReadHistory.enabled) return;
    LineVersion.Config version = LineVersion.get();
    if (version == null || version.messageEditHistory.menuListBuilderClass.isEmpty()) return;
    cfg = version.messageEditHistory;

    hookMenu(lpparam.classLoader);
    hookIcon(lpparam.classLoader);
  }

  private static void hookMenu(ClassLoader cl) {
    try {
      Class<?> presentationEnum = Reflect.findClass(cfg.menuPresentationEnumClass, cl);

      Knot.hookAll(
          Reflect.findClass(cfg.menuListBuilderClass, cl),
          cfg.menuListMethod,
          chain -> {
            Object result = chain.proceed();
            if (!recordingEnabled() || !(result instanceof List)) {
              activeMenuInjected = false;
              return result;
            }
            List<?> items = (List<?>) result;
            Object item = menuItem(cl);
            Object data = messageData(chain.getArg(2));
            boolean inject = shouldInject(data, items, item) && action(cl) != null;
            activeMenuInjected = inject;
            return inject ? withItem(items, item) : result;
          });

      Knot.module
          .hook(Reflect.findMethodExact(presentationEnum, cfg.methodMenuLabel, Context.class))
          .intercept(
              chain ->
                  isActivePlaceholder(chain)
                      ? ModuleResources.get(R.string.read_history_title)
                      : chain.proceed());

      Knot.module
          .hook(Reflect.findMethodExact(presentationEnum, cfg.methodMenuIcon))
          .intercept(chain -> isActivePlaceholder(chain) ? ICON_ID : chain.proceed());

      Knot.module
          .hook(Reflect.findMethodExact(presentationEnum, cfg.methodMenuActionAccessor))
          .intercept(
              chain -> {
                Object action = isActivePlaceholder(chain) ? action(cl) : null;
                return action != null ? action : chain.proceed();
              });
    } catch (Throwable t) {
      Knot.log("Knot: message read history menu hook failed: " + t);
    }
  }

  private static void hookIcon(ClassLoader cl) {
    try {
      Knot.module
          .hook(Reflect.findMethodExact(APPCOMPAT_IMAGE_VIEW, cl, "setImageResource", int.class))
          .intercept(
              chain -> {
                if ((int) chain.getArg(0) != ICON_ID) return chain.proceed();
                Drawable icon = ModuleResources.drawable(ICON_DRAWABLE);
                if (icon == null) return chain.proceed();
                ((ImageView) chain.getThisObject()).setImageDrawable(icon);
                return null;
              });
    } catch (Throwable t) {
      Knot.log("Knot: message read history icon hook failed: " + t);
    }
  }

  private static boolean recordingEnabled() {
    return SettingsStore.get("record_read_history", false);
  }

  private static boolean shouldInject(Object data, List<?> items, Object item) {
    if (data == null || item == null || items.contains(item)) return false;
    if (!isMyMessage(data)) return false;
    String chatId = chatId(data);
    // OpenChat (Square) IDs start with "m"; keep their native UNSEND_SQUARE_MESSAGE item intact.
    if (chatId == null || chatId.startsWith("m")) return false;
    return messageId(data) != null;
  }

  private static List<Object> withItem(List<?> items, Object item) {
    List<Object> copy = new ArrayList<>(items);
    copy.add(item);
    return copy;
  }

  private static boolean isActivePlaceholder(XposedInterface.Chain chain) {
    Object self = chain.getThisObject();
    return activeMenuInjected
        && recordingEnabled()
        && self instanceof Enum
        && PLACEHOLDER_ITEM.equals(((Enum<?>) self).name());
  }

  private static Object menuItem(ClassLoader cl) {
    if (menuItem == null) {
      try {
        menuItem = enumConstant(Reflect.findClass(cfg.menuItemEnumClass, cl), PLACEHOLDER_ITEM);
      } catch (Throwable t) {
        Knot.log("Knot: message read history menu item unavailable: " + t);
      }
    }
    return menuItem;
  }

  private static Object action(ClassLoader cl) {
    if (menuAction == null) {
      try {
        menuAction = buildAction(cl);
      } catch (Throwable t) {
        Knot.log("Knot: message read history menu action unavailable: " + t);
      }
    }
    return menuAction;
  }

  private static Object buildAction(ClassLoader cl) throws Throwable {
    Class<?> lambdaClass = Reflect.findClass(cfg.menuActionLambdaClass, cl);
    Object callback =
        Proxy.newProxyInstance(
            cl,
            new Class<?>[] {lambdaClass.getDeclaredConstructors()[0].getParameterTypes()[0]},
            (proxy, method, args) -> {
              if (args != null && args.length == 3 && args[0] instanceof Context) {
                Object data = messageData(args[1]);
                String chatId = chatId(data);
                String msgId = messageId(data);
                if (chatId != null && msgId != null) {
                  MessageReadSheet.show((Context) args[0], chatId, msgId);
                }
              }
              return kotlinUnit(cl);
            });
    return Reflect.newInstance(lambdaClass, callback);
  }

  private static Object messageData(Object menuContext) {
    if (menuContext == null) return null;
    try {
      Object holder = Reflect.getObjectField(menuContext, cfg.menuContextMessageField);
      return holder == null ? null : Reflect.getObjectField(holder, cfg.menuMessageDataField);
    } catch (Throwable t) {
      return null;
    }
  }

  private static boolean isMyMessage(Object data) {
    if (data == null || cfg.menuIsMyMessageField.isEmpty()) return false;
    try {
      return Boolean.TRUE.equals(Reflect.getObjectField(data, cfg.menuIsMyMessageField));
    } catch (Throwable t) {
      return false;
    }
  }

  private static String chatId(Object data) {
    if (data == null || cfg.menuMessageChatIdField.isEmpty()) return null;
    try {
      Object value = Reflect.getObjectField(data, cfg.menuMessageChatIdField);
      return value instanceof String && !((String) value).isEmpty() ? (String) value : null;
    } catch (Throwable t) {
      return null;
    }
  }

  private static String messageId(Object data) {
    if (data == null) return null;
    try {
      Object id = Reflect.getObjectField(data, cfg.menuMessageIdField);
      String value = id == null ? "" : String.valueOf(id);
      return value.isEmpty() || "0".equals(value) ? null : value;
    } catch (Throwable t) {
      return null;
    }
  }

  private static Object kotlinUnit(ClassLoader cl) {
    try {
      LineVersion.Config.Kotlin kotlin = LineVersion.get().kotlin;
      return Reflect.findClass(kotlin.unitClass, cl).getField(kotlin.fieldUnitInstance).get(null);
    } catch (Throwable t) {
      return null;
    }
  }

  private static Object enumConstant(Class<?> enumClass, String name) {
    for (Object constant : enumClass.getEnumConstants()) {
      if (((Enum<?>) constant).name().equals(name)) return constant;
    }
    return null;
  }
}
