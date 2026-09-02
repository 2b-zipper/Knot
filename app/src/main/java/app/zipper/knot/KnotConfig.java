package app.zipper.knot;

import app.zipper.knot.utils.ModuleResources;
import java.util.ArrayList;
import java.util.List;

public class KnotConfig {

  public enum Category {
    PRIVACY(R.string.cat_privacy),
    CHAT(R.string.cat_chat),
    DISPLAY(R.string.cat_display),
    NOTIFICATION(R.string.cat_notification),
    SYSTEM(R.string.cat_system),
    BACKUP(R.string.cat_backup),
    OTHER(R.string.cat_other);

    public final int labelRes;

    Category(int labelRes) {
      this.labelRes = labelRes;
    }

    public String label() {
      return ModuleResources.get(labelRes);
    }
  }

  public static final class Item {
    public final String key;
    public final int labelRes;
    public final int descriptionRes;
    public boolean enabled;
    public String value = "";
    public final Category category;
    public final int sectionRes;
    public String disabledWhenEnabledKey;

    Item(String key, int labelRes, int descRes, boolean def, Category cat, int sectionRes) {
      this.key = key;
      this.labelRes = labelRes;
      this.descriptionRes = descRes;
      this.enabled = def;
      this.category = cat;
      this.sectionRes = sectionRes;
    }

    public String label() {
      return ModuleResources.get(labelRes);
    }

    public String description() {
      return ModuleResources.get(descriptionRes);
    }

    public String section() {
      return sectionRes == 0 ? "" : ModuleResources.get(sectionRes);
    }
  }

  private final List<Item> _reg = new ArrayList<>();

  private Item item(String key, int label, int desc, boolean def, Category cat, int sec) {
    Item i = new Item(key, label, desc, def, cat, sec);
    _reg.add(i);
    return i;
  }

  private Item item(
      String key,
      int label,
      int desc,
      boolean def,
      Category cat,
      int sec,
      String disabledWhenEnabledKey) {
    Item i = item(key, label, desc, def, cat, sec);
    i.disabledWhenEnabledKey = disabledWhenEnabledKey;
    return i;
  }

