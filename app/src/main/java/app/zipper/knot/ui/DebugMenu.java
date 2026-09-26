package app.zipper.knot.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;
import app.zipper.knot.BuildConfig;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.Main;
import app.zipper.knot.RestartActivity;
import app.zipper.knot.SettingsStore;
import app.zipper.knot.ui.settings.KnotSettingsDialog;
import app.zipper.knot.utils.LineTheme;
import app.zipper.knot.utils.ModuleResources;
import io.github.libxposed.api.XposedInterface;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public final class DebugMenu {

  private static final ShakeDetector detector = new ShakeDetector(DebugMenu::onShake);

  private static WeakReference<Activity> foreground = new WeakReference<>(null);
  private static WeakReference<AlertDialog> shown = new WeakReference<>(null);

  private DebugMenu() {}

  public static void install() {
    // currentApplication() stays null until bindApplication returns, and by then the first
    // activity launch is already queued, so this has to jump ahead of it
    new Handler(Looper.getMainLooper()).postAtFrontOfQueue(DebugMenu::register);
  }

  private static void register() {
    Application app = Knot.currentApplication();
    if (app == null) return;
    SensorManager sensors = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);
    Sensor accelerometer =
        sensors == null ? null : sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    if (accelerometer == null) return;
    app.registerActivityLifecycleCallbacks(new Lifecycle(sensors, accelerometer));
  }

  private static final class Lifecycle implements Application.ActivityLifecycleCallbacks {
    private final SensorManager sensors;
    private final Sensor accelerometer;

    Lifecycle(SensorManager sensors, Sensor accelerometer) {
      this.sensors = sensors;
      this.accelerometer = accelerometer;
    }

    @Override
    public void onActivityResumed(Activity activity) {
      foreground = new WeakReference<>(activity);
      if (!Main.options.debugMenu.enabled) return;
      detector.reset();
      sensors.registerListener(detector, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onActivityPaused(Activity activity) {
      if (foreground.get() != activity) return;
      sensors.unregisterListener(detector);
      foreground = new WeakReference<>(null);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}
  }

  private static void onShake() {
    if (!Main.options.debugMenu.enabled) return;
    Activity activity = foreground.get();
    if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
    AlertDialog open = shown.get();
    if (open != null && open.isShowing() && open.getOwnerActivity() == activity) return;
    runSafely(() -> showMenu(activity));
  }

  private static void runSafely(Runnable action) {
    try {
      action.run();
    } catch (Throwable t) {
      Knot.log("Knot: debug menu failed", t);
    }
  }

  private static void showMenu(Activity activity) {
    Map<String, Runnable> items = new LinkedHashMap<>();
    if (LineVersion.get() != null) {
      items.put("Open Knot settings", () -> KnotSettingsDialog.show(activity));
    }
    items.put("Restart LINE", () -> RestartActivity.restartNow(activity));
    items.put("Debug info", () -> showText(activity, "Debug info", debugInfo(activity), null));
    items.put("Hook status", () -> showText(activity, "Hook status", hookStatus(), null));
    items.put("Logs", () -> showText(activity, "Logs", logs(), () -> clearLogs(activity)));

    String[] labels = items.keySet().toArray(new String[0]);
    LineTheme.invalidate();
    AlertDialog.Builder builder =
        new AlertDialog.Builder(activity, LineTheme.dialogTheme(activity))
            .setTitle("Knot Debug")
            .setItems(labels, (d, which) -> runSafely(items.get(labels[which])));
    present(activity, builder);
  }

  private static void showText(Activity activity, String title, String text, Runnable onClear) {
    AlertDialog.Builder builder =
        new AlertDialog.Builder(activity, LineTheme.dialogTheme(activity))
            .setTitle(title)
            .setMessage(text)
            .setPositiveButton("Copy", (d, w) -> runSafely(() -> copy(activity, title, text)));
    if (onClear != null) builder.setNeutralButton("Clear", (d, w) -> runSafely(onClear));
    TextView message = present(activity, builder).findViewById(android.R.id.message);
    if (message != null) {
      message.setTextIsSelectable(true);
      message.setTypeface(Typeface.MONOSPACE);
      message.setTextSize(12);
    }
  }

  private static AlertDialog present(Activity activity, AlertDialog.Builder builder) {
    AlertDialog dialog = builder.setNegativeButton("Close", null).show();
    LineTheme.applyDialogColors(dialog, activity);
    // Builder dialogs never get an owner on their own, and onShake matches on it
    dialog.setOwnerActivity(activity);
    shown = new WeakReference<>(dialog);
    return dialog;
  }

  private static void clearLogs(Context ctx) {
    Knot.clearLogs();
    Toast.makeText(ctx, "Cleared", Toast.LENGTH_SHORT).show();
  }

  private static void copy(Context ctx, String label, String text) {
    ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
    if (clipboard == null) return;
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text));
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show();
    }
  }

  private static String debugInfo(Activity activity) {
    StringBuilder sb = new StringBuilder();
    line(
        sb,
        "Knot",
        () ->
            BuildConfig.VERSION_NAME
                + " ("
                + BuildConfig.VERSION_CODE
                + ", "
                + BuildConfig.BUILD_TYPE
                + ")");
    line(sb, "LINE", () -> lineVersion(activity));
    line(sb, "Android", () -> Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")");
    line(sb, "Device", () -> Build.MANUFACTURER + " " + Build.MODEL + " (" + Build.DEVICE + ")");
    line(sb, "Framework", DebugMenu::framework);
    line(sb, "Process", () -> Knot.processName);
    line(sb, "Screen", () -> activity.getClass().getName());
    line(
        sb,
        "Storage",
        () -> SettingsStore.isConfigured() ? SettingsStore.getSettingsDir() : "not set");
    line(
        sb,
        "Language",
        () -> ModuleResources.language().isEmpty() ? "system" : ModuleResources.language());
    line(sb, "Hooks", DebugMenu::hookSummary);
    line(sb, "Enabled", DebugMenu::enabledOptions);
    return sb.toString().trim();
  }

  private static void line(StringBuilder sb, String key, Callable<String> value) {
    String text;
    try {
      text = value.call();
    } catch (Throwable t) {
      text = "error: " + t;
    }
    sb.append(key).append(": ").append(text).append('\n');
  }

  private static String lineVersion(Activity activity) throws Exception {
    PackageInfo info = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
    long code =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
            ? info.getLongVersionCode()
            : info.versionCode;
    String version = info.versionName + " (" + code + ")";
    if (LineVersion.get() != null) return version;
    return version + ", unsupported (supported: " + LineVersion.getSupportedVersions() + ")";
  }

  private static String framework() {
    XposedInterface module = Knot.module;
    if (module == null) return "unknown";
    return module.getFrameworkName()
        + " "
        + module.getFrameworkVersion()
        + " ("
        + module.getFrameworkVersionCode()
        + "), API "
        + module.getApiVersion();
  }

  private static String hookSummary() {
    List<Main.HookResult> results = Main.hookResults();
    int failed = 0;
    for (Main.HookResult result : results) {
      if (result.failed) failed++;
    }
    return (results.size() - failed) + " installed, " + failed + " failed";
  }

  private static String hookStatus() {
    List<Main.HookResult> results = Main.hookResults();
    if (results.isEmpty()) {
      return LineVersion.get() == null
          ? "No hooks applied (unsupported LINE version)"
          : "No hooks applied";
    }
    StringBuilder sb = new StringBuilder();
    for (Main.HookResult result : results) {
      sb.append(result.failed ? "FAILED     " : "installed  ").append(result.name).append('\n');
      for (String entry : result.logs) {
        sb.append("    ").append(entry.replace("\n", "\n    ")).append('\n');
      }
    }
    return sb.toString().trim();
  }

  private static String enabledOptions() {
    if (!SettingsStore.isLoaded()) return "settings not loaded";
    List<String> parts = new ArrayList<>();
    for (KnotConfig.Item item : Main.options.items) {
      boolean hasValue = item.value != null && !item.value.isEmpty();
      if (!item.enabled && !hasValue) continue;
      if (!hasValue) {
        parts.add(item.key);
      } else if (item.key.endsWith("_path")) {
        parts.add(item.key + "=(set)");
      } else {
        parts.add(item.key + "=" + item.value);
      }
    }
    return parts.isEmpty() ? "none" : String.join(", ", parts);
  }

  private static String logs() {
    List<String> entries = Knot.recentLogs();
    return entries.isEmpty() ? "No logs" : String.join("\n", entries);
  }
}
