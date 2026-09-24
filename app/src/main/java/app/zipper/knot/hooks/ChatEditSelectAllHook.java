package app.zipper.knot.hooks;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.StateSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.Reflect;
import java.lang.reflect.Method;
import java.util.Set;

public class ChatEditSelectAllHook implements BaseHook {

  private static final String LAYOUT_BOTTOM_BUTTONS = "chat_ui_edit_mode_bottom_button";
  private static final String ID_TOGGLE_BUTTON = "chat_ui_edit_mode_bottom_option_toggle_button";
  private static final String ID_EXECUTION_BUTTON = "chat_ui_edit_mode_bottom_execution_button";
  private static final String ID_MESSAGE_LIST = "chathistory_message_list";

  private static final int[] STATE_PRESSED = {android.R.attr.state_pressed};
  private static final int[] STATE_SELECTED = {android.R.attr.state_selected};

  private LineVersion.Config.ChatEditSelectAll cfg;
  private Class<?> providerClass;
  private volatile int bottomButtonsLayoutId = -1;

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    if (!config.selectAllInEditMode.enabled) return;
    LineVersion.Config version = LineVersion.get();
    if (version == null) return;
    cfg = version.chatEditSelectAll;
    if (cfg.selectionProviderClass.isEmpty()) return;
    providerClass = Reflect.findClass(cfg.selectionProviderClass, lpparam.classLoader);