  // @formatter:off
  public final Item preventMarkAsRead            = item("prevent_mark_as_read",             R.string.opt_prevent_mark_as_read_label,             R.string.opt_prevent_mark_as_read_desc,             false, Category.PRIVACY,      R.string.sec_privacy_read);
  public final Item recordReadHistory            = item("record_read_history",              R.string.opt_record_read_history_label,              R.string.opt_record_read_history_desc,              false, Category.PRIVACY,      R.string.sec_privacy_read);
  public final Item showEditHistory              = item("show_edit_history",                R.string.opt_show_edit_history_label,                R.string.opt_show_edit_history_desc,                false, Category.PRIVACY,      R.string.sec_privacy_edit);
  public final Item preventUnsendMessage         = item("prevent_unsend_message",           R.string.opt_prevent_unsend_message_label,           R.string.opt_prevent_unsend_message_desc,           false, Category.PRIVACY,      R.string.sec_privacy_unsend);
  public final Item spoofVersionUnsendOnly       = item("spoof_version_unsend_only",        R.string.opt_spoof_version_unsend_only_label,        R.string.opt_spoof_version_unsend_only_desc,        false, Category.PRIVACY,      R.string.sec_privacy_unsend);
  public final Item showProfileTimestamps        = item("show_profile_timestamps",          R.string.opt_show_profile_timestamps_label,          R.string.opt_show_profile_timestamps_desc,          false, Category.PRIVACY,      R.string.sec_privacy_profile);
  public final Item enableMuteMessage            = item("enable_mute_message",              R.string.opt_enable_mute_message_label,              R.string.opt_enable_mute_message_desc,              false, Category.CHAT,         R.string.sec_chat_send);
  public final Item highQualityPhoto             = item("high_quality_photo",               R.string.opt_high_quality_photo_label,               R.string.opt_high_quality_photo_desc,               false, Category.CHAT,         R.string.sec_chat_media);
  public final Item longVideo                    = item("long_video",                       R.string.opt_long_video_label,                       R.string.opt_long_video_desc,                       false, Category.CHAT,         R.string.sec_chat_media);
  public final Item useDefaultCamera             = item("use_default_camera",               R.string.opt_use_default_camera_label,               R.string.opt_use_default_camera_desc,               false, Category.CHAT,         R.string.sec_chat_media);
  public final Item muteCameraShutter            = item("mute_camera_shutter",              R.string.opt_mute_camera_shutter_label,              R.string.opt_mute_camera_shutter_desc,              false, Category.CHAT,         R.string.sec_chat_media, "use_default_camera");
  public final Item searchByMember               = item("search_by_member",                 R.string.opt_search_by_member_label,                 R.string.opt_search_by_member_desc,                 false, Category.CHAT,         R.string.sec_chat_search);
  public final Item searchMin1Char               = item("search_min_1_char",                R.string.opt_search_min_1_char_label,                R.string.opt_search_min_1_char_desc,                false, Category.CHAT,         R.string.sec_chat_search);
  public final Item showSecondsInChatTime        = item("show_seconds_in_chat_time",        R.string.opt_show_seconds_in_chat_time_label,        R.string.opt_show_seconds_in_chat_time_desc,        false, Category.CHAT,         R.string.sec_chat_display);
  public final Item selectAllInEditMode          = item("select_all_in_edit_mode",          R.string.opt_select_all_in_edit_mode_label,          R.string.opt_select_all_in_edit_mode_desc,          false, Category.CHAT,         R.string.sec_chat_display);
  public final Item hideAiIconPermanently        = item("hide_ai_icon_permanently",         R.string.opt_hide_ai_icon_permanently_label,         R.string.opt_hide_ai_icon_permanently_desc,         false, Category.CHAT,         R.string.sec_chat_display);
  public final Item fixAnnouncementName          = item("fix_announcement_name",            R.string.opt_fix_announcement_name_label,            R.string.opt_fix_announcement_name_desc,            false, Category.CHAT,         R.string.sec_chat_display);
  public final Item openUrlInDefaultBrowser      = item("open_url_in_default_browser",      R.string.opt_open_url_in_default_browser_label,      R.string.opt_open_url_in_default_browser_desc,      false, Category.CHAT,         R.string.sec_chat_display);
  public final Item removeAds                    = item("remove_ads",                       R.string.opt_remove_ads_label,                       R.string.opt_remove_ads_desc,                       false, Category.DISPLAY,      R.string.sec_ads);
  public final Item removeHomeRecommendations    = item("remove_home_recommendations",      R.string.opt_remove_home_recommendations_label,      R.string.opt_remove_home_recommendations_desc,      false, Category.DISPLAY,      R.string.sec_ads);
  public final Item removeHomeServices           = item("remove_home_services",             R.string.opt_remove_home_services_label,             R.string.opt_remove_home_services_desc,             false, Category.DISPLAY,      R.string.sec_ads);
  public final Item removeHomeAccordion          = item("remove_home_accordion",            R.string.opt_remove_home_accordion_label,            R.string.opt_remove_home_accordion_desc,            false, Category.DISPLAY,      R.string.sec_ads);
  public final Item removeTabVoom                = item("remove_tab_voom",                  R.string.opt_remove_tab_voom_label,                  R.string.opt_remove_tab_voom_desc,                  false, Category.DISPLAY,      R.string.sec_tabs);
  public final Item removeTabNews                = item("remove_tab_news",                  R.string.opt_remove_tab_news_label,                  R.string.opt_remove_tab_news_desc,                  false, Category.DISPLAY,      R.string.sec_tabs);
  public final Item removeTabMini                = item("remove_tab_mini",                  R.string.opt_remove_tab_mini_label,                  R.string.opt_remove_tab_mini_desc,                  false, Category.DISPLAY,      R.string.sec_tabs);
  public final Item removeTabCommerce            = item("remove_tab_commerce",              R.string.opt_remove_tab_commerce_label,              R.string.opt_remove_tab_commerce_desc,              false, Category.DISPLAY,      R.string.sec_tabs);
  public final Item removeTabWallet              = item("remove_tab_wallet",                R.string.opt_remove_tab_wallet_label,                R.string.opt_remove_tab_wallet_desc,                false, Category.DISPLAY,      R.string.sec_tabs);
  public final Item hideTabText                  = item("hide_tab_text",                    R.string.opt_hide_tab_text_label,                    R.string.opt_hide_tab_text_desc,                    false, Category.DISPLAY,      R.string.sec_tabs);
  public final Item extendTabClickArea           = item("extend_tab_click_area",            R.string.opt_extend_tab_click_area_label,            R.string.opt_extend_tab_click_area_desc,            false, Category.DISPLAY,      R.string.sec_tabs);
  public final Item homeTabType                  = item("home_tab_type",                    R.string.opt_home_tab_type_label,                    R.string.opt_home_tab_type_desc,                    false, Category.DISPLAY,      R.string.sec_tabs);
  public final Item removeAiFriendsButton        = item("remove_ai_friends_button",         R.string.opt_remove_ai_friends_button_label,         R.string.opt_remove_ai_friends_button_desc,         false, Category.DISPLAY,      R.string.sec_header_btn);
  public final Item removeOpenChatButton         = item("remove_open_chat_button",          R.string.opt_remove_open_chat_button_label,          R.string.opt_remove_open_chat_button_desc,          false, Category.DISPLAY,      R.string.sec_header_btn);
  public final Item removeCalendarButton         = item("remove_calendar_button",           R.string.opt_remove_calendar_button_label,           R.string.opt_remove_calendar_button_desc,           false, Category.DISPLAY,      R.string.sec_header_btn);
  public final Item removeAlbumButton            = item("remove_album_button",              R.string.opt_remove_album_button_label,              R.string.opt_remove_album_button_desc,              false, Category.DISPLAY,      R.string.sec_header_btn);
  public final Item removeSearchBarAgentIButton  = item("remove_search_bar_agent_i_button", R.string.opt_remove_search_bar_agent_i_button_label, R.string.opt_remove_search_bar_agent_i_button_desc, false, Category.DISPLAY,      R.string.sec_header_btn);
  public final Item useCustomFont                = item("use_custom_font",                  R.string.opt_use_custom_font_label,                  R.string.opt_use_custom_font_desc,                  false, Category.DISPLAY,      R.string.sec_font);
  public final Item customFontPath               = item("custom_font_path",                 R.string.opt_custom_font_path_label,                 R.string.opt_custom_font_path_desc,                 false, Category.DISPLAY,      R.string.sec_font);
  public final Item useAmoledTheme               = item("use_amoled_theme",                 R.string.opt_use_amoled_theme_label,                 R.string.opt_use_amoled_theme_desc,                 false, Category.DISPLAY,      R.string.sec_theme);
  public final Item forceDarkModeUi              = item("force_dark_mode_ui",               R.string.opt_force_dark_mode_ui_label,               R.string.opt_force_dark_mode_ui_desc,               false, Category.DISPLAY,      R.string.sec_theme, "use_amoled_theme");
  public final Item showThemeOnSubDevice         = item("show_theme_on_sub_device",         R.string.opt_show_theme_on_sub_device_label,         R.string.opt_show_theme_on_sub_device_desc,         false, Category.DISPLAY,      R.string.sec_theme);
  public final Item reactionNotification         = item("reaction_notification",            R.string.opt_reaction_notification_label,            R.string.opt_reaction_notification_desc,            false, Category.NOTIFICATION, 0);
  public final Item stackMessageNotifications    = item("stack_message_notifications",      R.string.opt_stack_message_notifications_label,      R.string.opt_stack_message_notifications_desc,      false, Category.NOTIFICATION, 0);
  public final Item removeNotificationMuteButton = item("remove_notification_mute_button",  R.string.opt_remove_notification_mute_button_label,  R.string.opt_remove_notification_mute_button_desc,  false, Category.NOTIFICATION, 0);
  public final Item experimentalFcmFix           = item("experimental_fcm_fix",             R.string.opt_experimental_fcm_fix_label,             R.string.opt_experimental_fcm_fix_desc,             false, Category.NOTIFICATION, 0);
  public final Item fcmFixMode                   = item("fcm_fix_mode",                     R.string.opt_fcm_fix_mode_label,                     R.string.opt_fcm_fix_mode_desc,                     false, Category.NOTIFICATION, 0);
  public final Item fcmForceRegistration         = item("fcm_force_registration",           R.string.opt_fcm_force_registration_label,           R.string.opt_fcm_force_registration_desc,           false, Category.NOTIFICATION, 0);
  public final Item lineForegroundKeepAlive      = item("line_foreground_keep_alive",       R.string.opt_line_foreground_keep_alive_label,       R.string.opt_line_foreground_keep_alive_desc,       false, Category.NOTIFICATION, 0);
  public final Item safeSettingsResources        = item("safe_settings_resources",          R.string.opt_fix_settings_talk_crash_label,          R.string.opt_fix_settings_talk_crash_desc,          true,  Category.SYSTEM,       0);
  public final Item spoofVersion                 = item("spoof_version",                    R.string.opt_spoof_version_label,                    R.string.opt_spoof_version_desc,                    false, Category.SYSTEM,       0);
  public final Item fixSignatureMismatch         = item("fix_signature_mismatch",           R.string.opt_fix_signature_mismatch_label,           R.string.opt_fix_signature_mismatch_desc,           true,  Category.SYSTEM,       0);
  // @formatter:on

  public final Item[] items = _reg.toArray(new Item[0]);

  public Item find(String key) {
    for (Item i : items) {
      if (i.key.equals(key)) return i;
    }
    return null;
  }
}
