package app.zipper.knot.hooks;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.R;
import app.zipper.knot.Reflect;
import app.zipper.knot.utils.LineDBUtils;
import app.zipper.knot.utils.ModuleResources;
import io.github.libxposed.api.XposedInterface;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.json.JSONObject;

public class ReactionNotification implements BaseHook {

  private static final String CHANNEL_ID = "knot_reaction";
  private static final int NOTIFICATION_BASE_ID = 8000;
  private static final String OP_TYPE_REACTION = "NOTIFIED_SEND_REACTION";
  private static final String PKG_LINE = "jp.naver.line.android";

  private static volatile String currentChatMid = null;

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    if (!config.reactionNotification.enabled) return;

    LineVersion.Config cfg = LineVersion.get();

    XposedInterface.Hooker activityTracker =
        chain -> {
          Object result = chain.proceed();
          boolean isChat =
              chain.getThisObject().getClass().getName().equals(cfg.chatHeader.chatHistoryActivity);

          if (chain.getExecutable().getName().equals("onResume")) {
            if (isChat) {
              try {
                android.app.Activity activity = (android.app.Activity) chain.getThisObject();
                String chatId = activity.getIntent().getStringExtra("chatId");
                if (chatId == null) chatId = activity.getIntent().getStringExtra("chat_id");

                if (chatId == null) {
                  Object request = Reflect.getObjectField(activity, cfg.chat.chatIdField);
                  if (request != null) {
                    chatId = (String) Reflect.callMethod(request, cfg.chat.methodGetChatId);
                  }
                }
                currentChatMid = chatId;
                clearChatNotifications(activity, chatId);
              } catch (Throwable ignored) {
              }
            } else {
              currentChatMid = null;
            }
          } else if (isChat) {
            currentChatMid = null;
          }
          return result;
        };

    try {
      Knot.module
          .hook(Reflect.findMethodExact(android.app.Activity.class, "onResume"))
          .intercept(activityTracker);
      Knot.module
          .hook(Reflect.findMethodExact(android.app.Activity.class, "onPause"))
          .intercept(activityTracker);
    } catch (Throwable t) {
      Knot.log("Knot: Chat tracking hook failed: " + t.getMessage());
    }

