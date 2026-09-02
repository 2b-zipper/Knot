package app.zipper.knot.hooks;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import app.zipper.knot.BuildConfig;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LoadParam;
import app.zipper.knot.R;
import app.zipper.knot.Reflect;
import app.zipper.knot.SettingsStore;
import app.zipper.knot.utils.LineTheme;
import app.zipper.knot.utils.ModuleResources;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class ReleaseNotesPopup implements BaseHook {

  private static final String SHOWN_VERSION_KEY = "release_notes_version";
  private static final String RELEASE_API =
      "https://api.github.com/repos/2b-zipper/Knot/releases/tags/";
  private static final String RELEASE_PAGE = "https://github.com/2b-zipper/Knot/releases/tag/";
  private static final int TIMEOUT_MS = 5000;

  private static volatile boolean handled;

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    Class<?> activityCls =
        lpparam.classLoader.loadClass("jp.naver.line.android.activity.main.MainActivity");

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
    if (BuildConfig.VERSION_NAME.equals(SettingsStore.getString(SHOWN_VERSION_KEY, ""))) return;

    WeakReference<Activity> hostRef = new WeakReference<>(host);
    new Thread(
            () -> {
              String notes = fetchNotes();
              Activity activity = hostRef.get();
              if (notes == null || activity == null) return;
              activity.runOnUiThread(() -> show(activity, notes));
            },
            "knot-release-notes")
        .start();
  }

  private static void show(Activity host, String notes) {
    if (host.isFinishing() || host.isDestroyed()) return;
    try {
      LineTheme.applyDialogColors(
          new AlertDialog.Builder(host, LineTheme.dialogTheme(host))
              .setTitle(ModuleResources.BRAND_NAME + " v" + BuildConfig.VERSION_NAME)
              .setMessage(notes)
              .setPositiveButton(ModuleResources.get(R.string.common_close), null)
              .setNeutralButton(
                  ModuleResources.get(R.string.release_notes_open), (d, w) -> openReleasePage(host))
              .show(),
          host);
      SettingsStore.save(SHOWN_VERSION_KEY, BuildConfig.VERSION_NAME);
    } catch (Throwable t) {
      Knot.log("Knot: release notes dialog failed: " + t);
    }
  }

  private static String tag() {
    return "v" + BuildConfig.VERSION_NAME;
  }

  private static void openReleasePage(Activity host) {
    try {
      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(RELEASE_PAGE + tag()));
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      host.startActivity(intent);
    } catch (Throwable ignored) {
    }
  }

  private static String fetchNotes() {
    HttpURLConnection conn = null;
    try {
      conn = (HttpURLConnection) new URL(RELEASE_API + tag()).openConnection();
      conn.setConnectTimeout(TIMEOUT_MS);
      conn.setReadTimeout(TIMEOUT_MS);
      conn.setRequestProperty("Accept", "application/vnd.github+json");

      ByteArrayOutputStream body = new ByteArrayOutputStream();
      byte[] buffer = new byte[4096];
      try (InputStream in = conn.getInputStream()) {
        int read;
        while ((read = in.read(buffer)) != -1) body.write(buffer, 0, read);
      }

      String notes = new JSONObject(body.toString("UTF-8")).optString("body", "");
      return notes.isEmpty() || "null".equals(notes) ? null : toPlainText(notes);
    } catch (Throwable t) {
      Knot.log("Knot: release notes fetch failed: " + t);
      return null;
    } finally {
      if (conn != null) conn.disconnect();
    }
  }

  private static String toPlainText(String markdown) {
    return markdown
        .replace("\r\n", "\n")
        .replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1")
        .replaceAll("(?m)^#{1,6}[ \t]*", "")
        .replaceAll("(?m)^[ \t]*>[ \t]?", "")
        .replaceAll("(?m)^[ \t]*[*-][ \t]+", "・")
        .replace("**", "")
        .replace("`", "")
        .replaceAll("\n{3,}", "\n\n")
        .trim();
  }
}
