package app.zipper.knot.ui.settings;

import static app.zipper.knot.ui.settings.SettingsViews.dp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import app.zipper.knot.BuildConfig;
import app.zipper.knot.utils.ContributorProfiles;
import app.zipper.knot.utils.LineTheme;
import app.zipper.knot.utils.ModuleStrings;
import java.util.ArrayList;
import java.util.List;

public final class AboutPage {

  private static final String REPO_URL = "https://github.com/2b-zipper/Knot";
  private static final String LICENSE_URL = REPO_URL + "/blob/main/LICENSE";
  private static final String[][] CONTRIBUTOR_SECTIONS = {
    {ModuleStrings.ABOUT_SEC_DEVELOPERS, "2b-zipper", "Nich87"},
    {ModuleStrings.ABOUT_SEC_CONTRIBUTORS, "atuy1219"},
  };

  private final Context ctx;
  private final LinearLayout root;

  private AboutPage(Context ctx) {
    this.ctx = ctx;
    this.root = new LinearLayout(ctx);
  }

  public static View build(Context ctx) {
    return new AboutPage(ctx).render();
  }

  public static String[] contributorHandles() {
    List<String> handles = new ArrayList<>();
    for (String[] section : CONTRIBUTOR_SECTIONS) {
      for (int i = 1; i < section.length; i++) handles.add(section[i]);
    }
    return handles.toArray(new String[0]);
  }

  private View render() {
    int bg = LineTheme.backgroundColor(ctx);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(bg);
    root.setPadding(0, 0, 0, dp(ctx, 64));

    root.addView(buildHero());

    for (String[] section : CONTRIBUTOR_SECTIONS) {
      SettingsViews.sectionHeader(ctx, root, section[0]);
      for (int i = 1; i < section.length; i++) {
        addContributorRow(section[i]);
      }
    }

    addLinks();
    root.addView(buildDisclaimer());

    ScrollView scroller = new ScrollView(ctx);
    scroller.setBackgroundColor(bg);
    scroller.addView(root);
    return scroller;
  }

  private LinearLayout buildHero() {
    LinearLayout hero = new LinearLayout(ctx);
    hero.setOrientation(LinearLayout.VERTICAL);
    hero.setGravity(Gravity.CENTER_HORIZONTAL);
    hero.setPadding(dp(ctx, 32), dp(ctx, 32), dp(ctx, 32), dp(ctx, 28));

    Drawable icon = SettingsViews.moduleIcon(ctx);
    if (icon != null) {
      ImageView logo = new ImageView(ctx);
      logo.setImageDrawable(icon);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(ctx, 84), dp(ctx, 84));
      lp.bottomMargin = dp(ctx, 16);
      logo.setLayoutParams(lp);
      hero.addView(logo);
    }

    TextView name = centeredLabel(ModuleStrings.BRAND_NAME, 26, LineTheme.primaryTextColor(ctx), 0);
    name.setTypeface(null, Typeface.BOLD);
    hero.addView(name);

    int subColor = LineTheme.secondaryTextColor(ctx);
    hero.addView(centeredLabel("v" + BuildConfig.VERSION_NAME, 13, subColor, 4));
    hero.addView(centeredLabel(ModuleStrings.ABOUT_TAGLINE, 13, subColor, 10));

    return hero;
  }

  private TextView centeredLabel(CharSequence text, int textSize, int color, int topMarginDp) {
    TextView label = new TextView(ctx);
    label.setText(text);
    label.setTextSize(textSize);
    label.setTextColor(color);
    label.setGravity(Gravity.CENTER_HORIZONTAL);
    if (topMarginDp > 0) {
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
      lp.topMargin = dp(ctx, topMarginDp);
      label.setLayoutParams(lp);
    }
    return label;
  }

  private TextView buildDisclaimer() {
    TextView disclaimer = new TextView(ctx);
    disclaimer.setText(ModuleStrings.ABOUT_DISCLAIMER);
    disclaimer.setTextSize(12);
    disclaimer.setTextColor(LineTheme.secondaryTextColor(ctx));
    disclaimer.setGravity(Gravity.CENTER_HORIZONTAL);

    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.topMargin = dp(ctx, 28);
    lp.leftMargin = dp(ctx, 24);
    lp.rightMargin = dp(ctx, 24);
    disclaimer.setLayoutParams(lp);
    return disclaimer;
  }

  private void addLinks() {
    SettingsViews.sectionHeader(ctx, root, ModuleStrings.ABOUT_SEC_LINKS);
    SettingsViews.row(ctx, root)
        .title(ModuleStrings.ABOUT_LINK_REPO)
        .description("github.com/2b-zipper/Knot")
        .onClick(v -> openUrl(REPO_URL))
        .add();
    SettingsViews.row(ctx, root)
        .title(ModuleStrings.ABOUT_LINK_LICENSE)
        .description(ModuleStrings.ABOUT_LICENSE_VALUE)
        .onClick(v -> openUrl(LICENSE_URL))
        .add();
  }

  private void addContributorRow(String handle) {
    try {
      LinearLayout wrapper = new LinearLayout(ctx);
      wrapper.setOrientation(LinearLayout.HORIZONTAL);
      wrapper.setGravity(Gravity.CENTER_VERTICAL);
      wrapper.setOnClickListener(v -> openUrl("https://github.com/" + handle));

      ImageView avatar = buildAvatar();
      wrapper.addView(avatar);

      View row = SettingsViews.row(ctx, wrapper).title(handle).description("@" + handle).add();
      if (row == null) return;
      row.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
      moveHighlightToWrapper(row, wrapper);

      root.addView(wrapper);
      ContributorProfiles.loadAvatar(avatar, handle);
      ContributorProfiles.loadDisplayName(handle, name -> LineTheme.setRowTitle(row, name));
    } catch (Throwable ignored) {
    }
  }

  private ImageView buildAvatar() {
    int size = dp(ctx, 40);
    ImageView avatar = new ImageView(ctx);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
    lp.setMarginStart(dp(ctx, 20));
    avatar.setLayoutParams(lp);
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
    return avatar;
  }

  // The pressed state drawn by the row itself has to cover the avatar as well
  private void moveHighlightToWrapper(View row, View wrapper) {
    int containerId =
        ctx.getResources().getIdentifier("setting_item_container", "id", "jp.naver.line.android");
    View container = containerId != 0 ? row.findViewById(containerId) : null;
    if (container == null) return;

    Drawable bg = container.getBackground();
    if (bg == null || bg.getConstantState() == null) return;

    container.setBackground(null);
    wrapper.setBackground(bg.getConstantState().newDrawable().mutate());
  }

  private void openUrl(String url) {
    try {
      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      ctx.startActivity(intent);
    } catch (Throwable ignored) {
    }
  }
}
