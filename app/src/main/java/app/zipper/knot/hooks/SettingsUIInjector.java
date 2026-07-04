package app.zipper.knot.hooks;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import app.zipper.knot.BuildConfig;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.Reflect;
import app.zipper.knot.SettingsStore;
import app.zipper.knot.utils.ContributorProfiles;
import app.zipper.knot.utils.LineTheme;
import app.zipper.knot.utils.ModuleStrings;
import java.io.File;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SettingsUIInjector implements BaseHook {

  public static volatile Runnable openSettingsAction = null;
  private static volatile SettingsUIInjector instance = null;
  private static volatile Object cachedToggle = null;
  private static volatile Object cachedSuccess = null;

  private static final String BRAND_TAG = "Knot";
  private static final String[][] CONTRIBUTOR_SECTIONS = {
    {ModuleStrings.ABOUT_SEC_DEVELOPERS, "2b-zipper", "Nich87"},
    {ModuleStrings.ABOUT_SEC_CONTRIBUTORS, "atuy1219"},
  };

  public static void openSettings(android.app.Activity activity) {
    SettingsUIInjector ui = instance;
    if (ui != null) ui.displaySettingsDialog(activity);
  }

  private volatile Runnable onSettingsReloadRequest = null;
  private static final int PICK_DIRECTORY_CODE = 0x4C58;
  private static final int PICK_FONT_CODE = 0x4C59;
  private static final int PICK_RESTORE_DB_CODE = 0x4C5A;

  private volatile Object targetAdapter = null;
  private volatile Object targetFragment = null;

  private volatile Dialog settingsDialog = null;
  private volatile Activity dialogHost = null;
  private volatile boolean pendingRestart = false;
  private volatile KnotConfig.Category currentActiveCategory = null;
  private volatile boolean aboutPageActive = false;
  private volatile FrameLayout cachedPageContainer = null;
  private volatile View aboutPageView = null;
  private volatile FrameLayout cachedItemHost = null;
  private volatile View cachedNavHeader = null;
  private volatile View cachedSearchView = null;
  private static final java.util.List<Runnable> uiUpdateCallbacks = new java.util.ArrayList<>();

  private static final KnotConfig.Category[] DISPLAY_CATEGORIES = {
    KnotConfig.Category.PRIVACY,
    KnotConfig.Category.CHAT,
    KnotConfig.Category.DISPLAY,
    KnotConfig.Category.NOTIFICATION,
    KnotConfig.Category.SYSTEM
  };

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    instance = this;
    LineVersion.Config cfg = LineVersion.get();

    Class<?> fragmentClass =
        Reflect.findClass(cfg.settings.mainSettingsFragmentClass, lpparam.classLoader);
    Knot.module
        .hook(Reflect.findMethodExact(fragmentClass, "onViewCreated", View.class, Bundle.class))
        .intercept(
            chain -> {
              Object result = chain.proceed();
              try {
                LineVersion.Config c = LineVersion.get();
                targetFragment = chain.getThisObject();
                View listView = ((View) chain.getArg(0)).findViewById(c.res.idSettingList);
                if (listView != null) targetAdapter = Reflect.callMethod(listView, "getAdapter");
                openSettingsAction =
                    () ->
                        displaySettingsDialog(
                            (Context) Reflect.callMethod(targetFragment, "requireContext"));
              } catch (Throwable ignored) {
              }
              return result;
            });

    final Class<?> proxyInterface =
        Reflect.findClass(cfg.settings.settingsItemClass, lpparam.classLoader);
    final Class<?> settingsSearchHelperClass =
        Reflect.findClass(cfg.settings.settingsSearchHelperClass, lpparam.classLoader);
    Knot.module
        .hook(
            Reflect.findMethodExact(
                cfg.settings.settingsAdapterClass,
                lpparam.classLoader,
                cfg.settings.methodSetItems,
                Collection.class))
        .intercept(
            chain -> {
              LineVersion.Config c = LineVersion.get();
              Collection<?> sourceItems = (Collection<?>) chain.getArg(0);
              if (chain.getThisObject() != targetAdapter
                  && !settingsSearchHelperClass.isInstance(chain.getThisObject())) {
                return chain.proceed();
              }
              if (containsKnotItem(sourceItems, c)) return chain.proceed();

              List<Object> items = new ArrayList<>(sourceItems);
              int insertPos = items.size();
              findPosition:
              for (int i = 0; i < items.size(); i++) {
                try {
                  Object model = Reflect.getObjectField(items.get(i), c.settings.fieldItemModel);
                  if (model == null) continue;
                  for (java.lang.reflect.Field f : model.getClass().getDeclaredFields()) {
                    if (f.getType() == int.class) {
                      f.setAccessible(true);
                      if (f.getInt(model) == c.res.idPersonalInfo) {
                        insertPos = i;
                        break findPosition;
                      }
                    }
                  }
                } catch (Throwable ignored) {
                }
              }
              Object section =
                  createAdapterItemProxy(proxyInterface, lpparam.classLoader, c.res.typeSection);
              Object row =
                  createAdapterItemProxy(proxyInterface, lpparam.classLoader, c.res.typeRow);

              if (c.settings.settingsAdapterWrapperClass != null
                  && !c.settings.settingsAdapterWrapperClass.isEmpty()) {
                try {
                  Class<?> wrapperCls =
                      Reflect.findClass(
                          c.settings.settingsAdapterWrapperClass, lpparam.classLoader);
                  Class<?> headerCls =
                      Reflect.findClass(c.settings.settingsHeaderItemClass, lpparam.classLoader);
                  Class<?> itemCls =
                      Reflect.findClass(c.settings.settingsRowItemClass, lpparam.classLoader);

                  Class<?> unsafeCls = Reflect.findClass("sun.misc.Unsafe", (ClassLoader) null);
                  Object unsafe = Reflect.getStaticObjectField(unsafeCls, "theUnsafe");

                  Object dummyHeader = Reflect.callMethod(unsafe, "allocateInstance", headerCls);
                  Object dummyRow = Reflect.callMethod(unsafe, "allocateInstance", itemCls);

                  Reflect.setIntField(dummyHeader, cfg.settings.fieldLayoutId, c.res.typeSection);
                  Reflect.setIntField(dummyRow, cfg.settings.fieldLayoutId, c.res.typeRow);

                  section = Reflect.newInstance(wrapperCls, dummyHeader);
                  row = Reflect.newInstance(wrapperCls, dummyRow);

                  Reflect.setObjectField(dummyHeader, cfg.settings.fieldModelTag, BRAND_TAG);
                  Reflect.setObjectField(dummyRow, cfg.settings.fieldModelTag, BRAND_TAG);

                  Reflect.setBooleanField(dummyHeader, cfg.settings.fieldIsVisible, true);

                  Class<?> bc =
                      Reflect.findClass(c.settings.settingsHandlerBaseClass, lpparam.classLoader);
                  Object dummyHandler =
                      Reflect.getStaticObjectField(bc, cfg.settings.fieldDefaultHandler);

                  String[] handlerFields = {
                    cfg.settings.fieldActionHandler,
                    cfg.settings.fieldIconProvider,
                    cfg.settings.fieldDescriptionProvider,
                    cfg.settings.fieldSubActionHandler,
                    cfg.settings.fieldVisibilityFilter
                  };
                  for (String f : handlerFields) {
                    try {
                      Reflect.setObjectField(dummyRow, f, dummyHandler);
                      Reflect.setObjectField(dummyHeader, f, dummyHandler);
                    } catch (Throwable ignored) {
                    }
                  }

                  Reflect.setObjectField(
                      dummyRow,
                      cfg.settings.fieldVisibilityFilter,
                      Reflect.getStaticObjectField(bc, cfg.settings.fieldCommonHandler));
                  Reflect.setObjectField(
                      dummyHeader,
                      cfg.settings.fieldVisibilityFilter,
                      Reflect.getStaticObjectField(bc, cfg.settings.fieldCommonHandler));
                } catch (Throwable e) {
                  Knot.log("Knot: Adapter wrapper failed: " + e);
                }
              }

              items.add(insertPos, section);
              items.add(insertPos + 1, row);
              return chain.proceed(new Object[] {items});
            });

    Class<?> itemBindingClass =
        Reflect.findClass(cfg.settings.settingsBaseAdapterClass, lpparam.classLoader);
    Knot.module
        .hook(
            Reflect.findMethodExact(
                cfg.settings.settingsSearchHelperClass,
                lpparam.classLoader,
                cfg.settings.methodBindViewHolder,
                itemBindingClass,
                int.class))
        .intercept(
            chain -> {
              if (chain.getThisObject() != targetAdapter
                  && !settingsSearchHelperClass.isInstance(chain.getThisObject())) {
                return chain.proceed();
              }
              LineVersion.Config c = LineVersion.get();
              int currentPos = (int) chain.getArg(1);
              boolean ours = false;
              try {
                Object currentItem =
                    Reflect.callMethod(chain.getThisObject(), c.settings.methodGetItem, currentPos);
                if (currentItem == null) return chain.proceed();
                if (currentItem
                    .getClass()
                    .getName()
                    .equals(c.settings.settingsAdapterWrapperClass)) {
                  currentItem = Reflect.getObjectField(currentItem, c.settings.fieldItemModel);
                }
                if (currentItem == null) return chain.proceed();

                String sourceTag =
                    (String) Reflect.getObjectField(currentItem, c.settings.fieldModelTag);
                if (!BRAND_TAG.equals(sourceTag)) return chain.proceed();

                ours = true;

                int entryType = Reflect.getIntField(currentItem, cfg.settings.fieldLayoutId);
                View itemView =
                    (View) Reflect.getObjectField(chain.getArg(0), c.settings.fieldViewHolderView);
                if (entryType == c.res.typeSection) {
                  if (itemView instanceof TextView) ((TextView) itemView).setText(BRAND_TAG);
                } else if (entryType == c.res.typeRow) {
                  applyVisibility(itemView, c.res.idIcon, View.VISIBLE);
                  applyVisibility(itemView, c.res.idDesc, View.GONE);
                  applyVisibility(itemView, c.res.idMark, View.GONE);
                  applyVisibility(itemView, c.res.idSeparator, View.GONE);
                  applyVisibility(itemView, c.res.idNewMark, View.GONE);
                  applyVisibility(itemView, c.res.idNoticeDot, View.GONE);
                  applyVisibility(itemView, c.res.idArrow, View.VISIBLE);

                  android.widget.ImageView iconView = itemView.findViewById(c.res.idIcon);
                  if (iconView != null) {
                    try {
                      Context modCtx =
                          itemView
                              .getContext()
                              .createPackageContext(
                                  "app.zipper.knot", Context.CONTEXT_IGNORE_SECURITY);
                      int resId =
                          modCtx
                              .getResources()
                              .getIdentifier("ic_knot", "drawable", "app.zipper.knot");

                      if (resId != 0) {
                        iconView.setImageDrawable(modCtx.getDrawable(resId));
                        iconView.setVisibility(android.view.View.VISIBLE);

                        float density =
                            itemView.getContext().getResources().getDisplayMetrics().density;
                        int size = (int) (24 * density);
                        android.view.ViewGroup.LayoutParams lp = iconView.getLayoutParams();
                        if (lp != null) {
                          lp.width = size;
                          lp.height = size;
                          iconView.setLayoutParams(lp);
                        }
                        iconView.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
                      }
                    } catch (Throwable ignored) {
                    }
                  }
                  TextView title = itemView.findViewById(c.res.idTitle);
                  if (title != null) title.setText(ModuleStrings.SETTINGS_TITLE);
                  itemView.setOnClickListener(v -> displaySettingsDialog(v.getContext()));
                }
              } catch (Throwable ignored) {
              }
              return ours ? null : chain.proceed();
            });

    Knot.module
        .hook(
            Reflect.findMethodExact(
                android.app.Activity.class, "onActivityResult", int.class, int.class, Intent.class))
        .intercept(
            chain -> {
              int requestCode = (int) chain.getArg(0);
              if (requestCode == PICK_DIRECTORY_CODE) {
                if ((int) chain.getArg(1) != Activity.RESULT_OK || chain.getArg(2) == null)
                  return null;
                Uri treeUri = ((Intent) chain.getArg(2)).getData();
                if (treeUri == null) return null;
                try {
                  ((Activity) chain.getThisObject())
                      .getContentResolver()
                      .takePersistableUriPermission(
                          treeUri,
                          Intent.FLAG_GRANT_READ_URI_PERMISSION
                              | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                } catch (Throwable ignored) {
                }
                SettingsStore.setSettingsDir(treeUri.toString());
                SettingsStore.load(Main.options);
                pendingRestart = true;
                if (onSettingsReloadRequest != null) onSettingsReloadRequest.run();
                return null;
              } else if (requestCode == PICK_FONT_CODE) {
                if ((int) chain.getArg(1) != Activity.RESULT_OK || chain.getArg(2) == null)
                  return null;
                Uri fontUri = ((Intent) chain.getArg(2)).getData();
                if (fontUri == null) return null;

                try {
                  Context ctx = (Context) chain.getThisObject();
                  java.io.InputStream is = ctx.getContentResolver().openInputStream(fontUri);
                  File out = new File(ctx.getFilesDir(), "knot_custom_font.ttf");
                  java.io.FileOutputStream os = new java.io.FileOutputStream(out);
                  byte[] buffer = new byte[8192];
                  int len;
                  while ((len = is.read(buffer)) != -1) os.write(buffer, 0, len);
                  os.close();
                  is.close();

                  String localPath = out.getAbsolutePath();
                  SettingsStore.save("custom_font_path", localPath);
                  for (KnotConfig.Item itm : Main.options.items) {
                    if (itm.key.equals("custom_font_path")) {
                      itm.value = localPath;
                      break;
                    }
                  }
                  pendingRestart = true;
                  if (onSettingsReloadRequest != null) onSettingsReloadRequest.run();
                } catch (Throwable t) {
                  Knot.log("Knot: Failed to copy font file: " + t.getMessage());
                }
                return null;
              } else if (requestCode == PICK_RESTORE_DB_CODE) {
                if ((int) chain.getArg(1) != Activity.RESULT_OK || chain.getArg(2) == null)
                  return null;
                Uri dbUri = ((Intent) chain.getArg(2)).getData();
                if (dbUri == null) return null;

                Context ctx = (Context) chain.getThisObject();
                new Thread(
                        () -> {
                          File tempFile = null;
                          try {
                            tempFile =
                                File.createTempFile("knot_restore_", ".db", ctx.getCacheDir());
                            try (java.io.InputStream is =
                                    ctx.getContentResolver().openInputStream(dbUri);
                                java.io.FileOutputStream os =
                                    new java.io.FileOutputStream(tempFile)) {
                              byte[] buffer = new byte[8192];
                              int len;
                              while ((len = is.read(buffer)) != -1) os.write(buffer, 0, len);
                            }

                            final File finalFile = tempFile;
                            new Handler(Looper.getMainLooper())
                                .post(
                                    () -> {
                                      int themeId = LineTheme.dialogTheme(ctx);
                                      LineTheme.applyDialogColors(
                                          new AlertDialog.Builder(ctx, themeId)
                                              .setTitle(ModuleStrings.RESTORE_CONFIRM_TITLE)
                                              .setMessage(ModuleStrings.RESTORE_CONFIRM_MSG)
                                              .setPositiveButton(
                                                  ModuleStrings.SETTINGS_YES,
                                                  (d, w) -> {
                                                    BackupRestoreHook.runRestore(ctx, finalFile);
                                                  })
                                              .setNegativeButton(
                                                  ModuleStrings.SETTINGS_CANCEL,
                                                  (d, w) -> finalFile.delete())
                                              .show(),
                                          ctx);
                                    });
                          } catch (Throwable t) {
                            Knot.log("Knot: Failed to prepare restore DB: " + t.getMessage());
                            if (tempFile != null) tempFile.delete();
                          }
                        })
                    .start();
                return null;
              }
              return chain.proceed();
            });

    Knot.module
        .hook(Reflect.findMethodExact(android.app.Activity.class, "onDestroy"))
        .intercept(
            chain -> {
              if (chain.getThisObject() == dialogHost) {
                try {
                  Dialog d = settingsDialog;
                  if (d != null && d.isShowing()) d.dismiss();
                } catch (Throwable ignored) {
                }
                settingsDialog = null;
                dialogHost = null;
              }
              return chain.proceed();
            });
  }

  private void displaySettingsDialog(Context ctx) {
    if (settingsDialog != null && settingsDialog.isShowing()) return;
    try {
      Activity host = resolveActivity(ctx);
      if (host == null) return;
      ContributorProfiles.prefetch(host, contributorHandles());
      LineTheme.invalidate();
      cacheUiConstants(host);
      SettingsStore.init(host);
      SettingsStore.load(Main.options);
      pendingRestart = false;
      boolean isDark = LineTheme.isDark(host);

      Dialog dialog =
          new Dialog(host, android.R.style.Theme_DeviceDefault_NoActionBar) {
            @Override
            public void onBackPressed() {
              if (aboutPageActive) {
                closeAboutPage(host);
              } else if (currentActiveCategory != null) {
                switchPage(host, cachedToggle, cachedSuccess, null);
              } else {
                initiateDialogClosure();
              }
            }
          };
      settingsDialog = dialog;
      dialogHost = host;
      dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

      View content = createSettingsView(host, cachedToggle, cachedSuccess, dialog.getWindow());

      dialog.setContentView(content);

      Window win = dialog.getWindow();
      if (win != null) {
        win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        win.setDimAmount(0);
        win.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        win.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        win.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        win.setStatusBarColor(Color.TRANSPARENT);
        int visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        if (!isDark && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
          visibility |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        if (!isDark && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
          visibility |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        win.getDecorView().setSystemUiVisibility(visibility);
        win.getDecorView().setPadding(0, 0, 0, 0);
        win.getDecorView().requestApplyInsets();
      }

      content.setTranslationX(host.getResources().getDisplayMetrics().widthPixels);
      dialog.show();
      content
          .animate()
          .translationX(0)
          .setDuration(300)
          .setInterpolator(new DecelerateInterpolator())
          .start();
    } catch (Throwable e) {
      Knot.log("Knot: Dialog display failed: " + e.getMessage());
    }
  }

  private void initiateDialogClosure() {
    if (settingsDialog == null || !settingsDialog.isShowing()) return;

    if (pendingRestart) {
      int themeId = LineTheme.dialogTheme(settingsDialog.getContext());
      LineTheme.applyDialogColors(
          new AlertDialog.Builder(settingsDialog.getContext(), themeId)
              .setTitle(ModuleStrings.RESTART_TITLE)
              .setMessage(ModuleStrings.RESTART_MESSAGE)
              .setPositiveButton(ModuleStrings.RESTART_OK, (d, w) -> System.exit(0))
              .setNegativeButton(
                  ModuleStrings.RESTART_LATER,
                  (d, w) -> {
                    pendingRestart = false;
                    initiateDialogClosure();
                  })
              .show(),
          settingsDialog.getContext());
      return;
    }

    LineVersion.Config currentCfg = LineVersion.get();
    View topHeader = settingsDialog.findViewById(currentCfg.res.idHeader);
    if (topHeader == null) {
      settingsDialog.dismiss();
      settingsDialog = null;
      dialogHost = null;
      return;
    }
    View rootPane = topHeader.getRootView();
    rootPane
        .animate()
        .translationX(rootPane.getWidth())
        .setDuration(250)
        .setInterpolator(new DecelerateInterpolator())
        .withEndAction(
            () -> {
              settingsDialog.dismiss();
              settingsDialog = null;
              dialogHost = null;
              currentActiveCategory = null;
              aboutPageActive = false;
              cachedPageContainer = null;
              aboutPageView = null;
              cachedItemHost = null;
              cachedNavHeader = null;
              cachedSearchView = null;
              uiUpdateCallbacks.clear();
            })
        .start();
  }

  private View createSettingsView(Activity host, Object toggleType, Object statusEnum, Window win) {
    try {
      LineVersion.Config currentCfg = LineVersion.get();
      boolean isDark = LineTheme.isDark(host);
      LayoutInflater infl = LayoutInflater.from(host);
      ViewGroup hostContainer = (ViewGroup) infl.inflate(currentCfg.res.layoutSettingsMain, null);
      hostContainer.setClickable(true);
      hostContainer.setFocusable(true);
      hostContainer.setPadding(0, 0, 0, 0);
      uiUpdateCallbacks.clear();

      try {
        int composeHeaderId =
            host.getResources().getIdentifier("compose_header", "id", "jp.naver.line.android");
        if (composeHeaderId != 0) {
          View composeHeader = hostContainer.findViewById(composeHeaderId);
          if (composeHeader != null && composeHeader.getParent() instanceof ViewGroup)
            ((ViewGroup) composeHeader.getParent()).removeView(composeHeader);
        }
      } catch (Throwable ignored) {
      }

      View navHeader = hostContainer.findViewById(currentCfg.res.idHeader);
      if (navHeader != null) {
        try {
          Reflect.callMethod(navHeader, currentCfg.main.methodRefreshNavHeader, win);
        } catch (Throwable t) {
          if (currentCfg.res.idStatusBarGuide != 0) {
            View guide = navHeader.findViewById(currentCfg.res.idStatusBarGuide);
            if (guide != null) {
              int statusBarHeight = 0;
              int resId =
                  host.getResources().getIdentifier("status_bar_height", "dimen", "android");
              if (resId > 0) statusBarHeight = host.getResources().getDimensionPixelSize(resId);
              if (statusBarHeight > 0) {
                ViewGroup.LayoutParams lp = guide.getLayoutParams();
                lp.height = statusBarHeight;
                guide.setLayoutParams(lp);
              }
            }
          }
        }
        Reflect.callMethod(
            navHeader, currentCfg.main.methodHeaderSetTitle, ModuleStrings.SETTINGS_TITLE);
        try {
          Reflect.callMethod(navHeader, currentCfg.main.methodHeaderSetButtonVisibility, true);
        } catch (Throwable ignored) {
        }
        Reflect.callMethod(
            navHeader,
            currentCfg.main.methodHeaderSetButtonListener,
            (View.OnClickListener) v -> initiateDialogClosure());

        navHeader.setBackgroundColor(LineTheme.backgroundColor(host));
        LineTheme.tintTextAndIcons(navHeader, LineTheme.primaryTextColor(host));
      }

      View itemListView = hostContainer.findViewById(currentCfg.res.idSettingList);
      if (itemListView != null) {
        ViewGroup viewParent = (ViewGroup) itemListView.getParent();
        int viewIndex = viewParent.indexOfChild(itemListView);
        ViewGroup.LayoutParams viewLp = itemListView.getLayoutParams();
        viewParent.removeView(itemListView);

        LinearLayout settingsRoot = new LinearLayout(host);
        settingsRoot.setOrientation(LinearLayout.VERTICAL);
        settingsRoot.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));

        final FrameLayout itemHost = new FrameLayout(host);
        itemHost.addView(renderSettingsItems(host, toggleType, statusEnum, null, false));
        cachedItemHost = itemHost;
        cachedNavHeader = navHeader;

        setupSearchBox(host, isDark, settingsRoot, itemHost, toggleType, statusEnum);

        settingsRoot.addView(itemHost, new LinearLayout.LayoutParams(-1, -1));

        FrameLayout pageContainer = new FrameLayout(host);
        pageContainer.addView(settingsRoot);
        cachedPageContainer = pageContainer;
        viewParent.addView(pageContainer, viewIndex, viewLp);

        hostContainer.setBackgroundColor(LineTheme.backgroundColor(host));
      }
      return hostContainer;
    } catch (Throwable t) {
      TextView errorLabel = new TextView(host);
      errorLabel.setText("Error: " + t.getMessage());
      return errorLabel;
    }
  }

  private void switchPage(
      Context ctx, Object toggleType, Object statusEnum, KnotConfig.Category category) {
    if (cachedItemHost == null || cachedNavHeader == null) return;

    boolean isGoingForward = (category != null && currentActiveCategory == null);
    currentActiveCategory = category;

    final View oldView = cachedItemHost.getChildAt(0);
    final View newView = renderSettingsItems(ctx, toggleType, statusEnum, category, false);

    float width = cachedItemHost.getWidth();
    newView.setTranslationX(isGoingForward ? width : -width);
    cachedItemHost.addView(newView);

    oldView
        .animate()
        .translationX(isGoingForward ? -width : width)
        .setDuration(250)
        .setInterpolator(new DecelerateInterpolator())
        .start();

    newView
        .animate()
        .translationX(0)
        .setDuration(250)
        .setInterpolator(new DecelerateInterpolator())
        .withEndAction(
            () -> {
              cachedItemHost.removeView(oldView);
            })
        .start();

    LineVersion.Config currentCfg = LineVersion.get();
    String title = (category == null) ? ModuleStrings.SETTINGS_TITLE : category.label;
    Reflect.callMethod(
        cachedNavHeader, currentCfg.main.methodRefreshNavHeader, settingsDialog.getWindow());
    Reflect.callMethod(cachedNavHeader, currentCfg.main.methodHeaderSetTitle, title);

    Reflect.callMethod(
        cachedNavHeader,
        currentCfg.main.methodHeaderSetButtonListener,
        (View.OnClickListener)
            v -> {
              if (currentActiveCategory != null) {
                switchPage(ctx, toggleType, statusEnum, null);
              } else {
                initiateDialogClosure();
              }
            });

    cachedNavHeader.setBackgroundColor(LineTheme.backgroundColor(ctx));
    LineTheme.tintTextAndIcons(cachedNavHeader, LineTheme.primaryTextColor(ctx));
  }

  private View renderSettingsItems(
      Context ctx,
      Object toggleType,
      Object statusEnum,
      KnotConfig.Category targetCategory,
      boolean showAll) {
    LineVersion.Config currentCfg = LineVersion.get();
    LayoutInflater infl = LayoutInflater.from(ctx);
    int bgColor = LineTheme.backgroundColor(ctx);

    ScrollView scroller = new ScrollView(ctx);
    scroller.setBackgroundColor(bgColor);

    LinearLayout mainList = new LinearLayout(ctx);
    mainList.setOrientation(LinearLayout.VERTICAL);
    mainList.setBackgroundColor(bgColor);

    int bottomOffset =
        (int)
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 64, ctx.getResources().getDisplayMetrics());
    mainList.setPadding(0, 0, 0, bottomOffset);

    if (targetCategory == null) {
      if (showAll) {
        injectStorageSection(infl, mainList, ctx);

        for (KnotConfig.Category cat : DISPLAY_CATEGORIES) {
          injectSectionHeader(infl, mainList, cat.label);
          injectCategoryItems(infl, mainList, ctx, cat, currentCfg, toggleType, statusEnum);
        }

        injectBackupSection(infl, mainList, ctx);
        injectOtherSection(infl, mainList, ctx, Main.options);
      } else {
        injectStorageSection(infl, mainList, ctx);
        injectSectionHeader(infl, mainList, ModuleStrings.SETTINGS_TITLE);

        for (KnotConfig.Category cat : DISPLAY_CATEGORIES) {
          injectCategoryRow(infl, mainList, ctx, cat, toggleType, statusEnum);
        }

        injectBackupSection(infl, mainList, ctx);
        injectOtherSection(infl, mainList, ctx, Main.options);
      }
    } else {
      injectCategoryItems(infl, mainList, ctx, targetCategory, currentCfg, toggleType, statusEnum);
    }

    scroller.addView(mainList);
    return scroller;
  }

  private View injectInfoRow(
      LayoutInflater infl,
      LinearLayout parent,
      Context ctx,
      CharSequence title,
      CharSequence description,
      boolean showArrow,
      Integer titleColorOverride,
      View.OnClickListener onClick) {
    View row = LineTheme.createTextRow(ctx);
    if (row != null) {
      LineTheme.setRowTitle(row, title);
      if (description != null && description.length() > 0) {
        LineTheme.setRowDescription(row, description);
      }
      LineTheme.setRowArrowVisible(row, showArrow);
      LineTheme.setRowDividerVisible(row, false);
      if (titleColorOverride != null) LineTheme.setRowTitleColor(row, titleColorOverride);
      if (onClick != null) row.setOnClickListener(onClick);
      parent.addView(row);
      return row;
    }
    return injectInfoRowFallback(
        infl, parent, ctx, title, description, showArrow, titleColorOverride, onClick);
  }

  private View injectInfoRowFallback(
      LayoutInflater infl,
      LinearLayout parent,
      Context ctx,
      CharSequence title,
      CharSequence description,
      boolean showArrow,
      Integer titleColorOverride,
      View.OnClickListener onClick) {
    LineVersion.Config currentCfg = LineVersion.get();
    View row = infl.inflate(currentCfg.res.typeRow, parent, false);
    applyNativeHighlight(row, ctx);
    applyVisibility(row, currentCfg.res.idIcon, View.GONE);
    applyVisibility(row, currentCfg.res.idMark, View.GONE);
    applyVisibility(row, currentCfg.res.idSeparator, View.GONE);
    applyVisibility(row, currentCfg.res.idNewMark, View.GONE);
    applyVisibility(row, currentCfg.res.idNoticeDot, View.GONE);
    applyVisibility(row, currentCfg.res.idArrow, showArrow ? View.VISIBLE : View.GONE);

    TextView titleLabel = row.findViewById(currentCfg.res.idTitle);
    if (titleLabel != null) {
      titleLabel.setText(title);
      titleLabel.setTextColor(
          titleColorOverride != null ? titleColorOverride : LineTheme.primaryTextColor(ctx));
    }
    TextView descLabel = row.findViewById(currentCfg.res.idDesc);
    if (descLabel != null) {
      if (description != null && description.length() > 0) {
        descLabel.setText(description);
        descLabel.setTextColor(LineTheme.secondaryTextColor(ctx));
        descLabel.setVisibility(View.VISIBLE);
      } else {
        descLabel.setVisibility(View.GONE);
      }
    }
    if (onClick != null) row.setOnClickListener(onClick);
    parent.addView(row);
    return row;
  }

  private void injectStorageSection(LayoutInflater infl, LinearLayout parent, Context ctx) {
    injectSectionHeader(infl, parent, ModuleStrings.CAT_STORAGE);
    injectPathSelectorRow(infl, parent, ctx, ModuleStrings.DESC_PATH_ROW);
    parent
        .getChildAt(parent.getChildCount() - 1)
        .setTag((ModuleStrings.CAT_STORAGE + " " + ModuleStrings.DESC_PATH_ROW).toLowerCase());
  }

  private void injectBackupSection(LayoutInflater infl, LinearLayout parent, Context ctx) {
    injectSectionHeader(infl, parent, ModuleStrings.CAT_BACKUP);
    injectBackupRow(infl, parent, ctx);
    parent
        .getChildAt(parent.getChildCount() - 1)
        .setTag(
            (ModuleStrings.OPT_BACKUP_LABEL + " " + ModuleStrings.OPT_BACKUP_DESC).toLowerCase());
    injectRestoreRow(infl, parent, ctx);
    parent
        .getChildAt(parent.getChildCount() - 1)
        .setTag(
            (ModuleStrings.OPT_RESTORE_LABEL + " " + ModuleStrings.OPT_RESTORE_DESC).toLowerCase());
  }

  private void injectOtherSection(
      LayoutInflater infl, LinearLayout parent, Context ctx, KnotConfig config) {
    injectSectionHeader(infl, parent, ModuleStrings.CAT_OTHER);
    injectAboutRow(infl, parent, ctx);
    parent
        .getChildAt(parent.getChildCount() - 1)
        .setTag((ModuleStrings.OPT_ABOUT_LABEL + " " + ModuleStrings.OPT_ABOUT_DESC).toLowerCase());

    injectResetRow(infl, parent, ctx, config, ModuleStrings.DESC_RESET_ROW);
    parent
        .getChildAt(parent.getChildCount() - 1)
        .setTag((ModuleStrings.SETTINGS_RESET + " " + ModuleStrings.DESC_RESET_ROW).toLowerCase());
  }

  private void injectItemRow(
      LayoutInflater infl,
      LinearLayout parent,
      Context ctx,
      KnotConfig.Item i,
      LineVersion.Config currentCfg,
      Object toggleType,
      Object statusEnum) {
    try {
      final String settingKey = i.key;
      if (settingKey.equals("custom_font_path")) {
        View row =
            injectInfoRow(
                infl,
                parent,
                ctx,
                i.label,
                i.description,
                true,
                null,
                v -> {
                  Activity host = resolveActivity(ctx);
                  if (host == null) return;
                  Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                  intent.addCategory(Intent.CATEGORY_OPENABLE);
                  intent.setType("*/*");
                  String[] mimeTypes = {
                    "font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-otf"
                  };
                  intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                  host.startActivityForResult(intent, PICK_FONT_CODE);
                });
        if (row != null) row.setTag((i.label + " " + i.description).toLowerCase());
        return;
      }

      View row = infl.inflate(currentCfg.res.layoutCheckbox, parent, false);
      boolean isEnabled = SettingsStore.get(i.key, i.enabled);

      Reflect.callMethod(row, currentCfg.settings.methodSetTitleText, i.label);
      Reflect.callMethod(row, currentCfg.settings.methodSetDescription, i.description, null, null);

      if (toggleType != null)
        Reflect.callMethod(row, currentCfg.settings.methodSetItemType, toggleType);
      if (statusEnum != null)
        Reflect.callMethod(row, currentCfg.settings.methodSetSyncStatus, statusEnum);

      Reflect.callMethod(row, currentCfg.settings.methodSetChecked, isEnabled);
      Reflect.callMethod(row, currentCfg.settings.methodSetDividerVisible, true);

      Runnable updateUI =
          () -> {
            if (i.disabledWhenEnabledKey != null) {
              boolean isDisabled = SettingsStore.get(i.disabledWhenEnabledKey, false);
              row.setAlpha(isDisabled ? 0.5f : 1.0f);
              if (isDisabled) {
                Reflect.callMethod(row, currentCfg.settings.methodSetChecked, false);
                if (SettingsStore.get(settingKey, false)) {
                  SettingsStore.save(settingKey, false);
                  for (KnotConfig.Item itm : Main.options.items) {
                    if (itm.key.equals(settingKey)) {
                      itm.enabled = false;
                      break;
                    }
                  }
                }
              }
            }
          };
      uiUpdateCallbacks.add(updateUI);
      updateUI.run();

      row.setOnClickListener(
          v -> {
            if (i.disabledWhenEnabledKey != null
                && SettingsStore.get(i.disabledWhenEnabledKey, false)) {
              Reflect.callMethod(v, currentCfg.settings.methodSetChecked, false);
              return;
            }
            boolean newState = !SettingsStore.get(settingKey, i.enabled);
            Reflect.callMethod(v, currentCfg.settings.methodSetChecked, newState);
            for (KnotConfig.Item itm : Main.options.items) {
              if (itm.key.equals(settingKey)) {
                itm.enabled = newState;
                break;
              }
            }
            SettingsStore.save(settingKey, newState);

            for (Runnable r : uiUpdateCallbacks) {
              r.run();
            }

            pendingRestart = true;
            cachedSearchView = null;
          });
      row.setTag((i.label + " " + i.description).toLowerCase());
      parent.addView(row);
    } catch (Throwable ignored) {
    }
  }

  private void injectCategoryRow(
      LayoutInflater infl,
      LinearLayout parent,
      Context ctx,
      KnotConfig.Category category,
      Object toggleType,
      Object statusEnum) {
    try {
      View cRow =
          injectInfoRow(
              infl,
              parent,
              ctx,
              category.label,
              null,
              true,
              null,
              v -> switchPage(ctx, toggleType, statusEnum, category));
      if (cRow != null) cRow.setTag(category.label.toLowerCase());
    } catch (Throwable ignored) {
    }
  }

  private void injectCategoryItems(
      LayoutInflater infl,
      LinearLayout parent,
      Context ctx,
      KnotConfig.Category category,
      LineVersion.Config currentCfg,
      Object toggleType,
      Object statusEnum) {
    String lastSection = null;
    for (KnotConfig.Item i : Main.options.items) {
      if (i.category != category) continue;
      if (i.section != null && !i.section.isEmpty() && !i.section.equals(lastSection)) {
        injectSectionHeader(infl, parent, i.section);
        lastSection = i.section;
      }
      injectItemRow(infl, parent, ctx, i, currentCfg, toggleType, statusEnum);
    }
  }

  private void injectSectionHeader(LayoutInflater infl, LinearLayout parent, String text) {
    try {
      LineVersion.Config currentCfg = LineVersion.get();
      View hView = infl.inflate(currentCfg.res.layoutSectionHeader, parent, false);
      if (hView instanceof TextView) ((TextView) hView).setText(text);
      hView.setTag("section_header");
      parent.addView(hView);
    } catch (Throwable ignored) {
    }
  }

  private void filterSettings(View settingsList, String query) {
    ViewGroup list;
    if (settingsList instanceof ScrollView) {
      list = (ViewGroup) ((ScrollView) settingsList).getChildAt(0);
    } else if (settingsList instanceof ViewGroup) {
      list = (ViewGroup) settingsList;
    } else {
      return;
    }

    if (list == null) return;

    boolean isSearching = query.length() > 0;
    int childCount = list.getChildCount();
    View lastHeader = null;
    int itemsInCurrentSection = 0;

    for (int i = 0; i < childCount; i++) {
      View child = list.getChildAt(i);
      Object tag = child.getTag();

      if (tag instanceof String && ((String) tag).equals("section_header")) {
        if (lastHeader != null) {
          lastHeader.setVisibility(
              itemsInCurrentSection > 0 || !isSearching ? View.VISIBLE : View.GONE);
        }
        lastHeader = child;
        itemsInCurrentSection = 0;
        continue;
      }

      if (!isSearching) {
        child.setVisibility(View.VISIBLE);
        continue;
      }

      if (tag instanceof String) {
        String searchable = (String) tag;
        if (searchable.contains(query)) {
          child.setVisibility(View.VISIBLE);
          itemsInCurrentSection++;
        } else {
          child.setVisibility(View.GONE);
        }
      }
    }

    if (lastHeader != null) {
      lastHeader.setVisibility(
          itemsInCurrentSection > 0 || !isSearching ? View.VISIBLE : View.GONE);
    }
  }

  private void injectPathSelectorRow(
      LayoutInflater infl, LinearLayout parent, Context ctx, String description) {
    try {
      String activePath = SettingsStore.getSettingsDir();
      CharSequence pathTitle =
          activePath == null ? ModuleStrings.SETTINGS_PATH_PICKER_HINT : activePath;
      int pathColor = activePath == null ? Color.RED : LineTheme.accentGreen(ctx);
      injectInfoRow(
          infl,
          parent,
          ctx,
          pathTitle,
          description,
          true,
          pathColor,
          v -> openSystemFolderPicker(ctx));
    } catch (Throwable ignored) {
    }
  }

  private void injectResetRow(
      LayoutInflater infl,
      LinearLayout parent,
      Context ctx,
      KnotConfig config,
      String description) {
    try {
      injectInfoRow(
          infl,
          parent,
          ctx,
          ModuleStrings.SETTINGS_RESET,
          description,
          false,
          Color.RED,
          v -> {
            Context activeCtx = settingsDialog != null ? settingsDialog.getContext() : ctx;
            int themeId = LineTheme.dialogTheme(activeCtx);
            LineTheme.applyDialogColors(
                new AlertDialog.Builder(activeCtx, themeId)
                    .setTitle(ModuleStrings.SETTINGS_RESET)
                    .setMessage(ModuleStrings.SETTINGS_RESET_CONFIRM)
                    .setPositiveButton(
                        ModuleStrings.SETTINGS_RESET_OK,
                        (d, w) -> {
                          SettingsStore.reset();
                          SettingsStore.load(Main.options);
                          pendingRestart = true;
                          if (onSettingsReloadRequest != null) onSettingsReloadRequest.run();
                        })
                    .setNegativeButton(ModuleStrings.SETTINGS_CANCEL, null)
                    .show(),
                activeCtx);
          });
    } catch (Throwable ignored) {
    }
  }

  private void injectBackupRow(LayoutInflater infl, LinearLayout parent, Context ctx) {
    try {
      injectInfoRow(
          infl,
          parent,
          ctx,
          ModuleStrings.OPT_BACKUP_LABEL,
          ModuleStrings.OPT_BACKUP_DESC,
          true,
          null,
          v -> BackupRestoreHook.runBackup(ctx));
    } catch (Throwable ignored) {
    }
  }

  private void injectRestoreRow(LayoutInflater infl, LinearLayout parent, Context ctx) {
    try {
      injectInfoRow(
          infl,
          parent,
          ctx,
          ModuleStrings.OPT_RESTORE_LABEL,
          ModuleStrings.OPT_RESTORE_DESC,
          true,
          null,
          v -> {
            Activity host = resolveActivity(ctx);
            if (host == null) return;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_TITLE, "Knot_*.knotbak");

            try {
              String dirUriStr = SettingsStore.getSettingsDirUri();
              if (dirUriStr != null) {
                Uri treeUri = Uri.parse(dirUriStr);
                String treeId = android.provider.DocumentsContract.getTreeDocumentId(treeUri);

                androidx.documentfile.provider.DocumentFile root =
                    androidx.documentfile.provider.DocumentFile.fromTreeUri(ctx, treeUri);
                androidx.documentfile.provider.DocumentFile backupDir = root.findFile("KnotBackup");

                String targetId = treeId;
                if (backupDir != null && backupDir.isDirectory()) {
                  targetId = treeId + (treeId.endsWith(":") ? "" : "/") + "KnotBackup";
                }

                Uri initialUri =
                    android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, targetId);
                intent.putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI, initialUri);
              }
            } catch (Throwable ignored) {
            }

            host.startActivityForResult(intent, PICK_RESTORE_DB_CODE);
          });
    } catch (Throwable ignored) {
    }
  }

  private void injectAboutRow(LayoutInflater infl, LinearLayout parent, Context ctx) {
    try {
      injectInfoRow(
          infl,
          parent,
          ctx,
          ModuleStrings.OPT_ABOUT_LABEL,
          ModuleStrings.OPT_ABOUT_DESC,
          true,
          null,
          v -> openAboutPage(settingsDialog != null ? settingsDialog.getContext() : ctx));
    } catch (Throwable ignored) {
    }
  }

  private void openAboutPage(Context ctx) {
    if (cachedPageContainer == null || cachedItemHost == null || cachedNavHeader == null) return;
    final View settingsPage = (View) cachedItemHost.getParent();
    if (settingsPage == null) return;
    aboutPageActive = true;

    final View about = buildAboutPageView(ctx);
    float width = cachedPageContainer.getWidth();
    about.setTranslationX(width);
    cachedPageContainer.addView(about, new FrameLayout.LayoutParams(-1, -1));
    aboutPageView = about;

    settingsPage
        .animate()
        .translationX(-width)
        .setDuration(250)
        .setInterpolator(new DecelerateInterpolator())
        .start();
    about
        .animate()
        .translationX(0)
        .setDuration(250)
        .setInterpolator(new DecelerateInterpolator())
        .withEndAction(() -> settingsPage.setVisibility(View.GONE))
        .start();

    configureNavHeader(ctx, ModuleStrings.OPT_ABOUT_LABEL, v -> closeAboutPage(ctx));
  }

  private void closeAboutPage(Context ctx) {
    if (!aboutPageActive || cachedPageContainer == null || cachedItemHost == null) return;
    aboutPageActive = false;

    final View settingsPage = (View) cachedItemHost.getParent();
    final View about = aboutPageView;
    aboutPageView = null;

    float width = cachedPageContainer.getWidth();
    if (settingsPage != null) {
      settingsPage.setVisibility(View.VISIBLE);
      settingsPage.setTranslationX(-width);
      settingsPage
          .animate()
          .translationX(0)
          .setDuration(250)
          .setInterpolator(new DecelerateInterpolator())
          .start();
    }
    if (about != null) {
      about
          .animate()
          .translationX(width)
          .setDuration(250)
          .setInterpolator(new DecelerateInterpolator())
          .withEndAction(() -> cachedPageContainer.removeView(about))
          .start();
    }

    restoreMainHeader(ctx);
  }

  private void restoreMainHeader(Context ctx) {
    String title =
        (currentActiveCategory == null)
            ? ModuleStrings.SETTINGS_TITLE
            : currentActiveCategory.label;
    configureNavHeader(
        ctx,
        title,
        v -> {
          if (currentActiveCategory != null) {
            switchPage(ctx, cachedToggle, cachedSuccess, null);
          } else {
            initiateDialogClosure();
          }
        });
  }

  private void configureNavHeader(Context ctx, String title, View.OnClickListener back) {
    LineVersion.Config currentCfg = LineVersion.get();
    Reflect.callMethod(
        cachedNavHeader, currentCfg.main.methodRefreshNavHeader, settingsDialog.getWindow());
    Reflect.callMethod(cachedNavHeader, currentCfg.main.methodHeaderSetTitle, title);
    Reflect.callMethod(cachedNavHeader, currentCfg.main.methodHeaderSetButtonListener, back);
    cachedNavHeader.setBackgroundColor(LineTheme.backgroundColor(ctx));
    LineTheme.tintTextAndIcons(cachedNavHeader, LineTheme.primaryTextColor(ctx));
  }

  private View buildAboutPageView(Context ctx) {
    LayoutInflater infl = LayoutInflater.from(ctx);
    float density = ctx.getResources().getDisplayMetrics().density;
    int bg = LineTheme.backgroundColor(ctx);

    ScrollView scroller = new ScrollView(ctx);
    scroller.setBackgroundColor(bg);

    LinearLayout root = new LinearLayout(ctx);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(bg);
    root.setPadding(0, 0, 0, (int) (64 * density));

    LinearLayout hero = new LinearLayout(ctx);
    hero.setOrientation(LinearLayout.VERTICAL);
    hero.setGravity(Gravity.CENTER_HORIZONTAL);
    hero.setPadding(
        (int) (32 * density), (int) (32 * density), (int) (32 * density), (int) (28 * density));

    try {
      Context modCtx = ctx.createPackageContext("app.zipper.knot", Context.CONTEXT_IGNORE_SECURITY);
      int resId = modCtx.getResources().getIdentifier("ic_knot", "drawable", "app.zipper.knot");
      if (resId != 0) {
        ImageView logo = new ImageView(ctx);
        logo.setImageDrawable(modCtx.getDrawable(resId));
        LinearLayout.LayoutParams lp =
            new LinearLayout.LayoutParams((int) (84 * density), (int) (84 * density));
        lp.bottomMargin = (int) (16 * density);
        logo.setLayoutParams(lp);
        hero.addView(logo);
      }
    } catch (Throwable ignored) {
    }

    TextView name = new TextView(ctx);
    name.setText(BRAND_TAG);
    name.setTextSize(26);
    name.setTypeface(null, Typeface.BOLD);
    name.setTextColor(LineTheme.primaryTextColor(ctx));
    name.setGravity(Gravity.CENTER_HORIZONTAL);
    hero.addView(name);

    TextView ver = new TextView(ctx);
    ver.setText("v" + BuildConfig.VERSION_NAME);
    ver.setTextSize(13);
    ver.setTextColor(LineTheme.secondaryTextColor(ctx));
    ver.setGravity(Gravity.CENTER_HORIZONTAL);
    LinearLayout.LayoutParams verLp = new LinearLayout.LayoutParams(-2, -2);
    verLp.topMargin = (int) (4 * density);
    ver.setLayoutParams(verLp);
    hero.addView(ver);

    TextView tagline = new TextView(ctx);
    tagline.setText(ModuleStrings.ABOUT_TAGLINE);
    tagline.setTextSize(13);
    tagline.setTextColor(LineTheme.secondaryTextColor(ctx));
    tagline.setGravity(Gravity.CENTER_HORIZONTAL);
    LinearLayout.LayoutParams tagLp = new LinearLayout.LayoutParams(-2, -2);
    tagLp.topMargin = (int) (10 * density);
    tagline.setLayoutParams(tagLp);
    hero.addView(tagline);

    root.addView(hero);

    for (String[] section : CONTRIBUTOR_SECTIONS) {
      injectSectionHeader(infl, root, section[0]);
      for (int i = 1; i < section.length; i++) {
        injectContributorRow(infl, root, ctx, section[i]);
      }
    }

    injectSectionHeader(infl, root, ModuleStrings.ABOUT_SEC_LINKS);
    injectInfoRow(
        infl,
        root,
        ctx,
        ModuleStrings.ABOUT_LINK_REPO,
        "github.com/2b-zipper/Knot",
        true,
        null,
        v -> openUrl(ctx, "https://github.com/2b-zipper/Knot"));
    injectInfoRow(
        infl,
        root,
        ctx,
        ModuleStrings.ABOUT_LINK_LICENSE,
        ModuleStrings.ABOUT_LICENSE_VALUE,
        true,
        null,
        v -> openUrl(ctx, "https://github.com/2b-zipper/Knot/blob/main/LICENSE"));

    TextView disclaimer = new TextView(ctx);
    disclaimer.setText(ModuleStrings.ABOUT_DISCLAIMER);
    disclaimer.setTextSize(12);
    disclaimer.setTextColor(LineTheme.secondaryTextColor(ctx));
    disclaimer.setGravity(Gravity.CENTER_HORIZONTAL);
    LinearLayout.LayoutParams discLp = new LinearLayout.LayoutParams(-1, -2);
    discLp.topMargin = (int) (28 * density);
    discLp.leftMargin = (int) (24 * density);
    discLp.rightMargin = (int) (24 * density);
    disclaimer.setLayoutParams(discLp);
    root.addView(disclaimer);

    scroller.addView(root);
    return scroller;
  }

  private static String[] contributorHandles() {
    List<String> handles = new ArrayList<>();
    for (String[] section : CONTRIBUTOR_SECTIONS) {
      for (int i = 1; i < section.length; i++) handles.add(section[i]);
    }
    return handles.toArray(new String[0]);
  }

  private void injectContributorRow(
      LayoutInflater infl, LinearLayout parent, Context ctx, String handle) {
    try {
      String profileUrl = "https://github.com/" + handle;
      float density = ctx.getResources().getDisplayMetrics().density;

      LinearLayout wrapper = new LinearLayout(ctx);
      wrapper.setOrientation(LinearLayout.HORIZONTAL);
      wrapper.setGravity(Gravity.CENTER_VERTICAL);
      wrapper.setOnClickListener(v -> openUrl(ctx, profileUrl));

      int size = (int) (40 * density);
      ImageView avatar = new ImageView(ctx);
      LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(size, size);
      avatarLp.setMarginStart((int) (20 * density));
      avatar.setLayoutParams(avatarLp);
      avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
      GradientDrawable placeholder = new GradientDrawable();
      placeholder.setShape(GradientDrawable.OVAL);
      placeholder.setColor(LineTheme.fieldColor(ctx));
      avatar.setImageDrawable(placeholder);
      avatar.setClipToOutline(true);
      avatar.setOutlineProvider(
          new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline o) {
              o.setOval(0, 0, v.getWidth(), v.getHeight());
            }
          });
      wrapper.addView(avatar);

      View row = injectInfoRow(infl, wrapper, ctx, handle, "@" + handle, true, null, null);
      if (row == null) return;
      row.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
      moveHighlightToWrapper(ctx, row, wrapper);

      parent.addView(wrapper);
      ContributorProfiles.loadAvatar(avatar, handle);
      ContributorProfiles.loadDisplayName(handle, name -> LineTheme.setRowTitle(row, name));
    } catch (Throwable ignored) {
    }
  }

  private void moveHighlightToWrapper(Context ctx, View row, View wrapper) {
    int containerId =
        ctx.getResources().getIdentifier("setting_item_container", "id", "jp.naver.line.android");
    View container = containerId != 0 ? row.findViewById(containerId) : null;
    if (container != null) {
      Drawable bg = container.getBackground();
      container.setBackground(null);
      if (bg != null && bg.getConstantState() != null) {
        wrapper.setBackground(bg.getConstantState().newDrawable().mutate());
        return;
      }
    }
    applyNativeHighlight(wrapper, ctx);
  }

  private void openUrl(Context ctx, String url) {
    try {
      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      ctx.startActivity(intent);
    } catch (Throwable ignored) {
    }
  }

  private static boolean containsKnotItem(Collection<?> items, LineVersion.Config c) {
    if (items == null) return false;
    for (Object item : items) {
      try {
        Object model = Reflect.getObjectField(item, c.settings.fieldItemModel);
        if (model == null) continue;
        Object tag = Reflect.getObjectField(model, c.settings.fieldModelTag);
        if (BRAND_TAG.equals(tag)) return true;
      } catch (Throwable ignored) {
      }
    }
    return false;
  }

  private static void applyVisibility(View root, int viewId, int state) {
    View v = root.findViewById(viewId);
    if (v != null) v.setVisibility(state);
  }

  private void openSystemFolderPicker(Context ctx) {
    Activity host = resolveActivity(ctx);
    if (host == null) return;
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
    intent.addFlags(3);
    host.startActivityForResult(intent, PICK_DIRECTORY_CODE);
  }

  private Activity resolveActivity(Context ctx) {
    if (ctx instanceof Activity) return (Activity) ctx;
    if (ctx instanceof ContextWrapper)
      return resolveActivity(((ContextWrapper) ctx).getBaseContext());
    return null;
  }

  private static void cacheUiConstants(Context ctx) {
    if (cachedToggle != null && cachedSuccess != null) return;
    try {
      LineVersion.Config currentCfg = LineVersion.get();
      LayoutInflater infl = LayoutInflater.from(ctx);
      View view = infl.inflate(currentCfg.res.layoutCheckbox, null, false);
      for (java.lang.reflect.Method m : view.getClass().getMethods()) {
        if (m.getParameterCount() != 1) continue;
        Class<?> p = m.getParameterTypes()[0];
        if (!p.isEnum()) continue;
        if ("setItemType".equals(m.getName())) {
          for (Object c : p.getEnumConstants()) if ("TOGGLE".equals(c.toString())) cachedToggle = c;
        } else if ("setSyncStatus".equals(m.getName())) {
          for (Object c : p.getEnumConstants())
            if ("SUCCESS".equals(c.toString())) cachedSuccess = c;
        }
      }
    } catch (Throwable ignored) {
    }
  }

  private static Object createAdapterItemProxy(Class<?> itf, ClassLoader cl, int type) {
    LineVersion.Config currentCfg = LineVersion.get();
    return Proxy.newProxyInstance(
        cl,
        new Class[] {itf},
        (proxy, method, args) ->
            currentCfg.settings.methodProxyGetItemType.equals(method.getName()) ? type : null);
  }

  private void setupSearchBox(
      Context ctx,
      boolean isDark,
      LinearLayout root,
      FrameLayout itemHost,
      Object toggleType,
      Object statusEnum) {
    float density = ctx.getResources().getDisplayMetrics().density;
    RelativeLayout searchContainer = new RelativeLayout(ctx);
    LinearLayout.LayoutParams containerLp = new LinearLayout.LayoutParams(-1, -2);
    int margin = (int) (12 * density);
    containerLp.setMargins(margin, margin / 2, margin, margin / 2);
    searchContainer.setLayoutParams(containerLp);

    EditText searchBox = new EditText(ctx);
    searchBox.setHint(ModuleStrings.SETTINGS_SEARCH_HINT);
    searchBox.setSingleLine(true);
    searchBox.setTextSize(14);
    searchBox.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);

    int pHorizontal = (int) (16 * density);
    int pVertical = (int) (8 * density);
    int pRight = (int) (40 * density);
    searchBox.setPadding(pHorizontal, pVertical, pRight, pVertical);

    GradientDrawable searchBg = new GradientDrawable();
    searchBg.setColor(LineTheme.fieldColor(ctx));
    searchBg.setCornerRadius(20 * density);
    searchBox.setBackground(searchBg);

    searchBox.setTextColor(LineTheme.primaryTextColor(ctx));
    searchBox.setHintTextColor(LineTheme.secondaryTextColor(ctx));

    RelativeLayout.LayoutParams boxLp = new RelativeLayout.LayoutParams(-1, -2);
    searchBox.setLayoutParams(boxLp);
    searchContainer.addView(searchBox);

    TextView clearButton = new TextView(ctx);
    clearButton.setText("✕");
    clearButton.setGravity(Gravity.CENTER);
    clearButton.setTextSize(18);
    clearButton.setTextColor(LineTheme.secondaryTextColor(ctx));
    clearButton.setVisibility(View.GONE);

    int btnSize = (int) (32 * density);
    RelativeLayout.LayoutParams btnLp = new RelativeLayout.LayoutParams(btnSize, btnSize);
    btnLp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
    btnLp.addRule(RelativeLayout.CENTER_VERTICAL);
    btnLp.rightMargin = (int) (8 * density);
    clearButton.setLayoutParams(btnLp);
    searchContainer.addView(clearButton);

    root.addView(searchContainer, 0);

    clearButton.setOnClickListener(v -> searchBox.setText(""));

    searchBox.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            String query = s.toString().toLowerCase();
            boolean isSearching = query.length() > 0;
            clearButton.setVisibility(isSearching ? View.VISIBLE : View.GONE);

            if (isSearching) {
              if (currentActiveCategory == null) {
                if (cachedSearchView == null) {
                  cachedSearchView = renderSettingsItems(ctx, toggleType, statusEnum, null, true);
                }
                if (cachedItemHost.getChildAt(0) != cachedSearchView) {
                  cachedItemHost.removeAllViews();
                  cachedItemHost.addView(cachedSearchView);
                }
                filterSettings(cachedSearchView, query);
              } else {
                filterSettings(cachedItemHost.getChildAt(0), query);
              }
            } else {
              if (cachedItemHost.getChildAt(0) == cachedSearchView) {
                View normalView =
                    renderSettingsItems(ctx, toggleType, statusEnum, currentActiveCategory, false);
                cachedItemHost.removeAllViews();
                cachedItemHost.addView(normalView);
              } else {
                filterSettings(cachedItemHost.getChildAt(0), "");
              }
            }
          }

          @Override
          public void afterTextChanged(Editable s) {}
        });

    onSettingsReloadRequest =
        () -> {
          Activity a = resolveActivity(ctx);
          if (a != null)
            a.runOnUiThread(
                () -> {
                  cachedSearchView = null;
                  itemHost.removeAllViews();
                  String query = searchBox.getText().toString().toLowerCase();
                  boolean isSearching = query.length() > 0;
                  View newList =
                      renderSettingsItems(
                          ctx, toggleType, statusEnum, currentActiveCategory, isSearching);
                  itemHost.addView(newList);
                  filterSettings(newList, query);
                });
        };
  }

  private void applyNativeHighlight(View v, Context ctx) {
    if (v == null) return;
    android.graphics.drawable.StateListDrawable states =
        new android.graphics.drawable.StateListDrawable();
    int normalColor = LineTheme.backgroundColor(ctx);
    int pressedColor = LineTheme.fieldColor(ctx);

    states.addState(new int[] {android.R.attr.state_pressed}, new ColorDrawable(pressedColor));
    states.addState(new int[] {android.R.attr.state_focused}, new ColorDrawable(pressedColor));
    states.addState(new int[] {}, new ColorDrawable(normalColor));
    v.setBackground(states);
  }
}
