package app.zipper.knot;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import app.zipper.knot.hooks.*;
import app.zipper.knot.utils.LineTheme;
import app.zipper.knot.utils.ModuleResources;
import io.github.libxposed.api.XposedModule;
import java.lang.reflect.Method;

public class Main extends XposedModule {

  public static final String TAG = "Knot";
  public static final KnotConfig options = new KnotConfig();
  private static final String LINE_PKG = "jp.naver.line.android";

  private static volatile boolean initialized;

  @Override
  public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
    Knot.module = this;
    Knot.processName = param.getProcessName();
    log(Log.INFO, TAG, "Knot loaded in " + param.getProcessName());
  }

  @Override
  public void onPackageReady(@NonNull PackageReadyParam param) {
    if (!LINE_PKG.equals(param.getPackageName())) return;
    Knot.module = this;
    bootstrap(param);
  }

  private void bootstrap(PackageReadyParam param) {
    LoadParam lpparam =
        new LoadParam(param.getClassLoader(), param.getPackageName(), Knot.processName);

    // AOT can inline the small ContextWrapper.attachBaseContext and bypass that hook (issue #39).
    // LINE's own override cannot be: its only caller is Application.attach in the boot image.
    hookAttachBaseContext(applicationClass(param), lpparam);
    hookAttachBaseContext(ContextWrapper.class, lpparam);

    // Non-null only once the application attached, so the hooks above went in too late for it
    Context attached = Knot.currentApplication();
    if (attached != null) initialize(attached, lpparam, "late dispatch");
  }

  private void hookAttachBaseContext(Class<?> owner, LoadParam lpparam) {
    if (owner == null) return;
    Method method;
    try {
      method = owner.getDeclaredMethod("attachBaseContext", Context.class);
    } catch (NoSuchMethodException absent) {
      return;
    }
    try {
      hook(method)
          .intercept(
              chain -> {
                Object result = chain.proceed();
                Context context = (Context) chain.getArg(0);
                if (context != null) initialize(context, lpparam, owner.getSimpleName());
                return result;
              });
    } catch (Throwable t) {
      Knot.log("Knot: attachBaseContext hook failed on " + owner.getName() + ": " + t);
    }
  }

  private static Class<?> applicationClass(PackageReadyParam param) {
    ApplicationInfo info = param.getApplicationInfo();
    String name = info == null ? null : info.className;
    if (name == null || name.isEmpty()) return null;
    try {
      return Class.forName(name, false, param.getClassLoader());
    } catch (Throwable t) {
      Knot.log("Knot: application class unavailable: " + t);
      return null;
    }
  }

  // via is logged so a stuck install shows which route reached this, if any
  private void initialize(Context context, LoadParam lpparam, String via) {
    synchronized (Main.class) {
      if (initialized) return;
      initialized = true;
    }
    Knot.log("Knot: bootstrap via " + via);

    ModuleResources.attach(context);

    LineVersion.Config cfg = LineVersion.detectWithContext(context);
    if (cfg == null) cfg = LineVersion.detect(lpparam.classLoader);
    if (cfg == null) {
      handleUnsupportedVersion(lpparam);
    } else {
      initializeModule(context, lpparam);
    }
  }

  private void initializeModule(Context context, LoadParam lpparam) {
    SettingsStore.setContext(context);
    SettingsStore.load(options);
    SettingsStore.setLoaded(true);
    // Re-read the language now that the store can serve it
    ModuleResources.invalidate();

    Knot.log("Knot: Initializing Knot hooks...");

    applyHook(new SettingsUIInjector(), lpparam);
    applyHook(new SettingsButtonLongPress(), lpparam);
    applyHook(new ShowConfigWarning(), lpparam);
    applyHook(new ReleaseNotesPopup(), lpparam);
    applyHook(new HomeSettingsTooltip(), lpparam);
    applyHook(new SafeResourceFix(), lpparam);

    // Always installed; self-gates at runtime to avoid the cold-start settings-load race
    applyHook(new ReadReceiptHandler(), lpparam);
    applyHook(new UnsendProtector(), lpparam);

    if (options.recordReadHistory.enabled || options.preventMarkAsRead.enabled) {
      applyHook(new PlusMenuHook(), lpparam);
      applyHook(new ChatListMoreMenuHook(), lpparam);
    }
    if (options.recordReadHistory.enabled) applyHook(new HeaderButtonInjector(), lpparam);
    if (options.hideAiIconPermanently.enabled) {
      applyHook(new RemoveTalkRoomAgentIToggle(), lpparam);
      applyHook(new HideAiIconPermanently(), lpparam);
    }
    if (options.openUrlInDefaultBrowser.enabled) {
      applyHook(new OpenInExternalBrowserHook(), lpparam);
    }
    if (options.highQualityPhoto.enabled) applyHook(new ImageQuality(), lpparam);
    if (options.longVideo.enabled) applyHook(new LongVideoHook(), lpparam);
    if (options.searchByMember.enabled) applyHook(new SearchByMemberHook(), lpparam);
    if (options.searchMin1Char.enabled) {
      applyHook(new SearchMin1CharHook(), lpparam);
      applyHook(new SearchResultCountHook(), lpparam);
    }
    if (options.fixAnnouncementName.enabled) applyHook(new AnnouncementNameFix(), lpparam);
    if (options.showSecondsInChatTime.enabled) applyHook(new ChatTimestampSeconds(), lpparam);
    if (options.selectAllInEditMode.enabled) applyHook(new ChatEditSelectAllHook(), lpparam);
    if (options.showEditHistory.enabled) applyHook(new EditHistoryHook(), lpparam);
    if (options.enableMuteMessage.enabled) applyHook(new MuteMessageHook(), lpparam);
    if (options.useDefaultCamera.enabled) applyHook(new UseDefaultCameraHook(), lpparam);
    if (options.muteCameraShutter.enabled) applyHook(new CameraShutterMuteHook(), lpparam);
    if (options.showProfileTimestamps.enabled) applyHook(new ProfileTimestampsHook(), lpparam);

    if (options.removeAds.enabled) applyHook(new RemoveAds(), lpparam);
    if (options.removeHomeRecommendations.enabled
        || options.removeHomeServices.enabled
        || options.removeHomeAccordion.enabled) {
      applyHook(new RemoveHomeContents(), lpparam);
    }
    if (options.removeTabVoom.enabled
        || options.removeTabNews.enabled
        || options.removeTabMini.enabled
        || options.hideTabText.enabled
        || options.extendTabClickArea.enabled) {
      applyHook(new RemoveTabs(), lpparam);
    }
    if (options.removeAiFriendsButton.enabled
        || options.removeOpenChatButton.enabled
        || options.removeAlbumButton.enabled
        || options.removeCalendarButton.enabled
        || options.removeSearchBarAgentIButton.enabled) {
      applyHook(new RemoveHeaderButtons(), lpparam);
    }
    if (options.homeTabType.value != null && !options.homeTabType.value.isEmpty()) {
      applyHook(new HomeTabTypeHook(), lpparam);
    }
    if (options.useCustomFont.enabled) applyHook(new FontUnlockHook(), lpparam);
    if (options.useAmoledTheme.enabled) applyHook(new AmoledThemeHook(), lpparam);
    if (options.forceDarkModeUi.enabled) applyHook(new ForceDarkModeUiHook(), lpparam);
    if (options.showThemeOnSubDevice.enabled) applyHook(new ShowThemeOnSubDeviceHook(), lpparam);

    if (options.reactionNotification.enabled) applyHook(new ReactionNotification(), lpparam);
    if (options.removeNotificationMuteButton.enabled) applyHook(new NotificationHook(), lpparam);
    if (options.stackMessageNotifications.enabled) {
      applyHook(new StackMessageNotificationsHook(), lpparam);
    }
    if (options.lineForegroundKeepAlive.enabled) {
      applyHook(new LineForegroundKeepAliveHook(), lpparam);
    }
    if (options.experimentalFcmFix.enabled) applyHook(new FcmFixHook(), lpparam);
    if (options.spoofVersion.enabled || options.spoofVersionUnsendOnly.enabled) {
      applyHook(new VersionSpoof(), lpparam);
    }
    if (options.fixSignatureMismatch.enabled) applyHook(new SignatureSpoofHook(), lpparam);
  }

  private void applyHook(BaseHook hook, LoadParam lpparam) {
    try {
      hook.hook(options, lpparam);
    } catch (Throwable t) {
      Knot.log("Knot: Hook failed for " + hook.getClass().getSimpleName() + ": " + t);
    }
  }

  private void handleUnsupportedVersion(LoadParam lpparam) {
    final String msg =
        ModuleResources.get(R.string.unsupported_version_msg)
            + " (Supported: "
            + LineVersion.getSupportedVersions()
            + ")";

    try {
      Method onCreate =
          Reflect.findMethodExact(
              "jp.naver.line.android.activity.main.MainActivity",
              lpparam.classLoader,
              "onCreate",
              Bundle.class);
      hook(onCreate)
          .intercept(
              chain -> {
                Object result = chain.proceed();
                Activity activity = (Activity) chain.getThisObject();
                LineTheme.applyDialogColors(
                    new AlertDialog.Builder(activity, LineTheme.dialogTheme(activity))
                        .setTitle(ModuleResources.get(R.string.unsupported_version_title))
                        .setMessage(msg)
                        .setPositiveButton("OK", null)
                        .show(),
                    activity);
                return result;
              });
    } catch (Throwable t) {
      Knot.log("Knot: unsupported-version dialog hook failed: " + t);
    }
  }
}
