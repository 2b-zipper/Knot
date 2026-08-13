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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RemoveHomeContents implements BaseHook {

  private static int recId = 0;
  private static int svcCarouselId = 0;
  private static int svcTitleId = 0;
  private static int noServicesId = 0;
  private static boolean isSetupDone = false;
  private static Object emptySectionInstance = null;

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

    if (cfg == null
        || cfg.home.lypRecommendationControllerClass.isEmpty()
        || cfg.home.lypRecommendationModuleArgClass.isEmpty()
        || cfg.home.lypRecommendationContextClass.isEmpty()
        || cfg.compose.composerClass.isEmpty()) return;

    Knot.module
        .hook(
            Reflect.findMethodExact(
                cfg.home.lypRecommendationControllerClass,
                lpparam.classLoader,
                "a",
                String.class,
                cfg.home.lypRecommendationModuleArgClass,
                cfg.home.lypRecommendationContextClass,
                cfg.compose.composerClass))
        .intercept(
            chain -> {
              if (!SettingsStore.get(
                  config.removeHomeAccordion.key, config.removeHomeAccordion.enabled)) {
                return chain.proceed();
              }

              Object module = chain.getArg(1);
              if (module == null
                  || !module.getClass().getName().equals(cfg.home.lypRecommendationModuleClass)) {
                return chain.proceed();
              }

              return getEmptySectionInstance(lpparam.classLoader);
            });

    hookHome26ModuleFiltering(config, lpparam);
  }

  private static void hookHome26ModuleFiltering(KnotConfig config, LoadParam lpparam) {
    LineVersion.Config cfg = LineVersion.get();
    if (cfg == null || cfg.home.home26LoadingMoreDataClass.isEmpty()) return;
    try {
      Class<?> dataCls =
          Reflect.findClass(cfg.home.home26LoadingMoreDataClass, lpparam.classLoader);
      Constructor<?> ctor =
          Reflect.findConstructorExact(
              dataCls,
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
              Boolean.TYPE);
      Knot.module
          .hook(ctor)
          .intercept(
              chain -> {
                Object[] args = chain.getArgs().toArray();
                if (args.length != 11) return chain.proceed();

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

                if (feedOff && args.length > 5 && Boolean.TRUE.equals(args[5])) {
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

  private static Object getEmptySectionInstance(ClassLoader classLoader) {
    if (emptySectionInstance != null) return emptySectionInstance;
    LineVersion.Config c = LineVersion.get();
    String sectionClassName =
        (c != null && !c.home.lypRecommendationSectionClass.isEmpty())
            ? c.home.lypRecommendationSectionClass
            : "l02.e";
    Class<?> sectionClass = Reflect.findClass(sectionClassName, classLoader);
    emptySectionInstance = Reflect.getStaticObjectField(sectionClass, "e");
    return emptySectionInstance;
  }
}
