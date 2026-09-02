package app.zipper.knot.hooks;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.R;
import app.zipper.knot.Reflect;
import app.zipper.knot.SettingsStore;
import app.zipper.knot.utils.ModuleResources;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

public class PlusMenuHook implements BaseHook {

  private static volatile boolean isMenuDisplayed = false;
  private static volatile Object menuContextScope = null;
  private static volatile boolean injectionActive = false;

  private static volatile boolean currentReadState = true;
  private static volatile boolean currentSendMarkState = false;

  private static final int ID_READ_ON = 0x64000001;
  private static final int ID_READ_OFF = 0x64000002;
  private static final int ID_MARK_ON = 0x64000003;
  private static final int ID_MARK_OFF = 0x64000004;

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

    final Object readToggleCallback =
        generateToggleHandler(
            lpparam.classLoader,
            callbackCls,
            "prevent_read_state",
            ModuleResources.get(R.string.label_prevent_read),
            true,
            null);
    final Object markToggleCallback =
        generateToggleHandler(
            lpparam.classLoader,
            callbackCls,
            "send_mark_state",
            ModuleResources.get(R.string.label_send_mark_read),
            false,
            "prevent_read_state");

    Knot.module
        .hook(mainEntry)
        .intercept(
            chain -> {
              isMenuDisplayed = true;
              currentReadState = SettingsStore.get("prevent_read_state", true);
              currentSendMarkState = SettingsStore.get("send_mark_state", false);
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

              if (targetDrawableId == 0) {
                try {
                  Context ctx = fetchApplicationContext();
                  if (ctx != null) {
                    targetDrawableId =
                        ctx.getResources()
                            .getIdentifier(
                                cfg.plusMenu.editChatDrawable, "drawable", cfg.plusMenu.targetPkg);
                  }
                } catch (Throwable ignored) {
                }
              }
              if (targetDrawableId == 0) return result;

              int iconId = (int) chain.getArg(0);
              if (iconId != targetDrawableId) return result;

              Object composer = chain.getArg(composerArg);
              injectionActive = true;
              try {
                if (Main.options.preventMarkAsRead.enabled) {
                  boolean readOn = currentReadState;
                  String labelR =
                      ModuleResources.get(R.string.label_prevent_read)
                          + ": "
                          + (readOn ? "ON" : "OFF");
                  addPlusMenuItem(
                      itemEntry,
                      composerCls,
                      callbackCls,
                      readOn ? ID_READ_ON : ID_READ_OFF,
                      labelR,
                      readToggleCallback,
                      composer);

                  if (readOn) {
                    boolean markOn = currentSendMarkState;
                    String labelM =
                        ModuleResources.get(R.string.label_send_mark_read)
                            + ": "
                            + (markOn ? "ON" : "OFF");
                    addPlusMenuItem(
                        itemEntry,
                        composerCls,
                        callbackCls,
                        markOn ? ID_MARK_ON : ID_MARK_OFF,
                        labelM,
                        markToggleCallback,
                        composer);
                  }
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
                Bitmap b = retrieveModuleIcon(id, cfg);
                if (b != null) return new BitmapDrawable((Resources) chain.getThisObject(), b);
              } catch (Throwable ignored) {
              }
              return chain.proceed();
            });
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
      int id,
      String label,
      Object callback,
      Object composer)
      throws Exception {
    Class<?>[] types = itemEntry.getParameterTypes();
    Object[] args = new Object[types.length];
    boolean idAssigned = false;
    for (int i = 0; i < types.length; i++) {
      Class<?> type = types[i];
      if (type == int.class) {
        args[i] = idAssigned ? 0 : id;
        idAssigned = true;
      } else if (type == composerCls) {
        args[i] = composer;
      } else if (type == callbackCls) {
        args[i] = callback;
      } else if (type == String.class) {
        args[i] = label;
      }
    }
    itemEntry.invoke(null, args);
  }

  private static Object generateToggleHandler(
      ClassLoader cl, Class<?> callbackCls, String key, String label, boolean def, String depKey) {
    return Proxy.newProxyInstance(
        cl,
        new Class[] {callbackCls},
        (proxy, method, args) -> {
          if ("invoke".equals(method.getName())) {
            if (depKey != null && !currentReadState) return null;
            boolean nextValue;
            if (key.equals("prevent_read_state")) {
              nextValue = !currentReadState;
              currentReadState = nextValue;
            } else {
              nextValue = !currentSendMarkState;
              currentSendMarkState = nextValue;
            }
            SettingsStore.save(key, nextValue);
            new Handler(Looper.getMainLooper())
                .post(
                    () -> {
                      if (menuContextScope != null) {
                        try {
                          Reflect.callMethod(menuContextScope, "invalidate");
                        } catch (Throwable ignored) {
                        }
                      }
                    });
            return null;
          }
          if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
          return null;
        });
  }

  private static Bitmap retrieveModuleIcon(int id, LineVersion.Config cfg) {
    Bitmap stored = iconStorage.get(id);
    if (stored != null) return stored;
    String name;
    if (id == ID_READ_ON) name = "ic_prevent_read_on";
    else if (id == ID_READ_OFF) name = "ic_prevent_read_off";
    else if (id == ID_MARK_ON) name = "ic_send_mark_read_on";
    else if (id == ID_MARK_OFF) name = "ic_send_mark_read_off";
    else return null;

    try {
      Context appCtx = fetchApplicationContext();
      if (appCtx == null) return null;
      Context modCtx =
          appCtx.createPackageContext(cfg.plusMenu.moduleId, Context.CONTEXT_IGNORE_SECURITY);
      int resId = modCtx.getResources().getIdentifier(name, "drawable", cfg.plusMenu.moduleId);
      Drawable d = modCtx.getResources().getDrawable(resId, null);
      Bitmap bmp = ((BitmapDrawable) d).getBitmap();
      iconStorage.put(id, bmp);
      return bmp;
    } catch (Throwable t) {
      return null;
    }
  }

  private static Context fetchApplicationContext() {
    return Knot.currentApplication();
  }
}
