package app.zipper.knot.hooks;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import app.zipper.knot.Knot;
import app.zipper.knot.SettingsStore;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import org.json.JSONException;
import org.json.JSONObject;

final class AmoledThemeBundle {

  static final String THEME_JSON = "theme.json";
  static final String THEME_FILE_PREFIX = "themefile.";

  private static final String MODULE_PKG = "app.zipper.knot";
  private static final String ASSET_BUNDLE = "assets/amoled.themefile";
  private static final String CACHE_SUBDIR = "knot_amoled";
  private static final String IMAGES_SUBDIR = "images";
  private static final String IMAGES_PREFIX = IMAGES_SUBDIR + "/";

  private static final String SEMANTIC_SECTION = "theme.semantic";
  private static final String SEMANTIC_SUFFIX = ".background.color";
  private static final Set<String> SEMANTIC_SKIP_TOKENS =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList("primaryFill")));

  private static byte[] themeBundleBytes;
  private static byte[] themeJsonBytes;
  private static int themeRevision = -1;
  private static final Map<String, byte[]> imageBlobs = new HashMap<>();
  private static volatile Set<String> imageNames = Collections.emptySet();
  private static volatile Map<String, Integer> semanticColors;

  private static volatile boolean extracted;
  private static File cachedThemeJson;
  private static File cachedThemeFile;
  private static final Map<String, File> cachedImages = new HashMap<>();

  private AmoledThemeBundle() {}

  static void load() throws IOException, JSONException {
    Context ctx = SettingsStore.getContext();
    if (ctx == null) throw new IOException("SettingsStore has no context");
    ApplicationInfo info;
    try {
      info = ctx.getPackageManager().getApplicationInfo(MODULE_PKG, 0);
    } catch (PackageManager.NameNotFoundException e) {
      throw new IOException("module package not found: " + MODULE_PKG, e);
    }

    try (ZipFile apk = new ZipFile(info.sourceDir)) {
      ZipEntry entry = apk.getEntry(ASSET_BUNDLE);
      if (entry == null) throw new IOException(ASSET_BUNDLE + " missing from module APK");
      try (InputStream in = apk.getInputStream(entry)) {
        themeBundleBytes = readAll(in);
      }
    }

    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(themeBundleBytes))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        if (entry.isDirectory()) continue;
        String name = entry.getName();
        if (THEME_JSON.equals(name)) {
          themeJsonBytes = readAll(zis);
        } else if (name.startsWith(IMAGES_PREFIX)) {
          String base = name.substring(IMAGES_PREFIX.length());
          if (base.isEmpty() || base.contains("/")) continue;
          imageBlobs.put(base, readAll(zis));
        }
      }
    }
    if (themeJsonBytes == null) {
      throw new IOException(THEME_JSON + " missing from bundled themefile");
    }
    imageNames = Collections.unmodifiableSet(new HashSet<>(imageBlobs.keySet()));

    JSONObject root = new JSONObject(new String(themeJsonBytes, StandardCharsets.UTF_8));
    JSONObject manifest = root.optJSONObject("manifest");
    if (manifest != null) themeRevision = manifest.optInt("revision", -1);
    semanticColors = parseSemantic(root);

    log(
        "loaded: rev="
            + themeRevision
            + " images="
            + imageBlobs.size()
            + " bundleBytes="
            + themeBundleBytes.length);
  }

  static Map<String, Integer> semanticColors() {
    return semanticColors;
  }

  static boolean hasImage(String name) {
    return imageNames.contains(name);
  }

  static File themeJson() {
    return ensureExtracted() ? cachedThemeJson : null;
  }

  static File themeFile() {
    return ensureExtracted() ? cachedThemeFile : null;
  }

  static File image(String name) {
    return ensureExtracted() ? cachedImages.get(name) : null;
  }

  // Returns null until the bundle is extracted. Extraction itself runs through the hooked
  // File.exists(), so this must never trigger it.
  static File alreadyExtractedImage(String name) {
    return extracted ? cachedImages.get(name) : null;
  }

  private static synchronized boolean ensureExtracted() {
    if (extracted) return true;
    try {
      Context ctx = SettingsStore.getContext();
      File base = new File(ctx.getCacheDir(), CACHE_SUBDIR);
      File imagesDir = new File(base, IMAGES_SUBDIR);
      base.mkdirs();
      imagesDir.mkdirs();

      cachedThemeJson = writeBytes(new File(base, THEME_JSON), themeJsonBytes);
      cachedThemeFile =
          writeBytes(
              new File(base, THEME_FILE_PREFIX + Math.max(themeRevision, 0)), themeBundleBytes);
      for (Map.Entry<String, byte[]> e : imageBlobs.entrySet()) {
        cachedImages.put(e.getKey(), writeBytes(new File(imagesDir, e.getKey()), e.getValue()));
      }

      themeBundleBytes = null;
      themeJsonBytes = null;
      imageBlobs.clear();

      extracted = true;
      log("cached: " + base.getAbsolutePath() + " (" + (2 + cachedImages.size()) + " files)");
      return true;
    } catch (Throwable t) {
      log("cache extract failed: " + t);
      return false;
    }
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

  private static File writeBytes(File file, byte[] data) throws IOException {
    try (FileOutputStream out = new FileOutputStream(file)) {
      out.write(data);
    }
    return file;
  }

  private static byte[] readAll(InputStream in) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    byte[] buf = new byte[8192];
    int n;
    while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);
    return bos.toByteArray();
  }

  private static void log(String message) {
    Knot.log("Knot: AmoledTheme: " + message);
  }
}
