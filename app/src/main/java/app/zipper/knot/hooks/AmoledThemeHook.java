package app.zipper.knot.hooks;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.view.View;
import android.view.Window;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.Reflect;
import io.github.libxposed.api.XposedInterface.Hooker;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipFile;

public class AmoledThemeHook implements BaseHook {

  private static final int BLACK = 0xFF000000;

  private static final String[] THEME_PATH_HINTS = {
    "jp.naver.line.android", "/Themes/", "/themes/", "/.theme", "/theme/"
  };

  private static final String PRIMARY_BACKGROUND = "primaryBackground";
  private static final String[] PASSCODE_BACKGROUND_VIEWS = {
    "passcode_bg", "passcode_top", "passcode_fake_status_bar"
  };
  private static final String NAVIGATION_BAR_BACKGROUND = "main_navigation_bar_background";

  private static final Set<String> VALIDATION_METHODS =
      Collections.unmodifiableSet(
          new HashSet<>(
              Arrays.asList(
                  "getProductValidationScheme",
                  "getProductValidationScheme_args",
                  "getProductValidationScheme_result",
                  "getProductLatestVersionForUser",
                  "getProductLatestVersionForUser_args",
                  "getProductLatestVersionForUser_result")));

  private static final int[] NO_COLOR = new int[0];

  private static final Map<Integer, int[]> resIdCache = new ConcurrentHashMap<>();
  private static volatile int navigationBarBackgroundId = -1;

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    AmoledThemeBundle.load();

