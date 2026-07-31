package app.zipper.knot.hooks;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Reflect;
import io.github.libxposed.api.XposedInterface;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class SettingsButtonLongPress implements BaseHook {

  // combinedClickable(Modifier, interactionSource, enabled, role, onLongClick, onClick, mask)
  private static final int COMBINED_CLICKABLE_PARAMS = 7;
  private static final int ON_LONG_CLICK_PARAM = 4;

  private static volatile Method combinedClickable = null;
  private static volatile Class<?> longPressCallbackType = null;
  private static volatile Object longPressCallback = null;
  private static volatile WeakReference<Object> settingsOnClick = null;
  private static volatile boolean clickableHookInstalled = false;

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    LineVersion.Config cfg = LineVersion.get();

    hookHeaderButton(cfg, lpparam);
    hookSettingsButtonView();
    hookComposeNavSettingsButton(cfg, lpparam);
  }

  private void hookHeaderButton(LineVersion.Config cfg, LoadParam lpparam) {
    try {
      Knot.module
          .hook(
              Reflect.findMethodExact(
                  cfg.main.headerButton,
                  lpparam.classLoader,
                  "setButtonOnClickListener",
                  View.OnClickListener.class))
          .intercept(
              chain -> {
                Object result = chain.proceed();
                attachInteractionHandler((View) chain.getThisObject());
                return result;
              });
    } catch (Throwable ignored) {
    }
  }

  private void hookSettingsButtonView() {
    try {
      Knot.module
          .hook(
              Reflect.findMethodExact(View.class, "setOnClickListener", View.OnClickListener.class))
          .intercept(
              chain -> {
                Object result = chain.proceed();
                View target = (View) chain.getThisObject();
                if (target == null) return result;
                int id = target.getId();
                if (id != View.NO_ID) {
                  LineVersion.Config c = LineVersion.get();
                  String entry = "";
                  try {
                    entry = target.getResources().getResourceEntryName(id);
                  } catch (Throwable ignored) {
                  }
                  if (c.res.resSettingsHeaderBtn.equals(entry)
                      || c.res.resSettingsBtn.equals(entry)) {
                    attachInteractionHandler(target);
                  }
                }
                return result;
              });
    } catch (Throwable ignored) {
    }
  }

  private void attachInteractionHandler(View root) {
    if (root == null) return;
    root.setOnLongClickListener(interactionListener);
  }

  private final View.OnLongClickListener interactionListener =
      v -> openKnotSettings(findHostActivity(v.getContext()));

  private static Activity findHostActivity(Context ctx) {
    while (ctx instanceof ContextWrapper) {
      if (ctx instanceof Activity) return (Activity) ctx;
      ctx = ((ContextWrapper) ctx).getBaseContext();
    }
    return null;
  }

  private static boolean openKnotSettings(Activity host) {
    if (host == null) return false;
    try {
      HomeSettingsTooltip.markShown();
      SettingsUIInjector.openSettings(host);
      return true;
    } catch (Throwable t) {
      Knot.log("Knot: Interaction error: " + t);
      return false;
    }
  }

  // Compose header: no View to hang setOnLongClickListener on; clickable -> combinedClickable
  // so Compose times the press
  private void hookComposeNavSettingsButton(LineVersion.Config cfg, LoadParam lpparam) {
    if (cfg.home26NavIcon.rendererClass.isEmpty()
        || cfg.home26NavIcon.rendererMethod.isEmpty()
        || cfg.home26NavIcon.settingsDrawableId == 0) return;

    Method clickable = resolveClickableMethods(cfg, lpparam);
    if (clickable == null) return;

    try {
      Class<?> navCls = Reflect.findClass(cfg.home26NavIcon.rendererClass, lpparam.classLoader);
      Method renderer =
          findMethod(
              navCls,
              cfg.home26NavIcon.rendererMethod,
              params -> params.length > 0 && params[0] == int.class);
      if (renderer == null) {
        Knot.log("Knot: SettingsButtonLongPress could not resolve the nav icon renderer.");
        return;
      }

      final int settingsDrawableId = cfg.home26NavIcon.settingsDrawableId;
      final int onClickParam = Reflect.paramIndex(renderer, longPressCallbackType);
      Knot.module
          .hook(renderer)
          .intercept(
              chain -> {
                if ((Integer) chain.getArg(0) == settingsDrawableId) {
                  rememberSettingsOnClick(chain, onClickParam);
                  installClickableHook(clickable);
                }
                return chain.proceed();
              });
      Knot.log("Knot: SettingsButtonLongPress hooked Compose nav settings button.");
    } catch (Throwable t) {
      Knot.log(
          "Knot: SettingsButtonLongPress could not hook the Compose nav settings button: " + t);
    }
  }

  private Method resolveClickableMethods(LineVersion.Config cfg, LoadParam lpparam) {
    if (cfg.compose.clickableClass.isEmpty()
        || cfg.compose.methodClickable.isEmpty()
        || cfg.compose.methodCombinedClickable.isEmpty()) return null;

    try {
      Class<?> cls = Reflect.findClass(cfg.compose.clickableClass, lpparam.classLoader);
      combinedClickable =
          findMethod(
              cls,
              cfg.compose.methodCombinedClickable,
              params -> params.length == COMBINED_CLICKABLE_PARAMS);
      if (combinedClickable == null) {
        Knot.log("Knot: SettingsButtonLongPress could not resolve combinedClickable.");
        return null;
      }
      longPressCallbackType = combinedClickable.getParameterTypes()[ON_LONG_CLICK_PARAM];

      // clickable() core takes the callback last; the trailing int mask marks the $default bridges
      Method clickable =
          findMethod(
              cls,
              cfg.compose.methodClickable,
              params ->
                  params.length >= 2
                      && params[params.length - 1] == longPressCallbackType
                      && !Arrays.asList(params).contains(int.class));
      if (clickable == null) Knot.log("Knot: SettingsButtonLongPress could not resolve clickable.");
      return clickable;
    } catch (Throwable t) {
      Knot.log("Knot: SettingsButtonLongPress could not resolve the Compose clickable API: " + t);
      return null;
    }
  }

  private static Method findMethod(Class<?> cls, String name, Predicate<Class<?>[]> params) {
    for (Method m : cls.getDeclaredMethods()) {
      if (m.getName().equals(name) && params.test(m.getParameterTypes())) {
        m.setAccessible(true);
        return m;
      }
    }
    return null;
  }

  private static void rememberSettingsOnClick(XposedInterface.Chain chain, int onClickParam) {
    if (onClickParam < 0) return;
    Object onClick = chain.getArg(onClickParam);
    if (onClick == null) return;
    WeakReference<Object> current = settingsOnClick;
    if (current == null || current.get() != onClick) settingsOnClick = new WeakReference<>(onClick);
  }

  // Deferred: hooks every Modifier.clickable in the app, so only pay for it on a Compose header
  private static void installClickableHook(Method clickable) {
    if (clickableHookInstalled) return;
    synchronized (SettingsButtonLongPress.class) {
      if (clickableHookInstalled) return;
      clickableHookInstalled = true;
    }

    final int onClickParam = clickable.getParameterCount() - 1;
    try {
      Knot.module
          .hook(clickable)
          .intercept(
              chain -> {
                if (!isSettingsClickable(chain, onClickParam)) return chain.proceed();
                Object modifier = buildLongPressModifier(chain);
                return modifier != null ? modifier : chain.proceed();
              });
    } catch (Throwable t) {
      Knot.log("Knot: SettingsButtonLongPress could not hook clickable: " + t);
    }
  }

  // Instance match, not call frame; the leaf renderer recomposes without re-entering the hook above
  private static boolean isSettingsClickable(XposedInterface.Chain chain, int onClickParam) {
    WeakReference<Object> ref = settingsOnClick;
    Object onClick = ref == null ? null : ref.get();
    return onClick != null && chain.getArg(onClickParam) == onClick;
  }

  private static Object buildLongPressModifier(XposedInterface.Chain chain) {
    try {
      Method combined = combinedClickable;
      Class<?>[] target = combined.getParameterTypes();
      Class<?>[] source = chain.getExecutable().getParameterTypes();
      List<Object> args = chain.getArgs();
      if (args.size() != source.length) return null;

      Object callback = longPressCallback();
      if (callback == null) return null;

      return combined.invoke(
          null,
          args.get(0),
          argOfType(source, args, target[1]),
          argOfType(source, args, target[2]),
          argOfType(source, args, target[3]),
          callback,
          args.get(args.size() - 1),
          0);
    } catch (Throwable t) {
      Knot.log("Knot: SettingsButtonLongPress could not build the long-press modifier: " + t);
      return null;
    }
  }

  private static Object argOfType(Class<?>[] paramTypes, List<Object> args, Class<?> type) {
    int index = Reflect.paramIndex(paramTypes, type);
    return index < 0 ? null : args.get(index);
  }

  private static synchronized Object longPressCallback() {
    if (longPressCallback != null) return longPressCallback;

    Class<?> callbackType = longPressCallbackType;
    final Object unit =
        Reflect.getStaticObjectField(
            Reflect.findClass("kotlin.Unit", callbackType.getClassLoader()), "INSTANCE");
    longPressCallback =
        Proxy.newProxyInstance(
            callbackType.getClassLoader(),
            new Class<?>[] {callbackType},
            (proxy, method, methodArgs) -> {
              switch (method.getName()) {
                case "equals":
                  return proxy == methodArgs[0];
                case "hashCode":
                  return System.identityHashCode(proxy);
                case "toString":
                  return "KnotSettingsLongPress";
                default:
                  openKnotSettings(SettingsUIInjector.getForegroundActivity());
                  return unit;
              }
            });
    return longPressCallback;
  }
}
