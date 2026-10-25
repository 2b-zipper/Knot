package app.zipper.knot.mapping;

import android.content.res.Resources;
import app.zipper.knot.Knot;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.BatchFindClassUsingStrings;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindField;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.enums.AnnotationEncodeValueType;
import org.luckypray.dexkit.query.enums.StringMatchType;
import org.luckypray.dexkit.query.matchers.AnnotationMatcher;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.AnnotationData;
import org.luckypray.dexkit.result.AnnotationElementData;
import org.luckypray.dexkit.result.AnnotationEncodeValue;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.FieldData;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.UsingFieldData;

// Lookups return "" (or null / 0) unless exactly one candidate matches
// Member lookups include superclasses except declaredMethods; scoped ones try the package first
final class Finder {

  private static final Pattern LABEL = Pattern.compile("(?:([\\w$]+)\\(|, )(\\w+)=");

  private final DexKitBridge bridge;
  private final Resources res;
  private final String pkg;
  int errors;

  private final boolean planning;
  private final Set<List<String>> plannedStrings = new LinkedHashSet<>();
  private final Map<List<String>, List<String>> stringHits = new HashMap<>();
  private final Map<String, ClassData> classes = new HashMap<>();
  private final Map<String, List<ClassData>> packages = new HashMap<>();
  private final LocalMatch local = new LocalMatch(this::lookup);

  // Only the debug harness sets this
  static boolean checkLocal;

  Finder(DexKitBridge bridge, Resources res, String pkg) {
    this(bridge, res, pkg, false);
  }

  private Finder(DexKitBridge bridge, Resources res, String pkg, boolean planning) {
    this.bridge = bridge;
    this.res = res;
    this.pkg = pkg;
    this.planning = planning;
  }

  Finder planner() {
    return new Finder(bridge, res, pkg, true);
  }

  void prefetch(Finder plan) {
    if (plan.plannedStrings.isEmpty()) return;
    try {
      BatchFindClassUsingStrings batch = BatchFindClassUsingStrings.create();
      List<List<String>> groups = new ArrayList<>(plan.plannedStrings);
      for (int i = 0; i < groups.size(); i++) {
        batch.addSearchGroup(String.valueOf(i), groups.get(i), StringMatchType.Equals);
      }
      Map<String, ? extends List<ClassData>> found = bridge.batchFindClassUsingStrings(batch);
      for (int i = 0; i < groups.size(); i++) {
        List<String> names = new ArrayList<>();
        List<ClassData> users = found.get(String.valueOf(i));
        if (users != null) for (ClassData c : users) names.add(c.getName());
        stringHits.put(groups.get(i), names);
      }
    } catch (Throwable t) {
      // Each anchor falls back to its own search
      stringHits.clear();
      Knot.log("Knot: mapping prefetch failed", t);
    }
  }

  // null leaves the return type or that parameter open
  static MethodMatcher sig(String ret, String... params) {
    MethodMatcher m = MethodMatcher.create();
    if (ret != null) m.returnType(ret);
    return m.paramTypes(params);
  }

  static String[] leading(int count, String... types) {
    return Arrays.copyOf(types, count);
  }

  // DebugMetadata keeps the original names of coroutine classes
  static ClassMatcher annotated(String... strings) {
    return ClassMatcher.create().addAnnotation(AnnotationMatcher.create().usingEqStrings(strings));
  }

  String cls(ClassMatcher matcher) {
    if (planning) return "";
    try {
      ClassData found = one(bridge.findClass(FindClass.create().matcher(matcher)));
      return found == null ? "" : found.getName();
    } catch (Throwable t) {
      return none(t);
    }
  }

  String clsIn(String scope, ClassMatcher matcher) {
    List<String> found = classesIn(scope, matcher);
    return found.size() == 1 ? found.get(0) : cls(matcher);
  }

  List<String> classesIn(String scope, ClassMatcher matcher) {
    List<String> found = new ArrayList<>();
    if (planning || scope.isEmpty()) return found;
    try {
      for (ClassData c :
          bridge.findClass(FindClass.create().searchPackages(scope).matcher(matcher))) {
        if (!packageOf(c.getName()).equals(scope)) continue;
        found.add(c.getName());
        classes.putIfAbsent(c.getName(), c);
      }
    } catch (Throwable t) {
      error(t);
    }
    return found;
  }

