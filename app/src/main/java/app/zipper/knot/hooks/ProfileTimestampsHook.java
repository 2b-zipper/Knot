package app.zipper.knot.hooks;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.R;
import app.zipper.knot.Reflect;
import app.zipper.knot.utils.LineDBUtils;
import app.zipper.knot.utils.LineTheme;
import app.zipper.knot.utils.ModuleResources;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileTimestampsHook implements BaseHook {

  private static final String MARKER_TAG = "knot_profile_timestamps";
  private static final String MODULE_PKG = "app.zipper.knot";
  private static final SimpleDateFormat FMT =
      new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    if (!config.showProfileTimestamps.enabled) return;

    final LineVersion.Config cfg = LineVersion.get();
    if (cfg == null || cfg.profileTimestamps.activityClass.isEmpty()) return;

    Knot.module
        .hook(
            Reflect.findMethodExact(
                cfg.profileTimestamps.activityClass, lpparam.classLoader, "onCreate", Bundle.class))
        .intercept(
            chain -> {
              Object result = chain.proceed();
              if (!Main.options.showProfileTimestamps.enabled) return result;
              try {
                inject((Activity) chain.getThisObject(), cfg);
              } catch (Throwable t) {
                Knot.log("Knot: ProfileTimestampsHook inject error: " + t);
              }
              return result;
            });
  }

  private static void inject(Activity activity, LineVersion.Config cfg) {
    Intent intent = activity.getIntent();
    if (intent == null) return;
    final String mid = intent.getStringExtra(cfg.profileTimestamps.midExtraKey);
    if (mid == null || mid.isEmpty()) return;

    activity
        .getWindow()
        .getDecorView()
        .post(
            () -> {
              try {
                addIcon(activity, cfg, mid);
              } catch (Throwable t) {
                Knot.log("Knot: ProfileTimestampsHook addIcon error: " + t);
              }
            });
  }

  private static void addIcon(Activity activity, LineVersion.Config cfg, String mid) {
    Resources res = activity.getResources();
    String pkg = activity.getPackageName();

    View found = findByIdName(activity, res, pkg, cfg.profileTimestamps.resHeaderButtonContainer);
    if (!(found instanceof LinearLayout)) {
      Knot.log("Knot: ProfileTimestampsHook header button container not found");
      return;
    }
    LinearLayout container = (LinearLayout) found;
    if (container.findViewWithTag(MARKER_TAG) != null) return;

    float density = res.getDisplayMetrics().density;
    int width =
        resolveDimen(
            res, pkg, cfg.profileTimestamps.optionalButtonWidthDimen, (int) (30 * density));
    int spacing =
        resolveDimen(
            res, pkg, cfg.profileTimestamps.optionalButtonSpacingDimen, (int) (8 * density));

    ImageView icon = new ImageView(activity);
    icon.setTag(MARKER_TAG);
    icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
    int pad = (int) (9 * density);
    icon.setPadding(pad, pad, pad, pad);
    icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);

    Drawable iconDrawable = loadModuleDrawable(activity, "ic_info_circle");
    if (iconDrawable != null) icon.setImageDrawable(iconDrawable);

    LinearLayout.LayoutParams lp =
        new LinearLayout.LayoutParams(width, LinearLayout.LayoutParams.MATCH_PARENT);
    lp.setMarginEnd(spacing);
    icon.setLayoutParams(lp);

    icon.setOnClickListener(v -> showDialog(activity, mid));

    container.addView(icon, 0);
  }

  private static void showDialog(Activity activity, String mid) {
    try {
      LineTheme.invalidate();
      LineDBUtils.ContactTimes t = LineDBUtils.getContactTimes(mid);
      StringBuilder sb = new StringBuilder();
      appendRow(sb, ModuleResources.get(R.string.profile_ts_mid), mid);
      appendRow(
          sb,
          ModuleResources.get(R.string.profile_ts_friend_created),
          t == null ? null : t.friendCreated);
      appendRow(
          sb, ModuleResources.get(R.string.profile_ts_favorite), t == null ? null : t.favorite);
      appendRow(
          sb,
          ModuleResources.get(R.string.profile_ts_profile_updated),
          t == null ? null : t.profileUpdated);

      int themeId = LineTheme.dialogTheme(activity);
      LineTheme.applyDialogColors(
          new AlertDialog.Builder(activity, themeId)
              .setTitle(ModuleResources.get(R.string.profile_ts_dialog_title))
              .setMessage(sb.toString().trim())
              .setNeutralButton(
                  ModuleResources.get(R.string.profile_ts_copy_mid),
                  (d, which) -> copyMid(activity, mid))
              .setPositiveButton(ModuleResources.get(R.string.common_close), null)
              .show(),
          activity);
    } catch (Throwable e) {
      Knot.log("Knot: ProfileTimestampsHook showDialog error: " + e);
    }
  }

  private static void copyMid(Activity activity, String mid) {
    try {
      ClipboardManager clipboard =
          (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
      if (clipboard == null) return;
      clipboard.setPrimaryClip(
          ClipData.newPlainText(ModuleResources.get(R.string.profile_ts_mid), mid));
      Toast.makeText(
              activity, ModuleResources.get(R.string.profile_ts_mid_copied), Toast.LENGTH_SHORT)
          .show();
    } catch (Throwable e) {
      Knot.log("Knot: ProfileTimestampsHook copyMid error: " + e);
    }
  }

  private static void appendRow(StringBuilder sb, String label, Long millis) {
    appendRow(sb, label, format(millis));
  }

  private static void appendRow(StringBuilder sb, String label, String value) {
    sb.append(label).append(": ").append(value).append("\n");
  }

  private static String format(Long millis) {
    if (millis == null || millis <= 0) return ModuleResources.get(R.string.profile_ts_empty);
    return FMT.format(new Date(millis));
  }

  private static View findByIdName(Activity activity, Resources res, String pkg, String name) {
    if (name == null || name.isEmpty()) return null;
    int id = res.getIdentifier(name, "id", pkg);
    if (id == 0) return null;
    return activity.findViewById(id);
  }

  private static int resolveDimen(Resources res, String pkg, String name, int fallback) {
    try {
      int id = res.getIdentifier(name, "dimen", pkg);
      if (id != 0) return res.getDimensionPixelSize(id);
    } catch (Throwable ignored) {
    }
    return fallback;
  }

  private static Drawable loadModuleDrawable(Context context, String name) {
    try {
      Context modCtx = context.createPackageContext(MODULE_PKG, Context.CONTEXT_IGNORE_SECURITY);
      int id = modCtx.getResources().getIdentifier(name, "drawable", modCtx.getPackageName());
      if (id != 0) return modCtx.getDrawable(id);
    } catch (Throwable t) {
      Knot.log("Knot: ProfileTimestampsHook loadModuleDrawable failed: " + t);
    }
    return null;
  }
}
