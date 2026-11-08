package app.zipper.knot.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import app.zipper.knot.Knot;
import app.zipper.knot.R;
import app.zipper.knot.SettingsStore;
import app.zipper.knot.utils.ChatJumpUtil;
import app.zipper.knot.utils.LineDBUtils;
import app.zipper.knot.utils.LineTheme;
import app.zipper.knot.utils.ModuleResources;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.json.JSONObject;

public class ReadHistoryViewer {

  private static final int READER_COLLAPSE_THRESHOLD = 5;

  private static final class ReaderEntry {
    final String name;
    final String time;

    ReaderEntry(String name, String time) {
      this.name = name;
      this.time = time;
    }
  }

  private static final class MessageEntry {
    final String chatId;
    final String msgId;
    final long sortKey;
    final boolean numericId;
    final String text;
    final List<ReaderEntry> readers;
    boolean expanded;

    MessageEntry(String chatId, String msgId, String text, List<ReaderEntry> readers) {
      this.chatId = chatId;
      this.msgId = msgId;
      this.text = text;
      this.readers = readers;
      long parsed = 0L;
      boolean numeric = false;
      try {
        parsed = Long.parseLong(msgId);
        numeric = true;
      } catch (NumberFormatException ignored) {
      }
      this.sortKey = parsed;
      this.numericId = numeric;
    }
  }

