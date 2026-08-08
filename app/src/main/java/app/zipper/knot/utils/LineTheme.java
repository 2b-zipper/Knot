package app.zipper.knot.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import app.zipper.knot.LineVersion;
import app.zipper.knot.Reflect;

// Reads colors off an inflated, self-theming LINE settings row (LINE themes at runtime).
public final class LineTheme {

  private static final String LINE_PKG = "jp.naver.line.android";
  private static final long SAMPLE_TTL_MS = 750L;

  private static volatile long resolvedAt = 0L;
  private static volatile boolean ready = false;
  private static volatile int cBackground = 0;
  private static volatile int cPrimaryText = 0;
  private static volatile int cSecondaryText = 0;

  private LineTheme() {}

  public static void invalidate() {
    ready = false;
    resolvedAt = 0L;
    cBackground = cPrimaryText = cSecondaryText = 0;
  }

  public static int backgroundColor(Context ctx) {
    ensure(ctx);
    return cBackground != 0 ? cBackground : (nightConfig(ctx) ? 0xFF111111 : Color.WHITE);
  }

  public static int primaryTextColor(Context ctx) {
    ensure(ctx);
    return cPrimaryText != 0 ? cPrimaryText : (isDark(ctx) ? Color.WHITE : 0xFF111111);
  }

  public static int secondaryTextColor(Context ctx) {
    ensure(ctx);
    return cSecondaryText != 0 ? cSecondaryText : (isDark(ctx) ? Color.GRAY : Color.DKGRAY);
  }

  public static int linkColor(Context ctx) {
    return semantic(ctx, "primaryLink", 0xFF4D73FF);
  }

  public static int accentGreen(Context ctx) {
    return semantic(ctx, "linelime600", 0xFF06C755);
  }

  public static int fieldColor(Context ctx) {
    int bg = backgroundColor(ctx);
    return isDark(ctx) ? lighten(bg, 0.09f) : darken(bg, 0.045f);
  }

  public static int cardColor(Context ctx) {
    int bg = backgroundColor(ctx);
    return isDark(ctx) ? lighten(bg, 0.05f) : darken(bg, 0.03f);
  }

  public static boolean isDark(Context ctx) {
    int bg = backgroundColor(ctx);
    double luminance =
        (0.2126 * Color.red(bg) + 0.7152 * Color.green(bg) + 0.0722 * Color.blue(bg)) / 255.0;
    return luminance < 0.5;
  }

  public static int dialogTheme(Context ctx) {
    return isDark(ctx)
        ? AlertDialog.THEME_DEVICE_DEFAULT_DARK
        : AlertDialog.THEME_DEVICE_DEFAULT_LIGHT;
  }

  private static void ensure(Context ctx) {
    if (ready && System.currentTimeMillis() - resolvedAt < SAMPLE_TTL_MS) return;
    cBackground = cPrimaryText = cSecondaryText = 0;
    try {
      View row = createTextRow(ctx);
      if (row != null) {
        cPrimaryText = textColorOf(ctx, row, "setting_title");
        cSecondaryText = textColorOf(ctx, row, "setting_description");
        cBackground = surfaceColorOf(ctx, row);
      }
    } catch (Throwable ignored) {
    }
    ready = true;
    resolvedAt = System.currentTimeMillis();
  }

  private static int textColorOf(Context ctx, View row, String idName) {
    try {
      int id = ctx.getResources().getIdentifier(idName, "id", LINE_PKG);
      if (id != 0) {
        View tv = row.findViewById(id);
        if (tv instanceof TextView) return ((TextView) tv).getCurrentTextColor();
      }
    } catch (Throwable ignored) {
    }
    return 0;
  }

