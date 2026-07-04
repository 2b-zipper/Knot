package app.zipper.knot.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

public final class ContributorProfiles {

  public interface NameCallback {
    void onName(String name);
  }

  private static final Map<String, Bitmap> AVATAR_CACHE = new ConcurrentHashMap<>();
  private static final Map<String, String> NAME_CACHE = new ConcurrentHashMap<>();
  private static final Map<String, Object> LOCKS = new ConcurrentHashMap<>();
  private static final int TIMEOUT_MS = 8000;

  private static final ExecutorService IO =
      Executors.newCachedThreadPool(
          r -> {
            Thread t = new Thread(r, "knot-contributor-io");
            t.setDaemon(true);
            return t;
          });

  private ContributorProfiles() {}

  public static void prefetch(Context ctx, String[] handles) {
    final Context app = ctx.getApplicationContext();
    for (String handle : handles) {
      IO.execute(
          () -> {
            fetchAvatar(app, handle);
            fetchName(handle);
          });
    }
  }

  public static void loadAvatar(ImageView target, String handle) {
    Bitmap cached = AVATAR_CACHE.get(avatarUrl(handle));
    if (cached != null) {
      target.setImageBitmap(cached);
      return;
    }
    final Context app = target.getContext().getApplicationContext();
    final Handler ui = new Handler(Looper.getMainLooper());
    IO.execute(
        () -> {
          Bitmap bmp = fetchAvatar(app, handle);
          if (bmp != null) ui.post(() -> target.setImageBitmap(bmp));
        });
  }

  public static void loadDisplayName(String handle, NameCallback callback) {
    String cached = NAME_CACHE.get(handle);
    if (cached != null) {
      callback.onName(cached);
      return;
    }
    final Handler ui = new Handler(Looper.getMainLooper());
    IO.execute(
        () -> {
          String name = fetchName(handle);
          if (name != null) ui.post(() -> callback.onName(name));
        });
  }

  private static Bitmap fetchAvatar(Context app, String handle) {
    String url = avatarUrl(handle);
    Bitmap cached = AVATAR_CACHE.get(url);
    if (cached != null) return cached;

    synchronized (lockFor(url)) {
      cached = AVATAR_CACHE.get(url);
      if (cached != null) return cached;

      Bitmap bmp = null;
      File cacheFile = avatarCacheFile(app, url);
      try {
        if (cacheFile != null && cacheFile.exists()) {
          bmp = BitmapFactory.decodeFile(cacheFile.getAbsolutePath());
        }
        if (bmp == null) {
          HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
          conn.setConnectTimeout(TIMEOUT_MS);
          conn.setReadTimeout(TIMEOUT_MS);
          conn.setInstanceFollowRedirects(true);
          bmp = BitmapFactory.decodeStream(conn.getInputStream());
          conn.disconnect();
          if (bmp != null && cacheFile != null) {
            try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
              bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            } catch (Throwable ignored) {
            }
          }
        }
      } catch (Throwable ignored) {
      }
      if (bmp != null) AVATAR_CACHE.put(url, bmp);
      return bmp;
    }
  }

  private static String fetchName(String handle) {
    String cached = NAME_CACHE.get(handle);
    if (cached != null) return cached;

    synchronized (lockFor("name:" + handle)) {
      cached = NAME_CACHE.get(handle);
      if (cached != null) return cached;

      String name = null;
      try {
        HttpURLConnection conn =
            (HttpURLConnection) new URL("https://api.github.com/users/" + handle).openConnection();
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = conn.getInputStream().read(buf)) != -1) bos.write(buf, 0, n);
        conn.disconnect();
        String nm = new JSONObject(bos.toString("UTF-8")).optString("name", "");
        if (!nm.isEmpty() && !"null".equals(nm)) name = nm;
      } catch (Throwable ignored) {
      }
      if (name != null) NAME_CACHE.put(handle, name);
      return name;
    }
  }

  private static Object lockFor(String key) {
    return LOCKS.computeIfAbsent(key, k -> new Object());
  }

  private static File avatarCacheFile(Context ctx, String url) {
    try {
      File dir = new File(ctx.getCacheDir(), "knot_avatars");
      if (!dir.exists()) dir.mkdirs();
      return new File(dir, Integer.toHexString(url.hashCode()) + ".png");
    } catch (Throwable t) {
      return null;
    }
  }

  private static String avatarUrl(String handle) {
    return "https://github.com/" + handle + ".png?size=120";
  }
}