  String classUsing(String... strings) {
    List<String> group = Arrays.asList(strings);
    if (planning) {
      plannedStrings.add(group);
      return "";
    }
    List<String> hits = stringHits.get(group);
    return hits != null ? oneName(hits) : cls(ClassMatcher.create().usingEqStrings(strings));
  }

  String classUsing(ClassMatcher matcher, String... strings) {
    List<String> group = Arrays.asList(strings);
    if (planning) {
      plannedStrings.add(group);
      return "";
    }
    List<String> hits = stringHits.get(group);
    return hits != null ? classAmong(hits, matcher) : cls(matcher.usingEqStrings(strings));
  }

  String classAmong(List<String> names, ClassMatcher matcher) {
    if (planning) return "";
    try {
      List<ClassData> candidates = new ArrayList<>();
      for (String name : names) {
        ClassData c = lookup(name);
        if (c != null) candidates.add(c);
      }
      if (candidates.isEmpty()) return none("no candidates");
      return oneName(matching(candidates, matcher));
    } catch (Throwable t) {
      return none(t);
    }
  }

  String fieldTypeWhere(String owner, ClassMatcher matcher) {
    List<ClassData> chain = hierarchy(owner);
    if (chain.isEmpty()) return "";
    Set<String> types = new LinkedHashSet<>();
    for (ClassData c : chain) for (FieldData fd : c.getFields()) types.add(fd.getTypeName());
    return classAmong(new ArrayList<>(types), matcher);
  }

  String returnTypeWhere(String owner, ClassMatcher matcher) {
    List<ClassData> chain = hierarchy(owner);
    if (chain.isEmpty()) return "";
    Set<String> types = new LinkedHashSet<>();
    for (ClassData c : chain) {
      for (MethodData m : c.getMethods()) if (m.isMethod()) types.add(m.getReturnTypeName());
    }
    return classAmong(new ArrayList<>(types), matcher);
  }

  private List<String> matching(List<ClassData> candidates, ClassMatcher matcher) {
    List<String> found = new ArrayList<>();
    try {
      for (ClassData c : candidates) if (local.cls(matcher, c)) found.add(c.getName());
      if (!checkLocal) return found;
    } catch (LocalMatch.Unsupported e) {
      found = null;
    }
    List<String> expected = new ArrayList<>();
    for (ClassData c : bridge.findClass(FindClass.create().searchIn(candidates).matcher(matcher))) {
      expected.add(c.getName());
    }
    return compared(found, expected);
  }

  String classUsingId(String type, String name) {
    return classUsingId(MethodMatcher.create(), type, name);
  }

  // DexKit checks the signature before reading code
  String classUsingId(MethodMatcher using, String type, String name) {
    int id = id(type, name);
    if (planning || id == 0) return "";
    try {
      Set<String> users = new LinkedHashSet<>();
      for (MethodData m : bridge.findMethod(FindMethod.create().matcher(using.usingNumbers(id)))) {
        users.add(m.getClassName());
      }
      return oneName(new ArrayList<>(users));
    } catch (Throwable t) {
      return none(t);
    }
  }

  String classUsingIdIn(String scope, String type, String name) {
    int id = id(type, name);
    if (!planning && id != 0 && !scope.isEmpty()) {
      try {
        Set<String> users = new LinkedHashSet<>();
        for (MethodData m :
            bridge.findMethod(
                FindMethod.create()
                    .searchPackages(scope)
                    .matcher(MethodMatcher.create().usingNumbers(id)))) {
          if (packageOf(m.getClassName()).equals(scope)) users.add(m.getClassName());
        }
        if (users.size() == 1) return users.iterator().next();
      } catch (Throwable t) {
        error(t);
      }
    }
    return classUsingId(type, name);
  }

  String classAnnotated(String... strings) {
    return cls(annotated(strings));
  }

