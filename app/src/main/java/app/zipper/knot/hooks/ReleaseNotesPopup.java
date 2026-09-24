package app.zipper.knot.hooks;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;

public class ReleaseNotesPopup implements BaseHook {

  private static final String SHOWN_VERSION_KEY = "release_notes_version";
  private static final String RELEASE_API =
      "https://api.github.com/repos/2b-zipper/Knot/releases/tags/";
  private static final String RELEASE_PAGE = "https://github.com/2b-zipper/Knot/releases/tag/";
  private static final int TIMEOUT_MS = 5000;
  // [label](url), <url>, or a bare URL that stops before trailing punctuation as GitHub's does.
  private static final Pattern LINK =
      Pattern.compile(
          "\\[([^\\]]+)\\]\\(([^)\\s]+)\\)"
              + "|<(https?://[^>\\s]+)>"
              + "|(https?://[\\p{Graph}&&[^<>()]]*[\\p{Graph}&&[^<>().,:;!?'\"*_~]])");

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
      AlertDialog dialog =
          new AlertDialog.Builder(host, LineTheme.dialogTheme(host))
              .setTitle(ModuleResources.BRAND_NAME + " v" + BuildConfig.VERSION_NAME)
              .setMessage(render(host, notes))
              .setPositiveButton(ModuleResources.get(R.string.common_close), null)
              .setNeutralButton(
                  ModuleResources.get(R.string.release_notes_open),
                  (d, w) -> openUrl(host, RELEASE_PAGE + tag()))
              .show();
      LineTheme.applyDialogColors(dialog, host);
      TextView message = dialog.findViewById(android.R.id.message);
      if (message != null) {
        message.setMovementMethod(LinkMovementMethod.getInstance());
        message.setLinkTextColor(LineTheme.linkColor(host));
      }
      SettingsStore.save(SHOWN_VERSION_KEY, BuildConfig.VERSION_NAME);
    } catch (Throwable t) {
      Knot.log("Knot: release notes dialog failed: " + t);
    }
  }

  private static String tag() {
    return "v" + BuildConfig.VERSION_NAME;
  }

  private static void openUrl(Activity host, String url) {
    try {
      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
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
      return notes.isEmpty() || "null".equals(notes) ? null : notes;
    } catch (Throwable t) {
      Knot.log("Knot: release notes fetch failed: " + t);
      return null;
    } finally {
      if (conn != null) conn.disconnect();
    }
  }

  private static CharSequence render(Activity host, String markdown) {
    String text =
        markdown
            .replace("\r\n", "\n")
            .replaceAll("(?m)^#{1,6}[ \t]*", "")
            .replaceAll("(?m)^[ \t]*>[ \t]?", "")
            .replaceAll("(?m)^[ \t]*[*-][ \t]+", "・")
            .replace("**", "")
            .replace("`", "")
            .replaceAll("\n{3,}", "\n\n")
            .trim();

    SpannableStringBuilder out = new SpannableStringBuilder();
    Matcher m = LINK.matcher(text);
    int last = 0;
    while (m.find()) {
      out.append(text, last, m.start());
      String label = m.group(1);
      String url = m.group(2);
      if (label == null) {
        url = m.group(3) != null ? m.group(3) : m.group(4);
        label = url;
      }
      int start = out.length();
      out.append(label);
      if (url.startsWith("https://") || url.startsWith("http://")) {
        out.setSpan(link(host, url), start, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
      last = m.end();
    }
    return out.append(text, last, text.length());
  }

  private static ClickableSpan link(Activity host, String url) {
    return new ClickableSpan() {
      @Override
      public void onClick(View widget) {
        openUrl(host, url);
      }
    };
  }
}