  public static void show(Activity activity, String targetChatId) {
    try {
      LineTheme.invalidate();
      List<MessageEntry> entries = loadEntries(targetChatId);

      String chatName = LineDBUtils.resolveChatName(targetChatId);

      ListView listView = new ListView(activity);
      listView.setPadding(40, 20, 40, 40);
      listView.setClipToPadding(false);
      listView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
      listView.setDivider(null);
      listView.setDividerHeight(0);
      listView.setSelector(new ColorDrawable(Color.TRANSPARENT));
      listView.setCacheColorHint(Color.TRANSPARENT);

      TextView header = new TextView(activity);
      header.setText(
          chatName != null ? chatName : ModuleResources.get(R.string.read_history_title));
      header.setTextSize(18);
      header.setTextColor(LineTheme.primaryTextColor(activity));
      header.setPadding(0, 20, 0, 30);
      listView.addHeaderView(header, null, false);

      final AlertDialog[] dialogRef = new AlertDialog[1];

      boolean found = !entries.isEmpty();
      if (!found) {
        TextView empty = new TextView(activity);
        empty.setText(ModuleResources.get(R.string.read_history_empty));
        empty.setPadding(0, 100, 0, 100);
        empty.setGravity(Gravity.CENTER);
        empty.setTextColor(LineTheme.secondaryTextColor(activity));
        listView.addHeaderView(empty, null, false);
      }

      listView.setAdapter(
          new BaseAdapter() {
            @Override
            public int getCount() {
              return entries.size();
            }

            @Override
            public MessageEntry getItem(int position) {
              return entries.get(position);
            }

            @Override
            public long getItemId(int position) {
              return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
              return bindMessageCard(activity, convertView, entries.get(position), dialogRef);
            }
          });

      int themeId = LineTheme.dialogTheme(activity);
      AlertDialog.Builder builder = new AlertDialog.Builder(activity, themeId);
      builder.setView(listView);
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

  private static List<MessageEntry> loadEntries(String targetChatId) {
    List<MessageEntry> entries = new ArrayList<>();
    synchronized (SettingsStore.class) {
      JSONObject historyJson = SettingsStore.loadReadHistory();
      JSONObject chats = historyJson.optJSONObject("c");
      if (chats == null) return entries;

      if (targetChatId != null) {
        collectChatEntries(chats.optJSONObject(targetChatId), targetChatId, entries);
      } else {
        Iterator<String> chatKeys = chats.keys();
        while (chatKeys.hasNext()) {
          String chatKey = chatKeys.next();
          collectChatEntries(chats.optJSONObject(chatKey), chatKey, entries);
        }
      }
    }
    return entries;
  }

  private static void collectChatEntries(
      JSONObject chat, String chatId, List<MessageEntry> outEntries) {
    if (chat == null) return;
    JSONObject messages = chat.optJSONObject("m");
    if (messages == null) return;

    List<MessageEntry> chatEntries = new ArrayList<>();
    Iterator<String> keys = messages.keys();
    while (keys.hasNext()) {
      String msgId = keys.next();
      JSONObject msg = messages.optJSONObject(msgId);
      if (msg == null) continue;

      List<ReaderEntry> readers = new ArrayList<>();
      JSONObject readersJson = msg.optJSONObject("r");
      if (readersJson != null) {
        Iterator<String> rKeys = readersJson.keys();
        while (rKeys.hasNext()) {
          JSONObject reader = readersJson.optJSONObject(rKeys.next());
          if (reader != null) {
            readers.add(
                new ReaderEntry(reader.optString("n", "Unknown"), reader.optString("t", "")));
          }
        }
      }
      chatEntries.add(new MessageEntry(chatId, msgId, msg.optString("c", ""), readers));
    }

    Collections.sort(
        chatEntries,
        (a, b) -> {
          if (a.numericId && b.numericId) {
            return Long.compare(b.sortKey, a.sortKey);
          }
          return b.msgId.compareTo(a.msgId);
        });
    outEntries.addAll(chatEntries);
  }

  private static View bindMessageCard(
      Activity activity, View convertView, MessageEntry entry, AlertDialog[] dialogRef) {
    FrameLayout wrapper;
    LinearLayout card;
    TextView contentText;

    if (convertView instanceof FrameLayout) {
      wrapper = (FrameLayout) convertView;
      card = (LinearLayout) wrapper.getChildAt(0);
      contentText = (TextView) card.getChildAt(0);
      if (card.getChildCount() > 1) {
        card.removeViews(1, card.getChildCount() - 1);
      }
    } else {
      wrapper = new FrameLayout(activity);
      wrapper.setPadding(0, 5, 0, 20);

      card = new LinearLayout(activity);
      card.setOrientation(LinearLayout.VERTICAL);
      card.setPadding(25, 20, 25, 20);

      GradientDrawable gd = new GradientDrawable();
      gd.setColor(LineTheme.cardColor(activity));
      gd.setCornerRadius(12f);
      card.setBackground(gd);

      contentText = new TextView(activity);
      contentText.setTextColor(LineTheme.primaryTextColor(activity));
      contentText.setTextSize(17);
      contentText.setPadding(0, 0, 0, 15);
      card.addView(contentText);

      wrapper.addView(
          card,
          new FrameLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    String messageText = entry.text;
    contentText.setText(
        messageText != null && !messageText.isEmpty()
            ? messageText
            : ModuleResources.get(R.string.read_history_unknown_msg));

    if (!entry.readers.isEmpty()) {
      addReaderList(activity, card, entry);
    }

    card.setOnClickListener(
        v -> {
          boolean ok = ChatJumpUtil.jumpToMessage(activity, entry.chatId, entry.msgId);
          if (ok && dialogRef[0] != null) {
            dialogRef[0].dismiss();
          }
        });

    return wrapper;
  }

  private static void addReaderList(Activity activity, LinearLayout card, MessageEntry entry) {
    List<ReaderEntry> readers = entry.readers;
    int initialCount = Math.min(readers.size(), READER_COLLAPSE_THRESHOLD);
    for (int i = 0; i < initialCount; i++) {
      card.addView(buildReaderRow(activity, readers.get(i)));
    }

    if (readers.size() > READER_COLLAPSE_THRESHOLD) {
      addReaderToggle(
          activity, card, entry, readers.subList(READER_COLLAPSE_THRESHOLD, readers.size()));
    }
  }

  private static void addReaderToggle(
      Activity activity, LinearLayout card, MessageEntry entry, List<ReaderEntry> extraReaders) {
    final String showAll = ModuleResources.get(R.string.read_history_show_all, extraReaders.size());
    final List<View> overflowViews = new ArrayList<>();
    if (entry.expanded) {
      for (ReaderEntry reader : extraReaders) {
        View row = buildReaderRow(activity, reader);
        overflowViews.add(row);
        card.addView(row);
      }
    }

    TextView toggle = new TextView(activity);
    toggle.setText(entry.expanded ? ModuleResources.get(R.string.read_history_collapse) : showAll);
    toggle.setTextSize(13);
    toggle.setTypeface(null, Typeface.BOLD);
    toggle.setTextColor(LineTheme.secondaryTextColor(activity));
    toggle.setPadding(0, 8, 0, 4);

    toggle.setOnClickListener(
        v -> {
          entry.expanded = !entry.expanded;
          if (overflowViews.isEmpty()) {
            int insertIndex = card.indexOfChild(toggle);
            for (ReaderEntry reader : extraReaders) {
              View row = buildReaderRow(activity, reader);
              overflowViews.add(row);
              card.addView(row, insertIndex++);
            }
          } else {
            for (View row : overflowViews) {
              row.setVisibility(entry.expanded ? View.VISIBLE : View.GONE);
            }
          }
          toggle.setText(
              entry.expanded ? ModuleResources.get(R.string.read_history_collapse) : showAll);
        });
    card.addView(toggle);
  }

  private static View buildReaderRow(Activity activity, ReaderEntry reader) {
    LinearLayout detailRow = new LinearLayout(activity);
    detailRow.setOrientation(LinearLayout.HORIZONTAL);
    detailRow.setGravity(Gravity.CENTER_VERTICAL);
    detailRow.setPadding(0, 5, 0, 5);

    TextView nameText = new TextView(activity);
    nameText.setText(reader.name);
    nameText.setTextColor(LineTheme.secondaryTextColor(activity));
    nameText.setTextSize(15);
    detailRow.addView(nameText);

    TextView timeText = new TextView(activity);
    timeText.setText(reader.time);
    timeText.setTextSize(12);
    timeText.setTextColor(LineTheme.secondaryTextColor(activity));
    timeText.setPadding(20, 0, 0, 0);
    detailRow.addView(timeText);

    return detailRow;
  }

  private static void clearChatHistory(String chatId) {
    synchronized (SettingsStore.class) {
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
}
