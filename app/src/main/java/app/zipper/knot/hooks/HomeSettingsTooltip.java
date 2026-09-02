package app.zipper.knot.hooks;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.R;
import app.zipper.knot.Reflect;
import app.zipper.knot.SettingsStore;
import app.zipper.knot.utils.ModuleResources;
import java.lang.ref.WeakReference;

public class HomeSettingsTooltip implements BaseHook {

  private static final String SHOWN_KEY = "knot_settings_tooltip_shown";
  private static volatile PopupWindow activePopup = null;
  private static volatile WeakReference<Object> visibleTabFragment = null;
  private static volatile int[] composeIconRect = null;
  private static volatile boolean finished = false;

  // finished only short-circuits once shown; isConfigured() still re-reads the pointer file on
  // every call until a folder is picked
  private static boolean pending() {
    if (finished) return false;
    if (SettingsStore.get(SHOWN_KEY, false)) {
      finished = true;
      return false;
    }
    return SettingsStore.isConfigured();
  }

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    LineVersion.Config cfg = LineVersion.get();
    if (cfg == null) return;

    hookTabVisibility(cfg, lpparam);
    hookHomeSearchBar(cfg, lpparam);
  }

  private void hookHomeSearchBar(LineVersion.Config cfg, LoadParam lpparam) {
    if (cfg.res.resSettingsHeaderBtn.isEmpty()
        || cfg.searchBarAgentI.homeSearchBarClass.isEmpty()
        || cfg.searchBarAgentI.homeRefreshMethod.isEmpty()) return;

    Knot.module
        .hook(
            Reflect.findMethodExact(
                cfg.searchBarAgentI.homeSearchBarClass,
                lpparam.classLoader,
                cfg.searchBarAgentI.homeRefreshMethod))
        .intercept(
            chain -> {
              Object result = chain.proceed();
              if (isHomeSearchBar(cfg, chain.getThisObject())) {
                View rootView =
                    (View)
                        Reflect.getObjectField(
                            chain.getThisObject(), cfg.searchBarAgentI.homeRootViewField);
                if (rootView != null) {
                  onHomeTabEntered(rootView, cfg);
                }
              } else {
                dismissSilently();
              }
              return result;
            });
  }

  private void hookTabVisibility(LineVersion.Config cfg, LoadParam lpparam) {
    if (cfg.main.baseMainTabFragment.isEmpty()) return;
    try {
      Class<?> tabFragment = Reflect.findClass(cfg.main.baseMainTabFragment, lpparam.classLoader);
      Knot.module
          .hook(Reflect.findMethodExact(tabFragment, "setUserVisibleHint", boolean.class))
          .intercept(
              chain -> {
                Object result = chain.proceed();
                Object fragment = chain.getThisObject();
                if ((boolean) chain.getArg(0)) {
                  visibleTabFragment = new WeakReference<>(fragment);
                } else {
                  WeakReference<Object> ref = visibleTabFragment;
                  if (ref != null && ref.get() == fragment) {
                    visibleTabFragment = null;
                    dismissSilently();
                  }
                }
                return result;
              });

      // The header lays out once; without this a skipped tooltip never gets a second chance
      Knot.module
          .hook(Reflect.findMethodExact(tabFragment, "onResume"))
          .intercept(
              chain -> {
                Object result = chain.proceed();
                retryPending(cfg);
                return result;
              });
    } catch (Throwable t) {
      Knot.log("Knot: HomeSettingsTooltip could not track tab visibility: " + t);
    }
  }

  private static void retryPending(LineVersion.Config cfg) {
    Activity host = SettingsUIInjector.getForegroundActivity();
    if (host == null || !pending()) return;

    int[] rect = composeIconRect;
    if (rect != null) {
      showForComposeIcon(host, rect[0], rect[1], rect[2], rect[3]);
      return;
    }
    View btn = findViewByEntryName(host.getWindow().getDecorView(), cfg.res.resSettingsHeaderBtn);
    if (btn != null && btn.isShown() && btn.getWidth() > 0) showTooltip(host, btn, btn);
  }

  private static boolean isHomeSearchBar(LineVersion.Config cfg, Object instance) {
    if (cfg.searchBarAgentI.homeTabTypeField.isEmpty()) return false;
    Object tabType = Reflect.getObjectField(instance, cfg.searchBarAgentI.homeTabTypeField);
    if (!(tabType instanceof Enum<?>)) return false;
    String name = ((Enum<?>) tabType).name();
    return name.equals(cfg.searchBarAgentI.homeTabName)
        || name.equals(cfg.searchBarAgentI.homeTabV2Name);
  }

  private static void onHomeTabEntered(View homeView, LineVersion.Config cfg) {
    if (finished) return;

    android.graphics.Rect rect = new android.graphics.Rect();
    if (homeView.getGlobalVisibleRect(rect) && !rect.isEmpty()) {
      triggerShow(homeView, cfg);
    } else {
      homeView
          .getViewTreeObserver()
          .addOnGlobalLayoutListener(
              new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                  if (finished) {
                    removeListener(homeView, this);
                    return;
                  }
                  android.graphics.Rect r = new android.graphics.Rect();
                  if (homeView.getGlobalVisibleRect(r) && !r.isEmpty()) {
                    triggerShow(homeView, cfg);
                    removeListener(homeView, this);
                  }
                }
              });
    }
  }

  private static void triggerShow(View homeView, LineVersion.Config cfg) {
    if (!homeView.isShown() || !pending()) return;

    Activity host = findActivity(homeView.getContext());
    if (host == null) return;

    host.runOnUiThread(
        () -> {
          if (activePopup != null && activePopup.isShowing()) return;
          View btn =
              findViewByEntryName(host.getWindow().getDecorView(), cfg.res.resSettingsHeaderBtn);
          if (btn == null) return;

          if (btn.getWidth() > 0) {
            showTooltip(host, btn, homeView);
          } else {
            btn.post(
                () -> {
                  if (btn.getWidth() > 0) showTooltip(host, btn, homeView);
                });
          }
        });
  }

  public static void markShown() {
    finished = true;
    dismissSilently();
    SettingsStore.save(SHOWN_KEY, true);
  }

  private static void dismissSilently() {
    PopupWindow p = activePopup;
    activePopup = null;
    if (p != null) {
      try {
        p.dismiss();
      } catch (Throwable ignored) {
      }
    }
  }

  private static void removeListener(View v, ViewTreeObserver.OnGlobalLayoutListener l) {
    try {
      v.getViewTreeObserver().removeOnGlobalLayoutListener(l);
    } catch (Throwable ignored) {
    }
  }

  private static View findViewByEntryName(View root, String entryName) {
    if (root == null) return null;
    int id = root.getId();
    if (id != View.NO_ID) {
      try {
        if (entryName.equals(root.getResources().getResourceEntryName(id))) return root;
      } catch (Throwable ignored) {
      }
    }
    if (!(root instanceof ViewGroup)) return null;
    ViewGroup group = (ViewGroup) root;
    for (int i = 0; i < group.getChildCount(); i++) {
      View found = findViewByEntryName(group.getChildAt(i), entryName);
      if (found != null) return found;
    }
    return null;
  }

  private static Activity findActivity(Context ctx) {
    while (ctx instanceof ContextWrapper) {
      if (ctx instanceof Activity) return (Activity) ctx;
      ctx = ((ContextWrapper) ctx).getBaseContext();
    }
    return null;
  }

  // x/y are window coordinates
  public static void showForComposeIcon(Activity host, int x, int y, int width, int height) {
    if (finished) return;
    composeIconRect = new int[] {x, y, width, height};
    if (host == null || !pending()) return;
    if (!isIconOnVisibleTab(host, x, y, width, height)) {
      dismissSilently();
      return;
    }

    // Posted: this runs inside a Compose layout pass and showing a PopupWindow adds a window
    View decor = host.getWindow().getDecorView();
    decor.post(
        () -> {
          if (activePopup != null && activePopup.isShowing()) return;
          LinearLayout bubble = buildBubble(host, width);
          if (bubble == null) return;

          PopupWindow popup = buildPopup(bubble);
          popup.showAtLocation(
              decor, Gravity.TOP | Gravity.LEFT, x + width - bubble.getMeasuredWidth(), y + height);
          activePopup = popup;
        });
  }

  // Neighbouring tabs stay laid out, so the icon reports coordinates while another tab is shown
  private static boolean isIconOnVisibleTab(Activity host, int x, int y, int width, int height) {
    View decor = host.getWindow().getDecorView();
    if (x < 0 || y < 0 || x + width > decor.getWidth() || y + height > decor.getHeight())
      return false;

    WeakReference<Object> ref = visibleTabFragment;
    Object fragment = ref == null ? null : ref.get();
    if (fragment == null) return true;

    View tabView = (View) Reflect.callMethod(fragment, "getView");
    if (tabView == null || !tabView.isShown()) return false;

    int[] location = new int[2];
    tabView.getLocationInWindow(location);
    return x >= location[0]
        && y >= location[1]
        && x + width <= location[0] + tabView.getWidth()
        && y + height <= location[1] + tabView.getHeight();
  }

  private static PopupWindow buildPopup(LinearLayout bubble) {
    PopupWindow popup =
        new PopupWindow(
            bubble, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    popup.setOutsideTouchable(true);
    popup.setFocusable(false);
    popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    bubble.setOnClickListener(v -> markShown());
    return popup;
  }

  private static LinearLayout buildBubble(Activity host, int anchorWidth) {
    try {
      LineVersion.Config cfg = LineVersion.get();
      String pkg = cfg.linePkg;
      float dp = host.getResources().getDisplayMetrics().density;

      ImageView arrow = new ImageView(host);
      int arrowId = host.getResources().getIdentifier(cfg.res.resTooltipArrowUp, "drawable", pkg);
      if (arrowId != 0) arrow.setImageResource(arrowId);
      arrow.measure(
          View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
          View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
      LinearLayout.LayoutParams arrowLp =
          new LinearLayout.LayoutParams(
              LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
      arrowLp.gravity = Gravity.END;
      arrowLp.rightMargin = Math.max(0, anchorWidth / 2 - arrow.getMeasuredWidth() / 2);

      TextView text = new TextView(host);
      text.setText(ModuleResources.get(R.string.tooltip_settings_long_press));
      text.setTextColor(Color.WHITE);
      text.setTextSize(13f);
      int ph = (int) (12 * dp), pv = (int) (7 * dp);
      text.setPadding(ph, pv, ph, pv);
      int bgId = host.getResources().getIdentifier(cfg.res.resTooltipBackground, "drawable", pkg);
      if (bgId != 0) text.setBackgroundResource(bgId);

      LinearLayout container = new LinearLayout(host);
      container.setOrientation(LinearLayout.VERTICAL);
      container.addView(arrow, arrowLp);
      container.addView(
          text,
          new LinearLayout.LayoutParams(
              LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

      container.measure(
          View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
          View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
      return container;
    } catch (Throwable t) {
      Knot.log("Knot: HomeSettingsTooltip error: " + t);
      return null;
    }
  }

  private static void showTooltip(Activity host, View anchor, View homeView) {
    try {
      LinearLayout container = buildBubble(host, anchor.getWidth());
      if (container == null) return;

      PopupWindow popup = buildPopup(container);
      popup.showAsDropDown(anchor, anchor.getWidth() - container.getMeasuredWidth(), 0);
      activePopup = popup;

      homeView
          .getViewTreeObserver()
          .addOnScrollChangedListener(
              new ViewTreeObserver.OnScrollChangedListener() {
                @Override
                public void onScrollChanged() {
                  android.graphics.Rect r = new android.graphics.Rect();
                  if (!homeView.getGlobalVisibleRect(r) || r.isEmpty()) {
                    dismissSilently();
                    try {
                      homeView.getViewTreeObserver().removeOnScrollChangedListener(this);
                    } catch (Throwable ignored) {
                    }
                  }
                }
              });

    } catch (Throwable t) {
      Knot.log("Knot: HomeSettingsTooltip error: " + t);
    }
  }
}
