package app.zipper.knot.hooks;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.R;
import app.zipper.knot.Reflect;
import app.zipper.knot.SettingsStore;
import app.zipper.knot.mapping.Mappings;
import app.zipper.knot.ui.settings.KnotSettingsDialog;
import app.zipper.knot.ui.settings.SettingsPage;
import app.zipper.knot.ui.settings.SettingsViews;
import app.zipper.knot.utils.LineTheme;
import app.zipper.knot.utils.ModuleResources;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BrokenFeatureWarning implements BaseHook {

  private static final String WARNED_VERSION_KEY = "broken_feature_warned";

  private static volatile boolean handled;

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    LineVersion.Config cfg = LineVersion.get();
    if (cfg == null) return;
    Class<?> activityCls = lpparam.classLoader.loadClass(cfg.main.mainActivity);

    Knot.module
        .hook(Reflect.findMethodExact(activityCls, "onResume"))
        .intercept(
            chain -> {
              Object result = chain.proceed();
              maybeShow((Activity) chain.getThisObject());
              return result;
            });
  }

  private static void maybeShow(Activity host) {
    if (handled || !SettingsStore.isConfigured()) return;
    handled = true;
    String version = LineVersion.getVersionName(host);
    if (version == null || version.equals(SettingsStore.getString(WARNED_VERSION_KEY, ""))) return;

    Context context = host.getApplicationContext();
    WeakReference<Activity> hostRef = new WeakReference<>(host);
    new Thread(
            () -> {
              List<String> broken = brokenFeatures(context, version);
              Activity activity = hostRef.get();
              if (!broken.isEmpty() && activity != null) {
                activity.runOnUiThread(() -> show(activity, version, broken));
              }
            },
            "knot-broken-features")
        .start();
  }

  private static void show(Activity host, String version, List<String> broken) {
    if (host.isFinishing() || host.isDestroyed()) return;
    try {
      StringBuilder message =
          new StringBuilder(ModuleResources.get(R.string.broken_features_msg, version));
      message.append('\n');
      for (String feature : broken) message.append("\n・").append(feature);
      LineTheme.applyDialogColors(
          new AlertDialog.Builder(host, LineTheme.dialogTheme(host))
              .setTitle(ModuleResources.get(R.string.broken_features_title))
              .setMessage(message)
              .setPositiveButton(ModuleResources.get(R.string.common_close), null)
              .show(),
          host);
      SettingsStore.save(WARNED_VERSION_KEY, version);
    } catch (Throwable t) {
      Knot.log("Knot: broken features notice failed: " + t);
    }
  }

  private static List<String> brokenFeatures(Context context, String version) {
    List<String> unresolved = Mappings.unresolved(LineVersion.get(), version);
    if (unresolved.isEmpty()) return Collections.emptyList();
    Map<String, Set<String>> features = features();
    Set<String> owners = new HashSet<>();
    for (Set<String> classes : features.values()) owners.addAll(classes);
    Set<String> users = Mappings.users(context, unresolved, owners::contains);

    List<String> broken = new ArrayList<>();
    for (Map.Entry<String, Set<String>> e : features.entrySet()) {
      if (!Collections.disjoint(e.getValue(), users)) broken.add(e.getKey());
    }
    return broken;
  }

  // The classes Main installs for each setting, plus the module's own settings screens
  private static Map<String, Set<String>> features() {
    KnotConfig o = Main.options;
    Map<String, Set<String>> f = new LinkedHashMap<>();
    add(
        f,
        ModuleResources.get(R.string.settings_title),
        SettingsUIInjector.class,
        SettingsButtonLongPress.class,
        HomeSettingsTooltip.class,
        ShowConfigWarning.class,
        ReleaseNotesPopup.class,
        KnotSettingsDialog.class,
        SettingsPage.class,
        SettingsViews.class,
        LineTheme.class);
    add(
        f,
        o.preventMarkAsRead,
        ReadReceiptHandler.class,
        PlusMenuHook.class,
        ChatListMoreMenuHook.class);
    add(f, o.recordReadHistory, HeaderButtonInjector.class);
    add(f, o.showEditHistory, EditHistoryHook.class);
    add(
        f,
        o.permanentHideChat,
        PermanentHideChatHook.class,
        UnhideChatHook.class,
        HiddenChatRowHook.class,
        PlusMenuHook.class,
        ChatListMoreMenuHook.class);
    add(f, o.preventUnsendMessage, UnsendProtector.class);
    add(f, o.spoofVersionUnsendOnly, VersionSpoof.class);
    add(f, o.showProfileTimestamps, ProfileTimestampsHook.class);
    add(f, o.enableMuteMessage, MuteMessageHook.class);
    add(f, o.highQualityPhoto, ImageQuality.class);
    add(f, o.longVideo, LongVideoHook.class);
    add(f, o.useDefaultCamera, UseDefaultCameraHook.class);
    add(f, o.muteCameraShutter, CameraShutterMuteHook.class);
    add(f, o.searchByMember, SearchByMemberHook.class);
    add(f, o.searchMin1Char, SearchMin1CharHook.class, SearchResultCountHook.class);
    add(f, o.keywordAssistInSearchBar, KeywordAssistInSearchBarHook.class);
    add(f, o.showSecondsInChatTime, ChatTimestampSeconds.class);
    add(f, o.selectAllInEditMode, ChatEditSelectAllHook.class);
    add(f, o.hideAiIconPermanently, HideAiIconPermanently.class);
    add(f, o.openUrlInDefaultBrowser, OpenInExternalBrowserHook.class);
    add(f, o.removeAds, RemoveAds.class);
    add(f, o.removeHomeRecommendations, RemoveHomeContents.class);
    add(f, o.removeHomeServices, RemoveHomeContents.class);
    add(f, o.removeTabNews, RemoveTabs.class);
    add(f, o.removeTabMini, RemoveTabs.class);
    add(f, o.removeTabCommerce, RemoveTabs.class);
    add(f, o.removeTabWallet, RemoveTabs.class);
    add(f, o.hideTabText, RemoveTabs.class);
    add(f, o.extendTabClickArea, RemoveTabs.class);
    add(f, o.homeTabType, HomeTabTypeHook.class);
    add(f, o.removeAiFriendsButton, RemoveHeaderButtons.class);
    add(f, o.removeOpenChatButton, RemoveHeaderButtons.class);
    add(f, o.removeCalendarButton, RemoveHeaderButtons.class);
    add(f, o.removeAlbumButton, RemoveHeaderButtons.class);
    add(
        f,
        o.removeSearchBarAgentIButton,
        RemoveHeaderButtons.class,
        RemoveTalkRoomAgentIToggle.class);
    add(f, o.useCustomFont, FontUnlockHook.class);
    add(f, o.useAmoledTheme, AmoledThemeHook.class);
    add(f, o.forceDarkModeUi, ForceDarkModeUiHook.class);
    add(f, o.showThemeOnSubDevice, ShowThemeOnSubDeviceHook.class);
    add(f, o.reactionNotification, ReactionNotification.class);
    add(f, o.stackMessageNotifications, StackMessageNotificationsHook.class);
    add(
        f,
        o.notificationMediaPreview,
        NotificationMediaSourceHook.class,
        NotificationMediaPreviewHook.class);
    add(f, o.removeNotificationMuteButton, NotificationHook.class);
    add(f, o.notificationMarkAsRead, NotificationMarkReadHook.class);
    add(f, o.experimentalFcmFix, FcmFixHook.class);
    add(f, o.lineForegroundKeepAlive, LineForegroundKeepAliveHook.class);
    add(f, o.disableCustomRingback, DisableCustomRingbackHook.class);
    add(f, o.useCustomRingtone, CustomRingtoneHook.class);
    add(f, o.fixSignatureMismatch, SignatureSpoofHook.class);
    add(f, o.fixDowngradeCrash, RoomDowngradeHook.class);
    return f;
  }

  private static void add(Map<String, Set<String>> f, KnotConfig.Item item, Class<?>... owners) {
    add(f, item.label(), owners);
  }

  private static void add(Map<String, Set<String>> f, String label, Class<?>... owners) {
    Set<String> names = f.computeIfAbsent(label, k -> new HashSet<>());
    for (Class<?> owner : owners) names.add(owner.getName());
  }
}
