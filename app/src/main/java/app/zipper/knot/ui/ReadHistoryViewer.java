package app.zipper.knot.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import app.zipper.knot.Knot;
import app.zipper.knot.R;
import app.zipper.knot.SettingsStore;
import app.zipper.knot.utils.ChatJumpUtil;
import app.zipper.knot.utils.LineDBUtils;
import app.zipper.knot.utils.LineTheme;
import app.zipper.knot.utils.ModuleResources;
import org.json.JSONObject;

public class ReadHistoryViewer {

  public static void show(Activity activity, String targetChatId) {
    try {
      LineTheme.invalidate();
      JSONObject historyJson = SettingsStore.loadReadHistory();
      JSONObject chats = historyJson.optJSONObject("c");

      String chatName = LineDBUtils.resolveChatName(targetChatId);

      ScrollView scrollView = new ScrollView(activity);
      LinearLayout container = new LinearLayout(activity);
      container.setOrientation(LinearLayout.VERTICAL);
      container.setPadding(40, 20, 40, 40);
      scrollView.addView(container);

      TextView header = new TextView(activity);
      header.setText(
          chatName != null ? chatName : ModuleResources.get(R.string.read_history_title));
      header.setTextSize(18);
      header.setTextColor(LineTheme.primaryTextColor(activity));
      header.setPadding(0, 20, 0, 30);
      container.addView(header);

      final AlertDialog[] dialogRef = new AlertDialog[1];

      boolean found = false;
      if (chats != null) {
        if (targetChatId != null) {
          JSONObject chat = chats.optJSONObject(targetChatId);
          if (chat != null) {
            JSONObject messages = chat.optJSONObject("m");
            if (messages != null) {
              found = true;
              renderMessages(activity, container, messages, targetChatId, dialogRef);
            }
          }
        } else {
          java.util.Iterator<String> chatKeys = chats.keys();
          while (chatKeys.hasNext()) {
            String chatKey = chatKeys.next();
            JSONObject chat = chats.optJSONObject(chatKey);
            if (chat != null) {
              JSONObject messages = chat.optJSONObject("m");
              if (messages != null) {
                found = true;
                renderMessages(activity, container, messages, chatKey, dialogRef);
              }
            }
          }
        }
      }

      if (!found) {
        TextView empty = new TextView(activity);
        empty.setText(ModuleResources.get(R.string.read_history_empty));
        empty.setPadding(0, 100, 0, 100);
        empty.setGravity(Gravity.CENTER);
        empty.setTextColor(LineTheme.secondaryTextColor(activity));
        container.addView(empty);
      }

      int themeId = LineTheme.dialogTheme(activity);
      AlertDialog.Builder builder = new AlertDialog.Builder(activity, themeId);
      builder.setView(scrollView);
      builder.setPositiveButton(ModuleResources.get(R.string.common_close), null);
      if (targetChatId != null && found) {
        builder.setNeutralButton(
            ModuleResources.get(R.string.read_history_delete),
            (dialog, which) ->
                LineTheme.applyDialogColors(
                    new AlertDialog.Builder(activity, themeId)
                        .setTitle(ModuleResources.get(R.string.read_history_delete_confirm_title))
                        .setMessage(ModuleResources.get(R.string.read_history_delete_confirm_msg))
                        .setPositiveButton(
                            ModuleResources.get(R.string.settings_yes),
                            (d, w) -> clearChatHistory(targetChatId))
                        .setNegativeButton(ModuleResources.get(R.string.settings_cancel), null)
                        .show(),
                    activity));
      }
      AlertDialog dialog = builder.create();
      dialogRef[0] = dialog;
      dialog.show();
      LineTheme.applyDialogColors(dialog, activity);

    } catch (Throwable t) {
      Knot.log("Knot: error: " + t.getMessage());
    }
  }

  private static void renderMessages(
      Activity activity,
      LinearLayout container,
      JSONObject messages,
      String chatId,
      AlertDialog[] dialogRef) {
    java.util.List<String> sortedKeys = new java.util.ArrayList<>();
    java.util.Iterator<String> keys = messages.keys();
    while (keys.hasNext()) {
      sortedKeys.add(keys.next());
    }
    java.util.Collections.sort(
        sortedKeys,
        (a, b) -> {
          try {
            return Long.compare(Long.parseLong(b), Long.parseLong(a));
          } catch (Exception e) {
            return b.compareTo(a);
          }
        });

    for (String msgId : sortedKeys) {
      JSONObject msg = messages.optJSONObject(msgId);
      if (msg == null) continue;
      addMessageCard(activity, container, msg, chatId, msgId, dialogRef);
    }
  }

