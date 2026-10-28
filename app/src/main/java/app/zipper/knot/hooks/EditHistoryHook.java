package app.zipper.knot.hooks;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.R;
import app.zipper.knot.Reflect;
import app.zipper.knot.SettingsStore;
import app.zipper.knot.ui.EditHistoryViewer;
import app.zipper.knot.utils.LineDBUtils;
import app.zipper.knot.utils.ModuleResources;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Proxy;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONObject;

public class EditHistoryHook implements BaseHook {

  private static final String PLACEHOLDER_ITEM = "INVALID";
  private static final String ICON_DRAWABLE = "clock_edit";
  private static final int ICON_ID = 0x64000010;
  private static final String APPCOMPAT_IMAGE_VIEW = "androidx.appcompat.widget.AppCompatImageView";
  private static final String TIME_FMT = "yyyy/MM/dd HH:mm:ss";

  private static final Map<String, JSONArray> cache = new ConcurrentHashMap<>();
  private static final Object persistLock = new Object();
  private static volatile boolean loaded = false;
  private static LineVersion.Config.MessageEditHistory cfg;
  private static volatile Object menuItem;
  private static volatile Object menuAction;

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    if (!config.showEditHistory.enabled) return;
    LineVersion.Config version = LineVersion.get();
    if (version == null || version.messageEditHistory.editRequestClass.isEmpty()) return;
    cfg = version.messageEditHistory;