  // An annotation query indexes the whole app (about 1 s on a phone)
  String classAnnotatedIn(String scope, String... strings) {
    if (!planning && !scope.isEmpty()) {
      try {
        List<String> found = new ArrayList<>();
        for (ClassData c : packageClasses(scope)) {
          for (AnnotationData a : c.getAnnotations()) {
            Set<String> used = new HashSet<>();
            collectStrings(a, used);
            if (used.containsAll(Arrays.asList(strings))) {
              found.add(c.getName());
              break;
            }
          }
        }
        if (found.size() == 1) return found.get(0);
      } catch (Throwable t) {
        error(t);
      }
    }
    return classAnnotated(strings);
  }

  // Costs more than a query in the package, so only the annotation scans use it
  private List<ClassData> packageClasses(String scope) {
    List<ClassData> found = packages.get(scope);
    if (found != null) return found;
    found = new ArrayList<>();
    for (ClassData c : bridge.findClass(FindClass.create().searchPackages(scope))) {
      if (!packageOf(c.getName()).equals(scope)) continue;
      found.add(c);
      classes.putIfAbsent(c.getName(), c);
    }
    packages.put(scope, found);
    return found;
  }

  private static void collectStrings(AnnotationData annotation, Set<String> out) {
    for (AnnotationElementData e : annotation.getElements()) collectStrings(e.getValue(), out);
  }

  private static void collectStrings(AnnotationEncodeValue value, Set<String> out) {
    switch (value.getType()) {
      case StringValue:
        out.add(value.stringValue());
        break;
      case ArrayValue:
        for (AnnotationEncodeValue v : value.arrayValue().getValues()) collectStrings(v, out);
        break;
      case AnnotationValue:
        collectStrings(value.annotationValue(), out);
        break;
      default:
        break;
    }
  }

  ClassData classData(String name) {
    if (planning || !ready(name)) return null;
    try {
      ClassData data = lookup(name);
      if (data == null) fail("no class " + name);
      return data;
    } catch (Throwable t) {
      return error(t);
    }
  }

  // null outside the app, without logging
  private ClassData lookup(String name) {
    ClassData data = classes.get(name);
    if (data == null) {
      data = bridge.getClassData(name);
      if (data != null) classes.put(name, data);
    }
    return data;
  }

  String superOf(String name) {
    ClassData data = classData(name);
    if (data == null) return "";
    ClassData sup = data.getSuperClass();
    return sup == null ? "" : sup.getName();
  }

  String interfaceOf(String name) {
    ClassData data = classData(name);
    if (data == null) return "";
    try {
      ClassData found = one(data.getInterfaces());
      return found == null ? "" : found.getName();
    } catch (Throwable t) {
      return none(t);
    }
  }

  String method(String owner, MethodMatcher matcher) {
    return name(methodData(owner, matcher));
  }

  MethodData methodData(String owner, MethodMatcher matcher) {
    try {
      for (ClassData c : hierarchy(owner)) {
        List<MethodData> found = new ArrayList<>();
        for (MethodData m : declared(c, matcher)) if (m.isMethod()) found.add(m);
        if (!found.isEmpty()) return one(found);
      }
      return ready(owner) ? fail("no match") : null;
    } catch (Throwable t) {
      return error(t);
    }
  }

  List<MethodData> declaredMethods(String owner, MethodMatcher matcher) {
    List<MethodData> found = new ArrayList<>();
    ClassData c = classData(owner);
    if (c == null) return found;
    try {
      for (MethodData m : declared(c, matcher)) if (m.isMethod()) found.add(m);
    } catch (Throwable t) {
      error(t);
    }
    return found;
  }

  String field(String owner, FieldMatcher matcher) {
    FieldData found = fieldData(owner, matcher);
    return found == null ? "" : found.getName();
  }

  FieldData fieldData(String owner, FieldMatcher matcher) {
    try {
      for (ClassData c : hierarchy(owner)) {
        List<FieldData> found = declared(c, matcher);
        if (!found.isEmpty()) return one(found);
      }
      return ready(owner) ? fail("no match") : null;
    } catch (Throwable t) {
      return error(t);
    }
  }

  private List<MethodData> declared(ClassData c, MethodMatcher matcher) {
    List<MethodData> found = new ArrayList<>();
    try {
      for (MethodData m : c.getMethods()) if (local.method(matcher, m)) found.add(m);
      if (!checkLocal) return found;
    } catch (LocalMatch.Unsupported e) {
      found = null;
    }
    return compared(found, c.findMethod(FindMethod.create().matcher(matcher)));
  }

