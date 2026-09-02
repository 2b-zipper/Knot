package app.zipper.knot.ui.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.Main;
import app.zipper.knot.R;
import app.zipper.knot.Reflect;
import app.zipper.knot.SettingsStore;
import app.zipper.knot.hooks.FcmFixHook;
import app.zipper.knot.hooks.SettingsUIInjector;
import app.zipper.knot.utils.ContributorProfiles;
import app.zipper.knot.utils.LineTheme;
import app.zipper.knot.utils.ModuleResources;
import java.util.ArrayList;
import java.util.List;

public final class KnotSettingsDialog {

  private static final long OPEN_ANIM_MS = 300;

  private static volatile KnotSettingsDialog active = null;

  private final Activity host;
  private final Dialog dialog;
  private final List<Runnable> stateRefreshers = new ArrayList<>();

  private View navHeader;
  private FrameLayout pageContainer;
  private FrameLayout itemHost;
  private SettingsSearchBar searchBar;
  private View searchPage;
  private View aboutView;
  private KnotConfig.Category currentCategory;
  private boolean aboutActive;
  private boolean restartPending;

  public static void show(Context ctx) {
    KnotSettingsDialog current = active;
    if (current != null && current.dialog.isShowing()) return;
    Activity host = SettingsViews.activityOf(ctx);
    if (host == null) return;
    try {
      ContributorProfiles.prefetch(host, AboutPage.contributorHandles());
      LineTheme.invalidate();
      SettingsStore.init(host);
      SettingsStore.load(Main.options);
      FcmFixHook.migrateStoredMode(Main.options);
      new KnotSettingsDialog(host).open();
    } catch (Throwable e) {
      Knot.log("Knot: Dialog display failed: " + e.getMessage());
    }
  }

  public static void onActivityResumed(Activity activity) {
    KnotSettingsDialog current = active;
    if (current != null && current.host != activity) current.dismissNow();
  }

  public static void onActivityDestroyed(Activity activity) {
    KnotSettingsDialog current = active;
    if (current != null && current.host == activity) current.dismissNow();
  }

  static void notifyConfigChanged() {
    KnotSettingsDialog current = active;
    if (current != null) current.onConfigChanged();
  }

  private KnotSettingsDialog(Activity host) {
    this.host = host;
    this.dialog =
        new Dialog(host, android.R.style.Theme_DeviceDefault_NoActionBar) {
          @Override
          public void onBackPressed() {
            if (aboutActive) {
              closeAbout(host);
            } else if (currentCategory != null) {
              backToTopLevel(host);
            } else {
              close();
            }
          }
        };
  }

  private void open() {
    active = this;
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

    View content = buildContent();
    dialog.setContentView(content);
    applyWindowDecoration();

    content.setTranslationX(host.getResources().getDisplayMetrics().widthPixels);
    dialog.show();
    SettingsViews.slide(content, 0, OPEN_ANIM_MS, null);
  }

