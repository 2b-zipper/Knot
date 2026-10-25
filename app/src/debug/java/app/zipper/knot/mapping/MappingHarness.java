package app.zipper.knot.mapping;

import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import app.zipper.knot.Knot;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindField;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.FieldData;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.UsingFieldData;

// CLASSPATH=knot.apk app_process -Djava.library.path=<dir with libdexkit.so> \
//     [-Dknot.checkLocal=true] / app.zipper.knot.mapping.MappingHarness <mode> <apk> ...
// A rules change is checked by diffing resolve's non-# lines against the previous build's output
public final class MappingHarness {

  private static final String LINE_PKG = "jp.naver.line.android";

  private MappingHarness() {}

  public static void main(String[] args) {
    try {
      run(args[0], args[1], Arrays.copyOfRange(args, 2, args.length));
    } catch (Throwable t) {
      t.printStackTrace(System.out);
    }
  }

  private static void run(String mode, String apk, String[] args) throws Exception {
    System.loadLibrary("dexkit");
    if (mode.equals("resolve")) {
      resolve(apk);
      for (String other : args) resolve(other);
      return;
    }
    try (DexKitBridge bridge = DexKitBridge.create(apk)) {
      switch (mode) {
        case "describe":
          for (String name : args) describe(bridge, name);
          break;
        case "users":
          for (String user : users(bridge, apk, args[0], args[1])) System.out.println(user);
          break;
        case "match":
          JSONObject spec = new JSONObject(args[0]);
          FindClass query = FindClass.create().matcher(matcher(spec));
          if (spec.has("pkg")) query.searchPackages(spec.getString("pkg"));
          for (ClassData c : bridge.findClass(query)) System.out.println(c.getName());
          break;
        case "refs":
          for (String type : args) refs(bridge, type);
          break;
        case "readers":
          for (String field : args) {
            FieldData fd = bridge.getFieldData(field);
            System.out.println(field + " -> " + (fd == null ? "no field" : fd.getReaders()));
          }
          break;
        case "pkganchors":
          try (Writer out = new FileWriter(args[1])) {
            MappingPackageAnchors.run(bridge, args[0], out);
          }
          break;
        default:
          throw new IllegalArgumentException(mode);
      }
    }
  }

  private static void resolve(String apk) throws Exception {
    System.out.println("=== " + apk);
    Knot.clearLogs();
    long start = System.nanoTime();
    Mappings.Result result;
    try (DexKitBridge bridge = DexKitBridge.create(apk)) {
      String threads = System.getProperty("knot.threads");
      if (threads != null) bridge.setThreadNum(Integer.parseInt(threads));
      Finder.checkLocal = Boolean.getBoolean("knot.checkLocal");
      result = Mappings.resolve(bridge, resources(apk), LINE_PKG);
    }
    long ms = (System.nanoTime() - start) / 1_000_000;
    for (String line : Knot.recentLogs()) System.out.println("# " + line);
    for (Map.Entry<String, Object> e : ConfigKeys.read(result.config).entrySet()) {
      System.out.println(e.getKey() + "\t" + e.getValue());
    }
    System.out.println("# " + ms + " ms, errors " + result.errors);
  }

  private static void describe(DexKitBridge bridge, String name) {
    ClassData c = bridge.getClassData(name);
    if (c == null) {
      System.out.println("no class " + name);
      return;
    }
    System.out.println(
        "class "
            + c.getName()
            + " extends "
            + (c.getSuperClass() == null ? "-" : c.getSuperClass().getName())
            + " implements "
            + c.getInterfaces()
            + " flags 0x"
            + Integer.toHexString(c.getModifiers())
            + " annos "
            + c.getAnnotations());
    for (FieldData f : c.getFields()) {
      System.out.println(
          "  field 0x"
              + Integer.toHexString(f.getModifiers())
              + " "
              + f.getTypeName()
              + " "
              + f.getName());
    }
    for (MethodData m : c.getMethods()) {
      System.out.println(
          "  method 0x"
              + Integer.toHexString(m.getModifiers())
              + " "
              + m.getReturnTypeName()
              + " "
              + m.getName()
              + m.getParamTypeNames()
              + " strings="
              + m.getUsingStrings()
              + " fields="
              + fieldUses(m)
              + " calls="
              + invokes(m));
    }
  }

  private static Set<String> users(DexKitBridge bridge, String apk, String kind, String what)
      throws Exception {
    Set<String> out = new LinkedHashSet<>();
    switch (kind) {
      case "id":
        int slash = what.indexOf('/');
        int id =
            resources(apk)
                .getIdentifier(what.substring(slash + 1), what.substring(0, slash), LINE_PKG);
        System.out.println("id 0x" + Integer.toHexString(id));
        for (MethodData m : findMethods(bridge, MethodMatcher.create().usingNumbers(id))) {
          out.add(m.getClassName() + "#" + m.getName());
        }
        break;
      case "num":
        for (MethodData m :
            findMethods(bridge, MethodMatcher.create().usingNumbers(Long.parseLong(what)))) {
          out.add(m.getClassName() + "#" + m.getName() + " " + m.getParamTypeNames());
        }
        break;
      case "string":
        for (MethodData m : findMethods(bridge, MethodMatcher.create().usingEqStrings(what))) {
          out.add(m.getClassName() + "#" + m.getName());
        }
        break;
      case "impl":
        for (ClassData c : findClasses(bridge, ClassMatcher.create().addInterface(what))) {
          out.add(c.getName());
        }
        for (ClassData c : findClasses(bridge, ClassMatcher.create().superClass(what))) {
          out.add(c.getName() + " (subclass)");
        }
        break;
      case "anno":
        for (ClassData c : findClasses(bridge, Finder.annotated(what))) out.add(c.getName());
        break;
      default:
        throw new IllegalArgumentException(kind);
    }
    return out;
  }

