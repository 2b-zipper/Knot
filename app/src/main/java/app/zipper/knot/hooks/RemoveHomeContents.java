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