    Knot.module
        .hook(
            Reflect.findMethodExact(
                LayoutInflater.class, "inflate", int.class, ViewGroup.class, boolean.class))
        .intercept(
            chain -> {
              View result = (View) chain.proceed();
              if (result != null && Main.options.selectAllInEditMode.enabled) {
                injectToggleButton(result, (int) chain.getArg(0));
              }
              return result;
            });
  }

  private void injectToggleButton(View inflated, int layoutId) {
    Context context = inflated.getContext();
    if (context == null) return;
    try {
      Resources res = context.getResources();
      String pkg = context.getPackageName();
      if (layoutId != bottomButtonsLayoutId(res, pkg)) return;

      int toggleBtnId = res.getIdentifier(ID_TOGGLE_BUTTON, "id", pkg);
      int execBtnId = res.getIdentifier(ID_EXECUTION_BUTTON, "id", pkg);
      if (toggleBtnId == 0 || execBtnId == 0) return;

      TextView toggleBtn = inflated.findViewById(toggleBtnId);
      if (toggleBtn == null) return;

      toggleBtn.setText(selectAllLabel(context));
      toggleBtn.setEnabled(true);
      applyOutlineStyle(toggleBtn);
      toggleBtn
          .getViewTreeObserver()
          .addOnGlobalLayoutListener(() -> onToggleLayout(toggleBtn, execBtnId));
    } catch (Exception ignored) {
    }
  }

  private int bottomButtonsLayoutId(Resources res, String pkg) {
    int id = bottomButtonsLayoutId;
    if (id == -1) {
      id = res.getIdentifier(LAYOUT_BOTTOM_BUTTONS, "layout", pkg);
      bottomButtonsLayoutId = id;
    }
    return id;
  }

  private void onToggleLayout(TextView toggleBtn, int execBtnId) {
    try {
      Context context = toggleBtn.getContext();
      boolean ours = ownsSlot(toggleBtn, context);

      if (!isDeleteMode(toggleBtn, execBtnId, context)) {
        // LINE resolves this view only in capture mode, and its hide path no-ops until it does.
        if (ours) toggleBtn.setVisibility(View.GONE);
        return;
      }

      if (!ours) toggleBtn.setText(selectAllLabel(context));
      updateButtonText(toggleBtn);
      toggleBtn.setOnClickListener(v -> handleSelectAll(toggleBtn));
      toggleBtn.setEnabled(true);
      if (toggleBtn.getVisibility() != View.VISIBLE) toggleBtn.setVisibility(View.VISIBLE);
    } catch (Exception ignored) {
    }
  }

  private static boolean isDeleteMode(TextView toggleBtn, int execBtnId, Context context) {
    View parent = (View) toggleBtn.getParent();
    if (parent == null) return false;
    TextView execBtn = parent.findViewById(execBtnId);
    return execBtn != null && execBtn.getText().toString().contains(deleteLabel(context));
  }

  private static boolean ownsSlot(TextView toggleBtn, Context context) {
    String label = toggleBtn.getText().toString();
    return label.equals(selectAllLabel(context)) || label.equals(deselectAllLabel(context));
  }

  private void handleSelectAll(TextView toggleBtn) {
    try {
      Context context = toggleBtn.getContext();
      Selection selection = resolve(context);
      if (selection == null) {
        Knot.log("Knot SelectAll: provider not found");
        return;
      }

      int selectedCount = selection.selectedCount();
      boolean deselect =
          selectedCount == selection.count
              || (selectedCount > 0
                  && deselectAllLabel(context).equals(toggleBtn.getText().toString()));

      selection.toggleAll(deselect);
      toggleBtn.setText(deselect ? selectAllLabel(context) : deselectAllLabel(context));
      selection.notifyChanged();
    } catch (Throwable t) {
      Knot.log("Knot SelectAll Error: " + t);
    }
  }

  private void updateButtonText(TextView toggleBtn) {
    try {
      Context context = toggleBtn.getContext();
      Selection selection = resolve(context);
      if (selection == null) return;

      int selectedCount = selection.selectedCount();
      if (selectedCount > 0 && selectedCount == selection.count) {
        toggleBtn.setText(deselectAllLabel(context));
      } else if (selectedCount < selection.count) {
        toggleBtn.setText(selectAllLabel(context));
      }
    } catch (Exception ignored) {
    }
  }

  private static String deleteLabel(Context context) {
    return lineString(context, "chat_edit_action_delete");
  }

  private static String selectAllLabel(Context context) {
    return lineString(context, "line_settings_button_selectall");
  }

  private static String deselectAllLabel(Context context) {
    return lineString(context, "line_settings_button_deselectall");
  }

  private static String lineString(Context context, String resName) {
    int id = context.getResources().getIdentifier(resName, "string", context.getPackageName());
    return context.getString(id);
  }

  // LINE styles this slot only in capture mode, as its "hide info" button. Rebuilt from single
  // colors rather than its selector XML so AmoledThemeHook's Resources.getColor override applies.
  private static void applyOutlineStyle(TextView button) {
    Context context = button.getContext();
    Resources res = context.getResources();
    String pkg = context.getPackageName();

    OutlineColors normal =
        OutlineColors.resolve(res, pkg, "primarySurface", "primaryNeutralFill", "defaultText");
    OutlineColors pressed =
        OutlineColors.resolve(
            res, pkg, "primarySurfacePressed", "primaryNeutralFill_pressed", "defaultText_pressed");
    if (normal == null || pressed == null) {
      applyOutlineSelectors(button, res, pkg);
      return;
    }

    float density = res.getDisplayMetrics().density;
    float radius = lineDimension(res, pkg, "chat_ui_editmode_bottom_button_radius", 5f * density);
    int strokeWidth =
        Math.round(lineDimension(res, pkg, "chat_ui_simple_button_stroke_width", density));

    StateListDrawable background = new StateListDrawable();
    background.addState(STATE_PRESSED, outlineShape(pressed, strokeWidth, radius));
    background.addState(StateSet.WILD_CARD, outlineShape(normal, strokeWidth, radius));
    button.setBackground(background);

    button.setTextColor(
        new ColorStateList(
            new int[][] {STATE_PRESSED, STATE_SELECTED, StateSet.WILD_CARD},
            new int[] {pressed.text, pressed.text, normal.text}));
  }

  private static void applyOutlineSelectors(TextView button, Resources res, String pkg) {
    int backgroundId = res.getIdentifier("chat_ui_selector_selection_mode_white", "drawable", pkg);
    int textId =
        res.getIdentifier("chat_ui_selector_message_edit_outline_button_text", "color", pkg);
    if (backgroundId != 0) button.setBackgroundResource(backgroundId);
    if (textId != 0) button.setTextColor(res.getColorStateList(textId, null));
  }

  private static GradientDrawable outlineShape(
      OutlineColors colors, int strokeWidth, float radius) {
    GradientDrawable shape = new GradientDrawable();
    shape.setShape(GradientDrawable.RECTANGLE);
    shape.setColor(colors.fill);
    shape.setStroke(strokeWidth, colors.stroke);
    shape.setCornerRadius(radius);
    return shape;
  }

  private static final class OutlineColors {
    final int fill;
    final int stroke;
    final int text;

    private OutlineColors(int fill, int stroke, int text) {
      this.fill = fill;
      this.stroke = stroke;
      this.text = text;
    }

    static OutlineColors resolve(
        Resources res, String pkg, String fill, String stroke, String text) {
      Integer fillColor = lineColor(res, pkg, fill);
      Integer strokeColor = lineColor(res, pkg, stroke);
      Integer textColor = lineColor(res, pkg, text);
      return fillColor == null || strokeColor == null || textColor == null
          ? null
          : new OutlineColors(fillColor, strokeColor, textColor);
    }
  }

  private static Integer lineColor(Resources res, String pkg, String name) {
    int id = res.getIdentifier(name, "color", pkg);
    return id == 0 ? null : res.getColor(id, null);
  }

  private static float lineDimension(Resources res, String pkg, String name, float fallback) {
    int id = res.getIdentifier(name, "dimen", pkg);
    return id == 0 ? fallback : res.getDimension(id);
  }

  private Selection resolve(Context context) {
    Object adapter = findAdapter(context);
    Object provider = findProvider(adapter);
    return provider == null ? null : new Selection(adapter, provider);
  }

  private final class Selection {
    final Object adapter;
    final Object provider;
    final Object state;
    final int count;

    Selection(Object adapter, Object provider) {
      this.adapter = adapter;
      this.provider = provider;
      this.state = Reflect.callMethod(provider, cfg.methodGetSelectionState);
      this.count = (int) Reflect.callMethod(provider, cfg.methodGetCount);
    }

    int selectedCount() {
      Object set = Reflect.callMethod(state, cfg.methodGetSelectedIds);
      return set instanceof Set ? ((Set<?>) set).size() : 0;
    }

    void toggleAll(boolean deselect) {
      for (int i = 0; i < count; i++) {
        try {
          Object item = Reflect.callMethod(provider, cfg.methodGetItem, i);
          if (item == null) continue;
          boolean selected = (boolean) Reflect.callMethod(state, cfg.methodIsItemSelected, item);
          if (selected == deselect) Reflect.callMethod(state, cfg.methodToggleItem, item);
        } catch (Exception ignored) {
        }
      }
    }

    void notifyChanged() {
      Reflect.callMethod(adapter, "notifyDataSetChanged");
    }
  }

  private Object findProvider(Object adapter) {
    if (adapter == null) return null;
    for (Method m : adapter.getClass().getMethods()) {
      if (m.getParameterCount() == 0 && providerClass.isAssignableFrom(m.getReturnType())) {
        try {
          m.setAccessible(true);
          Object provider = m.invoke(adapter);
          if (provider != null) return provider;
        } catch (Exception ignored) {
        }
      }
    }
    return null;
  }

  private static Object findAdapter(Context context) {
    Activity activity = resolveActivity(context);
    if (activity == null) return null;
    int listId =
        context.getResources().getIdentifier(ID_MESSAGE_LIST, "id", context.getPackageName());
    View recyclerView = activity.findViewById(listId);
    if (recyclerView == null) return null;
    try {
      return Reflect.callMethod(recyclerView, "getAdapter");
    } catch (Throwable t) {
      return null;
    }
  }

  private static Activity resolveActivity(Context context) {
    if (context instanceof Activity) return (Activity) context;
    if (context instanceof ContextWrapper) {
      Context base = ((ContextWrapper) context).getBaseContext();
      if (base instanceof Activity) return (Activity) base;
    }
    return null;
  }
}
