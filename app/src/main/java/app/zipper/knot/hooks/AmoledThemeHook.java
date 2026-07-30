package app.zipper.knot.hooks;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.Reflect;
import app.zipper.knot.SettingsStore;
import io.github.libxposed.api.XposedInterface.Hooker;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import org.json.JSONObject;

public class AmoledThemeHook implements BaseHook {

  private static final String MODULE_PKG = "app.zipper.knot";
  private static final String ASSET_BUNDLE = "assets/amoled.themefile";
  private static final String THEME_JSON = "theme.json";
  private static final String CACHE_SUBDIR = "knot_amoled";
  private static final String IMAGES_PREFIX = "images/";

  private static final String SEMANTIC_SECTION = "theme.semantic";
  private static final String SEMANTIC_SUFFIX = ".background.color";
  private static final int[] NO_COLOR = new int[0];

  private static final Set<String> SEMANTIC_SKIP_TOKENS =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList("primaryFill")));

  private static final String[] THEME_PATH_HINTS = {
    "jp.naver.line.android", "/Themes/", "/themes/", "/.theme", "/theme/"
  };

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

  private static byte[] themeBundleBytes;
  private static byte[] themeJsonBytes;
  private static final Map<String, byte[]> imageBlobs = new HashMap<>();
  private static int themeRevision = -1;

  private static volatile boolean cacheReady = false;
  private static File cacheJson;
  private static File cacheBundle;
  private static final Map<String, File> cacheImages = new HashMap<>();

  private static volatile Map<String, Integer> semanticColors;
  private static final Map<Integer, int[]> resIdCache = new ConcurrentHashMap<>();

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    Context ctx = SettingsStore.getContext();
    if (ctx == null) {
      Knot.log("Knot: AmoledTheme: SettingsStore has no context");
      return;
    }
    ApplicationInfo info = ctx.getPackageManager().getApplicationInfo(MODULE_PKG, 0);
    loadBundle(info.sourceDir);
    Knot.log(
        "Knot: AmoledTheme loaded: rev="
            + themeRevision
            + " images="
            + imageBlobs.size()
            + " bundleBytes="
            + themeBundleBytes.length);

    installFileRedirects();
    installThriftValidationHijack(lpparam);
    installNavigationBarBlackening();
    NightModePin.install(lpparam, () -> Main.options.useAmoledTheme.enabled, "Knot: AmoledTheme");
    installThemeSemanticColors();
  }

  private void installThemeSemanticColors() {
    Map<String, Integer> colors = semanticColors;
    if (colors == null || colors.isEmpty()) return;

    Hooker colorHook =
        chain -> {
          if (Main.options.useAmoledTheme.enabled) {
            Integer c = resolve((Resources) chain.getThisObject(), (Integer) chain.getArg(0));
            if (c != null) return c;
          }
          return chain.proceed();
        };
    Hooker colorStateListHook =
        chain -> {
          if (Main.options.useAmoledTheme.enabled) {
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

    Knot.log("Knot: AmoledTheme: routing " + colors.size() + " color tokens through theme");
  }

  private static Integer resolve(Resources res, int id) {
    int[] cached = resIdCache.get(id);
    if (cached != null) return cached.length == 0 ? null : cached[0];

    Integer color = null;
    Map<String, Integer> colors = semanticColors;
    if (colors != null) {
      try {
        color = colors.get(res.getResourceEntryName(id));
      } catch (Throwable ignored) {
      }
    }
    resIdCache.put(id, color == null ? NO_COLOR : new int[] {color});
    return color;
  }

  private void installNavigationBarBlackening() {
    try {
      Knot.module
          .hook(Reflect.findMethodExact(android.app.Activity.class, "onResume"))
          .intercept(
              chain -> {
                Object result = chain.proceed();
                try {
                  android.view.Window w =
                      ((android.app.Activity) chain.getThisObject()).getWindow();
                  if (w != null && !w.isFloating()) {
                    w.getDecorView().setBackgroundColor(0xFF000000);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                      w.setNavigationBarContrastEnforced(false);
                    }
                  }
                } catch (Throwable ignored) {
                }
                return result;
              });
    } catch (Throwable t) {
      Knot.log("Knot: AmoledTheme: nav bar blackening failed: " + t);
    }
  }

  private static void loadBundle(String apkPath) throws IOException, org.json.JSONException {
    try (ZipFile apk = new ZipFile(apkPath)) {
      ZipEntry e = apk.getEntry(ASSET_BUNDLE);
      if (e == null) throw new IOException(ASSET_BUNDLE + " missing from module APK");
      try (InputStream in = apk.getInputStream(e)) {
        themeBundleBytes = readAll(in);
      }
    }
    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(themeBundleBytes))) {
      ZipEntry e;
      while ((e = zis.getNextEntry()) != null) {
        if (e.isDirectory()) continue;
        String name = e.getName();
        if (THEME_JSON.equals(name)) {
          themeJsonBytes = readAll(zis);
        } else if (name.startsWith(IMAGES_PREFIX)) {
          String base = name.substring(IMAGES_PREFIX.length());
          if (base.isEmpty() || base.contains("/")) continue;
          imageBlobs.put(base, readAll(zis));
        }
      }
    }
    if (themeJsonBytes == null)
      throw new IOException(THEME_JSON + " missing from bundled themefile");

    JSONObject root = new JSONObject(new String(themeJsonBytes, StandardCharsets.UTF_8));
    JSONObject manifest = root.optJSONObject("manifest");
    if (manifest != null) themeRevision = manifest.optInt("revision", -1);
    semanticColors = parseSemantic(root);
  }

  private static Map<String, Integer> parseSemantic(JSONObject root) {
    JSONObject semantic = root.optJSONObject(SEMANTIC_SECTION);
    if (semantic == null) return null;
    Map<String, Integer> map = new HashMap<>();
    for (Iterator<String> it = semantic.keys(); it.hasNext(); ) {
      String key = it.next();
      if (!key.endsWith(SEMANTIC_SUFFIX)) continue;
      String token = key.substring(0, key.length() - SEMANTIC_SUFFIX.length());
      if (SEMANTIC_SKIP_TOKENS.contains(token)) continue;
      Integer color = parseColor(semantic.optString(key, null));
      if (color != null) map.put(token, color);
    }
    return map;
  }

  private static Integer parseColor(String hex) {
    if (hex == null || hex.isEmpty()) return null;
    try {
      return Color.parseColor(hex);
    } catch (Throwable t) {
      return null;
    }
  }

  private void installFileRedirects() {
    Hooker openHook =
        chain -> {
          Object[] args = chain.getArgs().toArray();
          if (args.length >= 1) {
            File mapped = mapAndPrepare(toFile(args[0]));
            if (mapped != null) {
              args[0] = (args[0] instanceof File) ? mapped : mapped.getAbsolutePath();
              return chain.proceed(args);
            }
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

  private static File mapAndPrepare(File requested) {
    if (requested == null) return null;
    String name = requested.getName();
    if (name == null || name.isEmpty()) return null;

    boolean isThemeJson = THEME_JSON.equals(name);
    boolean isBundle = name.startsWith("themefile.") && parseRevision(name) >= 0;
    boolean isImage = imageBlobs.containsKey(name) || cacheImages.containsKey(name);
    if (!isThemeJson && !isBundle && !isImage) return null;

    String path = requested.getAbsolutePath();
    if (path == null || !looksLikeThemePath(path)) return null;

    if (!ensureCacheExtracted()) return null;
    if (isThemeJson) return cacheJson;
    if (isBundle) return cacheBundle;
    return cacheImages.get(name);
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

  private static synchronized boolean ensureCacheExtracted() {
    if (cacheReady) return true;
    try {
      Context ctx = SettingsStore.getContext();
      File base = new File(ctx.getCacheDir(), CACHE_SUBDIR);
      File imagesDir = new File(base, "images");
      base.mkdirs();
      imagesDir.mkdirs();

      cacheJson = writeBytes(new File(base, THEME_JSON), themeJsonBytes);
      cacheBundle =
          writeBytes(new File(base, "themefile." + Math.max(themeRevision, 0)), themeBundleBytes);
      for (Map.Entry<String, byte[]> e : imageBlobs.entrySet()) {
        cacheImages.put(e.getKey(), writeBytes(new File(imagesDir, e.getKey()), e.getValue()));
      }

      themeBundleBytes = null;
      themeJsonBytes = null;
      imageBlobs.clear();

      cacheReady = true;
      Knot.log(
          "Knot: AmoledTheme cached: "
              + base.getAbsolutePath()
              + " ("
              + (2 + cacheImages.size())
              + " files)");
      return true;
    } catch (Throwable t) {
      Knot.log("Knot: AmoledTheme: cache extract failed: " + t);
      return false;
    }
  }

  private static File writeBytes(File f, byte[] data) throws IOException {
    try (FileOutputStream out = new FileOutputStream(f)) {
      out.write(data);
    }
    return f;
  }

  private static byte[] readAll(InputStream in) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    byte[] buf = new byte[8192];
    int n;
    while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);
    return bos.toByteArray();
  }

  private void installThriftValidationHijack(LoadParam lpparam) {
    LineVersion.Config cfg = LineVersion.get();
    if (cfg == null
        || cfg.thrift.protocolClass.isEmpty()
        || cfg.thrift.methodWriteMessageBegin.isEmpty()
        || cfg.thrift.methodReadMessageBegin.isEmpty()
        || cfg.thrift.messageClass.isEmpty()) {
      Knot.log("Knot: AmoledTheme: thrift config incomplete for current LINE version");
      return;
    }

    Hooker swap =
        chain -> {
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
    Knot.log(
        "Knot: AmoledTheme: Thrift hijack on "
            + cfg.thrift.protocolClass
            + "."
            + cfg.thrift.methodWriteMessageBegin
            + "/"
            + cfg.thrift.methodReadMessageBegin);
  }

  private static boolean isValidationMethod(String name) {
    return VALIDATION_METHODS.contains(name) || name.contains("validateProduct");
  }
}
