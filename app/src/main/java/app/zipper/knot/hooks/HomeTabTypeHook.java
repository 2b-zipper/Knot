package app.zipper.knot.hooks;

import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Reflect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeTabTypeHook implements BaseHook {

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    LineVersion.Config cfg = LineVersion.get();
    if (cfg == null
        || cfg.homeTab.tabListProviderClass.isEmpty()
        || cfg.homeTab.methodBuildTabList.isEmpty()
        || cfg.homeTab.mainTabEnumClass.isEmpty()) {
      Knot.log("Knot: HomeTabType: home tab mapping incomplete for current LINE version");
      return;
    }

    Class<?> enumCls = Reflect.findClass(cfg.homeTab.mainTabEnumClass, lpparam.classLoader);
    final List<String> homeTypes = homeTypeNames(enumCls);
    final String desired = config.homeTabType.value;
    if (!homeTypes.contains(desired)) return;
    final Object target = resolveConstant(enumCls, desired);
    if (target == null) return;

    Knot.module
        .hook(
            Reflect.findMethodExact(
                cfg.homeTab.tabListProviderClass,
                lpparam.classLoader,
                cfg.homeTab.methodBuildTabList))
        .intercept(
            chain -> {
              Object result = chain.proceed();
              if (!(result instanceof List)) return result;
              List<?> list = (List<?>) result;
              if (list.isEmpty()) return result;
              String current = list.get(0) instanceof Enum ? ((Enum<?>) list.get(0)).name() : null;
              if (current == null || !homeTypes.contains(current) || current.equals(desired)) {
                return result;
              }
              List<Object> copy = new ArrayList<>(list);
              copy.set(0, target);
              return copy;
            });
    Knot.log("Knot: HomeTabType forcing " + desired);
  }

  public static List<String> availableHomeTypes(ClassLoader cl) {
    LineVersion.Config cfg = LineVersion.get();
    if (cfg == null || cfg.homeTab.mainTabEnumClass.isEmpty()) return Collections.emptyList();
    try {
      return homeTypeNames(Reflect.findClass(cfg.homeTab.mainTabEnumClass, cl));
    } catch (Throwable t) {
      return Collections.emptyList();
    }
  }

  private static List<String> homeTypeNames(Class<?> enumCls) {
    List<String> out = new ArrayList<>();
    Object[] constants = enumCls.getEnumConstants();
    if (constants != null) {
      for (Object c : constants) {
        String name = ((Enum<?>) c).name();
        if (name.contains("HOME")) out.add(name);
      }
    }
    return out;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Object resolveConstant(Class<?> enumCls, String name) {
    try {
      return Enum.valueOf((Class) enumCls, name);
    } catch (Throwable t) {
      return null;
    }
  }
}
