package app.zipper.knot.hooks;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.R;
import app.zipper.knot.Reflect;
import app.zipper.knot.SettingsStore;
import app.zipper.knot.ui.settings.SettingsFilePickers;
import app.zipper.knot.utils.ModuleResources;

public class ShowConfigWarning implements BaseHook {

  private static final int WARNING_BANNER_TAG = 0x4C585F57;
  private static final int BROWSE_DIR_REQUEST = 0x4C5859;

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    LineVersion.Config cfg = LineVersion.get();
    if (cfg == null) return;
    Class<?> activityCls = lpparam.classLoader.loadClass(cfg.main.mainActivity);

    Knot.module
        .hook(Reflect.findMethodExact(activityCls, "onResume"))
        .intercept(
            chain -> {
              Object result = chain.proceed();
              Activity host = (Activity) chain.getThisObject();
              loadSettings(config, host);
              updateBanner(host);
              return result;
            });

    Knot.module
        .hook(
            Reflect.findMethodExact(
                activityCls, "onActivityResult", int.class, int.class, Intent.class))
        .intercept(
            chain -> {
              int code = (int) chain.getArg(0);
              int result = (int) chain.getArg(1);
              Intent data = (Intent) chain.getArg(2);

              if (code != BROWSE_DIR_REQUEST) return chain.proceed();

              if (result != Activity.RESULT_OK || data == null) return null;
              Uri treeUri = data.getData();
              if (treeUri == null) return null;

              Activity host = (Activity) chain.getThisObject();
              SettingsFilePickers.applyPickedDirectory(host, treeUri, config);
              host.runOnUiThread(() -> updateBanner(host));
              return null;
            });
  }

  private static void loadSettings(KnotConfig config, Activity host) {
    SettingsStore.init(host);
    SettingsStore.load(config);
  }

  private void updateBanner(Activity host) {
    ViewGroup layoutRoot = host.findViewById(android.R.id.content);
    if (layoutRoot == null) return;

    SettingsStore.StorageState state = SettingsStore.storageState();
    if (state == SettingsStore.StorageState.READY) {
      dismissBanner(layoutRoot);
      return;
    }

    String message =
        ModuleResources.get(
            state == SettingsStore.StorageState.PERMISSION_LOST
                ? R.string.warn_storage_permission_lost
                : R.string.warn_storage_unset);
    View existing = layoutRoot.findViewWithTag(WARNING_BANNER_TAG);
    if (existing instanceof TextView) {
      ((TextView) existing).setText(message);
    } else {
      if (existing != null) layoutRoot.removeView(existing);
      layoutRoot.addView(constructWarningBanner(host, message));
    }
  }

  private static void dismissBanner(ViewGroup root) {
    View banner = root.findViewWithTag(WARNING_BANNER_TAG);
    if (banner != null) root.removeView(banner);
  }

  private View constructWarningBanner(Activity host, String message) {
    TextView label = new TextView(host);
    label.setTag(WARNING_BANNER_TAG);
    label.setText(message);
    label.setTextColor(Color.WHITE);
    label.setTextSize(12);
    label.setTypeface(null, Typeface.BOLD);
    label.setBackgroundColor(Color.parseColor("#CC333333"));
    label.setGravity(Gravity.CENTER);

    float scale = host.getResources().getDisplayMetrics().density;
    int padding = (int) (10 * scale);
    label.setPadding(padding, padding, padding, padding);
    label.setClickable(true);
    label.setOnClickListener(
        v ->
            host.startActivityForResult(
                SettingsFilePickers.createFolderPickerIntent(), BROWSE_DIR_REQUEST));

    int statusH = 0, actionH = 0;
    try {
      int id = host.getResources().getIdentifier("status_bar_height", "dimen", "android");
      if (id > 0) statusH = host.getResources().getDimensionPixelSize(id);
    } catch (Throwable ignored) {
    }

    try {
      android.util.TypedValue val = new android.util.TypedValue();
      if (host.getTheme().resolveAttribute(android.R.attr.actionBarSize, val, true)) {
        actionH =
            android.util.TypedValue.complexToDimensionPixelSize(
                val.data, host.getResources().getDisplayMetrics());
      }
    } catch (Throwable ignored) {
    }

    FrameLayout.LayoutParams params =
        new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.TOP);
    params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
    params.topMargin = statusH + actionH;
    label.setLayoutParams(params);
    return label;
  }
}
