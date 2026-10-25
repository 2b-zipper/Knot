package app.zipper.knot.mapping;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import org.luckypray.dexkit.query.enums.MatchType;
import org.luckypray.dexkit.query.enums.UsingType;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.FieldsMatcher;
import org.luckypray.dexkit.query.matchers.InterfacesMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.query.matchers.MethodsMatcher;
import org.luckypray.dexkit.query.matchers.ParameterMatcher;
import org.luckypray.dexkit.query.matchers.ParametersMatcher;
import org.luckypray.dexkit.query.matchers.base.AccessFlagsMatcher;
import org.luckypray.dexkit.query.matchers.base.IntRange;
import org.luckypray.dexkit.query.matchers.base.StringMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.FieldData;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.UsingFieldData;

// What a DexKit query (~10 ms each on a phone) would match; Unsupported otherwise
final class LocalMatch {

  static final class Unsupported extends RuntimeException {
    Unsupported() {
      super(null, null, false, false);
    }
  }

  private static final Unsupported UNSUPPORTED = new Unsupported();

  private final Function<String, ClassData> classOf;
  private final Map<String, List<String>> strings = new HashMap<>();
  private final Map<String, Set<String>> classStrings = new HashMap<>();
  private final Map<String, List<UsingFieldData>> fields = new HashMap<>();
  private final Map<String, List<MethodData>> invokes = new HashMap<>();

  LocalMatch(Function<String, ClassData> classOf) {
    this.classOf = classOf;
  }

  boolean cls(ClassMatcher matcher, ClassData c) {
    if (matcher == null) return true;
    if (matcher.getSourceMatcher() != null || matcher.getAnnotationsMatcher() != null) {
      throw UNSUPPORTED;
    }
    if (!string(matcher.getClassNameMatcher(), c.getName())) return false;
    if (!flags(matcher.getModifiersMatcher(), c.getModifiers())) return false;
    if (!flags(matcher.getAccessFlagsMatcher(), c.getAccessFlags())) return false;
    if (matcher.getSuperClassMatcher() != null) {
      ClassData sup = c.getSuperClass();
      if (sup == null) throw UNSUPPORTED;
      if (!cls(matcher.getSuperClassMatcher(), sup)) return false;
    }
    InterfacesMatcher ifaces = matcher.getInterfacesMatcher();
    if (ifaces != null
        && !contains(
            ifaces.getMatchType(),
            ifaces.getRangeMatcher(),
            ifaces.getInterfacesMatcher(),
            c.getInterfaces(),
            this::cls)) {
      return false;
    }
    FieldsMatcher fs = matcher.getFieldsMatcher();
    if (fs != null
        && !contains(
            fs.getMatchType(),
            fs.getRangeMatcher(),
            fs.getFieldsMatcher(),
            c.getFields(),
            this::field)) {
      return false;
    }
    MethodsMatcher ms = matcher.getMethodsMatcher();
    if (ms != null
        && !contains(
            ms.getMatchType(),
            ms.getRangeMatcher(),
            ms.getMethodsMatcher(),
            c.getMethods(),
            this::method)) {
      return false;
    }
    if (!logic(
        matcher.getAllOfMatchers(),
        matcher.getAnyOfMatchers(),
        matcher.getNoneOfMatchers(),
        inner -> cls(inner, c))) {
      return false;
    }
    return empty(matcher.getUsingStringsMatcher())
        || containsAll(matcher.getUsingStringsMatcher(), classStrings(c), LocalMatch::string);
  }

  boolean method(MethodMatcher matcher, MethodData m) {
    if (matcher.getAnnotationsMatcher() != null
        || matcher.getOpCodesMatcher() != null
        || matcher.getCallerMethodsMatcher() != null
        || matcher.getProtoShortyMatcher() != null
        || !empty(matcher.getUsingNumbersMatcher())) {
      throw UNSUPPORTED;
    }
    // Code is read only for methods that pass the rest
    if (!string(matcher.getNameMatcher(), m.getName())) return false;
    if (!flags(matcher.getModifiersMatcher(), m.getModifiers())) return false;
    if (!flags(matcher.getAccessFlagsMatcher(), m.getAccessFlags())) return false;
    if (!type(matcher.getClassMatcher(), m.getClassName())) return false;
    if (!type(matcher.getReturnTypeMatcher(), m.getReturnTypeName())) return false;
    if (!params(matcher.getParamsMatcher(), m.getParamTypeNames())) return false;
    if (!logic(
        matcher.getAllOfMatchers(),
        matcher.getAnyOfMatchers(),
        matcher.getNoneOfMatchers(),
        inner -> method(inner, m))) {
      return false;
    }
    if (!empty(matcher.getUsingStringsMatcher())
        && !containsAll(matcher.getUsingStringsMatcher(), strings(m), LocalMatch::string)) {
      return false;
    }
    if (!empty(matcher.getUsingFieldsMatcher())
        && !containsAll(
            matcher.getUsingFieldsMatcher(),
            fields.computeIfAbsent(m.getDescriptor(), k -> m.getUsingFields()),
            (f, u) -> using(f.getUsingType(), u) && field(f.getMatcher(), u.getField()))) {
      return false;
    }
    MethodsMatcher called = matcher.getInvokeMethodsMatcher();
    if (called == null) return true;
    if (called.getMatchType() != MatchType.Contains || called.getRangeMatcher() != null) {
      throw UNSUPPORTED;
    }
    return containsAll(
        called.getMethodsMatcher(),
        invokes.computeIfAbsent(m.getDescriptor(), k -> m.getInvokes()),
        this::method);
  }

