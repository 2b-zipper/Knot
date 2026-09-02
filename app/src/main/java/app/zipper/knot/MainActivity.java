package app.zipper.knot;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.splashscreen.SplashScreen;

public class MainActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    if (getActionBar() != null) getActionBar().hide();

    ScrollView mainScroll = new ScrollView(this);
    mainScroll.setBackgroundColor(Color.parseColor("#F5F6F8"));

    LinearLayout parentLayout = new LinearLayout(this);
    parentLayout.setOrientation(LinearLayout.VERTICAL);
    parentLayout.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
    int margin = toPixels(24);
    parentLayout.setPadding(margin, toPixels(48), margin, margin);

    try {
      ImageView iconView = new ImageView(this);
      int resId = getResources().getIdentifier("ic_launcher", "mipmap", getPackageName());
      if (resId == 0) resId = getResources().getIdentifier("ic_knot", "drawable", getPackageName());
      if (resId != 0) {
        iconView.setImageResource(resId);
        LinearLayout.LayoutParams iconLp =
            new LinearLayout.LayoutParams(toPixels(80), toPixels(80));
        iconLp.bottomMargin = toPixels(16);
        iconView.setLayoutParams(iconLp);
        parentLayout.addView(iconView);
      }
    } catch (Throwable ignored) {
    }

    TextView titleLabel = new TextView(this);
    titleLabel.setText(R.string.main_title);
    titleLabel.setTextSize(26);
    titleLabel.setTextColor(Color.parseColor("#222222"));
    titleLabel.setTypeface(null, Typeface.BOLD);
    titleLabel.setGravity(Gravity.CENTER);
    titleLabel.setPadding(0, 0, 0, toPixels(32));
    parentLayout.addView(titleLabel);

    LinearLayout cardLayout = new LinearLayout(this);
    cardLayout.setOrientation(LinearLayout.VERTICAL);
    cardLayout.setPadding(toPixels(24), toPixels(32), toPixels(24), toPixels(32));

    GradientDrawable cardBg = new GradientDrawable();
    cardBg.setColor(Color.WHITE);
    cardBg.setCornerRadius(toPixels(20));
    cardBg.setStroke(toPixels(1), Color.parseColor("#EAEAEA"));
    cardLayout.setBackground(cardBg);

    TextView descLabel = new TextView(this);
    descLabel.setText(R.string.main_description);
    descLabel.setTextSize(15);
    descLabel.setTextColor(Color.parseColor("#555555"));
    descLabel.setLineSpacing(0, 1.4f);
    descLabel.setGravity(Gravity.CENTER);
    cardLayout.addView(descLabel);

    parentLayout.addView(cardLayout);

    Button openLineBtn = new Button(this);
    openLineBtn.setText(R.string.main_open_line);
    openLineBtn.setTextColor(Color.WHITE);
    openLineBtn.setTextSize(16);
    openLineBtn.setTypeface(null, Typeface.BOLD);
    openLineBtn.setAllCaps(false);

    GradientDrawable btnBg = new GradientDrawable();
    btnBg.setColor(Color.parseColor("#06C755"));
    btnBg.setCornerRadius(toPixels(30));
    openLineBtn.setBackground(btnBg);

    LinearLayout.LayoutParams btnLp =
        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, toPixels(54));
    btnLp.topMargin = toPixels(40);
    openLineBtn.setLayoutParams(btnLp);

    openLineBtn.setOnClickListener(
        v -> {
          Intent launchIntent =
              getPackageManager().getLaunchIntentForPackage("jp.naver.line.android");
          if (launchIntent != null) {
            startActivity(launchIntent);
          } else {
            android.widget.Toast.makeText(
                    this, R.string.main_line_not_installed, android.widget.Toast.LENGTH_SHORT)
                .show();
          }
        });

    parentLayout.addView(openLineBtn);

    mainScroll.addView(parentLayout);
    setContentView(mainScroll);

    if (Build.VERSION.SDK_INT < 31) return;

    long startMs = SystemClock.elapsedRealtime();
    splashScreen.setKeepOnScreenCondition(() -> SystemClock.elapsedRealtime() - startMs < 1100);

    splashScreen.setOnExitAnimationListener(
        provider -> {
          View splashIcon = provider.getIconView();
          FrameLayout overlay = (FrameLayout) provider.getView();

          TextView label = new TextView(this);
          label.setText(R.string.app_name);
          label.setTextSize(40);
          label.setLetterSpacing(0.03f);
          label.setTextColor(Color.parseColor("#333333"));
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
                                .withEndAction(() -> overlay.post(provider::remove))
                                .start();
                          },
                          750))
              .start();
        });
  }

  private int toPixels(int dp) {
    return (int) (dp * getResources().getDisplayMetrics().density);
  }
}
