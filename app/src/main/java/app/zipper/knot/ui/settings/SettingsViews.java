package app.zipper.knot.ui.settings;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import app.zipper.knot.LineVersion;
import app.zipper.knot.utils.LineTheme;

public final class SettingsViews {

  public static final String TAG_SECTION_HEADER = "section_header";

  private static final String MODULE_PACKAGE = "app.zipper.knot";
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

  public static void applyVisibility(View root, int viewId, int state) {
    View v = root.findViewById(viewId);
    if (v != null) v.setVisibility(state);
  }

  public static Drawable moduleIcon(Context ctx) {
    try {
      Context modCtx = ctx.createPackageContext(MODULE_PACKAGE, Context.CONTEXT_IGNORE_SECURITY);
      int resId = modCtx.getResources().getIdentifier("ic_knot", "drawable", MODULE_PACKAGE);
      return resId == 0 ? null : modCtx.getDrawable(resId);
    } catch (Throwable ignored) {
      return null;
    }
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
