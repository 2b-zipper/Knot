package app.zipper.knot.ui.settings;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import app.zipper.knot.LineVersion;
import app.zipper.knot.utils.LineTheme;
import app.zipper.knot.utils.ModuleResources;

public final class SettingsViews {

  public static final String TAG_SECTION_HEADER = "section_header";

  private static final long PAGE_ANIM_MS = 250;

  private SettingsViews() {}

  public static Activity activityOf(Context ctx) {
    if (ctx instanceof Activity) return (Activity) ctx;
    if (ctx instanceof ContextWrapper) return activityOf(((ContextWrapper) ctx).getBaseContext());
    return null;
  }

  public static int dp(Context ctx, float value) {
    return (int) (value * ctx.getResources().getDisplayMetrics().density);
  }

  public static void applyFullScreenWindow(Dialog dialog, Context ctx) {
    Window win = dialog.getWindow();
    if (win == null) return;

    win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    win.setDimAmount(0);
    win.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
    win.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    win.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    win.setStatusBarColor(Color.TRANSPARENT);

    int visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
    if (!LineTheme.isDark(ctx)) {
      visibility |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
    }
    win.getDecorView().setSystemUiVisibility(visibility);
    win.getDecorView().setPadding(0, 0, 0, 0);
    win.getDecorView().requestApplyInsets();
  }

  public static void padForSystemBars(View target) {
    target.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          if (Build.VERSION.SDK_INT >= 30) {
            padForApi30(v, insets);
          } else {
            padForLegacy(v, insets);
          }
          return insets;
        });
  }

  private static void padForApi30(View target, WindowInsets insets) {
    Insets bars =
        insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
    target.setPadding(bars.left, bars.top, bars.right, bars.bottom);
  }

  @SuppressWarnings("deprecation")
  private static void padForLegacy(View target, WindowInsets insets) {
    target.setPadding(
        insets.getSystemWindowInsetLeft(),
        insets.getSystemWindowInsetTop(),
        insets.getSystemWindowInsetRight(),
        insets.getSystemWindowInsetBottom());
  }

  public static void applyVisibility(View root, int viewId, int state) {
    View v = root.findViewById(viewId);
    if (v != null) v.setVisibility(state);
  }

  public static Drawable moduleIcon() {
    return ModuleResources.drawable("ic_knot");
  }

  public static void slide(View v, float toX) {
    slide(v, toX, PAGE_ANIM_MS, null);
  }

  public static void slide(View v, float toX, Runnable endAction) {
    slide(v, toX, PAGE_ANIM_MS, endAction);
  }

  public static void slide(View v, float toX, long durationMs, Runnable endAction) {
    ViewPropertyAnimator anim =
        v.animate()
            .translationX(toX)
            .setDuration(durationMs)
            .setInterpolator(new DecelerateInterpolator());
    if (endAction != null) anim.withEndAction(endAction);
    anim.start();
  }

  public static void sectionHeader(Context ctx, LinearLayout parent, String text) {
    try {
      LineVersion.Config cfg = LineVersion.get();
      View header = LayoutInflater.from(ctx).inflate(cfg.res.layoutSectionHeader, parent, false);
      if (header instanceof TextView) ((TextView) header).setText(text);
      header.setTag(TAG_SECTION_HEADER);
      parent.addView(header);
    } catch (Throwable ignored) {
    }
  }

  public static Row row(Context ctx, LinearLayout parent) {
    return new Row(ctx, parent);
  }

  static String searchTag(CharSequence title, CharSequence description) {
    String text = String.valueOf(title);
    if (description != null && description.length() > 0) text += " " + description;
    return text.toLowerCase();
  }

  public static final class Row {

    private final Context ctx;
    private final LinearLayout parent;

    private CharSequence title;
    private CharSequence description;
    private CharSequence value;
    private Integer titleColor;
    private boolean arrow = true;
    private String searchTag;
    private View.OnClickListener onClick;

    private Row(Context ctx, LinearLayout parent) {
      this.ctx = ctx;
      this.parent = parent;
    }

    public Row title(CharSequence title) {
      this.title = title;
      return this;
    }

    public Row description(CharSequence description) {
      this.description = description;
      return this;
    }

    public Row value(CharSequence value) {
      this.value = value;
      return this;
    }

    public Row titleColor(int color) {
      this.titleColor = color;
      return this;
    }

    public Row noArrow() {
      this.arrow = false;
      return this;
    }

    public Row searchTag(String searchTag) {
      this.searchTag = searchTag;
      return this;
    }

    public Row onClick(View.OnClickListener onClick) {
      this.onClick = onClick;
      return this;
    }

    public View add() {
      try {
        View row = LineTheme.createTextRow(ctx);
        if (row == null) return null;

        LineTheme.setRowTitle(row, title);
        if (hasDescription()) LineTheme.setRowDescription(row, description);
        LineTheme.setRowArrowVisible(row, arrow);
        LineTheme.setRowDividerVisible(row, false);
        if (titleColor != null) LineTheme.setRowTitleColor(row, titleColor);
        if (value != null) LineTheme.setRowValue(row, value);
        if (onClick != null) row.setOnClickListener(onClick);

        row.setTag(searchTag != null ? searchTag : SettingsViews.searchTag(title, description));
        parent.addView(row);
        return row;
      } catch (Throwable ignored) {
        return null;
      }
    }

    private boolean hasDescription() {
      return description != null && description.length() > 0;
    }
  }
}
