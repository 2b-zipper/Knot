package app.zipper.knot.hooks;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Reflect;
import app.zipper.knot.utils.ModuleResources;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class PlusMenuHook implements BaseHook {

  private static volatile boolean isMenuDisplayed = false;
  private static volatile Object menuContextScope = null;
  private static volatile boolean injectionActive = false;

  private static final int ICON_DP = 28;
  private static final int ICON_ID_BASE = 0x64000001;

  private static volatile int targetDrawableId = 0;
  private static final Map<Integer, Bitmap> iconStorage = new HashMap<>();

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    LineVersion.Config cfg = LineVersion.get();

    final Class<?> pCls;
    final Class<?> composerCls;
    final Class<?> composerImplCls;
    final Class<?> callbackCls;

    try {
      pCls = Reflect.findClass(cfg.plusMenu.plusMenuComponentClass, lpparam.classLoader);
      composerCls = Reflect.findClass(cfg.compose.composerClass, lpparam.classLoader);
      composerImplCls =
          Reflect.findClass(cfg.plusMenu.plusMenuComposerImplClass, lpparam.classLoader);
      callbackCls = Reflect.findClass(cfg.plusMenu.plusMenuCallbackClass, lpparam.classLoader);
    } catch (Throwable t) {
      return;
    }

    final Method mainEntry = findComposeEntry(pCls, cfg.plusMenu.methodAddMenuItem, composerCls);
    final Method itemEntry = findComposeEntry(pCls, cfg.plusMenu.methodCreateMenu, composerCls);
    if (mainEntry == null || itemEntry == null) {
      Knot.log("Knot: PlusMenu entry methods not found");
      return;
    }
    final int composerArg = Reflect.paramIndex(itemEntry, composerCls);
    final int iconArg = Reflect.paramIndex(itemEntry, int.class);
    if (iconArg < 0) {
      Knot.log("Knot: PlusMenu item entry has no icon parameter");
      return;
    }

    final Map<ReadToggle, Object> callbacks = new EnumMap<>(ReadToggle.class);
    for (ReadToggle toggle : ReadToggle.values()) {
      callbacks.put(toggle, createToggleCallback(lpparam.classLoader, callbackCls, toggle));
    }

    Knot.module
        .hook(mainEntry)
        .intercept(
            chain -> {
              isMenuDisplayed = true;
              try {
                return chain.proceed();
              } finally {
                isMenuDisplayed = false;
              }
            });

    Knot.module
        .hook(Reflect.findMethodExact(composerImplCls, cfg.plusMenu.methodExecuteAction))
        .intercept(
            chain -> {
              Object result = chain.proceed();
              if (isMenuDisplayed && result != null) {
                menuContextScope = result;
              }
              return result;
            });

    Knot.module
        .hook(itemEntry)
        .intercept(
            chain -> {
              Object result = chain.proceed();
              if (!isMenuDisplayed || injectionActive) return result;

              int drawableId = resolveTargetDrawableId(cfg);
              if (drawableId == 0 || (int) chain.getArg(iconArg) != drawableId) return result;

              Object composer = chain.getArg(composerArg);
              injectionActive = true;
              try {
                for (ReadToggle toggle : ReadToggle.values()) {
                  if (!toggle.isAvailable()) continue;
                  addPlusMenuItem(
                      itemEntry, composerCls, callbackCls, toggle, callbacks.get(toggle), composer);
                }
              } catch (Throwable t) {
                Knot.log("Knot: PlusMenu error: " + t);
              } finally {
                injectionActive = false;
              }
              return result;
            });

    Knot.module
        .hook(
            Reflect.findMethodExact(
                Resources.class, "getValue", int.class, TypedValue.class, boolean.class))
        .intercept(
            chain -> {
              int id = (int) chain.getArg(0);
              if ((id >>> 24) != 0x64) return chain.proceed();
              TypedValue tv = (TypedValue) chain.getArg(1);
              tv.string = "knot_res_" + Integer.toHexString(id) + ".png";
              tv.type = TypedValue.TYPE_STRING;
              return null;
            });

    Knot.module
        .hook(
            Reflect.findMethodExact(
                Resources.class, "getDrawable", int.class, Resources.Theme.class))
        .intercept(
            chain -> {
              int id = (int) chain.getArg(0);
              if ((id >>> 24) != 0x64) return chain.proceed();
              try {
                Resources res = (Resources) chain.getThisObject();
                Bitmap b = retrieveModuleIcon(id, res);
                if (b != null) return new BitmapDrawable(res, b);
              } catch (Throwable ignored) {
              }
              return chain.proceed();
            });
  }

  private static int resolveTargetDrawableId(LineVersion.Config cfg) {
    if (targetDrawableId == 0) {
      try {
        Context ctx = Knot.currentApplication();
        if (ctx != null) {
          targetDrawableId =
              ctx.getResources()
                  .getIdentifier(cfg.plusMenu.editChatDrawable, "drawable", cfg.plusMenu.targetPkg);
        }
      } catch (Throwable ignored) {
      }
    }
    return targetDrawableId;
  }

  private static Method findComposeEntry(Class<?> cls, String name, Class<?> composerCls) {
    for (Method m : cls.getDeclaredMethods()) {
      if (m.getName().equals(name) && Reflect.paramIndex(m, composerCls) >= 0) {
        m.setAccessible(true);
        return m;
      }
    }
    return null;
  }

  private static void addPlusMenuItem(
      Method itemEntry,
      Class<?> composerCls,
      Class<?> callbackCls,
      ReadToggle toggle,
      Object callback,
      Object composer)
      throws Exception {
    boolean on = toggle.isOn();
    Class<?>[] types = itemEntry.getParameterTypes();
    Object[] args = new Object[types.length];
    boolean idAssigned = false;
    for (int i = 0; i < types.length; i++) {
      Class<?> type = types[i];
      if (type == int.class) {
        args[i] = idAssigned ? 0 : iconId(toggle, on);
        idAssigned = true;
      } else if (type == composerCls) {
        args[i] = composer;
      } else if (type == callbackCls) {
        args[i] = callback;
      } else if (type == String.class) {
        args[i] = toggle.label(on);
      }
    }
    itemEntry.invoke(null, args);
  }

  private static Object createToggleCallback(
      ClassLoader cl, Class<?> callbackCls, ReadToggle toggle) {
    return Proxy.newProxyInstance(
        cl,
        new Class[] {callbackCls},
        (proxy, method, args) -> {
          switch (method.getName()) {
            case "invoke":
              if (toggle.isAvailable()) {
                toggle.toggle();
                new Handler(Looper.getMainLooper()).post(PlusMenuHook::invalidateMenu);
              }
              return null;
            case "equals":
              return proxy == args[0];
            case "hashCode":
              return System.identityHashCode(proxy);
            case "toString":
              return toggle.name();
            default:
              return null;
          }
        });
  }

  private static void invalidateMenu() {
    Object scope = menuContextScope;
    if (scope == null) return;
    try {
      Reflect.callMethod(scope, "invalidate");
    } catch (Throwable ignored) {
    }
  }

  private static Bitmap retrieveModuleIcon(int id, Resources res) {
    Bitmap stored = iconStorage.get(id);
    if (stored != null) return stored;
    String name = iconName(id);
    if (name == null) return null;

    Bitmap bmp =
        ModuleResources.bitmap(name, Math.round(ICON_DP * res.getDisplayMetrics().density));
    if (bmp != null) iconStorage.put(id, bmp);
    return bmp;
  }

  private static int iconId(ReadToggle toggle, boolean on) {
    return ICON_ID_BASE + toggle.ordinal() * 2 + (on ? 0 : 1);
  }

  private static String iconName(int id) {
    ReadToggle[] toggles = ReadToggle.values();
    int index = id - ICON_ID_BASE;
    if (index < 0 || index >= toggles.length * 2) return null;
    return toggles[index / 2].iconName(index % 2 == 0);
  }
}
