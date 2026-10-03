package app.zipper.knot.hooks;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Reflect;
import app.zipper.knot.SettingsStore;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RemoveHomeContents implements BaseHook {

  private static int recId = 0;
  private static int svcCarouselId = 0;
  private static int svcTitleId = 0;
  private static int noServicesId = 0;
  private static boolean isSetupDone = false;

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    LineVersion.Config cfg = LineVersion.get();

    Knot.module
        .hook(Reflect.findMethodExact(cfg.main.mainActivity, lpparam.classLoader, "onResume"))
        .intercept(
            chain -> {
              if (!isSetupDone) {
                android.app.Activity host = (android.app.Activity) chain.getThisObject();
                String pkg = cfg.linePkg;
                recId = host.getResources().getIdentifier(cfg.home.resRecommendation, "id", pkg);
                svcCarouselId =
                    host.getResources().getIdentifier(cfg.home.resServiceCarouselId, "id", pkg);
                svcTitleId =
                    host.getResources().getIdentifier(cfg.home.resServiceTitleId, "id", pkg);
                noServicesId =
                    host.getResources().getIdentifier(cfg.home.resNoServicesId, "id", pkg);
                isSetupDone = true;
              }
              return chain.proceed();
            });

    Knot.module
        .hook(Reflect.findMethodExact(View.class, "onAttachedToWindow"))
        .intercept(
            chain -> {
              View target = (View) chain.getThisObject();
              int id = target.getId();
              if (id == View.NO_ID) return chain.proceed();

              if (id == recId && recId != 0) {
                if (SettingsStore.get(
                    config.removeHomeRecommendations.key,
                    config.removeHomeRecommendations.enabled)) {
                  hideView(target);
                }
                return chain.proceed();
              }

              if (id == svcCarouselId && svcCarouselId != 0) {
                if (SettingsStore.get(
                    config.removeHomeServices.key, config.removeHomeServices.enabled)) {
                  hideView(target);
                }
                return chain.proceed();
              }

              if ((id == svcTitleId && svcTitleId != 0)
                  || (id == noServicesId && noServicesId != 0)) {
                if (SettingsStore.get(
                    config.removeHomeServices.key, config.removeHomeServices.enabled)) {
                  ViewParent parent = target.getParent();
                  if (parent instanceof View) hideView((View) parent);
                }
              }
              return chain.proceed();
            });

    hookHome26ModuleFiltering(config, lpparam);
  }

  private static void hookHome26ModuleFiltering(KnotConfig config, LoadParam lpparam) {
    LineVersion.Config cfg = LineVersion.get();
    if (cfg == null || cfg.home.home26LoadingMoreDataClass.isEmpty()) return;
    try {
      Class<?> dataCls =
          Reflect.findClass(cfg.home.home26LoadingMoreDataClass, lpparam.classLoader);
      Knot.module
          .hook(findPageDataConstructor(dataCls))
          .intercept(
              chain -> {
                Object[] args = chain.getArgs().toArray();

                boolean feedOff =
                    SettingsStore.get(
                        config.removeHomeRecommendations.key,
                        config.removeHomeRecommendations.enabled);
                boolean svcOff =
                    SettingsStore.get(
                        config.removeHomeServices.key, config.removeHomeServices.enabled);
                if (!feedOff && !svcOff) return chain.proceed();

                List<?> modules = args[0] instanceof List ? (List<?>) args[0] : null;
                if (modules != null && !modules.isEmpty()) {
                  Set<String> feedPrefixes = prefixSet(cfg.home.home26FeedTypePrefixes);
                  Set<String> svcPrefixes = prefixSet(cfg.home.home26ServiceTypePrefixes);
                  List<Object> filtered = new ArrayList<>();
                  boolean changed = false;
                  for (Object w : modules) {
                    Object body =
                        w != null
                            ? Reflect.getObjectField(w, cfg.home.home26ModuleBodyField)
                            : null;
                    String type = body != null ? (String) Reflect.callMethod(body, "getType") : "";
                    boolean remove =
                        (feedOff && hasAnyPrefix(type, feedPrefixes))
                            || (svcOff && hasAnyPrefix(type, svcPrefixes));
                    if (remove) {
                      changed = true;
                    } else {
                      filtered.add(w);
                    }
                  }
                  if (changed) args[0] = filtered;
                }

                if (feedOff && Boolean.TRUE.equals(args[5])) {
                  args[5] = Boolean.FALSE;
                }
                return chain.proceed(args);
              });
      Knot.log(
          "Knot: RemoveHomeContents HOME26 module filtering hooked: "
              + cfg.home.home26LoadingMoreDataClass);
    } catch (Throwable t) {
      Knot.log("Knot: RemoveHomeContents HOME26 module filtering hook failed: " + t);
    }
  }

  // 26.15.0 appended a parameter, so only the leading ones are matched.
  private static Constructor<?> findPageDataConstructor(Class<?> dataCls)
      throws NoSuchMethodException {
    Class<?>[] leading = {
      List.class,
      Boolean.TYPE,
      Boolean.TYPE,
      Boolean.TYPE,
      Boolean.TYPE,
      Boolean.TYPE,
      String.class,
      Long.class,
      Long.class,
      Integer.TYPE,
      Boolean.TYPE
    };
    Constructor<?> match = null;
    for (Constructor<?> ctor : dataCls.getDeclaredConstructors()) {
      Class<?>[] params = ctor.getParameterTypes();
      if (params.length < leading.length
          || !Arrays.equals(Arrays.copyOf(params, leading.length), leading)) continue;
      if (match == null || params.length < match.getParameterCount()) match = ctor;
    }
    if (match == null) throw new NoSuchMethodException(dataCls.getName() + ".<init>");
    match.setAccessible(true);
    return match;
  }

  private static Set<String> prefixSet(String csv) {
    Set<String> out = new HashSet<>();
    if (csv != null) {
      for (String s : csv.split(",")) {
        s = s.trim();
        if (!s.isEmpty()) out.add(s);
      }
    }
    return out;
  }

  private static boolean hasAnyPrefix(String value, Set<String> prefixes) {
    if (value == null) return false;
    for (String p : prefixes) {
      if (value.startsWith(p)) return true;
    }
    return false;
  }

  private static void hideView(View target) {
    target.setVisibility(View.GONE);
    ViewGroup.LayoutParams params = target.getLayoutParams();
    if (params != null && params.height != 0) {
      params.height = 0;
      target.setLayoutParams(params);
    }
  }
}
