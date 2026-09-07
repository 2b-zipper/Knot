package app.zipper.knot;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.splashscreen.SplashScreen;

public class MainActivity extends Activity {

  private static final String LINE_PACKAGE = "jp.naver.line.android";
  private static final String REPO_URL = "https://github.com/2b-zipper/Knot";
  private static final String LICENSE_URL = REPO_URL + "/blob/main/LICENSE";
  private static final String CROWDIN_URL = "https://crowdin.com/project/knot";
  private static final String REPO_HOST = "github.com/2b-zipper/Knot";
  private static final String CROWDIN_HOST = "crowdin.com/project/knot";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    if (getActionBar() != null) getActionBar().hide();

    setDecorFitsSystemWindows();
    setContentView(buildContent());
    // Building the hierarchy and the splash exit both reset the bar appearance.
    applySystemBarAppearance();
    getWindow().getDecorView().post(this::applySystemBarAppearance);

    if (Build.VERSION.SDK_INT < 31) return;
    playSplashExit(splashScreen);
  }

  @SuppressWarnings("deprecation")
  private void setDecorFitsSystemWindows() {
    if (Build.VERSION.SDK_INT >= 30) {
      getWindow().setDecorFitsSystemWindows(false);
      return;
    }
    View decor = getWindow().getDecorView();
    decor.setSystemUiVisibility(
        decor.getSystemUiVisibility()
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
  }

  private void applySystemBarAppearance() {
    boolean lightBars = !isNightMode();
    if (Build.VERSION.SDK_INT >= 30) {
      applyBarAppearanceApi30(lightBars);
    } else {
      applyBarAppearanceLegacy(lightBars);
    }
  }

  private void applyBarAppearanceApi30(boolean lightBars) {
    WindowInsetsController controller = getWindow().getDecorView().getWindowInsetsController();
    if (controller == null) return;
    int lightMask =
        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
    controller.setSystemBarsAppearance(lightBars ? lightMask : 0, lightMask);
  }

  @SuppressWarnings("deprecation")
  private void applyBarAppearanceLegacy(boolean lightBars) {
    View decor = getWindow().getDecorView();
    int flags = decor.getSystemUiVisibility();
    int lightMask = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
    decor.setSystemUiVisibility(lightBars ? flags | lightMask : flags & ~lightMask);
  }

  private boolean isNightMode() {
    int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    return mode == Configuration.UI_MODE_NIGHT_YES;
  }

  private View buildContent() {
    LinearLayout content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setGravity(Gravity.CENTER_HORIZONTAL);
    content.addView(buildHeader());
    content.addView(buildActionGroup());
    content.addView(buildLinkGroup(), spacedAbove(dp(14)));
    applyInsetPadding(content);

    ScrollView scroller = new ScrollView(this);
    scroller.setBackgroundColor(color(R.color.knot_background));
    scroller.setFillViewport(true);
    scroller.addView(
        content,
        new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    return scroller;
  }

  private void applyInsetPadding(View target) {
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

  private void padForApi30(View target, WindowInsets insets) {
    android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
    padAroundBars(target, bars.left, bars.top, bars.right, bars.bottom);
  }

  @SuppressWarnings("deprecation")
  private void padForLegacy(View target, WindowInsets insets) {
    padAroundBars(
        target,
        insets.getSystemWindowInsetLeft(),
        insets.getSystemWindowInsetTop(),
        insets.getSystemWindowInsetRight(),
        insets.getSystemWindowInsetBottom());
  }

  private void padAroundBars(View target, int left, int top, int right, int bottom) {
    int side = dp(24);
    target.setPadding(side + left, dp(40) + top, side + right, dp(32) + bottom);
  }

  private LinearLayout buildHeader() {
    LinearLayout header = new LinearLayout(this);
    header.setOrientation(LinearLayout.VERTICAL);
    header.setGravity(Gravity.CENTER_HORIZONTAL);
    header.setPadding(0, 0, 0, dp(24));

    int iconRes = getResources().getIdentifier("ic_launcher", "mipmap", getPackageName());
    if (iconRes == 0) {
      iconRes = getResources().getIdentifier("ic_knot", "drawable", getPackageName());
    }
    if (iconRes != 0) {
      ImageView icon = new ImageView(this);
      icon.setImageResource(iconRes);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(72), dp(72));
      lp.bottomMargin = dp(14);
      header.addView(icon, lp);
    }

    TextView title = centeredText(getString(R.string.main_title), 22, R.color.knot_text_primary);
    title.setTypeface(null, Typeface.BOLD);
    header.addView(title);

    TextView version =
        centeredText("v" + BuildConfig.VERSION_NAME, 13, R.color.knot_text_secondary);
    header.addView(version, spacedAbove(dp(4)));

    TextView hint =
        centeredText(getString(R.string.main_setup_hint), 14, R.color.knot_text_secondary);
    hint.setLineSpacing(0, 1.3f);
    header.addView(hint, spacedAbove(dp(18)));

    return header;
  }

  private TextView centeredText(CharSequence text, int textSize, int colorRes) {
    TextView view = new TextView(this);
    view.setText(text);
    view.setTextSize(textSize);
    view.setTextColor(color(colorRes));
    view.setGravity(Gravity.CENTER);
    return view;
  }

  private LinearLayout.LayoutParams spacedAbove(int topMargin) {
    LinearLayout.LayoutParams lp =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lp.topMargin = topMargin;
    return lp;
  }

  private LinearLayout buildActionGroup() {
    return buildRowGroup(accentRow(getString(R.string.main_open_line), v -> openLine()));
  }

  private LinearLayout buildLinkGroup() {
    return buildRowGroup(
        infoRow(
            getString(R.string.main_row_settings_title),
            getString(R.string.main_row_settings_desc)),
        linkRow(getString(R.string.about_link_repo), REPO_HOST, REPO_URL),
        linkRow(getString(R.string.about_link_translate), CROWDIN_HOST, CROWDIN_URL),
        linkRow(
            getString(R.string.about_link_license),
            getString(R.string.about_license_value),
            LICENSE_URL));
  }

  private LinearLayout buildRowGroup(View... rows) {
    LinearLayout group = new LinearLayout(this);
    group.setOrientation(LinearLayout.VERTICAL);

    GradientDrawable bg = new GradientDrawable();
    bg.setColor(color(R.color.knot_surface));
    bg.setCornerRadius(dp(16));
    group.setBackground(bg);
    group.setClipToOutline(true);

    for (int i = 0; i < rows.length; i++) {
      if (i > 0) group.addView(buildDivider());
      group.addView(rows[i]);
    }
    return group;
  }

  private View buildDivider() {
    View divider = new View(this);
    divider.setBackgroundColor(color(R.color.knot_stroke));
    LinearLayout.LayoutParams lp =
        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1) / 2));
    lp.setMarginStart(dp(18));
    divider.setLayoutParams(lp);
    return divider;
  }

  private View accentRow(CharSequence title, View.OnClickListener onClick) {
    View row = buildRow(title, null, R.color.knot_accent, true);
    makeTappable(row, onClick);
    return row;
  }

  private View linkRow(CharSequence title, CharSequence description, String url) {
    View row = buildRow(title, description, R.color.knot_text_primary, false);
    makeTappable(row, v -> openUrl(url));
    return row;
  }

  private View infoRow(CharSequence title, CharSequence description) {
    return buildRow(title, description, R.color.knot_text_secondary, false);
  }

  private View buildRow(
      CharSequence title, CharSequence description, int titleColorRes, boolean bold) {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.VERTICAL);
    row.setPadding(dp(18), dp(13), dp(18), dp(13));

    TextView label = new TextView(this);
    label.setText(title);
    label.setTextSize(16);
    label.setTextColor(color(titleColorRes));
    if (bold) label.setTypeface(null, Typeface.BOLD);
    row.addView(label);

    if (description != null) {
      TextView sub = new TextView(this);
      sub.setText(description);
      sub.setTextSize(13);
      sub.setTextColor(color(R.color.knot_text_secondary));
      sub.setLineSpacing(0, 1.25f);
      row.addView(sub, spacedAbove(dp(3)));
    }
    return row;
  }

  private void makeTappable(View row, View.OnClickListener onClick) {
    row.setBackground(
        new RippleDrawable(
            ColorStateList.valueOf(color(R.color.knot_ripple)),
            null,
            new ColorDrawable(0xFFFFFFFF)));
    row.setOnClickListener(onClick);
  }

  private void openUrl(String url) {
    try {
      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(intent);
    } catch (Throwable ignored) {
    }
  }

  private void openLine() {
    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(LINE_PACKAGE);
    if (launchIntent != null) {
      startActivity(launchIntent);
    } else {
      Toast.makeText(this, R.string.main_line_not_installed, Toast.LENGTH_SHORT).show();
    }
  }

  private void playSplashExit(SplashScreen splashScreen) {
    long startMs = SystemClock.elapsedRealtime();
    splashScreen.setKeepOnScreenCondition(() -> SystemClock.elapsedRealtime() - startMs < 1100);

    splashScreen.setOnExitAnimationListener(
        provider -> {
          View splashIcon;
          FrameLayout overlay;
          try {
            // Launches that hand over a splash without an icon make this throw, not return null.
            splashIcon = provider.getIconView();
            overlay = (FrameLayout) provider.getView();
          } catch (Throwable t) {
            provider.remove();
            applySystemBarAppearance();
            return;
          }

          TextView label = new TextView(this);
          label.setText(R.string.app_name);
          label.setTextSize(40);
          label.setLetterSpacing(0.03f);
          label.setTextColor(color(R.color.knot_text_primary));
          label.setTypeface(null, Typeface.BOLD);
          label.setAlpha(0f);
          label.measure(
              View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
              View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

          float density = getResources().getDisplayMetrics().density;
          float gap = 10 * density;
          int textW = label.getMeasuredWidth();
          if (textW == 0) textW = Math.round(90 * density);
          float logoHalfW = splashIcon.getWidth() * 0.389f * 0.30f;
          float logoTX = -(gap + textW) / 2f;
          float textTX = gap / 2f + logoHalfW;

          label.setTranslationX(textTX);
          overlay.addView(
              label,
              new FrameLayout.LayoutParams(
                  FrameLayout.LayoutParams.WRAP_CONTENT,
                  FrameLayout.LayoutParams.WRAP_CONTENT,
                  Gravity.CENTER));

          splashIcon
              .animate()
              .scaleX(0.30f)
              .scaleY(0.30f)
              .translationX(logoTX)
              .setDuration(350)
              .setInterpolator(new DecelerateInterpolator(1.6f))
              .start();

          label
              .animate()
              .alpha(1f)
              .setStartDelay(80)
              .setDuration(250)
              .withEndAction(
                  () ->
                      overlay.postDelayed(
                          () -> {
                            splashIcon.animate().alpha(0f).setDuration(250).start();
                            overlay
                                .animate()
                                .alpha(0f)
                                .setDuration(250)
                                .withEndAction(
                                    () ->
                                        overlay.post(
                                            () -> {
                                              provider.remove();
                                              applySystemBarAppearance();
                                            }))
                                .start();
                          },
                          750))
              .start();
        });
  }

  private int color(int resId) {
    return getResources().getColor(resId, getTheme());
  }

  private int dp(int value) {
    return (int) (value * getResources().getDisplayMetrics().density);
  }
}
