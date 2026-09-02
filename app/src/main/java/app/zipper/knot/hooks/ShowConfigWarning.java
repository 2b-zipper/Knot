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
import app.zipper.knot.LoadParam;
import app.zipper.knot.R;
import app.zipper.knot.Reflect;

public class ShowConfigWarning implements BaseHook {

  private static final int WARNING_BANNER_TAG = 0x4C585F57;
  private static final int BROWSE_DIR_REQUEST = 0x4C5859;

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    Class<?> activityCls =
        lpparam.classLoader.loadClass("jp.naver.line.android.activity.main.MainActivity");

    Knot.module
        .hook(Reflect.findMethodExact(activityCls, "onResume"))
        .intercept(
            chain -> {
              Object result = chain.proceed();
              Activity host = (Activity) chain.getThisObject();
              loadSettings(config, host);

              ViewGroup layoutRoot = host.findViewById(android.R.id.content);
              if (layoutRoot == null) return result;

              if (app.zipper.knot.SettingsStore.isConfigured()) {
                dismissBanner(layoutRoot);
              } else {
                if (layoutRoot.findViewWithTag(WARNING_BANNER_TAG) == null) {
                  layoutRoot.addView(constructWarningBanner(host));
                }
              }
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
              try {
                host.getContentResolver()
                    .takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
              } catch (Throwable ignored) {
              }
              app.zipper.knot.SettingsStore.setSettingsDir(treeUri.toString());

              host.runOnUiThread(
                  () -> {
                    ViewGroup root = host.findViewById(android.R.id.content);
                    if (root != null) dismissBanner(root);
                  });
              return null;
            });
  }

  private static void loadSettings(KnotConfig config, Activity host) {
    app.zipper.knot.SettingsStore.init(host);
    for (KnotConfig.Item item : config.items) {
      item.enabled = app.zipper.knot.SettingsStore.get(item.key, item.enabled);
    }
  }

  private static void dismissBanner(ViewGroup root) {
    View banner = root.findViewWithTag(WARNING_BANNER_TAG);
    if (banner != null) root.removeView(banner);
  }

  private View constructWarningBanner(Activity host) {
    TextView label = new TextView(host);
    label.setTag(WARNING_BANNER_TAG);
    label.setText(app.zipper.knot.utils.ModuleResources.get(R.string.warn_storage_unset));
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
        v -> {
          Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
          intent.addFlags(
              Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
          host.startActivityForResult(intent, BROWSE_DIR_REQUEST);
        });

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