    installFileRedirects();
    installThemeImageFileBridge();
    installThemeSemanticColors();
    installWindowBlackening();
    installNavigationBarBackgroundGuard();
    installPasscodeBackgroundOverride(lpparam);
    installNightModePin(lpparam);
    installDefaultThemeOverride(lpparam);
    installThriftValidationHijack(lpparam);
  }

  private static boolean enabled() {
    return Main.options.useAmoledTheme.enabled;
  }

  private void installFileRedirects() {
    Hooker openHook =
        chain -> {
          if (!enabled()) return chain.proceed();
          Object[] args = chain.getArgs().toArray();
          File mapped = mapToBundle(toFile(args[0]));
          if (mapped != null) {
            args[0] = (args[0] instanceof File) ? mapped : mapped.getAbsolutePath();
            return chain.proceed(args);
          }
          return chain.proceed();
        };

    for (Constructor<?> c : openableConstructors()) {
      Knot.module.hook(c).intercept(openHook);
    }
    for (Executable m : decodeFileMethods()) {
      Knot.module.hook(m).intercept(openHook);
    }
  }

  // LINE checks File.exists() before opening a theme image, so the redirect above never gets a
  // chance while the selected theme has no extracted images of its own.
  private void installThemeImageFileBridge() {
    try {
      Knot.module
          .hook(Reflect.findMethodExact(File.class, "exists"))
          .intercept(
              chain -> {
                Object original = chain.proceed();
                if (Boolean.TRUE.equals(original) || !enabled()) return original;

                File requested = (File) chain.getThisObject();
                String name = requested.getName();
                if (name == null || !AmoledThemeBundle.hasImage(name)) return original;

                String path = requested.getAbsolutePath();
                if (path == null || !looksLikeThemePath(path)) return original;

                File cached = AmoledThemeBundle.alreadyExtractedImage(name);
                if (cached == null || cached.length() <= 0L) return original;

                return Boolean.TRUE;
              });
      log("theme image File.exists bridge installed");
    } catch (Throwable t) {
      log("theme image File.exists bridge unavailable: " + t);
    }
  }

  private static List<Constructor<?>> openableConstructors() {
    return Arrays.asList(
        Reflect.findConstructorExact(FileInputStream.class, File.class),
        Reflect.findConstructorExact(FileInputStream.class, String.class),
        Reflect.findConstructorExact(ZipFile.class, File.class),
        Reflect.findConstructorExact(ZipFile.class, String.class),
        Reflect.findConstructorExact(ZipFile.class, File.class, int.class));
  }

  private static List<Executable> decodeFileMethods() {
    return Arrays.asList(
        Reflect.findMethodExact(BitmapFactory.class, "decodeFile", String.class),
        Reflect.findMethodExact(
            BitmapFactory.class, "decodeFile", String.class, BitmapFactory.Options.class));
  }

  private static File toFile(Object arg) {
    if (arg instanceof File) return (File) arg;
    if (arg instanceof String) return new File((String) arg);
    return null;
  }

  private static File mapToBundle(File requested) {
    if (requested == null) return null;
    String name = requested.getName();
    if (name == null || name.isEmpty()) return null;

    boolean isThemeJson = AmoledThemeBundle.THEME_JSON.equals(name);
    boolean isThemeFile =
        name.startsWith(AmoledThemeBundle.THEME_FILE_PREFIX) && parseRevision(name) >= 0;
    boolean isImage = AmoledThemeBundle.hasImage(name);
    if (!isThemeJson && !isThemeFile && !isImage) return null;

    String path = requested.getAbsolutePath();
    if (path == null || !looksLikeThemePath(path)) return null;

    if (isThemeJson) return AmoledThemeBundle.themeJson();
    if (isThemeFile) return AmoledThemeBundle.themeFile();
    return AmoledThemeBundle.image(name);
  }

  private static boolean looksLikeThemePath(String path) {
    for (String hint : THEME_PATH_HINTS) {
      if (path.contains(hint)) return true;
    }
    return false;
  }

  private static int parseRevision(String name) {
    int dot = name.lastIndexOf('.');
    if (dot < 0 || dot == name.length() - 1) return -1;
    try {
      return Integer.parseInt(name.substring(dot + 1));
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  private void installThemeSemanticColors() {
    Map<String, Integer> colors = AmoledThemeBundle.semanticColors();
    if (colors == null || colors.isEmpty()) return;

    Hooker colorHook =
        chain -> {
          if (enabled()) {
            Integer c = resolve((Resources) chain.getThisObject(), (Integer) chain.getArg(0));
            if (c != null) return c;
          }
          return chain.proceed();
        };
    Hooker colorStateListHook =
        chain -> {
          if (enabled()) {
            Integer c = resolve((Resources) chain.getThisObject(), (Integer) chain.getArg(0));
            if (c != null) return ColorStateList.valueOf(c);
          }
          return chain.proceed();
        };

    Knot.module
        .hook(Reflect.findMethodExact(Resources.class, "getColor", int.class))
        .intercept(colorHook);
    Knot.module
        .hook(
            Reflect.findMethodExact(Resources.class, "getColor", int.class, Resources.Theme.class))
        .intercept(colorHook);
    Knot.module
        .hook(Reflect.findMethodExact(Resources.class, "getColorStateList", int.class))
        .intercept(colorStateListHook);
    Knot.module
        .hook(
            Reflect.findMethodExact(
                Resources.class, "getColorStateList", int.class, Resources.Theme.class))
        .intercept(colorStateListHook);

    log("routing " + colors.size() + " color tokens through theme");
  }

  private static Integer resolve(Resources res, int id) {
    int[] cached = resIdCache.get(id);
    if (cached != null) return cached.length == 0 ? null : cached[0];

    Integer color = null;
    Map<String, Integer> colors = AmoledThemeBundle.semanticColors();
    if (colors != null) {
      try {
        color = colors.get(res.getResourceEntryName(id));
      } catch (Throwable ignored) {
      }
    }
    resIdCache.put(id, color == null ? NO_COLOR : new int[] {color});
    return color;
  }

  private void installWindowBlackening() {
    try {
      Knot.module
          .hook(Reflect.findMethodExact(Activity.class, "onResume"))
          .intercept(
              chain -> {
                Object result = chain.proceed();
                if (!enabled()) return result;
                try {
                  Window window = ((Activity) chain.getThisObject()).getWindow();
                  if (window != null && !window.isFloating()) {
                    window.getDecorView().setBackgroundColor(BLACK);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                      window.setNavigationBarContrastEnforced(false);
                    }
                  }
                } catch (Throwable ignored) {
                }
                return result;
              });
    } catch (Throwable t) {
      log("window blackening failed: " + t);
    }
  }

  // LINE draws the area behind the gesture/button bar with its own view: the window is edge-to-edge
  // and targetSdk 36 makes setNavigationBarColor a no-op, so only this view controls that strip.
  // With the built-in theme active LINE repaints it with a hardcoded grey on every tab switch.
  private void installNavigationBarBackgroundGuard() {
    try {
      Knot.module
          .hook(Reflect.findMethodExact(View.class, "setBackgroundColor", int.class))
          .intercept(
              chain -> {
                if (!enabled()) return chain.proceed();
                View view = (View) chain.getThisObject();
                int id = navigationBarBackgroundId;
                if (id == -1) id = navigationBarBackgroundId(view.getContext());
                if (id == 0 || view.getId() != id) return chain.proceed();
                return chain.proceed(new Object[] {BLACK});
              });
      log("navigation bar background guard installed");
    } catch (Throwable t) {
      log("navigation bar background guard unavailable: " + t);
    }
  }

  private static int navigationBarBackgroundId(Context context) {
    int id = navigationBarBackgroundId;
    if (id != -1) return id;
    try {
      id =
          context
              .getResources()
              .getIdentifier(NAVIGATION_BAR_BACKGROUND, "id", context.getPackageName());
    } catch (Throwable t) {
      id = 0;
    }
    navigationBarBackgroundId = id;
    return id;
  }

  private void installPasscodeBackgroundOverride(LoadParam lpparam) {
    LineVersion.Config cfg = LineVersion.get();
    if (cfg == null || cfg.nightMode.inputPassActivityClass.isEmpty()) return;

    try {
      Class<?> inputPassActivity =
          Reflect.findClass(cfg.nightMode.inputPassActivityClass, lpparam.classLoader);
      Knot.module
          .hook(Reflect.findMethodExact(inputPassActivity, "onStart"))
          .intercept(
              chain -> {
                Object result = chain.proceed();
                if (enabled()) applyPasscodeBackground((Activity) chain.getThisObject());
                return result;
              });
    } catch (Throwable t) {
      log("passcode background unavailable: " + t);
    }
  }

  private static void applyPasscodeBackground(Activity activity) {
    try {
      Resources res = activity.getResources();
      String pkg = activity.getPackageName();
      int primaryBackgroundId = res.getIdentifier(PRIMARY_BACKGROUND, "color", pkg);
      if (primaryBackgroundId == 0) return;

      Integer color = resolve(res, primaryBackgroundId);
      if (color == null) return;

      for (String name : PASSCODE_BACKGROUND_VIEWS) {
        int id = res.getIdentifier(name, "id", pkg);
        if (id == 0) continue;
        View view = activity.findViewById(id);
        if (view != null) view.setBackgroundColor(color);
      }
    } catch (Throwable t) {
      log("passcode background failed: " + t);
    }
  }

  private void installNightModePin(LoadParam lpparam) {
    try {
      NightModePin.install(lpparam, AmoledThemeHook::enabled, "Knot: AmoledTheme");
    } catch (Throwable t) {
      log("night mode pin failed: " + t);
    }
  }

  // LINE skips theme file loading entirely while the built-in light theme is selected, leaving the
  // AMOLED bundle unused. Reporting a non-default theme routes it through the file redirects.
  private void installDefaultThemeOverride(LoadParam lpparam) {
    LineVersion.Config cfg = LineVersion.get();
    if (cfg == null
        || cfg.nightMode.darkThemeManagerClass.isEmpty()
        || cfg.nightMode.methodIsDefaultTheme.isEmpty()) {
      log("default theme predicate not mapped for current LINE version");
      return;
    }

    try {
      Method predicate =
          Reflect.findMethodExact(
              cfg.nightMode.darkThemeManagerClass,
              lpparam.classLoader,
              cfg.nightMode.methodIsDefaultTheme);
      if (predicate.getReturnType() != boolean.class) {
        log("default theme predicate is not boolean");
        return;
      }

      Knot.module.hook(predicate).intercept(chain -> enabled() ? Boolean.FALSE : chain.proceed());
      log("default theme predicate overridden on " + cfg.nightMode.darkThemeManagerClass);
    } catch (Throwable t) {
      log("default theme predicate unavailable: " + t);
    }
  }

  private void installThriftValidationHijack(LoadParam lpparam) {
    LineVersion.Config cfg = LineVersion.get();
    if (cfg == null
        || cfg.thrift.protocolClass.isEmpty()
        || cfg.thrift.methodWriteMessageBegin.isEmpty()
        || cfg.thrift.methodReadMessageBegin.isEmpty()
        || cfg.thrift.messageClass.isEmpty()) {
      log("thrift config incomplete for current LINE version");
      return;
    }

    Hooker swap =
        chain -> {
          if (!enabled()) return chain.proceed();
          Object arg0 = chain.getArg(0);
          if (arg0 instanceof String && isValidationMethod((String) arg0)) {
            Object[] args = chain.getArgs().toArray();
            args[0] = "noop";
            return chain.proceed(args);
          }
          return chain.proceed();
        };

    for (String method :
        new String[] {cfg.thrift.methodWriteMessageBegin, cfg.thrift.methodReadMessageBegin}) {
      Knot.module
          .hook(
              Reflect.findMethodExact(
                  cfg.thrift.protocolClass,
                  lpparam.classLoader,
                  method,
                  String.class,
                  cfg.thrift.messageClass))
          .intercept(swap);
    }
    log(
        "Thrift hijack on "
            + cfg.thrift.protocolClass
            + "."
            + cfg.thrift.methodWriteMessageBegin
            + "/"
            + cfg.thrift.methodReadMessageBegin);
  }

  private static boolean isValidationMethod(String name) {
    return VALIDATION_METHODS.contains(name) || name.contains("validateProduct");
  }

  private static void log(String message) {
    Knot.log("Knot: AmoledTheme: " + message);
  }
}