  private static void refs(DexKitBridge bridge, String type) {
    System.out.println("== " + type);
    for (FieldData fd :
        bridge.findField(FindField.create().matcher(FieldMatcher.create().type(type)))) {
      System.out.println("field " + fd.getClassName() + " " + fd.getName());
    }
    for (MethodData m : findMethods(bridge, MethodMatcher.create().returnType(type))) {
      System.out.println("returns " + m.getClassName() + " " + m.getName());
    }
    ClassData c = bridge.getClassData(type);
    if (c == null) return;
    for (MethodData m : c.getMethods()) {
      if (!m.isConstructor()) continue;
      for (MethodData caller : m.getCallers()) {
        System.out.println("new " + caller.getClassName() + " " + caller.getName());
      }
    }
  }

  private static ClassMatcher matcher(JSONObject spec) throws Exception {
    ClassMatcher m = ClassMatcher.create();
    if (spec.has("super")) m.superClass(spec.getString("super"));
    if (spec.has("mods")) m.modifiers(spec.getInt("mods"));
    if (spec.has("superMatch")) m.superClass(matcher(spec.getJSONObject("superMatch")));
    for (String iface : strings(spec, "ifaces")) m.addInterface(iface);
    JSONArray ifaceMatch = spec.optJSONArray("ifaceMatch");
    if (ifaceMatch != null) {
      for (int i = 0; i < ifaceMatch.length(); i++) {
        m.addInterface(matcher(ifaceMatch.getJSONObject(i)));
      }
    }
    for (String type : strings(spec, "fields")) m.addField(FieldMatcher.create().type(type));
    for (String name : strings(spec, "fieldNames")) m.addField(FieldMatcher.create().name(name));
    for (String name : strings(spec, "usesFields")) {
      m.addMethod(MethodMatcher.create().addUsingField(FieldMatcher.create().name(name)));
    }
    JSONArray numbers = spec.optJSONArray("usesNumbers");
    if (numbers != null) {
      for (int i = 0; i < numbers.length(); i++) {
        long v = numbers.getLong(i);
        m.addMethod(MethodMatcher.create().usingNumbers(v == (int) v ? (Number) (int) v : v));
      }
    }
    JSONArray methods = spec.optJSONArray("methods");
    if (methods != null) {
      for (int i = 0; i < methods.length(); i++) {
        JSONObject mj = methods.getJSONObject(i);
        MethodMatcher mm = MethodMatcher.create();
        if (mj.optBoolean("ctor")) mm.name("<init>");
        else if (!mj.isNull("ret")) mm.returnType(mj.getString("ret"));
        JSONArray ps = mj.getJSONArray("params");
        List<String> params = new ArrayList<>();
        for (int p = 0; p < ps.length(); p++) params.add(ps.isNull(p) ? null : ps.getString(p));
        mm.paramTypes(params);
        m.addMethod(mm);
      }
    }
    List<String> used = strings(spec, "strings");
    if (!used.isEmpty()) m.usingEqStrings(used);
    if (spec.has("methodCount")) m.methodCount(spec.getInt("methodCount"));
    if (spec.has("fieldCount")) m.fieldCount(spec.getInt("fieldCount"));
    return m;
  }

  private static List<String> strings(JSONObject spec, String key) throws Exception {
    List<String> out = new ArrayList<>();
    JSONArray array = spec.optJSONArray(key);
    if (array != null) for (int i = 0; i < array.length(); i++) out.add(array.getString(i));
    return out;
  }

  private static List<MethodData> findMethods(DexKitBridge bridge, MethodMatcher matcher) {
    return bridge.findMethod(FindMethod.create().matcher(matcher));
  }

  private static List<ClassData> findClasses(DexKitBridge bridge, ClassMatcher matcher) {
    return bridge.findClass(FindClass.create().matcher(matcher));
  }

  private static String invokes(MethodData m) {
    StringBuilder sb = new StringBuilder("[");
    for (MethodData callee : m.getInvokes()) {
      if (sb.length() > 1) sb.append(", ");
      sb.append(callee.getDescriptor());
    }
    return sb.append(']').toString();
  }

  private static String fieldUses(MethodData m) {
    StringBuilder sb = new StringBuilder("[");
    for (UsingFieldData u : m.getUsingFields()) {
      if (sb.length() > 1) sb.append(", ");
      sb.append(u.getUsingType().isRead() ? "r " : "w ")
          .append(u.getField().getClassName())
          .append("->")
          .append(u.getField().getName());
    }
    return sb.append(']').toString();
  }

  private static Resources resources(String apk) throws Exception {
    AssetManager assets = AssetManager.class.getDeclaredConstructor().newInstance();
    AssetManager.class.getMethod("addAssetPath", String.class).invoke(assets, apk);
    return new Resources(assets, new DisplayMetrics(), new Configuration());
  }
}
