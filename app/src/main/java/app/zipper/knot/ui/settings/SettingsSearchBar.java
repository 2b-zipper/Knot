package app.zipper.knot.ui.settings;

import static app.zipper.knot.ui.settings.SettingsViews.dp;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import app.zipper.knot.utils.LineTheme;
import app.zipper.knot.utils.ModuleStrings;
import java.util.function.Consumer;

final class SettingsSearchBar {

  private final RelativeLayout container;
  private final EditText input;

  SettingsSearchBar(Context ctx, Consumer<String> onQueryChanged) {
    container = new RelativeLayout(ctx);
    LinearLayout.LayoutParams containerLp = new LinearLayout.LayoutParams(-1, -2);
    int margin = dp(ctx, 12);
    containerLp.setMargins(margin, margin / 2, margin, margin / 2);
    container.setLayoutParams(containerLp);

    input = buildInput(ctx);
    container.addView(input);

    ImageView clearButton = buildClearButton(ctx);
    container.addView(clearButton);
    clearButton.setOnClickListener(v -> input.setText(""));

    input.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            String query = s.toString().toLowerCase();
            clearButton.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
            onQueryChanged.accept(query);
          }

          @Override
          public void afterTextChanged(Editable s) {}
        });
  }

  View view() {
    return container;
  }

  String query() {
    return input.getText().toString().toLowerCase();
  }

  private static EditText buildInput(Context ctx) {
    EditText input = new EditText(ctx);
    input.setHint(ModuleStrings.SETTINGS_SEARCH_HINT);
    input.setSingleLine(true);
    input.setTextSize(14);
    input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
    input.setPadding(dp(ctx, 16), dp(ctx, 8), dp(ctx, 40), dp(ctx, 8));

    GradientDrawable background = new GradientDrawable();
    background.setColor(LineTheme.fieldColor(ctx));
    background.setCornerRadius(dp(ctx, 20));
    input.setBackground(background);

    input.setTextColor(LineTheme.primaryTextColor(ctx));
    input.setHintTextColor(LineTheme.secondaryTextColor(ctx));
    input.setLayoutParams(new RelativeLayout.LayoutParams(-1, -2));
    return input;
  }

  private static ImageView buildClearButton(Context ctx) {
    ImageView clearButton = new ImageView(ctx);
    clearButton.setImageDrawable(buildClearIcon(ctx));
    clearButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
    clearButton.setVisibility(View.GONE);

    int size = dp(ctx, 32);
    int padding = dp(ctx, 11);
    clearButton.setPadding(padding, padding, padding, padding);
    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(size, size);
    lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
    lp.addRule(RelativeLayout.CENTER_VERTICAL);
    lp.rightMargin = dp(ctx, 6);
    clearButton.setLayoutParams(lp);
    return clearButton;
  }

  private static Drawable buildClearIcon(Context ctx) {
    Path cross = new Path();
    cross.moveTo(0, 0);
    cross.lineTo(10, 10);
    cross.moveTo(10, 0);
    cross.lineTo(0, 10);

    ShapeDrawable icon = new ShapeDrawable(new PathShape(cross, 10, 10));
    icon.setIntrinsicWidth(dp(ctx, 10));
    icon.setIntrinsicHeight(dp(ctx, 10));

    Paint paint = icon.getPaint();
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(1.2f);
    paint.setStrokeCap(Paint.Cap.ROUND);
    paint.setAntiAlias(true);
    paint.setColor(LineTheme.secondaryTextColor(ctx));
    return icon;
  }
}