  boolean field(FieldMatcher matcher, FieldData f) {
    if (matcher == null) return true;
    if (matcher.getAnnotationsMatcher() != null
        || matcher.getGetMethodsMatcher() != null
        || matcher.getPutMethodsMatcher() != null) {
      throw UNSUPPORTED;
    }
    return string(matcher.getNameMatcher(), f.getName())
        && flags(matcher.getModifiersMatcher(), f.getModifiers())
        && flags(matcher.getAccessFlagsMatcher(), f.getAccessFlags())
        && type(matcher.getClassMatcher(), f.getClassName())
        && type(matcher.getTypeMatcher(), f.getTypeName())
        && logic(
            matcher.getAllOfMatchers(),
            matcher.getAnyOfMatchers(),
            matcher.getNoneOfMatchers(),
            inner -> field(inner, f));
  }

  private List<String> strings(MethodData m) {
    return strings.computeIfAbsent(m.getDescriptor(), k -> m.getUsingStrings());
  }

  private Set<String> classStrings(ClassData c) {
    return classStrings.computeIfAbsent(
        c.getName(),
        k -> {
          Set<String> all = new LinkedHashSet<>();
          for (MethodData m : c.getMethods()) all.addAll(strings(m));
          return all;
        });
  }

  private boolean type(ClassMatcher matcher, String name) {
    if (matcher == null) return true;
    if (onlyName(matcher)) return string(matcher.getClassNameMatcher(), name);
    ClassData c = classOf.apply(name);
    if (c == null) throw UNSUPPORTED;
    return cls(matcher, c);
  }

  private static boolean onlyName(ClassMatcher m) {
    return m.getClassNameMatcher() != null
        && m.getSourceMatcher() == null
        && m.getModifiersMatcher() == null
        && m.getAccessFlagsMatcher() == null
        && m.getSuperClassMatcher() == null
        && m.getInterfacesMatcher() == null
        && m.getAnnotationsMatcher() == null
        && m.getFieldsMatcher() == null
        && m.getMethodsMatcher() == null
        && empty(m.getUsingStringsMatcher())
        && empty(m.getAllOfMatchers())
        && empty(m.getAnyOfMatchers())
        && empty(m.getNoneOfMatchers());
  }

  private boolean params(ParametersMatcher matcher, List<String> types) {
    if (matcher == null) return true;
    List<ParameterMatcher> each = matcher.getParamsMatcher();
    IntRange range = matcher.getRangeMatcher();
    // DexKit holds a method to both the count and the listed types
    if (!count(range, types.size())) return false;
    if (each == null) return true;
    if (each.size() != types.size()) return false;
    for (int i = 0; i < each.size(); i++) {
      ParameterMatcher p = each.get(i);
      if (p == null) continue;
      if (p.getAnnotationsMatcher() != null) throw UNSUPPORTED;
      if (!type(p.getTypeMatcher(), types.get(i))) return false;
    }
    return true;
  }

  private static boolean using(UsingType type, UsingFieldData u) {
    if (type == null || type == UsingType.Any) return true;
    return type == UsingType.Read ? u.getUsingType().isRead() : u.getUsingType().isWrite();
  }

  private static boolean flags(AccessFlagsMatcher matcher, int actual) {
    if (matcher == null) return true;
    int wanted = matcher.getModifiers();
    return matcher.getMatchType() == MatchType.Equals
        ? actual == wanted
        : (actual & wanted) == wanted;
  }

  private static boolean string(StringMatcher matcher, String actual) {
    if (matcher == null) return true;
    if (actual == null) return false;
    String wanted = matcher.getValue();
    if (matcher.getIgnoreCase()) {
      wanted = wanted.toLowerCase();
      actual = actual.toLowerCase();
    }
    switch (matcher.getMatchType()) {
      case Equals:
        return actual.equals(wanted);
      case Contains:
        return actual.contains(wanted);
      case StartsWith:
        return actual.startsWith(wanted);
      case EndsWith:
        return actual.endsWith(wanted);
      default:
        throw UNSUPPORTED;
    }
  }

  private static <M, T> boolean contains(
      MatchType type, IntRange range, List<M> matchers, List<T> items, BiPredicate<M, T> test) {
    if (type != MatchType.Contains) throw UNSUPPORTED;
    return count(range, items.size()) && containsAll(matchers, items, test);
  }

  private static <M, T> boolean containsAll(
      List<M> matchers, Iterable<T> items, BiPredicate<M, T> test) {
    for (M matcher : each(matchers)) {
      boolean found = false;
      for (T item : items) {
        if (test.test(matcher, item)) {
          found = true;
          break;
        }
      }
      if (!found) return false;
    }
    return true;
  }

  private static <M> boolean logic(
      List<M> allOf, List<M> anyOf, List<M> noneOf, Predicate<M> test) {
    for (M m : each(allOf)) if (!test.test(m)) return false;
    if (!empty(anyOf)) {
      boolean any = false;
      for (M m : anyOf) any |= test.test(m);
      if (!any) return false;
    }
    for (M m : each(noneOf)) if (test.test(m)) return false;
    return true;
  }

  private static boolean count(IntRange range, int n) {
    return range == null || (n >= range.getMin() && n <= range.getMax());
  }

  // A matcher given only a count leaves its list null
  private static <T> List<T> each(List<T> list) {
    return list == null ? Collections.emptyList() : list;
  }

  private static boolean empty(List<?> list) {
    return list == null || list.isEmpty();
  }
}
