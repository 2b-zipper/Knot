package app.zipper.knot;

import static app.zipper.knot.utils.ModuleStrings.*;

import java.util.ArrayList;
import java.util.List;

public class KnotConfig {

  public enum Category {
    PRIVACY(CAT_PRIVACY),
    CHAT(CAT_CHAT),
    DISPLAY(CAT_DISPLAY),
    NOTIFICATION(CAT_NOTIFICATION),
    SYSTEM(CAT_SYSTEM),
    BACKUP(CAT_BACKUP),
    OTHER(CAT_OTHER);

    public final String label;

    Category(String label) {
      this.label = label;
    }
  }

  public static final class Item {
    public final String key;
    public final String label;
    public final String description;
    public boolean enabled;
    public String value = "";
    public final Category category;
    public final String section;
    public String disabledWhenEnabledKey;

    Item(String key, String label, String desc, boolean def, Category cat, String sec) {
      this.key = key;
      this.label = label;
      this.description = desc;
      this.enabled = def;
      this.category = cat;
      this.section = sec;
    }
  }

  private final List<Item> _reg = new ArrayList<>();

  private Item item(String key, String label, String desc, boolean def, Category cat, String sec) {
    Item i = new Item(key, label, desc, def, cat, sec);
    _reg.add(i);
    return i;
  }

  private Item item(
      String key,
      String label,
      String desc,
      boolean def,
      Category cat,
      String sec,
      String disabledWhenEnabledKey) {
    Item i = item(key, label, desc, def, cat, sec);
    i.disabledWhenEnabledKey = disabledWhenEnabledKey;
    return i;
  }