  private static int surfaceColorOf(Context ctx, View row) {
    try {
      int id = ctx.getResources().getIdentifier("setting_item_container", "id", LINE_PKG);
      View container = id != 0 ? row.findViewById(id) : null;
      Drawable bg = (container != null ? container : row).getBackground();
      if (bg == null) return 0;
      if (bg instanceof ColorDrawable) return ((ColorDrawable) bg).getColor();

      Bitmap bmp = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888);
      bg.setBounds(0, 0, 8, 8);
      bg.draw(new Canvas(bmp));
      int px = bmp.getPixel(4, 4);
      bmp.recycle();
      if (Color.alpha(px) == 255) return px;
    } catch (Throwable ignored) {
    }
    return 0;
  }

  private static int semantic(Context ctx, String name, int fallback) {
    try {
      int id = ctx.getResources().getIdentifier(name, "color", LINE_PKG);
      if (id != 0) return ctx.getColor(id);
    } catch (Throwable ignored) {
    }
    return fallback;
  }

  private static boolean nightConfig(Context ctx) {
    if (ctx == null) return false;
    try {
      int mode = ctx.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
      return mode == Configuration.UI_MODE_NIGHT_YES;
    } catch (Throwable ignored) {
      return false;
    }
  }

  public static View createTextRow(Context ctx) {
    try {
      LineVersion.Config cfg = LineVersion.get();
      if (cfg == null || cfg.settings.textItemViewClass.isEmpty()) return null;
      Class<?> cls = Reflect.findClass(cfg.settings.textItemViewClass, ctx.getClassLoader());
      View row = (View) Reflect.newInstance(cls, ctx);
      row.setLayoutParams(
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
      return row;
    } catch (Throwable t) {
      return null;
    }
  }

  public static void setRowTitle(View row, CharSequence text) {
    invokeRow(row, LineVersion.get().settings.methodRowSetTitleText, text);
  }

  public static void setRowDescription(View row, CharSequence desc) {
    invokeRow(row, LineVersion.get().settings.methodRowSetDescription, desc);
  }

  public static void setRowArrowVisible(View row, boolean visible) {
    invokeRow(row, LineVersion.get().settings.methodRowSetArrowVisible, visible);
  }

  public static void setRowDividerVisible(View row, boolean visible) {
    invokeRow(row, LineVersion.get().settings.methodRowSetDividerVisible, visible);
  }

  public static void setRowTitleColor(View row, int color) {
    invokeRow(row, LineVersion.get().settings.methodRowSetTitleColor, Integer.valueOf(color));
  }

  public static void setRowValue(View row, CharSequence value) {
    if (row == null) return;
    try {
      Context ctx = row.getContext();
      int id = ctx.getResources().getIdentifier("setting_inlined_value", "id", LINE_PKG);
      if (id == 0) return;
      View v = row.findViewById(id);
      if (v instanceof TextView) {
        ((TextView) v).setText(value);
        v.setVisibility(value != null && value.length() > 0 ? View.VISIBLE : View.GONE);
      }
    } catch (Throwable ignored) {
    }
  }

  private static void invokeRow(View row, String method, Object arg) {
    if (row == null || method == null || method.isEmpty()) return;
    try {
      Reflect.callMethod(row, method, arg);
    } catch (Throwable ignored) {
    }
  }

  public static void tintTextAndIcons(View view, int color) {
    if (view == null) return;
    if (view instanceof ViewGroup) {
      ViewGroup vg = (ViewGroup) view;
      for (int i = 0; i < vg.getChildCount(); i++) {
        tintTextAndIcons(vg.getChildAt(i), color);
      }
    } else if (view instanceof TextView) {
      ((TextView) view).setTextColor(color);
    } else if (view instanceof ImageView) {
      ((ImageView) view).setColorFilter(color);
    }
  }

  public static void applyDialogColors(AlertDialog dialog, Context ctx) {
    if (dialog == null || ctx == null) return;
    try {
      int text = primaryTextColor(ctx);
      int accent = linkColor(ctx);
      float density = ctx.getResources().getDisplayMetrics().density;

      Window w = dialog.getWindow();
      if (w != null) {
        GradientDrawable rounded = new GradientDrawable();
        rounded.setColor(backgroundColor(ctx));
        rounded.setCornerRadius(20 * density);
        w.setBackgroundDrawable(new InsetDrawable((Drawable) rounded, (int) (16 * density)));
      }

      int titleId = ctx.getResources().getIdentifier("alertTitle", "id", "android");
      View title = titleId != 0 ? dialog.findViewById(titleId) : null;
      if (title instanceof TextView) ((TextView) title).setTextColor(text);
      View message = dialog.findViewById(android.R.id.message);
      if (message instanceof TextView) ((TextView) message).setTextColor(text);

      for (int which :
          new int[] {
            DialogInterface.BUTTON_POSITIVE,
            DialogInterface.BUTTON_NEGATIVE,
            DialogInterface.BUTTON_NEUTRAL
          }) {
        Button button = dialog.getButton(which);
        if (button != null) button.setTextColor(accent);
      }
    } catch (Throwable ignored) {
    }
  }

  private static int lighten(int color, float fraction) {
    return blend(color, Color.WHITE, fraction);
  }

  private static int darken(int color, float fraction) {
    return blend(color, Color.BLACK, fraction);
  }

  private static int blend(int color, int towards, float fraction) {
    int r = (int) (Color.red(color) + (Color.red(towards) - Color.red(color)) * fraction);
    int g = (int) (Color.green(color) + (Color.green(towards) - Color.green(color)) * fraction);
    int b = (int) (Color.blue(color) + (Color.blue(towards) - Color.blue(color)) * fraction);
    return Color.argb(Color.alpha(color), clamp(r), clamp(g), clamp(b));
  }

  private static int clamp(int v) {
    return Math.max(0, Math.min(255, v));
  }
}
