package app.zipper.knot.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import app.zipper.knot.Knot;
import app.zipper.knot.R;
import app.zipper.knot.SettingsStore;
import java.util.Locale;

/**
 * Resolves the module's string resources, which the LINE process can only reach through a package
 * context for {@link #MODULE_PACKAGE}. See the README for how to add a language.
 */
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

  private static Resources build(String lang) {
    Context moduleContext = moduleContext();
    if (moduleContext == null) return null;

    Resources base = moduleContext.getResources();
    if (lang.isEmpty()) return base;

    try {
      Configuration config = new Configuration(base.getConfiguration());
      config.setLocale(localeOf(lang));
      return moduleContext.createConfigurationContext(config).getResources();
    } catch (Throwable t) {
      return base;
    }
  }

  private static Context moduleContext() {
    Context base = baseContext();
    if (base == null) return null;
    try {
      if (MODULE_PACKAGE.equals(base.getPackageName())) return base;
      return base.createPackageContext(MODULE_PACKAGE, Context.CONTEXT_IGNORE_SECURITY);
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