  // @formatter:off
  public final Item preventMarkAsRead            = item("prevent_mark_as_read",             OPT_PREVENT_MARK_AS_READ_LABEL,             OPT_PREVENT_MARK_AS_READ_DESC,             false, Category.PRIVACY,      SEC_PRIVACY_READ);
  public final Item recordReadHistory            = item("record_read_history",              OPT_RECORD_READ_HISTORY_LABEL,              OPT_RECORD_READ_HISTORY_DESC,              false, Category.PRIVACY,      SEC_PRIVACY_READ);
  public final Item preventUnsendMessage         = item("prevent_unsend_message",           OPT_PREVENT_UNSEND_MESSAGE_LABEL,           OPT_PREVENT_UNSEND_MESSAGE_DESC,           false, Category.PRIVACY,      SEC_PRIVACY_UNSEND);
  public final Item spoofVersionUnsendOnly       = item("spoof_version_unsend_only",        OPT_SPOOF_VERSION_UNSEND_ONLY_LABEL,        OPT_SPOOF_VERSION_UNSEND_ONLY_DESC,        false, Category.PRIVACY,      SEC_PRIVACY_UNSEND);
  public final Item showProfileTimestamps        = item("show_profile_timestamps",          OPT_SHOW_PROFILE_TIMESTAMPS_LABEL,          OPT_SHOW_PROFILE_TIMESTAMPS_DESC,          false, Category.PRIVACY,      SEC_PRIVACY_PROFILE);
  public final Item highQualityPhoto             = item("high_quality_photo",               OPT_HIGH_QUALITY_PHOTO_LABEL,               OPT_HIGH_QUALITY_PHOTO_DESC,               false, Category.CHAT,         SEC_CHAT_MEDIA);
  public final Item longVideo                    = item("long_video",                       OPT_LONG_VIDEO_LABEL,                       OPT_LONG_VIDEO_DESC,                       false, Category.CHAT,         SEC_CHAT_MEDIA);
  public final Item useDefaultCamera             = item("use_default_camera",               OPT_USE_DEFAULT_CAMERA_LABEL,               OPT_USE_DEFAULT_CAMERA_DESC,               false, Category.CHAT,         SEC_CHAT_MEDIA);
  public final Item muteCameraShutter            = item("mute_camera_shutter",              OPT_MUTE_CAMERA_SHUTTER_LABEL,              OPT_MUTE_CAMERA_SHUTTER_DESC,              false, Category.CHAT,         SEC_CHAT_MEDIA, "use_default_camera");
  public final Item searchByMember               = item("search_by_member",                 OPT_SEARCH_BY_MEMBER_LABEL,                 OPT_SEARCH_BY_MEMBER_DESC,                 false, Category.CHAT,         SEC_CHAT_SEARCH);
  public final Item searchMin1Char               = item("search_min_1_char",                OPT_SEARCH_MIN_1_CHAR_LABEL,                OPT_SEARCH_MIN_1_CHAR_DESC,                false, Category.CHAT,         SEC_CHAT_SEARCH);
  public final Item showSecondsInChatTime        = item("show_seconds_in_chat_time",        OPT_SHOW_SECONDS_IN_CHAT_TIME_LABEL,        OPT_SHOW_SECONDS_IN_CHAT_TIME_DESC,        false, Category.CHAT,         SEC_CHAT_DISPLAY);
  public final Item selectAllInEditMode          = item("select_all_in_edit_mode",          OPT_SELECT_ALL_IN_EDIT_MODE_LABEL,          OPT_SELECT_ALL_IN_EDIT_MODE_DESC,          false, Category.CHAT,         SEC_CHAT_DISPLAY);
  public final Item hideAiIconPermanently        = item("hide_ai_icon_permanently",         OPT_HIDE_AI_ICON_PERMANENTLY_LABEL,         OPT_HIDE_AI_ICON_PERMANENTLY_DESC,         false, Category.CHAT,         SEC_CHAT_DISPLAY);
  public final Item fixAnnouncementName          = item("fix_announcement_name",            OPT_FIX_ANNOUNCEMENT_NAME_LABEL,            OPT_FIX_ANNOUNCEMENT_NAME_DESC,            false, Category.CHAT,         SEC_CHAT_DISPLAY);
  public final Item openUrlInDefaultBrowser      = item("open_url_in_default_browser",      OPT_OPEN_URL_IN_DEFAULT_BROWSER_LABEL,      OPT_OPEN_URL_IN_DEFAULT_BROWSER_DESC,      false, Category.CHAT,         SEC_CHAT_DISPLAY);
  public final Item removeAds                    = item("remove_ads",                       OPT_REMOVE_ADS_LABEL,                       OPT_REMOVE_ADS_DESC,                       false, Category.DISPLAY,      SEC_ADS);
  public final Item removeHomeRecommendations    = item("remove_home_recommendations",      OPT_REMOVE_HOME_RECOMMENDATIONS_LABEL,      OPT_REMOVE_HOME_RECOMMENDATIONS_DESC,      false, Category.DISPLAY,      SEC_ADS);
  public final Item removeHomeServices           = item("remove_home_services",             OPT_REMOVE_HOME_SERVICES_LABEL,             OPT_REMOVE_HOME_SERVICES_DESC,             false, Category.DISPLAY,      SEC_ADS);
  public final Item removeHomeAccordion          = item("remove_home_accordion",            OPT_REMOVE_HOME_ACCORDION_LABEL,            OPT_REMOVE_HOME_ACCORDION_DESC,            false, Category.DISPLAY,      SEC_ADS);
  public final Item removeTabVoom                = item("remove_tab_voom",                  OPT_REMOVE_TAB_VOOM_LABEL,                  OPT_REMOVE_TAB_VOOM_DESC,                  false, Category.DISPLAY,      SEC_TABS);
  public final Item removeTabNews                = item("remove_tab_news",                  OPT_REMOVE_TAB_NEWS_LABEL,                  OPT_REMOVE_TAB_NEWS_DESC,                  false, Category.DISPLAY,      SEC_TABS);
  public final Item removeTabMini                = item("remove_tab_mini",                  OPT_REMOVE_TAB_MINI_LABEL,                  OPT_REMOVE_TAB_MINI_DESC,                  false, Category.DISPLAY,      SEC_TABS);
  public final Item removeTabCommerce            = item("remove_tab_commerce",              OPT_REMOVE_TAB_COMMERCE_LABEL,              OPT_REMOVE_TAB_COMMERCE_DESC,              false, Category.DISPLAY,      SEC_TABS);
  public final Item removeTabWallet              = item("remove_tab_wallet",                OPT_REMOVE_TAB_WALLET_LABEL,                OPT_REMOVE_TAB_WALLET_DESC,                false, Category.DISPLAY,      SEC_TABS);
  public final Item hideTabText                  = item("hide_tab_text",                    OPT_HIDE_TAB_TEXT_LABEL,                    OPT_HIDE_TAB_TEXT_DESC,                    false, Category.DISPLAY,      SEC_TABS);
  public final Item extendTabClickArea           = item("extend_tab_click_area",            OPT_EXTEND_TAB_CLICK_AREA_LABEL,            OPT_EXTEND_TAB_CLICK_AREA_DESC,            false, Category.DISPLAY,      SEC_TABS);
  public final Item homeTabType                  = item("home_tab_type",                    OPT_HOME_TAB_TYPE_LABEL,                    OPT_HOME_TAB_TYPE_DESC,                    false, Category.DISPLAY,      SEC_TABS);
  public final Item removeAiFriendsButton        = item("remove_ai_friends_button",         OPT_REMOVE_AI_FRIENDS_BUTTON_LABEL,         OPT_REMOVE_AI_FRIENDS_BUTTON_DESC,         false, Category.DISPLAY,      SEC_HEADER_BTN);
  public final Item removeOpenChatButton         = item("remove_open_chat_button",          OPT_REMOVE_OPEN_CHAT_BUTTON_LABEL,          OPT_REMOVE_OPEN_CHAT_BUTTON_DESC,          false, Category.DISPLAY,      SEC_HEADER_BTN);
  public final Item removeCalendarButton         = item("remove_calendar_button",           OPT_REMOVE_CALENDAR_BUTTON_LABEL,           OPT_REMOVE_CALENDAR_BUTTON_DESC,           false, Category.DISPLAY,      SEC_HEADER_BTN);
  public final Item removeAlbumButton            = item("remove_album_button",              OPT_REMOVE_ALBUM_BUTTON_LABEL,              OPT_REMOVE_ALBUM_BUTTON_DESC,              false, Category.DISPLAY,      SEC_HEADER_BTN);
  public final Item removeSearchBarAgentIButton  = item("remove_search_bar_agent_i_button", OPT_REMOVE_SEARCH_BAR_AGENT_I_BUTTON_LABEL, OPT_REMOVE_SEARCH_BAR_AGENT_I_BUTTON_DESC, false, Category.DISPLAY,      SEC_HEADER_BTN);
  public final Item useCustomFont                = item("use_custom_font",                  OPT_USE_CUSTOM_FONT_LABEL,                  OPT_USE_CUSTOM_FONT_DESC,                  false, Category.DISPLAY,      SEC_FONT);
  public final Item customFontPath               = item("custom_font_path",                 OPT_CUSTOM_FONT_PATH_LABEL,                 OPT_CUSTOM_FONT_PATH_DESC,                 false, Category.DISPLAY,      SEC_FONT);
  public final Item useAmoledTheme               = item("use_amoled_theme",                 OPT_USE_AMOLED_THEME_LABEL,                 OPT_USE_AMOLED_THEME_DESC,                 false, Category.DISPLAY,      SEC_THEME);
  public final Item forceDarkModeUi              = item("force_dark_mode_ui",               OPT_FORCE_DARK_MODE_UI_LABEL,               OPT_FORCE_DARK_MODE_UI_DESC,               false, Category.DISPLAY,      SEC_THEME, "use_amoled_theme");
  public final Item showThemeOnSubDevice         = item("show_theme_on_sub_device",         OPT_SHOW_THEME_ON_SUB_DEVICE_LABEL,         OPT_SHOW_THEME_ON_SUB_DEVICE_DESC,         false, Category.DISPLAY,      SEC_THEME);
  public final Item reactionNotification         = item("reaction_notification",            OPT_REACTION_NOTIFICATION_LABEL,            OPT_REACTION_NOTIFICATION_DESC,            false, Category.NOTIFICATION, "");
  public final Item stackMessageNotifications    = item("stack_message_notifications",      OPT_STACK_MESSAGE_NOTIFICATIONS_LABEL,      OPT_STACK_MESSAGE_NOTIFICATIONS_DESC,      false, Category.NOTIFICATION, "");
  public final Item removeNotificationMuteButton = item("remove_notification_mute_button",  OPT_REMOVE_NOTIFICATION_MUTE_BUTTON_LABEL,  OPT_REMOVE_NOTIFICATION_MUTE_BUTTON_DESC,  false, Category.NOTIFICATION, "");
  public final Item experimentalFcmFix           = item("experimental_fcm_fix",             OPT_EXPERIMENTAL_FCM_FIX_LABEL,             OPT_EXPERIMENTAL_FCM_FIX_DESC,             false, Category.NOTIFICATION, "");
  public final Item lineForegroundKeepAlive      = item("line_foreground_keep_alive",       OPT_LINE_FOREGROUND_KEEP_ALIVE_LABEL,       OPT_LINE_FOREGROUND_KEEP_ALIVE_DESC,       false, Category.NOTIFICATION, "");
  public final Item safeSettingsResources        = item("safe_settings_resources",          OPT_FIX_SETTINGS_TALK_CRASH_LABEL,          OPT_FIX_SETTINGS_TALK_CRASH_DESC,          true,  Category.SYSTEM,       "");
  public final Item spoofVersion                 = item("spoof_version",                    OPT_SPOOF_VERSION_LABEL,                    OPT_SPOOF_VERSION_DESC,                    false, Category.SYSTEM,       "");
  public final Item fixSignatureMismatch         = item("fix_signature_mismatch",           OPT_FIX_SIGNATURE_MISMATCH_LABEL,           OPT_FIX_SIGNATURE_MISMATCH_DESC,           true,  Category.SYSTEM,       "");
  // @formatter:on

  public final Item[] items = _reg.toArray(new Item[0]);
}
