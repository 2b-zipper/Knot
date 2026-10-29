package app.zipper.knot.mapping;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.os.SystemClock;
import android.widget.Toast;
import app.zipper.knot.BuildConfig;
import app.zipper.knot.Knot;
import app.zipper.knot.LineVersion;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.FieldData;
import org.luckypray.dexkit.result.MethodData;

public final class Mappings {

  interface Section {
    void resolve(Finder f, LineVersion.Config c);
  }

  private static final String RULES_PACKAGE =
      Mappings.class.getName().substring(0, Mappings.class.getName().lastIndexOf('.') + 1);

  private Mappings() {}

  public static LineVersion.Config load(Context host, ClassLoader cl) {
    MappingCache cache = new MappingCache(host, stamp(host));
    LineVersion.Config cached = cache.read();
    if (cached != null) return cached;

    Toast notice = showResolving(host);
    try {
      return resolveAndCache(host, cl, cache);
    } finally {
      if (notice != null) notice.cancel();
    }
  }

  private static Toast showResolving(Context host) {
    if (!host.getPackageName().equals(Knot.processName)) return null;
    try {
      Toast toast =
          Toast.makeText(host, "Knot: Finding LINE's obfuscated names…", Toast.LENGTH_LONG);
      toast.show();
      return toast;
    } catch (Throwable t) {
      Knot.log("Knot: mapping notice not shown: " + t);
      return null;
    }
  }

  private static LineVersion.Config resolveAndCache(
      Context host, ClassLoader cl, MappingCache cache) {
    if (!DexKitLoader.load(host)) {
      Knot.log("Knot: DexKit could not be loaded");
      return null;
    }
    long start = SystemClock.elapsedRealtime();
    Result result;
    // NPatch's APK path holds only its loader
    try (DexKitBridge bridge = DexKitBridge.create(cl, true)) {
      result = resolve(bridge, host.getResources(), host.getPackageName());
    } catch (Throwable t) {
      Knot.log("Knot: DexKit failed", t);
      return null;
    }
    Knot.log(
        "Knot: mappings resolved in "
            + (SystemClock.elapsedRealtime() - start)
            + " ms, unresolved: "
            + unresolved(result.config, LineVersion.getVersionName(host)));
    // Retried once: a transient error shouldn't stick, a lasting one shouldn't cost every launch
    if (result.errors == 0 || cache.failedBefore()) cache.write(result.config);
    else cache.markFailed();
    return result.config;
  }

  static final class Result {
    final LineVersion.Config config;
    final int errors;

    Result(LineVersion.Config config, int errors) {
      this.config = config;
      this.errors = errors;
    }
  }

  static Result resolve(DexKitBridge bridge, Resources res, String pkg) {
    LineVersion.Config c = new LineVersion.Config();
    Finder f = new Finder(bridge, res, pkg);
    // Feature rules read only what library and core sections found
    Section[][] all = {
      LibraryRules.SECTIONS,
      CoreRules.SECTIONS,
      SettingsRules.SECTIONS,
      HeaderRules.SECTIONS,
      ChatRules.SECTIONS,
      NotificationRules.SECTIONS,
      HideChatRules.SECTIONS,
      MessageRules.SECTIONS,
      MediaRules.SECTIONS
    };
    // Lookups come back empty while planning; the pass only gathers classUsing strings to batch
    Finder plan = f.planner();
    for (Section[] sections : all) {
      for (Section section : sections) {
        try {
          section.resolve(plan, new LineVersion.Config());
        } catch (Throwable ignored) {
          // The real pass reports
        }
      }
    }
    f.prefetch(plan);
    int errors = 0;
    for (Section[] sections : all) {
      for (Section section : sections) {
        try {
          section.resolve(f, c);
        } catch (Throwable t) {
          errors++;
          Knot.log("Knot: mapping section failed", t);
        }
      }
    }
    return new Result(c, errors + f.errors);
  }

  // Takes effect on the next launch
  public static void reset(Context host) {
    MappingCache.delete(host);
  }

