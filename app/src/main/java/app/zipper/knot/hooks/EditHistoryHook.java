package app.zipper.knot.hooks;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.TypedValue;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.Reflect;
import app.zipper.knot.SettingsStore;
import app.zipper.knot.ui.EditHistoryViewer;
import app.zipper.knot.utils.LineDBUtils;
import app.zipper.knot.utils.ModuleStrings;
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

  private static final String MODULE_PKG = "app.zipper.knot";
  private static final String PLACEHOLDER_ITEM = "INVALID";
  private static final String ICON_DRAWABLE = "clock_edit";
  private static final int ICON_ID = 0x64000010;

  private static final Map<String, JSONArray> cache = new ConcurrentHashMap<>();
  private static final Object persistLock = new Object();
  private static volatile boolean loaded = false;
  private static volatile Bitmap icon;
  private static LineVersion.Config.MessageEditHistory cfg;

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    if (!config.showEditHistory.enabled) return;
    LineVersion.Config version = LineVersion.get();
    if (version == null || version.messageEditHistory.editRequestClass.isEmpty()) return;
    cfg = version.messageEditHistory;

    loadCache();
    hookCapture(lpparam.classLoader);
    hookMenu(lpparam.classLoader);
    hookIcon();
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
    synchronized (persistLock) {
      JSONArray versions = cache.get(msgId);
      try {
        if (versions == null) {
          versions = new JSONArray();
          String oldText = LineDBUtils.resolveMessageContent(msgId);
          if (oldText != null && !oldText.isEmpty() && !oldText.equals(newText)) {
            versions.put(version(oldText, ""));
          }
        } else if (newText.equals(lastText(versions))) {
          return;
        }
        versions.put(version(newText, now()));
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
      Object item = enumConstant(Reflect.findClass(cfg.menuItemEnumClass, cl), PLACEHOLDER_ITEM);
      final Object placeholder = enumConstant(presentationEnum, PLACEHOLDER_ITEM);
      final Object action = buildAction(cl);

      if (cfg.menuListField.isEmpty()) {
        hookMenuList(cl, item);
      } else {
        hookMenuListField(cl, item);
      }

      Knot.module
          .hook(Reflect.findMethodExact(presentationEnum, cfg.methodMenuLabel, Context.class))
          .intercept(chain -> forPlaceholder(chain, placeholder, ModuleStrings.EDIT_HISTORY_TITLE));

      Knot.module
          .hook(Reflect.findMethodExact(presentationEnum, cfg.methodMenuIcon))
          .intercept(chain -> forPlaceholder(chain, placeholder, ICON_ID));

      Knot.module
          .hook(Reflect.findMethodExact(presentationEnum, cfg.methodMenuActionAccessor))
          .intercept(chain -> forPlaceholder(chain, placeholder, action));
    } catch (Throwable t) {
      Knot.log("Knot: edit history menu hook failed: " + t);
    }
  }

  // 26.10.0+ builds the item list in a method that returns it.
  private static void hookMenuList(ClassLoader cl, Object item) throws Throwable {
    Knot.hookAll(
        Reflect.findClass(cfg.menuListBuilderClass, cl),
        cfg.menuListMethod,
        chain -> {
          Object result = chain.proceed();
          if (!Main.options.showEditHistory.enabled || !(result instanceof List)) return result;
          return isEdited(messageData(chain.getArg(2))) ? withItem((List<?>) result, item) : result;
        });
  }

  // 26.8.0 / 26.9.0 build it inline in the popup constructor and keep it in a field.
  private static void hookMenuListField(ClassLoader cl, Object item) throws Throwable {
    final Class<?> contextClass = Reflect.findClass(cfg.menuContextClass, cl);
    Knot.hookAllCtors(
        Reflect.findClass(cfg.menuListBuilderClass, cl),
        chain -> {
          Object result = chain.proceed();
          if (!Main.options.showEditHistory.enabled) return result;
          try {
            int index = Reflect.paramIndex(chain.getExecutable().getParameterTypes(), contextClass);
            Object current =
                index < 0 ? null : Reflect.getObjectField(chain.getThisObject(), cfg.menuListField);
            if (current instanceof List && isEdited(messageData(chain.getArg(index)))) {
              Reflect.setObjectField(
                  chain.getThisObject(), cfg.menuListField, withItem((List<?>) current, item));
            }
          } catch (Throwable ignored) {
          }
          return result;
        });
  }

  private static List<Object> withItem(List<?> items, Object item) {
    List<Object> copy = new ArrayList<>(items);
    copy.add(item);
    return copy;
  }

  private static Object forPlaceholder(
      XposedInterface.Chain chain, Object placeholder, Object value) throws Throwable {
    if (Main.options.showEditHistory.enabled && chain.getThisObject() == placeholder) return value;
    return chain.proceed();
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
  private static void hookIcon() {
    try {
      Knot.module
          .hook(
              Reflect.findMethodExact(
                  Resources.class, "getValue", int.class, TypedValue.class, boolean.class))
          .intercept(
              chain -> {
                if ((int) chain.getArg(0) != ICON_ID) return chain.proceed();
                TypedValue value = (TypedValue) chain.getArg(1);
                value.string = ICON_DRAWABLE + ".png";
                value.type = TypedValue.TYPE_STRING;
                return null;
              });

      Knot.module
          .hook(
              Reflect.findMethodExact(
                  Resources.class, "getDrawable", int.class, Resources.Theme.class))
          .intercept(
              chain -> {
                if ((int) chain.getArg(0) != ICON_ID) return chain.proceed();
                Bitmap bitmap = icon();
                if (bitmap == null) return chain.proceed();
                return new BitmapDrawable((Resources) chain.getThisObject(), bitmap);
              });
    } catch (Throwable t) {
      Knot.log("Knot: edit history icon hook failed: " + t);
    }
  }

  private static Bitmap icon() {
    if (icon != null) return icon;
    try {
      Context appCtx = Knot.currentApplication();
      if (appCtx == null) return null;
      Resources res =
          appCtx.createPackageContext(MODULE_PKG, Context.CONTEXT_IGNORE_SECURITY).getResources();
      int id = res.getIdentifier(ICON_DRAWABLE, "drawable", MODULE_PKG);
      if (id != 0) icon = ((BitmapDrawable) res.getDrawable(id, null)).getBitmap();
    } catch (Throwable ignored) {
    }
    return icon;
  }

  public static JSONArray historyFor(String msgId) {
    return msgId == null ? null : cache.get(msgId);
  }

  public static void clearHistory(String msgId) {
    if (msgId == null) return;
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

  private static String now() {
    return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(new Date());
  }

  private static Object kotlinUnit(ClassLoader cl) {
    try {
      return Reflect.findClass("kotlin.Unit", cl).getField("INSTANCE").get(null);
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