  private static void addMessageCard(
      Activity activity,
      LinearLayout container,
      JSONObject msg,
      String chatId,
      String msgId,
      AlertDialog[] dialogRef) {
    String messageText = msg.optString("c", "");

    LinearLayout card = new LinearLayout(activity);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setPadding(25, 20, 25, 20);
    LinearLayout.LayoutParams cardLp =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    cardLp.setMargins(0, 5, 0, 5);
    card.setLayoutParams(cardLp);

    android.graphics.drawable.GradientDrawable gd =
        new android.graphics.drawable.GradientDrawable();
    gd.setColor(LineTheme.cardColor(activity));
    gd.setCornerRadius(12f);
    card.setBackground(gd);

    TextView contentText = new TextView(activity);
    contentText.setText(
        messageText != null && !messageText.isEmpty()
            ? messageText
            : ModuleResources.get(R.string.read_history_unknown_msg));
    contentText.setTextColor(LineTheme.primaryTextColor(activity));
    contentText.setTextSize(17);
    contentText.setPadding(0, 0, 0, 15);
    card.addView(contentText);

    JSONObject readers = msg.optJSONObject("r");
    if (readers != null && readers.length() > 0) {
      addReaderList(activity, card, readers);
    }

    card.setOnClickListener(
        v -> {
          boolean ok = ChatJumpUtil.jumpToMessage(activity, chatId, msgId);
          if (ok && dialogRef[0] != null) {
            dialogRef[0].dismiss();
          }
        });

    container.addView(card);
    View margin = new View(activity);
    container.addView(
        margin, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 15));
  }

  private static final int READER_COLLAPSE_THRESHOLD = 5;

  private static void addReaderList(Activity activity, LinearLayout card, JSONObject readers) {
    java.util.List<View> overflow = new java.util.ArrayList<>();

    int index = 0;
    java.util.Iterator<String> rKeys = readers.keys();
    while (rKeys.hasNext()) {
      JSONObject reader = readers.optJSONObject(rKeys.next());
      if (reader == null) continue;
      View row = buildReaderRow(activity, reader);
      if (index >= READER_COLLAPSE_THRESHOLD) {
        row.setVisibility(View.GONE);
        overflow.add(row);
      }
      card.addView(row);
      index++;
    }

    if (!overflow.isEmpty()) {
      addReaderToggle(activity, card, overflow);
    }
  }

  private static void addReaderToggle(
      Activity activity, LinearLayout card, java.util.List<View> overflow) {
    final String showAll = ModuleResources.get(R.string.read_history_show_all, overflow.size());

    TextView toggle = new TextView(activity);
    toggle.setText(showAll);
    toggle.setTextSize(13);
    toggle.setTypeface(null, android.graphics.Typeface.BOLD);
    toggle.setTextColor(LineTheme.secondaryTextColor(activity));
    toggle.setPadding(0, 8, 0, 4);

    final boolean[] expanded = {false};
    toggle.setOnClickListener(
        v -> {
          expanded[0] = !expanded[0];
          for (View row : overflow) {
            row.setVisibility(expanded[0] ? View.VISIBLE : View.GONE);
          }
          toggle.setText(
              expanded[0] ? ModuleResources.get(R.string.read_history_collapse) : showAll);
        });
    card.addView(toggle);
  }

  private static View buildReaderRow(Activity activity, JSONObject reader) {
    String readerName = reader.optString("n", "Unknown");
    String readTime = reader.optString("t", "");

    LinearLayout detailRow = new LinearLayout(activity);
    detailRow.setOrientation(LinearLayout.HORIZONTAL);
    detailRow.setGravity(Gravity.CENTER_VERTICAL);
    detailRow.setPadding(0, 5, 0, 5);

    TextView nameText = new TextView(activity);
    nameText.setText(readerName);
    nameText.setTextColor(LineTheme.secondaryTextColor(activity));
    nameText.setTextSize(15);
    detailRow.addView(nameText);

    TextView timeText = new TextView(activity);
    timeText.setText(readTime);
    timeText.setTextSize(12);
    timeText.setTextColor(LineTheme.secondaryTextColor(activity));
    timeText.setPadding(20, 0, 0, 0);
    detailRow.addView(timeText);

    return detailRow;
  }

  private static void clearChatHistory(String chatId) {
    try {
      JSONObject historyJson = SettingsStore.loadReadHistory();
      JSONObject chats = historyJson.optJSONObject("c");
      if (chats != null && chats.has(chatId)) {
        chats.remove(chatId);
        SettingsStore.saveReadHistory(historyJson);
      }
    } catch (Exception e) {
    }
  }
}