    try {
      Knot.hookAll(
          Reflect.findClass(cfg.unsend.notifiedSendReactionHandlerClass, lpparam.classLoader),
          cfg.unsend.methodReadBuffer,
          chain -> {
            Object result = chain.proceed();
            if (Main.options.reactionNotification.enabled) {
              try {
                processOperation(chain);
              } catch (Exception ignored) {
              }
            }
            return result;
          });
    } catch (Throwable t) {
      Knot.log("Knot: Reaction hook failed: " + t.getMessage());
    }
  }

  private void processOperation(XposedInterface.Chain chain) throws Exception {
    LineVersion.Config cfg = LineVersion.get();
    Object op = chain.getArg(1);
    if (op == null || op.getClass().getName().startsWith("java.")) return;

    Object type = Reflect.getObjectField(op, cfg.unsend.operationTypeField);
    if (type == null || !OP_TYPE_REACTION.equals(type.toString())) return;

    String messageId = (String) Reflect.getObjectField(op, cfg.unsend.operationParam1Field);
    String dataJson = (String) Reflect.getObjectField(op, cfg.unsend.operationParam2Field);
    String reactorMid = (String) Reflect.getObjectField(op, cfg.unsend.operationParam3Field);

    if (dataJson == null || reactorMid == null) return;

    JSONObject json = new JSONObject(dataJson);
    String chatMid = json.optString("chatMid");
    if (chatMid.isEmpty() || chatMid.equals(currentChatMid)) return;

    JSONObject curr = json.optJSONObject("curr");
    if (curr == null || curr.length() == 0) return;

    boolean hasReaction =
        curr.optInt("predefinedReactionType") > 0
            || !curr.optString("externalReactionType").isEmpty()
            || !curr.optString("productId").isEmpty()
            || !curr.optString("sticonId").isEmpty()
            || !curr.optString("emojiId").isEmpty()
            || curr.has("paidReactionType")
            || curr.has("paidReaction");
    if (!hasReaction) return;

    Context context = Knot.currentApplication();
    if (context == null) return;

    Bitmap reactionIcon = resolveReactionIcon(context, curr);
    if (reactionIcon == null) {
      reactionIcon = loadBitmapFromResource(context, "chat_ui_ic_reaction_fallback");
    }

    String reactorName = LineDBUtils.resolveMemberName(reactorMid);
    if (reactorName == null) reactorName = reactorMid;

    String messageSnippet = LineDBUtils.resolveMessageContent(messageId);
    if (messageSnippet == null || messageSnippet.isEmpty()) {
      messageSnippet = ModuleResources.get(R.string.read_history_unknown_msg);
    }

    long timestamp = Reflect.getLongField(op, cfg.unsend.operationCreatedTimeField);
    int notifId = NOTIFICATION_BASE_ID + (chatMid + messageId + reactorMid).hashCode();

    String title = ModuleResources.get(R.string.reaction_notif_title, reactorName);

    issueNotification(context, title, messageSnippet, chatMid, reactionIcon, notifId, timestamp);
  }

  private Bitmap resolveReactionIcon(Context context, JSONObject curr) {
    if (curr == null) return null;

    int reactionType = curr.optInt("predefinedReactionType");
    String resName = getReactionResourceName(reactionType);
    Bitmap bitmap = loadBitmapFromResource(context, resName);
    if (bitmap != null) return bitmap;

    String productId = curr.optString("productId");
    String sticonId = curr.optString("sticonId", curr.optString("emojiId"));

    JSONObject paid = curr.optJSONObject("paidReactionType");
    if (paid == null) paid = curr.optJSONObject("paidReaction");

    if (paid != null) {
      if (productId.isEmpty()) productId = paid.optString("productId");
      if (sticonId.isEmpty()) sticonId = paid.optString("sticonId", paid.optString("emojiId"));
    }

    if (!productId.isEmpty() && !sticonId.isEmpty()) {
      return loadSticonBitmap(context, productId, sticonId);
    }

    String external = curr.optString("externalReactionType");
    if (external != null && external.startsWith("PAID_")) {
      String[] parts = external.split("_");
      if (parts.length >= 3) return loadSticonBitmap(context, parts[1], parts[2]);
    }

    return null;
  }

  private Bitmap loadSticonBitmap(Context context, String productId, String sticonId) {
    String[] paths = {
      "/sdcard/Android/data/"
          + PKG_LINE
          + "/files/sticon/"
          + productId
          + "/sticon/android/"
          + sticonId
          + ".png",
      "/sdcard/Android/data/"
          + PKG_LINE
          + "/files/sticon/"
          + productId
          + "/android/"
          + sticonId
          + ".png",
      context.getFilesDir().getParent() + "/app_sticon/" + productId + "/" + sticonId + ".png"
    };

    for (String path : paths) {
      File f = new File(path);
      if (f.exists()) {
        Bitmap b = BitmapFactory.decodeFile(path);
        if (b != null) return b;
      }
    }

    String[] urls = {
      "https://stickershop.line-scdn.net/sticonshop/v1/"
          + productId
          + "/sticon/android/"
          + sticonId
          + ".png",
      "https://stickershop.line-scdn.net/sticonshop/v1/product/android/"
          + productId
          + "/"
          + sticonId
          + ".png",
      "https://stickershop.line-scdn.net/sticon/"
          + productId
          + "/ANDROID/sticon/"
          + sticonId
          + ".png"
    };

    return downloadBitmap(context, urls);
  }

  private Bitmap downloadBitmap(Context context, String[] urls) {
    String ua = "Line/26.6.0";
    try {
      String verName = LineVersion.getVersionName(context);
      if (verName != null && !verName.isEmpty()) ua = "Line/" + verName;
    } catch (Throwable ignored) {
    }

    for (String urlStr : urls) {
      try {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);
        conn.setRequestProperty("User-Agent", ua);

        if (conn.getResponseCode() == 200) {
          try (InputStream is = new BufferedInputStream(conn.getInputStream())) {
            Bitmap b = BitmapFactory.decodeStream(is);
            if (b != null) return b;
          }
        }
      } catch (Exception ignored) {
      }
    }
    return null;
  }

  private Bitmap loadBitmapFromResource(Context context, String resName) {
    if (resName == null) return null;
    try {
      Context lineCtx = context.createPackageContext(PKG_LINE, Context.CONTEXT_RESTRICTED);
      int resId = lineCtx.getResources().getIdentifier(resName, "drawable", PKG_LINE);
      if (resId != 0) return BitmapFactory.decodeResource(lineCtx.getResources(), resId);
    } catch (Exception ignored) {
    }
    return null;
  }

  private String getReactionResourceName(int type) {
    switch (type) {
      case 2:
        return "shop_predefined_reaction_nice";
      case 3:
        return "shop_predefined_reaction_love";
      case 4:
        return "shop_predefined_reaction_fun";
      case 5:
        return "shop_predefined_reaction_amazing";
      case 6:
        return "shop_predefined_reaction_sad";
      case 7:
        return "shop_predefined_reaction_omg";
      default:
        return null;
    }
  }

  private void issueNotification(
      Context context,
      String title,
      String body,
      String chatMid,
      Bitmap icon,
      int notifId,
      long timestamp) {
    NotificationManager nm =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    if (nm == null) return;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      nm.createNotificationChannel(
          new NotificationChannel(CHANNEL_ID, "Reactions", NotificationManager.IMPORTANCE_DEFAULT));
    }

    Intent intent = new Intent();
    try {
      ClassLoader cl = context.getClassLoader();
      Class<?> reqCls = cl.loadClass(LineVersion.get().notification.chatHistoryRequestClass);
      boolean isGroup = chatMid.startsWith("g") || chatMid.startsWith("c");
      Object request = Reflect.newInstance(reqCls, chatMid, isGroup);

      intent.setClassName(
          PKG_LINE, LineVersion.get().notification.chatHistoryActivityLaunchActivityClass);
      intent.putExtra("chatHistoryRequest", (Parcelable) request);
    } catch (Exception e) {
      intent.setClassName(PKG_LINE, LineVersion.get().main.mainActivity);
      intent.putExtra("chatId", chatMid);
    }
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

    PendingIntent pi =
        PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    Notification.Builder builder =
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            ? new Notification.Builder(context, CHANNEL_ID)
            : new Notification.Builder(context);

    try {
      Context knotCtx = context.createPackageContext("app.zipper.knot", 0);
      int resId = knotCtx.getResources().getIdentifier("ic_knot", "drawable", "app.zipper.knot");
      builder.setSmallIcon(Icon.createWithResource("app.zipper.knot", resId));
    } catch (Exception e) {
      builder.setSmallIcon(android.R.drawable.ic_dialog_info);
    }

    builder
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(new Notification.BigTextStyle().bigText(body))
        .setAutoCancel(true)
        .setContentIntent(pi);

    Bundle extras = new Bundle();
    extras.putString("chatMid", chatMid);
    builder.addExtras(extras);

    if (timestamp > 0) {
      builder.setWhen(timestamp);
      builder.setShowWhen(true);
    }

    String timeStr =
        new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
            .format(new Date(timestamp));
    builder.setSubText(timeStr);

    if (icon != null) builder.setLargeIcon(icon);
    nm.notify(notifId, builder.build());
  }

  private void clearChatNotifications(Context context, String chatMid) {
    if (chatMid == null || chatMid.isEmpty()) return;
    NotificationManager nm =
        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    if (nm == null) return;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      try {
        for (StatusBarNotification sbn : nm.getActiveNotifications()) {
          Notification n = sbn.getNotification();
          if (n != null && CHANNEL_ID.equals(n.getChannelId())) {
            String mid = n.extras.getString("chatMid");
            if (chatMid.equals(mid)) {
              nm.cancel(sbn.getId());
            }
          }
        }
      } catch (Throwable ignored) {
      }
    }
  }
}
