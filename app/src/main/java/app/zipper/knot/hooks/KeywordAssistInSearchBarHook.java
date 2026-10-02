package app.zipper.knot.hooks;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.Reflect;
import app.zipper.knot.ui.settings.SettingsViews;
import app.zipper.knot.utils.ModuleResources;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

public class KeywordAssistInSearchBarHook implements BaseHook {

  private static final String BUTTON_TAG = "knot_keyword_assist";
  private static final float BUTTON_DP = 28f;
  private static final float BUTTON_END_DP = 4f;
  private static final float ICON_PADDING_DP = 4f;
  // A toggle refers to its row, so a strong value would keep the key from being collected.
  private static final Map<View, WeakReference<Toggle>> toggles = new WeakHashMap<>();

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    if (!config.keywordAssistInSearchBar.enabled) return;
    LineVersion.Config version = LineVersion.get();
    if (version == null || version.keywordAssist.toggleViewClass.isEmpty()) return;
    LineVersion.Config.KeywordAssist cfg = version.keywordAssist;
    Class<?> rowClass = Reflect.findClass(cfg.toggleViewClass, lpparam.classLoader);

    Knot.hookAllCtors(
        rowClass,
        chain -> {
          Object result = chain.proceed();
          View row = (View) chain.getThisObject();
          row.addOnAttachStateChangeListener(
              new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                  install(row, cfg);
                }

                @Override
                public void onViewDetachedFromWindow(View v) {}
              });
          return result;
        });

    Knot.module
        .hook(Reflect.findMethodExact(rowClass, cfg.methodSetChecked, boolean.class))
        .intercept(
            chain -> {
              Object result = chain.proceed();
              Toggle toggle = toggleOf(chain.getThisObject());
              if (toggle != null) toggle.render((boolean) chain.getArg(0));
              return result;
            });
  }

  private static void install(View row, LineVersion.Config.KeywordAssist cfg) {
    if (!Main.options.keywordAssistInSearchBar.enabled
        || toggleOf(row) != null
        || !(row.getParent() instanceof ViewGroup)) return;

    try {
      Context context = row.getContext();
      SearchBar bar = SearchBar.find((ViewGroup) row.getParent(), cfg);
      int onColorId = hostId(context, cfg.resOnColor, "color");
      if (bar == null || onColorId == 0) {
        Knot.log("Knot: KeywordAssistInSearchBar could not find the in-chat search bar.");
        return;
      }

      ImageView button = bar.container.findViewWithTag(BUTTON_TAG);
      if (button == null) button = bar.addButton(hostString(context, cfg.resTitle));

      // Collapse the row instead of hiding it: its composition still shows the assist popups.
      ViewGroup.LayoutParams lp = row.getLayoutParams();
      lp.height = 0;
      row.setLayoutParams(lp);

      Toggle toggle =
          new Toggle(
              row,
              cfg,
              button,
              ColorStateList.valueOf(context.getColor(onColorId)),
              bar.searchIcon.getImageTintList());
      toggles.put(row, new WeakReference<>(toggle));
      toggle.render((boolean) Reflect.callMethod(row, cfg.methodGetChecked));
      Knot.log("Knot: KeywordAssistInSearchBar moved the toggle into the in-chat search bar.");
    } catch (Throwable t) {
      Knot.log("Knot: KeywordAssistInSearchBar install failed", t);
    }
  }

  private static Toggle toggleOf(Object row) {
    WeakReference<Toggle> ref = toggles.get(row);
    return ref == null ? null : ref.get();
  }

  private static View findView(ViewGroup root, String name) {
    int id = hostId(root.getContext(), name, "id");
    return id == 0 ? null : root.findViewById(id);
  }

  private static String hostString(Context context, String name) {
    int id = hostId(context, name, "string");
    return id == 0 ? null : context.getString(id);
  }

  private static int hostId(Context context, String name, String type) {
    return context.getResources().getIdentifier(name, type, context.getPackageName());
  }

  private static final class SearchBar {
    final ViewGroup container;
    final View background;
    final View input;
    final View clearButton;
    final ImageView searchIcon;

    private SearchBar(
        ViewGroup container, View background, View input, View clearButton, ImageView searchIcon) {
      this.container = container;
      this.background = background;
      this.input = input;
      this.clearButton = clearButton;
      this.searchIcon = searchIcon;
    }

    static SearchBar find(ViewGroup header, LineVersion.Config.KeywordAssist cfg) {
      View background = findView(header, cfg.resSearchBarBg);
      View input = findView(header, cfg.resSearchBarInput);
      View clearButton = findView(header, cfg.resSearchBarCancel);
      View searchIcon = findView(header, cfg.resSearchBarIcon);
      if (background == null
          || input == null
          || clearButton == null
          || !(searchIcon instanceof ImageView)
          || !(background.getParent() instanceof ViewGroup)) return null;
      return new SearchBar(
          (ViewGroup) background.getParent(),
          background,
          input,
          clearButton,
          (ImageView) searchIcon);
    }

    // The bar's constraint fields are obfuscated, so the button is placed by hand and the clear
    // button and the text make room for it.
    ImageView addButton(String description) {
      Context context = container.getContext();
      int size = SettingsViews.dp(context, BUTTON_DP);
      int padding = SettingsViews.dp(context, ICON_PADDING_DP);
      int endInset = SettingsViews.dp(context, BUTTON_END_DP);

      ImageView button = new ImageView(context);
      button.setTag(BUTTON_TAG);
      button.setImageDrawable(ModuleResources.drawable("ic_keyword_assist"));
      button.setPadding(padding, padding, padding, padding);
      button.setContentDescription(description);
      container.addView(button, new ViewGroup.LayoutParams(size, size));

      input.setPaddingRelative(
          input.getPaddingStart(),
          input.getPaddingTop(),
          input.getPaddingEnd() + size,
          input.getPaddingBottom());
      clearButton.setTranslationX(-size);

      Runnable place =
          () -> {
            button.setX(background.getRight() - endInset - size);
            button.setY(background.getTop() + (background.getHeight() - size) / 2f);
          };
      background.addOnLayoutChangeListener((v, l, t, r, b, oldL, oldT, oldR, oldB) -> place.run());
      background.post(place);
      return button;
    }
  }

  private static final class Toggle {
    final View row;
    final LineVersion.Config.KeywordAssist cfg;
    final ImageView button;
    final ColorStateList on;
    final ColorStateList off;

    Toggle(
        View row,
        LineVersion.Config.KeywordAssist cfg,
        ImageView button,
        ColorStateList on,
        ColorStateList off) {
      this.row = row;
      this.cfg = cfg;
      this.button = button;
      this.on = on;
      this.off = off;
      button.setOnClickListener(v -> flip());
    }

    void render(boolean checked) {
      button.setImageTintList(checked ? on : off);
      button.setSelected(checked);
    }

    private void flip() {
      try {
        Object onCheckedChange = Reflect.callMethod(row, cfg.methodGetOnCheckedChange);
        if (onCheckedChange == null) return;
        boolean checked = (boolean) Reflect.callMethod(row, cfg.methodGetChecked);
        Reflect.callMethod(onCheckedChange, "invoke", !checked);
      } catch (Throwable t) {
        Knot.log("Knot: KeywordAssistInSearchBar toggle failed", t);
      }
    }
  }
}
