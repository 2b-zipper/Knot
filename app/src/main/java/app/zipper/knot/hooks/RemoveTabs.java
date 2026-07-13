package app.zipper.knot.hooks;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Reflect;
import app.zipper.knot.SettingsStore;

public class RemoveTabs implements BaseHook {

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    LineVersion.Config cfg = LineVersion.get();

    Knot.module
        .hook(Reflect.findMethodExact(cfg.main.mainActivity, lpparam.classLoader, "onResume"))
        .intercept(
            chain -> {
              Object result = chain.proceed();
              Activity host = (Activity) chain.getThisObject();
              LineVersion.Config c = LineVersion.get();

              if (SettingsStore.get(config.removeTabVoom.key, config.removeTabVoom.enabled))
                deactivateTab(host, c.tabs.resVoom);
              if (SettingsStore.get(config.removeTabNews.key, config.removeTabNews.enabled)) {
                deactivateTab(host, c.tabs.resNews);
                deactivateTab(host, c.tabs.resCall);
              }
              if (SettingsStore.get(config.removeTabMini.key, config.removeTabMini.enabled))
                deactivateTab(host, c.tabs.resMini);
              if (SettingsStore.get(
                  config.removeTabCommerce.key, config.removeTabCommerce.enabled)) {
                deactivateTab(host, c.tabs.resCommerce);
                deactivateTab(host, c.tabs.resCommerceTw);
              }
              if (SettingsStore.get(config.removeTabWallet.key, config.removeTabWallet.enabled))
                deactivateTab(host, c.tabs.resWallet);
              if (SettingsStore.get(
                  config.extendTabClickArea.key, config.extendTabClickArea.enabled))
                expandInteractionArea(host);
              if (SettingsStore.get(config.hideTabText.key, config.hideTabText.enabled))
                applyCompactLayout(host);
              return result;
            });

    try {
      Class<?> bnbLabelCls =
          Reflect.findClass(cfg.tabs.bottomNavigationBarTextViewClass, lpparam.classLoader);
      Knot.hookAllCtors(
          bnbLabelCls,
          chain -> {
            Object result = chain.proceed();
            if (SettingsStore.get(config.hideTabText.key, config.hideTabText.enabled)) {
              ((View) chain.getThisObject()).setVisibility(View.INVISIBLE);
            }
            return result;
          });
    } catch (Throwable ignored) {
    }
  }

  private static void expandInteractionArea(Activity host) {
    LineVersion.Config c = LineVersion.get();
    int rootId = host.getResources().getIdentifier(c.tabs.resContainer, "id", c.linePkg);
    if (rootId == 0) return;
    ViewGroup root = host.findViewById(rootId);
    if (root == null) return;
    for (int i = 2; i < root.getChildCount(); i += 2) {
      View child = root.getChildAt(i);
      if (!(child instanceof ViewGroup) || child.getVisibility() == View.GONE) continue;
      ViewGroup tab = (ViewGroup) child;
      ViewGroup.LayoutParams lp = tab.getLayoutParams();
      lp.width = 0;
      tab.setLayoutParams(lp);
      View clickable = tab.getChildAt(tab.getChildCount() - 1);
      if (clickable != null) {
        ViewGroup.LayoutParams clp = clickable.getLayoutParams();
        clp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        clickable.setLayoutParams(clp);
      }
    }
  }

  private static void deactivateTab(Activity host, String resName) {
    LineVersion.Config c = LineVersion.get();
    int id = host.getResources().getIdentifier(resName, "id", c.linePkg);
    if (id != 0) {
      View tab = host.findViewById(id);
      if (tab != null) tab.setVisibility(View.GONE);
    }
    int spacerId = host.getResources().getIdentifier(resName + "_spacer", "id", c.linePkg);
    if (spacerId != 0) {
      View spacer = host.findViewById(spacerId);
      if (spacer != null) spacer.setVisibility(View.GONE);
    }
  }

  private static void applyCompactLayout(Activity host) {
    LineVersion.Config c = LineVersion.get();
    int rootId = host.getResources().getIdentifier(c.tabs.resContainer, "id", c.linePkg);
    int textId = host.getResources().getIdentifier(c.tabs.resBtnText, "id", c.linePkg);
    int clickableId =
        host.getResources().getIdentifier("bnb_button_clickable_area", "id", c.linePkg);
    if (rootId == 0) return;
    if (textId == 0) return;
    ViewGroup root = host.findViewById(rootId);
    if (root == null) return;
    for (int i = 0; i < root.getChildCount(); i++) {
      applyLabelOffset(root.getChildAt(i), textId, clickableId);
    }
    root.invalidate();
  }

  private static void applyLabelOffset(View view, int textId, int clickableId) {
    if (!(view instanceof ViewGroup)) return;
    ViewGroup container = (ViewGroup) view;
    View label = findDirectChildById(container, textId);
    if (label != null) {
      label.setVisibility(View.INVISIBLE);
      float offsetY = resolveLabelOffset(label);
      for (int i = 0; i < container.getChildCount(); i++) {
        View child = container.getChildAt(i);
        if (child.getId() == clickableId) {
          child.setTranslationY(0f);
          continue;
        }
        if (child.getId() == textId) {
          child.setTranslationY(0f);
          continue;
        }
        child.setTranslationY(offsetY);
      }
    }

    for (int i = 0; i < container.getChildCount(); i++) {
      applyLabelOffset(container.getChildAt(i), textId, clickableId);
    }
  }

  private static View findDirectChildById(ViewGroup container, int id) {
    for (int i = 0; i < container.getChildCount(); i++) {
      View child = container.getChildAt(i);
      if (child.getId() == id) return child;
    }
    return null;
  }

  private static float resolveLabelOffset(View label) {
    int height = label.getHeight();
    if (height <= 0) height = label.getMeasuredHeight();
    if (height <= 0 && label instanceof TextView) {
      height = ((TextView) label).getLineHeight();
    }
    return Math.max(height, 0);
  }
}
