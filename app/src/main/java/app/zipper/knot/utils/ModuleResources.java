package app.zipper.knot.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import app.zipper.knot.Knot;
import app.zipper.knot.R;
import app.zipper.knot.SettingsStore;
import io.github.libxposed.api.XposedInterface;
import java.util.Locale;

public final class ModuleResources {

  public static final String MODULE_PACKAGE = "app.zipper.knot";

  // A view tag as much as a label, so it is never translated
  public static final String BRAND_NAME = "Knot";

  public static final String LANGUAGE_KEY = "language";

  // Stored value meaning "follow the device language"
  public static final String LANGUAGE_SYSTEM = "";

  // Locales the module ships, in picker order; en is served by the default values folder
  public static final String[] SUPPORTED_LANGUAGES = {"en", "ja", "zh-Hant"};

  private static volatile Context hostContext;
  private static volatile String language;
  private static volatile Resources cached;
  private static volatile String cachedKey;
  private static volatile boolean resolveFailureLogged;

  private ModuleResources() {}

  // Remembers a context to resolve the module APK with, before SettingsStore has one
  public static void attach(Context context) {
    if (context == null || hostContext != null) return;
    Context app = context.getApplicationContext();
    hostContext = app != null ? app : context;
  }

  public static String get(int resId) {
    Resources res = resources();
    if (res == null) return "";
    try {
      return res.getString(resId);
    } catch (Throwable t) {
      return "";
    }
  }

  public static String get(int resId, Object... formatArgs) {
    Resources res = resources();
    if (res == null) return "";
    try {
      return res.getString(resId, formatArgs);
    } catch (Throwable t) {
      return "";
    }
  }

  public static String getQuantity(int resId, int quantity, Object... formatArgs) {
    Resources res = resources();
    if (res == null) return "";
    try {
      return res.getQuantityString(resId, quantity, formatArgs);
    } catch (Throwable t) {
      return "";
    }
  }

  public static Drawable drawable(String name) {
    Resources res = resources();
    if (res == null) return null;
    try {
      int id = res.getIdentifier(name, "drawable", MODULE_PACKAGE);
      return id == 0 ? null : res.getDrawable(id, null);
    } catch (Throwable t) {
      return null;
    }
  }

  public static int drawableId(String name) {
    Resources res = resources();
    if (res == null) return 0;
    try {
      return res.getIdentifier(name, "drawable", MODULE_PACKAGE);
    } catch (Throwable t) {
      return 0;
    }
  }

  public static String language() {
    String current = language;
    if (current != null) return current;
    synchronized (ModuleResources.class) {
      if (language == null) {
        String stored = null;
        try {
          stored = SettingsStore.getString(LANGUAGE_KEY, LANGUAGE_SYSTEM);
        } catch (Throwable ignored) {
        }
        language = stored != null ? stored : LANGUAGE_SYSTEM;
      }
      return language;
    }
  }

  public static void setLanguage(String tag) {
    String value = tag != null ? tag : LANGUAGE_SYSTEM;
    SettingsStore.save(LANGUAGE_KEY, value);
    synchronized (ModuleResources.class) {
      language = value;
      cached = null;
      cachedKey = null;
    }
  }

  public static void invalidate() {
    synchronized (ModuleResources.class) {
      language = null;
      cached = null;
      cachedKey = null;
    }
  }

  // Each translation names itself; Locale.getDisplayName would render zh-Hant as "中文 (繁體)"
  public static String displayName(String tag) {
    try {
      Resources res = build(tag);
      if (res != null) return res.getString(R.string.language_name);
    } catch (Throwable ignored) {
    }
    return tag;
  }

  // Accepts both BCP-47 (ja-JP) and the Java Locale form (ja_JP)
  private static Locale localeOf(String tag) {
    return Locale.forLanguageTag(tag.replace('_', '-'));
  }

  private static Resources resources() {
    String lang = language();
    String key = lang.isEmpty() ? "system:" + Locale.getDefault().toLanguageTag() : lang;

    Resources current = cached;
    if (current != null && key.equals(cachedKey)) return current;

    synchronized (ModuleResources.class) {
      if (cached != null && key.equals(cachedKey)) return cached;
      Resources built = build(lang);
      if (built == null) return cached;
      cached = built;
      cachedKey = key;
      return built;
    }
  }

  @SuppressWarnings("deprecation")
  private static Resources build(String lang) {
    Resources base = baseResources();
    if (base == null) return null;
    if (lang.isEmpty()) return base;

    try {
      Configuration config = new Configuration(base.getConfiguration());
      config.setLocale(localeOf(lang));
      return new Resources(base.getAssets(), base.getDisplayMetrics(), config);
    } catch (Throwable t) {
      return base;
    }
  }

  // Issue #38: the host's PackageManager may not see the module, so ask the framework first.
  public static ApplicationInfo applicationInfo(Context host) {
    XposedInterface module = Knot.module;
    if (module != null) {
      try {
        ApplicationInfo info = module.getModuleApplicationInfo();
        if (info != null) return withPublicSourceDir(info);
      } catch (Throwable ignored) {
      }
    }

    Context base = host != null ? host : baseContext();
    if (base == null) return null;
    try {
      return base.getPackageManager().getApplicationInfo(MODULE_PACKAGE, 0);
    } catch (Throwable t) {
      logResolveFailure(t);
      return null;
    }
  }

  // Another uid's resources are read from publicSourceDir, which the framework may leave unset.
  private static ApplicationInfo withPublicSourceDir(ApplicationInfo info) {
    if (info.publicSourceDir != null && !info.publicSourceDir.isEmpty()) return info;
    if (info.sourceDir == null || info.sourceDir.isEmpty()) return info;
    ApplicationInfo copy = new ApplicationInfo(info);
    copy.publicSourceDir = info.sourceDir;
    return copy;
  }

  private static Resources baseResources() {
    Context base = baseContext();
    if (base == null) return null;
    if (MODULE_PACKAGE.equals(base.getPackageName())) return base.getResources();

    ApplicationInfo info = applicationInfo(base);
    if (info == null) return null;
    try {
      return base.getPackageManager().getResourcesForApplication(info);
    } catch (Throwable t) {
      logResolveFailure(t);
      return null;
    }
  }

  // Every string would come back empty, so make the cause findable without spamming the log
  private static void logResolveFailure(Throwable t) {
    if (resolveFailureLogged) return;
    resolveFailureLogged = true;
    Knot.log("Knot: module resources unavailable: " + t);
  }

  private static Context baseContext() {
    Context base = hostContext;
    if (base == null) base = SettingsStore.getContext();
    if (base == null) base = Knot.currentApplication();
    return base;
  }
}
