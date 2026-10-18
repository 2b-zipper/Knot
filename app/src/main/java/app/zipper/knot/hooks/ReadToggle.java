package app.zipper.knot.hooks;

import android.os.Handler;
import android.os.Looper;
import app.zipper.knot.R;
import app.zipper.knot.SettingsStore;
import app.zipper.knot.utils.ModuleResources;

enum ReadToggle {
  PREVENT_READ("prevent_read_state", true, R.string.label_prevent_read, "ic_prevent_read"),
  SEND_MARK_READ("send_mark_state", false, R.string.label_send_mark_read, "ic_send_mark_read"),
  REACTION_MARK_READ(
      "reaction_mark_state", false, R.string.label_reaction_mark_read, "ic_reaction_mark_read"),
  SHOW_HIDDEN_CHATS(
      "show_hidden_chats", false, R.string.label_show_hidden_chats, "ic_show_hidden_chats");

  private final String key;
  private final boolean defaultValue;
  private final int labelRes;
  private final String icon;

  ReadToggle(String key, boolean defaultValue, int labelRes, String icon) {
    this.key = key;
    this.defaultValue = defaultValue;
    this.labelRes = labelRes;
    this.icon = icon;
  }

  boolean isOn() {
    return SettingsStore.get(key, defaultValue);
  }

  void toggle() {
    SettingsStore.save(key, !isOn());
    if (this == SHOW_HIDDEN_CHATS) {
      new Handler(Looper.getMainLooper()).post(PermanentHideChatHook::refreshChatList);
    }
  }

  boolean isAvailable() {
    switch (this) {
      case PREVENT_READ:
        return SettingsStore.get("prevent_mark_as_read", false);
      case SHOW_HIDDEN_CHATS:
        return SettingsStore.get("permanent_hide_chat", false);
      default:
        return PREVENT_READ.isActive();
    }
  }

  boolean isActive() {
    return isAvailable() && isOn();
  }

  String label(boolean on) {
    return labelPrefix() + (on ? "ON" : "OFF");
  }

  String iconName(boolean on) {
    return icon + (on ? "_on" : "_off");
  }

  private String labelPrefix() {
    return ModuleResources.get(labelRes) + ": ";
  }

  static ReadToggle fromLabel(String label) {
    for (ReadToggle toggle : values()) {
      if (label.startsWith(toggle.labelPrefix())) return toggle;
    }
    return null;
  }
}