  private List<FieldData> declared(ClassData c, FieldMatcher matcher) {
    List<FieldData> found = new ArrayList<>();
    try {
      for (FieldData fd : c.getFields()) if (local.field(matcher, fd)) found.add(fd);
      if (!checkLocal) return found;
    } catch (LocalMatch.Unsupported e) {
      found = null;
    }
    return compared(found, c.findField(FindField.create().matcher(matcher)));
  }

  private <T> List<T> compared(List<T> local, List<T> dexkit) {
    if (local != null && !new HashSet<>(local).equals(new HashSet<>(dexkit))) {
      Knot.log("Knot: LocalMatch disagrees at " + caller() + ": " + local + " vs " + dexkit);
    }
    return dexkit;
  }

  String methodOf(String owner, String ret, String... params) {
    return name(methodWhere(owner, m -> sigMatches(m, ret, params)));
  }

  String staticMethodOf(String owner, String ret, String... params) {
    return name(
        methodWhere(owner, m -> Modifier.isStatic(m.getModifiers()) && sigMatches(m, ret, params)));
  }

  String annotatedMethodOf(String owner, String annotation, String ret, String... params) {
    return name(
        methodWhere(
            owner,
            m -> {
              if (!sigMatches(m, ret, params)) return false;
              for (AnnotationData a : m.getAnnotations()) {
                if (a.getTypeName().equals(annotation)) return true;
              }
              return false;
            }));
  }

  String methodTaking(String owner, String type) {
    if (!ready(type)) return "";
    return name(methodWhere(owner, m -> m.getParamTypeNames().contains(type)));
  }

  String fieldOf(String owner, String type) {
    if (!ready(type)) return "";
    FieldData found = fieldWhere(owner, fd -> fd.getTypeName().equals(type));
    return found == null ? "" : found.getName();
  }

  String fieldType(String owner, String field) {
    if (!ready(field)) return "";
    FieldData found = fieldWhere(owner, fd -> fd.getName().equals(field));
    return found == null ? "" : found.getTypeName();
  }

  String returnType(String owner, String method) {
    if (!ready(method)) return "";
    MethodData found = methodWhere(owner, m -> m.getName().equals(method));
    return found == null ? "" : found.getReturnTypeName();
  }

  String paramType(String owner, String method, int index) {
    if (!ready(method)) return "";
    MethodData found = methodWhere(owner, m -> m.getName().equals(method));
    if (found == null) return "";
    List<String> params = found.getParamTypeNames();
    return index < params.size() ? params.get(index) : none("no parameter " + index);
  }

  String fieldReadIn(String owner, String method) {
    if (!ready(method)) return "";
    MethodData m = methodWhere(owner, x -> x.getName().equals(method));
    if (m == null) return "";
    try {
      return oneName(reads(m, m.getClassName()));
    } catch (Throwable t) {
      return none(t);
    }
  }

  String getter(String owner, String field) {
    if (!ready(field)) return "";
    List<ClassData> chain = hierarchy(owner);
    if (chain.isEmpty()) return "";
    try {
      List<String> found = new ArrayList<>();
      for (ClassData c : chain) {
        for (MethodData m : c.getMethods()) {
          if (!m.isMethod() || m.getParamCount() != 0) continue;
          if (m.getName().equals("hashCode") || m.getName().equals("toString")) continue;
          List<String> read = reads(m, owner);
          if (read.size() == 1 && read.get(0).equals(field)) found.add(m.getName());
        }
      }
      return oneName(found);
    } catch (Throwable t) {
      return none(t);
    }
  }

  // An added property shifts the index without failing, so the field's type is checked too
  String ctorField(String owner, int index, String type) {
    String field = ctorField(owner, index);
    if (!ready(field, type)) return "";
    String actual = fieldType(owner, field);
    return actual.equals(type) ? field : none(owner + "." + field + " is " + actual);
  }

