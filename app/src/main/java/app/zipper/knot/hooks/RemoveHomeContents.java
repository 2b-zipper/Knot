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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RemoveHomeContents implements BaseHook {

  private static int recId = 0;
  private static int svcCarouselId = 0;
  private static int svcTitleId = 0;
  private static int noServicesId = 0;
  private static boolean isSetupDone = false;
  private static Object emptySectionInstance = null;
  private static final Set<Class<?>> hookedControllerClasses = new HashSet<>();
  private static Set<String> feedModuleNames = null;
  private static Set<String> serviceModuleNames = null;

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

    hookHome26Modules(config, lpparam);
    hookLoadingMoreSuppression(config, lpparam);
  }

  private static void hookLoadingMoreSuppression(KnotConfig config, LoadParam lpparam) {
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
                if (!SettingsStore.get(
                    config.removeHomeRecommendations.key,
                    config.removeHomeRecommendations.enabled)) {
                  return chain.proceed();
                }
                Object[] args = chain.getArgs().toArray();
                if (args.length > 5 && Boolean.TRUE.equals(args[5])) {
                  args[5] = Boolean.FALSE;
                  return chain.proceed(args);
                }
                return chain.proceed();
              });
      Knot.log(
          "Knot: RemoveHomeContents LOADING_MORE suppression hooked: "
              + cfg.home.home26LoadingMoreDataClass);
    } catch (Throwable t) {
      Knot.log("Knot: RemoveHomeContents LOADING_MORE suppression hook failed: " + t);
    }
  }

  private static void hookHome26Modules(KnotConfig config, LoadParam lpparam) {
    LineVersion.Config cfg = LineVersion.get();
    if (cfg == null
        || cfg.home.home26RegistryClass.isEmpty()
        || cfg.home.home26RegistryControllersField.isEmpty()
        || cfg.home.lypRecommendationModuleArgClass.isEmpty()
        || cfg.home.lypRecommendationContextClass.isEmpty()
        || cfg.compose.composerClass.isEmpty()) return;

    feedModuleNames = moduleNameSet(cfg.home.home26FeedModules);
    serviceModuleNames = moduleNameSet(cfg.home.home26ServiceModules);

    Object[] paramTypes = {
      String.class,
      cfg.home.lypRecommendationModuleArgClass,
      cfg.home.lypRecommendationContextClass,
      cfg.compose.composerClass
    };

    try {
      Class<?> registryCls = Reflect.findClass(cfg.home.home26RegistryClass, lpparam.classLoader);
      Object mapObj =
          Reflect.getStaticObjectField(registryCls, cfg.home.home26RegistryControllersField);
      if (mapObj instanceof Map) {
        for (Object controller : ((Map<?, ?>) mapObj).values()) {
          if (controller != null) {
            hookModuleController(config, lpparam, controller.getClass(), paramTypes);
          }
        }
      }
    } catch (Throwable t) {
      Knot.log("Knot: RemoveHomeContents HOME26 registry hook failed: " + t);
    }

    if (!cfg.home.home26RendererAdapterClass.isEmpty()) {
      try {
        Class<?> adapterCls =
            Reflect.findClass(cfg.home.home26RendererAdapterClass, lpparam.classLoader);
        hookModuleController(config, lpparam, adapterCls, paramTypes);
      } catch (Throwable t) {
        Knot.log("Knot: RemoveHomeContents HOME26 adapter hook failed: " + t);
      }
    }
  }

  private static void hookModuleController(
      KnotConfig config, LoadParam lpparam, Class<?> cls, Object[] paramTypes) {
    if (!hookedControllerClasses.add(cls)) return;
    try {
      Knot.module
          .hook(Reflect.findMethodExact(cls, "a", paramTypes))
          .intercept(
              chain -> {
                boolean feedOff =
                    SettingsStore.get(
                        config.removeHomeRecommendations.key,
                        config.removeHomeRecommendations.enabled);
                boolean svcOff =
                    SettingsStore.get(
                        config.removeHomeServices.key, config.removeHomeServices.enabled);
                if (!feedOff && !svcOff) return chain.proceed();

                Object module = chain.getArg(1);
                if (module == null) return chain.proceed();
                String name = module.getClass().getName();
                if ((feedOff && feedModuleNames.contains(name))
                    || (svcOff && serviceModuleNames.contains(name))) {
                  return getEmptySectionInstance(lpparam.classLoader);
                }
                return chain.proceed();
              });
    } catch (Throwable t) {
      Knot.log("Knot: RemoveHomeContents failed to hook " + cls.getName() + ": " + t);
    }
  }

  private static Set<String> moduleNameSet(String csv) {
    Set<String> out = new HashSet<>();
    if (csv != null) {
      for (String s : csv.split(",")) {
        s = s.trim();
        if (!s.isEmpty()) out.add(s);
      }
    }
    return out;
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