  public static List<String> unresolved(LineVersion.Config config, String lineVersion) {
    Map<String, String> since = ConfigKeys.since();
    Map<String, String> until = ConfigKeys.until();
    List<String> keys = new ArrayList<>();
    for (Map.Entry<String, Object> e : ConfigKeys.read(config).entrySet()) {
      Object v = e.getValue();
      boolean empty = v == null || "".equals(v) || Integer.valueOf(0).equals(v);
      String added = since.get(e.getKey());
      String removed = until.get(e.getKey());
      if (empty
          && (added == null || !olderThan(lineVersion, added))
          && (removed == null || olderThan(lineVersion, removed))) {
        keys.add(e.getKey());
      }
    }
    return keys;
  }

  private static boolean olderThan(String version, String other) {
    if (version == null) return false;
    String[] a = version.split("\\.");
    String[] b = other.split("\\.");
    try {
      for (int i = 0; i < Math.min(a.length, b.length); i++) {
        int d = Integer.compare(Integer.parseInt(a[i]), Integer.parseInt(b[i]));
        if (d != 0) return d < 0;
      }
    } catch (NumberFormatException ignored) {
    }
    return false;
  }

  // Searches Knot's own dex, following callers until owner accepts a class
  public static Set<String> users(Context host, List<String> keys, Predicate<String> owner) {
    if (keys.isEmpty() || !DexKitLoader.load(host)) return new HashSet<>();
    try (DexKitBridge bridge =
        DexKitBridge.create(Knot.module.getModuleApplicationInfo().sourceDir)) {
      return users(bridge, keys, owner);
    } catch (Throwable t) {
      Knot.log("Knot: mapping users unavailable", t);
      return new HashSet<>();
    }
  }

  static Set<String> users(DexKitBridge bridge, List<String> keys, Predicate<String> owner)
      throws ReflectiveOperationException {
    Set<String> found = new HashSet<>();
    Set<String> seen = new HashSet<>();
    for (String key : keys) {
      int dot = key.indexOf('.');
      Field section = LineVersion.Config.class.getField(key.substring(0, dot));
      FieldData field = bridge.getFieldData(section.getType().getField(key.substring(dot + 1)));
      if (field != null) trace(bridge, field.getReaders(), owner, found, seen);
    }
    return found;
  }

  private static void trace(
      DexKitBridge bridge,
      List<MethodData> methods,
      Predicate<String> owner,
      Set<String> found,
      Set<String> seen) {
    for (MethodData method : methods) {
      if (!seen.add(method.getDescriptor())) continue;
      String cls = method.getClassName();
      int nested = cls.indexOf('$');
      String top = nested > 0 ? cls.substring(0, nested) : cls;
      // Rules read earlier keys to find later ones
      if (top.startsWith(RULES_PACKAGE)) continue;
      if (owner.test(top)) {
        found.add(top);
        continue;
      }
      List<MethodData> callers = method.getCallers();
      // Lambdas and listeners are called through interfaces, so go on from where they are created
      if (callers.isEmpty()) callers = creators(bridge, cls);
      trace(bridge, callers, owner, found, seen);
    }
  }

  private static List<MethodData> creators(DexKitBridge bridge, String cls) {
    List<MethodData> creators = new ArrayList<>();
    ClassData data = bridge.getClassData(cls);
    if (data == null) return creators;
    for (MethodData method : data.getMethods()) {
      if (method.isConstructor()) creators.addAll(method.getCallers());
    }
    // D8 keeps a stateless lambda in a static INSTANCE field
    for (FieldData field : data.getFields()) {
      if (cls.equals(field.getTypeName())) creators.addAll(field.getReaders());
    }
    return creators;
  }

  private static String stamp(Context host) {
    StringBuilder stamp = new StringBuilder();
    try {
      appendApk(stamp, host.getApplicationInfo());
      stamp.append('/').append(BuildConfig.VERSION_CODE);
      appendApk(stamp, Knot.module.getModuleApplicationInfo());
    } catch (Throwable t) {
      stamp.append("/?");
    }
    return stamp.toString();
  }

  private static void appendApk(StringBuilder stamp, ApplicationInfo info) {
    File apk = new File(info.sourceDir);
    stamp.append('/').append(apk.length()).append('/').append(apk.lastModified());
  }
}