  String ctorField(String owner, int index, ClassMatcher type) {
    String field = ctorField(owner, index);
    if (!ready(field)) return "";
    String matched = classAmong(Collections.singletonList(fieldType(owner, field)), type);
    return matched.isEmpty() ? "" : field;
  }

  // Kotlin and Thrift assign fields in declaration order; the primary constructor assigns most
  private String ctorField(String owner, int index) {
    ClassData c = classData(owner);
    if (c == null) return "";
    try {
      List<String> primary = null;
      boolean tie = false;
      for (MethodData m : c.getMethods()) {
        if (!m.isConstructor()) continue;
        List<String> written = writes(m, owner);
        if (primary == null || written.size() > primary.size()) {
          primary = written;
          tie = false;
        } else if (written.size() == primary.size() && !written.equals(primary)) {
          tie = true;
        }
      }
      if (primary == null || tie) return none("no single primary constructor");
      return index < primary.size() ? primary.get(index) : none("constructor writes too few");
    } catch (Throwable t) {
      return none(t);
    }
  }

  // toString lists the constructor properties in order; R8's outliner moves the first label last
  String propertyField(String owner, String label) {
    List<MethodData> found = declaredMethods(owner, sig("java.lang.String").name("toString"));
    if (found.size() != 1) return none("no toString in " + owner);
    try {
      List<String> labels = new ArrayList<>();
      int first = -1;
      for (String s : found.get(0).getUsingStrings()) {
        Matcher m = LABEL.matcher(s);
        if (!m.matches()) continue;
        if (m.group(1) != null && first < 0) first = labels.size();
        labels.add(m.group(2));
      }
      if (first > 0) labels.add(0, labels.remove(first));
      int index = labels.indexOf(label);
      if (index < 0) return none("no property " + label + " in " + owner);
      String field = ctorField(owner, index);
      if (!reads(found.get(0), owner).contains(field)) return none(field + " is not " + label);
      return field;
    } catch (Throwable t) {
      return none(t);
    }
  }

  // The scheme writes each field right after the static TField that names it
  String thriftField(String struct, String name) {
    MethodData init = initializer(struct);
    if (init == null) return "";
    try {
      String descriptorType = "";
      for (MethodData callee : init.getInvokes()) {
        if (callee.isConstructor()
            && callee
                .getParamTypeNames()
                .equals(Arrays.asList("java.lang.String", "byte", "short"))) {
          descriptorType = callee.getClassName();
        }
      }
      List<FieldData> descriptors = new ArrayList<>();
      for (UsingFieldData u : init.getUsingFields()) {
        FieldData fd = u.getField();
        if (u.getUsingType().isWrite()
            && fd.getClassName().equals(struct)
            && fd.getTypeName().equals(descriptorType)) {
          descriptors.add(fd);
        }
      }
      List<String> names = init.getUsingStrings();
      int index = names.indexOf(name);
      if (index < 0 || descriptors.size() != names.size()) {
        return none("no Thrift field " + name + " in " + struct);
      }
      String descriptor = descriptors.get(index).getName();
      Set<String> written = new LinkedHashSet<>();
      for (MethodData writer : descriptors.get(index).getReaders()) {
        boolean next = false;
        for (UsingFieldData u : writer.getUsingFields()) {
          FieldData fd = u.getField();
          if (!fd.getClassName().equals(struct)) continue;
          if (fd.getName().equals(descriptor)) {
            next = true;
          } else if (next && !fd.getTypeName().equals(descriptorType)) {
            written.add(fd.getName());
            break;
          }
        }
      }
      return oneName(new ArrayList<>(written));
    } catch (Throwable t) {
      return none(t);
    }
  }

  // Kotlin's default-argument constructor reads the owner's static defaults in parameter order
  String ctorRead(String owner, int index) {
    ClassData c = classData(owner);
    if (c == null) return "";
    try {
      MethodData widest = null;
      for (MethodData m : c.getMethods()) {
        if (m.isConstructor() && (widest == null || m.getParamCount() > widest.getParamCount())) {
          widest = m;
        }
      }
      if (widest == null) return none("no constructor");
      List<String> read = reads(widest, owner);
      return index < read.size() ? read.get(index) : none("constructor reads too few");
    } catch (Throwable t) {
      return none(t);
    }
  }

