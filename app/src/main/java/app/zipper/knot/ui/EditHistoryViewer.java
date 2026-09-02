package app.zipper.knot.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import app.zipper.knot.Knot;
import app.zipper.knot.R;
import app.zipper.knot.hooks.EditHistoryHook;
import app.zipper.knot.utils.LineTheme;
import app.zipper.knot.utils.ModuleResources;
import org.json.JSONArray;
import org.json.JSONObject;

public class EditHistoryViewer {

  public static void show(Context context, String msgId) {
    try {
      Activity activity = resolveActivity(context);
      if (activity == null) return;
      LineTheme.invalidate();

      JSONArray versions = EditHistoryHook.historyFor(msgId);
      boolean hasHistory = versions != null && versions.length() > 0;

      ScrollView scrollView = new ScrollView(activity);
      LinearLayout container = new LinearLayout(activity);
      container.setOrientation(LinearLayout.VERTICAL);
      container.setPadding(48, 24, 48, 40);
      scrollView.addView(container);

      TextView header = new TextView(activity);
      header.setText(ModuleResources.get(R.string.edit_history_title));
      header.setTextSize(18);
      header.setTextColor(LineTheme.primaryTextColor(activity));
      header.setPadding(0, 12, 0, 24);
      container.addView(header);

      if (!hasHistory) {
        container.addView(buildEmptyRow(activity));
      } else {
        for (int i = 0; i < versions.length(); i++) {
          JSONObject version = versions.optJSONObject(i);
          if (version != null) container.addView(buildVersionRow(activity, version, i));
        }
      }

      int themeId = LineTheme.dialogTheme(activity);
      AlertDialog.Builder builder =
          new AlertDialog.Builder(activity, themeId)
              .setView(scrollView)
              .setPositiveButton(ModuleResources.get(R.string.common_close), null);
      if (hasHistory) {
        builder.setNeutralButton(
            ModuleResources.get(R.string.read_history_delete),
            (d, which) ->
                LineTheme.applyDialogColors(
                    new AlertDialog.Builder(activity, themeId)
                        .setTitle(ModuleResources.get(R.string.read_history_delete_confirm_title))
                        .setMessage(ModuleResources.get(R.string.edit_history_delete_confirm_msg))
                        .setPositiveButton(
                            ModuleResources.get(R.string.settings_yes),
                            (dd, w) -> EditHistoryHook.clearHistory(msgId))
                        .setNegativeButton(ModuleResources.get(R.string.settings_cancel), null)
                        .show(),
                    activity));
      }
      AlertDialog dialog = builder.create();
      dialog.show();
      LineTheme.applyDialogColors(dialog, activity);
    } catch (Throwable t) {
      Knot.log("Knot: edit history viewer error: " + t);
    }
  }

  private static TextView buildEmptyRow(Context ctx) {
    TextView empty = new TextView(ctx);
    empty.setText(ModuleResources.get(R.string.edit_history_empty));
    empty.setPadding(0, 80, 0, 80);
    empty.setGravity(Gravity.CENTER);
    empty.setTextColor(LineTheme.secondaryTextColor(ctx));
    return empty;
  }

  private static LinearLayout buildVersionRow(Context ctx, JSONObject version, int index) {
    String timestamp = version.optString("ts", "");
    String label =
        index == 0
            ? ModuleResources.get(R.string.edit_history_original)
            : ModuleResources.get(R.string.edit_history_edited) + " " + index;
    if (!timestamp.isEmpty()) label = label + "  " + timestamp;
    String text = version.optString("t", "");

    LinearLayout row = new LinearLayout(ctx);
    row.setOrientation(LinearLayout.VERTICAL);
    row.setPadding(0, 12, 0, 12);

    TextView labelView = new TextView(ctx);
    labelView.setText(label);
    labelView.setTextSize(12);
    labelView.setTextColor(LineTheme.secondaryTextColor(ctx));
    labelView.setPadding(0, 0, 0, 6);
    row.addView(labelView);

    TextView textView = new TextView(ctx);
    textView.setText(text);
    textView.setTextSize(15);
    textView.setTextColor(LineTheme.primaryTextColor(ctx));
    textView.setTextIsSelectable(true);
    row.addView(textView);

    return row;
  }

  private static Activity resolveActivity(Context context) {
    while (context instanceof ContextWrapper) {
      if (context instanceof Activity) return (Activity) context;
      context = ((ContextWrapper) context).getBaseContext();
    }
    return null;
  }
}