  private void applyWindowDecoration() {
    Window win = dialog.getWindow();
    if (win == null) return;

    win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    win.setDimAmount(0);
    win.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
    win.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    win.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    win.setStatusBarColor(Color.TRANSPARENT);

    int visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
    if (!LineTheme.isDark(host)) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        visibility |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        visibility |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
      }
    }
    win.getDecorView().setSystemUiVisibility(visibility);
    win.getDecorView().setPadding(0, 0, 0, 0);
    win.getDecorView().requestApplyInsets();
  }

  void close() {
    if (!dialog.isShowing()) return;

    if (restartPending) {
      promptRestart();
      return;
    }

    View topHeader = dialog.findViewById(LineVersion.get().res.idHeader);
    if (topHeader == null) {
      dismissNow();
      return;
    }
    View rootPane = topHeader.getRootView();
    SettingsViews.slide(rootPane, rootPane.getWidth(), this::dismissNow);
  }

  private void promptRestart() {
    Context ctx = dialogContext();
    LineTheme.applyDialogColors(
        new AlertDialog.Builder(ctx, LineTheme.dialogTheme(ctx))
            .setTitle(ModuleResources.get(R.string.restart_title))
            .setMessage(ModuleResources.get(R.string.restart_message))
            .setPositiveButton(ModuleResources.get(R.string.restart_ok), (d, w) -> System.exit(0))
            .setNegativeButton(
                ModuleResources.get(R.string.restart_later),
                (d, w) -> {
                  restartPending = false;
                  close();
                })
            .show(),
        ctx);
  }

  private void dismissNow() {
    try {
      if (dialog.isShowing()) dialog.dismiss();
    } catch (Throwable ignored) {
    }
    stateRefreshers.clear();
    if (active == this) active = null;
  }

  Context dialogContext() {
    return dialog.getContext();
  }

  void addStateRefresher(Runnable refresher) {
    stateRefreshers.add(refresher);
  }

  void onItemToggled() {
    for (Runnable r : stateRefreshers) r.run();
    restartPending = true;
    searchPage = null;
  }

  void onConfigChanged() {
    restartPending = true;
    reloadItems();
  }

  // Knot's own views are rebuilt on demand, so a language switch needs no LINE restart
  void onLanguageChanged() {
    reloadItems();
    SettingsUIInjector.refreshInjectedRow();
  }

  private void refreshNavHeader() {
    if (navHeader == null) return;
    Context ctx = dialogContext();
    if (aboutActive) {
      setNavHeader(ctx, ModuleResources.get(R.string.opt_about_label), v -> closeAbout(ctx));
      return;
    }
    setNavHeader(
        ctx,
        currentCategory == null
            ? ModuleResources.get(R.string.settings_title)
            : currentCategory.label(),
        backListener(ctx));
  }

  private void reloadItems() {
    if (itemHost == null || searchBar == null) return;
    host.runOnUiThread(
        () -> {
          searchPage = null;
          searchBar.refreshHint();
          refreshNavHeader();
          String query = searchBar.query();
          View page = buildPage(query);
          itemHost.removeAllViews();
          itemHost.addView(page);
          SettingsPage.filter(page, query);
        });
  }

  private View buildContent() {
    try {
      LineVersion.Config cfg = LineVersion.get();
      ViewGroup container =
          (ViewGroup) LayoutInflater.from(host).inflate(cfg.res.layoutSettingsMain, null);
      container.setClickable(true);
      container.setFocusable(true);
      container.setPadding(0, 0, 0, 0);
      stateRefreshers.clear();

      removeComposeHeader(container);

      navHeader = container.findViewById(cfg.res.idHeader);
      if (navHeader != null) setupNavHeader(cfg);

      View itemListView = container.findViewById(cfg.res.idSettingList);
      if (itemListView != null) installBody(container, itemListView);

      return container;
    } catch (Throwable t) {
      TextView errorLabel = new TextView(host);
      errorLabel.setText("Error: " + t.getMessage());
      return errorLabel;
    }
  }

  private void removeComposeHeader(ViewGroup container) {
    try {
      int composeHeaderId =
          host.getResources().getIdentifier("compose_header", "id", "jp.naver.line.android");
      if (composeHeaderId == 0) return;
      View composeHeader = container.findViewById(composeHeaderId);
      if (composeHeader != null && composeHeader.getParent() instanceof ViewGroup) {
        ((ViewGroup) composeHeader.getParent()).removeView(composeHeader);
      }
    } catch (Throwable ignored) {
    }
  }

  private void setupNavHeader(LineVersion.Config cfg) {
    fitNavHeaderToWindow();
    try {
      Reflect.callMethod(navHeader, cfg.main.methodHeaderSetButtonVisibility, true);
    } catch (Throwable ignored) {
    }
    setNavHeader(host, ModuleResources.get(R.string.settings_title), v -> close());
  }

  private void fitNavHeaderToWindow() {
    Reflect.callMethod(
        navHeader, LineVersion.get().main.methodRefreshNavHeader, dialog.getWindow());
  }

  private void setNavHeader(Context ctx, String title, View.OnClickListener back) {
    LineVersion.Config cfg = LineVersion.get();
    Reflect.callMethod(navHeader, cfg.main.methodHeaderSetTitle, title);
    Reflect.callMethod(navHeader, cfg.main.methodHeaderSetButtonListener, back);
    navHeader.setBackgroundColor(LineTheme.backgroundColor(ctx));
    LineTheme.tintTextAndIcons(navHeader, LineTheme.primaryTextColor(ctx));
  }

  private View.OnClickListener backListener(Context ctx) {
    return v -> {
      if (currentCategory != null) {
        backToTopLevel(ctx);
      } else {
        close();
      }
    };
  }

  private void backToTopLevel(Context ctx) {
    switchPage(ctx, null);
  }

  private void installBody(ViewGroup container, View itemListView) {
    ViewGroup listParent = (ViewGroup) itemListView.getParent();
    int listIndex = listParent.indexOfChild(itemListView);
    ViewGroup.LayoutParams listLp = itemListView.getLayoutParams();
    listParent.removeView(itemListView);

    itemHost = new FrameLayout(host);
    itemHost.addView(SettingsPage.build(host, this, null));
    searchBar = new SettingsSearchBar(host, this::applySearchQuery);

    LinearLayout settingsRoot = new LinearLayout(host);
    settingsRoot.setOrientation(LinearLayout.VERTICAL);
    settingsRoot.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
    settingsRoot.addView(searchBar.view());
    settingsRoot.addView(itemHost, new LinearLayout.LayoutParams(-1, -1));

    pageContainer = new FrameLayout(host);
    pageContainer.addView(settingsRoot);
    listParent.addView(pageContainer, listIndex, listLp);

    container.setBackgroundColor(LineTheme.backgroundColor(host));
  }

  private void applySearchQuery(String query) {
    if (itemHost == null) return;

    if (query.isEmpty()) {
      if (itemHost.getChildAt(0) == searchPage) {
        showPage(SettingsPage.build(host, this, currentCategory));
      } else {
        SettingsPage.filter(itemHost.getChildAt(0), "");
      }
      return;
    }

    if (currentCategory != null) {
      SettingsPage.filter(itemHost.getChildAt(0), query);
      return;
    }

    if (searchPage == null) searchPage = SettingsPage.buildAllCategories(host, this);
    if (itemHost.getChildAt(0) != searchPage) showPage(searchPage);
    SettingsPage.filter(searchPage, query);
  }

  private View buildPage(String query) {
    return query.isEmpty() || currentCategory != null
        ? SettingsPage.build(host, this, currentCategory)
        : SettingsPage.buildAllCategories(host, this);
  }

  private void showPage(View page) {
    itemHost.removeAllViews();
    itemHost.addView(page);
  }

  void openCategory(Context ctx, KnotConfig.Category category) {
    switchPage(ctx, category);
  }

  private void switchPage(Context ctx, KnotConfig.Category category) {
    if (itemHost == null || navHeader == null) return;

    boolean forward = (category != null && currentCategory == null);
    currentCategory = category;

    final View oldView = itemHost.getChildAt(0);
    View newView = SettingsPage.build(ctx, this, category);

    float width = itemHost.getWidth();
    newView.setTranslationX(forward ? width : -width);
    itemHost.addView(newView);

    SettingsViews.slide(oldView, forward ? -width : width);
    SettingsViews.slide(newView, 0, () -> itemHost.removeView(oldView));

    fitNavHeaderToWindow();
    setNavHeader(
        ctx,
        category == null ? ModuleResources.get(R.string.settings_title) : category.label(),
        backListener(ctx));
  }

  void openAbout(Context ctx) {
    if (pageContainer == null || itemHost == null || navHeader == null) return;
    final View settingsPage = (View) itemHost.getParent();
    if (settingsPage == null) return;
    aboutActive = true;

    final View about = AboutPage.build(ctx);
    float width = pageContainer.getWidth();
    about.setTranslationX(width);
    pageContainer.addView(about, new FrameLayout.LayoutParams(-1, -1));
    aboutView = about;

    SettingsViews.slide(settingsPage, -width);
    SettingsViews.slide(about, 0, () -> settingsPage.setVisibility(View.GONE));

    fitNavHeaderToWindow();
    setNavHeader(ctx, ModuleResources.get(R.string.opt_about_label), v -> closeAbout(ctx));
  }

  private void closeAbout(Context ctx) {
    if (!aboutActive || pageContainer == null || itemHost == null) return;
    aboutActive = false;

    final View settingsPage = (View) itemHost.getParent();
    final View about = aboutView;
    aboutView = null;

    float width = pageContainer.getWidth();
    if (settingsPage != null) {
      settingsPage.setVisibility(View.VISIBLE);
      settingsPage.setTranslationX(-width);
      SettingsViews.slide(settingsPage, 0);
    }
    if (about != null) {
      SettingsViews.slide(about, width, () -> pageContainer.removeView(about));
    }

    fitNavHeaderToWindow();
    setNavHeader(
        ctx,
        currentCategory == null
            ? ModuleResources.get(R.string.settings_title)
            : currentCategory.label(),
        backListener(ctx));
  }
}