  List<String> ctorParams(String owner, int count) {
    ClassData c = classData(owner);
    if (c == null) return new ArrayList<>();
    try {
      List<MethodData> found = new ArrayList<>();
      for (MethodData m : c.getMethods()) {
        if (m.isConstructor() && m.getParamCount() == count) found.add(m);
      }
      return params(one(found));
    } catch (Throwable t) {
      error(t);
      return new ArrayList<>();
    }
  }

  MethodData initializer(String owner) {
    ClassData c = classData(owner);
    if (c == null) return null;
    for (MethodData m : c.getMethods()) if (m.isStaticInitializer()) return m;
    return fail("no static initializer in " + owner);
  }

  private static boolean sigMatches(MethodData m, String ret, String[] params) {
    if (ret != null && !ret.equals(m.getReturnTypeName())) return false;
    List<String> types = m.getParamTypeNames();
    if (types.size() != params.length) return false;
    for (int i = 0; i < params.length; i++) {
      if (params[i] != null && !params[i].equals(types.get(i))) return false;
    }
    return true;
  }

  private MethodData methodWhere(String owner, Predicate<MethodData> test) {
    try {
      for (ClassData c : hierarchy(owner)) {
        List<MethodData> found = new ArrayList<>();
        for (MethodData m : c.getMethods()) if (m.isMethod() && test.test(m)) found.add(m);
        if (!found.isEmpty()) return one(found);
      }
      return ready(owner) ? fail("no match") : null;
    } catch (Throwable t) {
      return error(t);
    }
  }

  private FieldData fieldWhere(String owner, Predicate<FieldData> test) {
    try {
      for (ClassData c : hierarchy(owner)) {
        List<FieldData> found = new ArrayList<>();
        for (FieldData fd : c.getFields()) if (test.test(fd)) found.add(fd);
        if (!found.isEmpty()) return one(found);
      }
      return ready(owner) ? fail("no match") : null;
    } catch (Throwable t) {
      return error(t);
    }
  }

  private List<ClassData> hierarchy(String owner) {
    List<ClassData> chain = new ArrayList<>();
    for (ClassData c = classData(owner); c != null; c = c.getSuperClass()) {
      if (c.getName().startsWith("java.") || c.getName().startsWith("android.")) break;
      chain.add(c);
    }
    return chain;
  }

  MethodData methodDataAnywhere(MethodMatcher matcher) {
    if (planning) return null;
    try {
      return one(bridge.findMethod(FindMethod.create().matcher(matcher)));
    } catch (Throwable t) {
      return error(t);
    }
  }

  MethodData methodDataIn(String scope, MethodMatcher matcher) {
    if (!planning && !scope.isEmpty()) {
      try {
        List<MethodData> found = new ArrayList<>();
        for (MethodData m :
            bridge.findMethod(FindMethod.create().searchPackages(scope).matcher(matcher))) {
          if (packageOf(m.getClassName()).equals(scope)) found.add(m);
        }
        if (found.size() == 1) return found.get(0);
      } catch (Throwable t) {
        error(t);
      }
    }
    return methodDataAnywhere(matcher);
  }

  MethodData creatorIn(String scope, String cls) {
    if (!ready(cls)) return null;
    return methodDataIn(
        scope,
        MethodMatcher.create().addInvoke(MethodMatcher.create().declaredClass(cls).name("<init>")));
  }

  // Implementations of one interface method count as one
  String methodNameAnywhere(MethodMatcher matcher) {
    if (planning) return "";
    try {
      Set<String> names = new LinkedHashSet<>();
      for (MethodData m : bridge.findMethod(FindMethod.create().matcher(matcher))) {
        names.add(m.getName());
      }
      return oneName(new ArrayList<>(names));
    } catch (Throwable t) {
      return none(t);
    }
  }

  static boolean ready(String... names) {
    for (String name : names) if (name == null || name.isEmpty()) return false;
    return true;
  }

  static String outer(String name) {
    int i = name.lastIndexOf('$');
    return i < 0 ? "" : name.substring(0, i);
  }

  static String packageOf(String name) {
    int i = name.lastIndexOf('.');
    return i < 0 ? "" : name.substring(0, i);
  }