    new Thread(EditHistoryHook::loadCache, "Knot-EditHistory").start();
    hookCapture(lpparam.classLoader);
    hookMenu(lpparam.classLoader);
    hookIcon(lpparam.classLoader);
  }

  // LINE builds this request just before overwriting the row, so the DB still holds the old text.
  private static void hookCapture(ClassLoader cl) {
    try {
      Knot.hookAllCtors(
          Reflect.findClass(cfg.editRequestClass, cl),
          chain -> {
            Object result = chain.proceed();
            if (Main.options.showEditHistory.enabled) {
              try {
                Object request = chain.getThisObject();
                Object id = Reflect.getObjectField(request, cfg.editRequestIdField);
                Object text = Reflect.getObjectField(request, cfg.editRequestTextField);
                if (id != null && text != null) record(String.valueOf(id), text.toString());
              } catch (Throwable ignored) {
              }
            }
            return result;
          });
    } catch (Throwable t) {
      Knot.log("Knot: edit history capture hook failed: " + t);
    }
  }

  private static void record(String msgId, String newText) {
    if (msgId.isEmpty() || "null".equals(msgId)) return;
    loadCache();
    synchronized (persistLock) {
      JSONArray versions = cache.get(msgId);
      try {
        if (versions == null) {
          versions = new JSONArray();
          String oldText = LineDBUtils.resolveMessageContent(msgId);
          if (oldText != null && !oldText.isEmpty() && !oldText.equals(newText)) {
            long createdTime = LineDBUtils.resolveMessageCreatedTime(msgId);
            versions.put(version(oldText, createdTime > 0 ? formatTime(createdTime) : ""));
          }
        } else if (newText.equals(lastText(versions))) {
          return;
        }
        versions.put(version(newText, formatTime(System.currentTimeMillis())));
        cache.put(msgId, versions);
        persist();
      } catch (Throwable ignored) {
      }
    }
  }

  // INVALID is a never-displayed fallback constant, so its label/icon/action can be repurposed.
  private static void hookMenu(ClassLoader cl) {
    if (cfg.menuListBuilderClass.isEmpty()) return;
    try {
      Class<?> presentationEnum = Reflect.findClass(cfg.menuPresentationEnumClass, cl);

      hookMenuList(cl);

      Knot.module
          .hook(Reflect.findMethodExact(presentationEnum, cfg.methodMenuLabel, Context.class))
          .intercept(
              chain ->
                  isPlaceholder(chain)
                      ? ModuleResources.get(R.string.edit_history_title)
                      : chain.proceed());

      Knot.module
          .hook(Reflect.findMethodExact(presentationEnum, cfg.methodMenuIcon))
          .intercept(chain -> isPlaceholder(chain) ? ICON_ID : chain.proceed());

      Knot.module
          .hook(Reflect.findMethodExact(presentationEnum, cfg.methodMenuActionAccessor))
          .intercept(
              chain -> {
                Object action = isPlaceholder(chain) ? action(cl) : null;
                return action != null ? action : chain.proceed();
              });
    } catch (Throwable t) {
      Knot.log("Knot: edit history menu hook failed: " + t);
    }
  }

  private static void hookMenuList(ClassLoader cl) throws Throwable {
    Knot.hookAll(
        Reflect.findClass(cfg.menuListBuilderClass, cl),
        cfg.menuListMethod,
        chain -> {
          Object result = chain.proceed();
          if (!Main.options.showEditHistory.enabled || !(result instanceof List)) return result;
          if (!isEdited(messageData(chain.getArg(2)))) return result;
          Object item = menuItem(cl);
          return item != null && action(cl) != null ? withItem((List<?>) result, item) : result;
        });
  }

  private static List<Object> withItem(List<?> items, Object item) {
    List<Object> copy = new ArrayList<>(items);
    copy.add(item);
    return copy;
  }

  // By name, so the menu enums and the action proxy are not built at startup
  private static boolean isPlaceholder(XposedInterface.Chain chain) {
    Object self = chain.getThisObject();
    return Main.options.showEditHistory.enabled
        && self instanceof Enum
        && PLACEHOLDER_ITEM.equals(((Enum<?>) self).name());
  }

  private static Object menuItem(ClassLoader cl) {
    if (menuItem == null) {
      try {
        menuItem = enumConstant(Reflect.findClass(cfg.menuItemEnumClass, cl), PLACEHOLDER_ITEM);
      } catch (Throwable t) {
        Knot.log("Knot: edit history menu item unavailable: " + t);
      }
    }
    return menuItem;
  }

  private static Object action(ClassLoader cl) {
    if (menuAction == null) {
      try {
        menuAction = buildAction(cl);
      } catch (Throwable t) {
        Knot.log("Knot: edit history menu action unavailable: " + t);
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
                String msgId = messageId(messageData(args[1]));
                if (msgId != null) EditHistoryViewer.show((Context) args[0], msgId);
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

  private static boolean isEdited(Object data) {
    if (data == null) return false;
    try {
      return Boolean.TRUE.equals(Reflect.getObjectField(data, cfg.menuEditedFlagField));
    } catch (Throwable t) {
      return false;
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

  // Module drawables are unresolvable in LINE's Resources, so the icon is served under a fake id.
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
      Knot.log("Knot: edit history icon hook failed: " + t);
    }
  }

  public static JSONArray historyFor(String msgId) {
    if (msgId == null) return null;
    loadCache();
    return cache.get(msgId);
  }

  public static void clearHistory(String msgId) {
    if (msgId == null) return;
    loadCache();
    synchronized (persistLock) {
      cache.remove(msgId);
      persist();
    }
  }

  private static void loadCache() {
    if (loaded) return;
    synchronized (persistLock) {
      if (loaded) return;
      try {
        JSONObject json = SettingsStore.loadEditHistory();
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
          String id = keys.next();
          JSONArray versions = json.optJSONArray(id);
          if (versions != null) cache.put(id, versions);
        }
      } catch (Throwable ignored) {
      }
      loaded = true;
    }
  }

  private static void persist() {
    try {
      JSONObject json = new JSONObject();
      for (Map.Entry<String, JSONArray> entry : cache.entrySet()) {
        json.put(entry.getKey(), entry.getValue());
      }
      SettingsStore.saveEditHistory(json);
    } catch (Throwable ignored) {
    }
  }

  private static JSONObject version(String text, String timestamp) throws Throwable {
    JSONObject version = new JSONObject();
    version.put("t", text);
    version.put("ts", timestamp);
    return version;
  }

  private static String lastText(JSONArray versions) {
    JSONObject last = versions.optJSONObject(versions.length() - 1);
    return last == null ? null : last.optString("t", null);
  }

  public static String formatTime(long epochMs) {
    return new SimpleDateFormat(TIME_FMT, Locale.getDefault()).format(new Date(epochMs));
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