  static String at(List<String> names, int index) {
    return index >= 0 && index < names.size() ? names.get(index) : "";
  }

  static String after(List<String> names, String name) {
    int i = names.indexOf(name);
    return i < 0 ? "" : at(names, i + 1);
  }

  static String declaring(MethodData m) {
    return m == null ? "" : m.getClassName();
  }

  static String name(MethodData m) {
    return m == null ? "" : m.getName();
  }

  static List<String> params(MethodData m) {
    return m == null ? new ArrayList<>() : m.getParamTypeNames();
  }

  static String param(MethodData m, int index) {
    return at(params(m), index);
  }

  static List<MethodData> invokes(MethodData m) {
    return m == null ? new ArrayList<>() : m.getInvokes();
  }

  static List<String> constructs(MethodData m) {
    Set<String> types = new LinkedHashSet<>();
    for (MethodData callee : invokes(m)) {
      if (callee.isConstructor()) types.add(callee.getClassName());
    }
    return new ArrayList<>(types);
  }

  static List<String> calls(MethodData m, String owner) {
    Set<String> names = new LinkedHashSet<>();
    for (MethodData callee : invokes(m)) {
      if (callee.getClassName().equals(owner)) names.add(callee.getName());
    }
    return new ArrayList<>(names);
  }

  static List<String> writes(MethodData m, String owner) {
    return fieldsUsed(m, owner, true);
  }

  static List<String> reads(MethodData m, String owner) {
    return fieldsUsed(m, owner, false);
  }

  private static List<String> fieldsUsed(MethodData m, String owner, boolean write) {
    Set<String> names = new LinkedHashSet<>();
    if (m == null) return new ArrayList<>();
    for (UsingFieldData u : m.getUsingFields()) {
      boolean wanted = write ? u.getUsingType().isWrite() : u.getUsingType().isRead();
      if (wanted && u.getField().getClassName().equals(owner)) names.add(u.getField().getName());
    }
    return new ArrayList<>(names);
  }

  static String typeArgument(FieldData field) {
    if (field == null) return "";
    StringBuilder signature = new StringBuilder();
    for (AnnotationData a : field.getAnnotations()) {
      for (AnnotationElementData e : a.getElements()) {
        if (e.getValue().getType() != AnnotationEncodeValueType.ArrayValue) continue;
        for (AnnotationEncodeValue v : e.getValue().arrayValue().getValues()) {
          if (v.getType() == AnnotationEncodeValueType.StringValue) {
            signature.append(v.stringValue());
          }
        }
      }
    }
    int open = signature.indexOf("<L");
    int close = open < 0 ? -1 : signature.indexOf(";", open);
    return close < 0 ? "" : signature.substring(open + 2, close).replace('/', '.');
  }

  int id(String type, String name) {
    if (!ready(name)) return 0;
    int id = res.getIdentifier(name, type, pkg);
    if (id == 0) fail("no resource " + type + "/" + name);
    return id;
  }

  private <T> T one(List<T> found) {
    if (found.size() == 1) return found.get(0);
    if (found.isEmpty()) return fail("no match");
    StringBuilder names = new StringBuilder();
    for (int i = 0; i < Math.min(found.size(), 4); i++) {
      if (i > 0) names.append(", ");
      names.append(found.get(i));
    }
    return fail(found.size() + " matches: " + names + (found.size() > 4 ? ", ..." : ""));
  }

  private String oneName(List<String> names) {
    String found = one(names);
    return found == null ? "" : found;
  }

  private <T> T error(Throwable t) {
    errors++;
    return fail(t.toString());
  }

  private String none(Throwable t) {
    error(t);
    return "";
  }

  private String none(String reason) {
    fail(reason);
    return "";
  }

  private <T> T fail(String reason) {
    if (planning) return null;
    Knot.log("Knot: mapping gave up at " + caller() + ": " + reason);
    return null;
  }

  private static String caller() {
    for (StackTraceElement e : new Throwable().getStackTrace()) {
      if (!e.getClassName().equals(Finder.class.getName())) {
        return e.getFileName() + ":" + e.getLineNumber();
      }
    }
    return "?";
  }
}
