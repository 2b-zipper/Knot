package app.zipper.knot.hooks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.R;
import app.zipper.knot.Reflect;
import app.zipper.knot.SettingsStore;
import app.zipper.knot.utils.ModuleResources;
import io.github.libxposed.api.XposedInterface;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONObject;

public class UnsendProtector implements BaseHook {

  private static final int TIMESTAMP_MSG_ID_TAG = 0x7f7a0001;
  private static final int TIMESTAMP_VIEW_CLEANUP_INTERVAL = 64;
  private static final Map<String, String> unsendEvents = new ConcurrentHashMap<>();
  private static final Map<String, WeakReference<TextView>> timestampViews =
      new ConcurrentHashMap<>();
  private static volatile Bitmap indicatorIcon;
  private static volatile Bitmap tintedIndicator;
  private static volatile int tintedIndicatorPx;
  private static Toast currentToast;
  private static int timestampBindCount = 0;

  private static boolean isBlank(String s) {
    return s == null || s.isEmpty();
  }

  private static boolean isEnabled() {
    return Main.options.preventUnsendMessage.enabled
        || SettingsStore.get("prevent_unsend_message", false);
  }

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    initializeUnsendCache();

    LineVersion.Config cfg = LineVersion.get();

    hookMessageConverter(cfg, lpparam);
    hookOperationHandler(cfg, lpparam);
    hookViewHolderBinding(cfg, lpparam);
  }

  private void hookMessageConverter(LineVersion.Config cfg, LoadParam lpparam) {
    if (isBlank(cfg.unsend.messageConverterClass) || isBlank(cfg.unsend.methodConvertMessage)) {
      return;
    }
    try {
      Class<?> clazz = Reflect.findClass(cfg.unsend.messageConverterClass, lpparam.classLoader);
      for (Method m : clazz.getDeclaredMethods()) {
        if (m.getName().equals(cfg.unsend.methodConvertMessage) && m.getParameterCount() == 2) {
          m.setAccessible(true);
          Knot.module
              .hook(m)
              .intercept(
                  chain -> {
                    if (isEnabled()) {
                      try {
                        Object iVar = chain.getArg(0);
                        if (iVar != null) {
                          handleIncomingMessageConversion(iVar, cfg, lpparam.classLoader);
                        }
                      } catch (Throwable t) {
                        Knot.log("Knot: Message converter hook error: " + t);
                      }
                    }
                    return chain.proceed();
                  });
        }
      }
      Knot.log("Knot: Message converter hooked (" + cfg.unsend.messageConverterClass + ")");
    } catch (Throwable t) {
      Knot.log("Knot: Message converter hook failed: " + t);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static void handleIncomingMessageConversion(
      Object iVar, LineVersion.Config cfg, ClassLoader cl) {
    try {
      String msgId = (String) Reflect.getObjectField(iVar, cfg.unsend.incomingMessageIdField);
      boolean isUnsent = false;

      Object paramsObj = Reflect.getObjectField(iVar, cfg.unsend.incomingMessageParamsField);
      if (paramsObj != null) {
        Object paramMapObj =
            Reflect.getObjectField(paramsObj, cfg.unsend.incomingMessageParamsMapField);
        if (paramMapObj instanceof Map) {
          Map<?, ?> map = (Map<?, ?>) paramMapObj;
          if (map.containsKey("UNSENT") || map.containsKey("SILENTLY_UNSENT")) {
            isUnsent = true;
            map.remove("UNSENT");
            map.remove("SILENTLY_UNSENT");
          }
        }
      }

      Object typeObj = Reflect.getObjectField(iVar, cfg.unsend.incomingMessageTypeField);
      if (typeObj != null && typeObj.toString().contains("UNSENT")) {
        isUnsent = true;
        try {
          if (!isBlank(cfg.unsend.incomingMessageTypeEnumClass)) {
            Class<?> bEnum = Reflect.findClass(cfg.unsend.incomingMessageTypeEnumClass, cl);
            if (bEnum != null && bEnum.isEnum()) {
              Object normalType = Enum.valueOf((Class<Enum>) bEnum, "MESSAGE");
              Reflect.setObjectField(iVar, cfg.unsend.incomingMessageTypeField, normalType);
            }
          }
        } catch (Throwable ignored) {
        }
      }

      if (isUnsent) {
        recordIfPresent(msgId, "converter");
      }
    } catch (Throwable ignored) {
    }
  }

  private void hookOperationHandler(LineVersion.Config cfg, LoadParam lpparam) {
    hookSingleHandlerClass(cfg.unsend.unsendDestroyHandlerClass, cfg, lpparam);
    if (!isBlank(cfg.unsend.destroyMessageHandlerClass)
        && !cfg.unsend.destroyMessageHandlerClass.equals(cfg.unsend.unsendDestroyHandlerClass)) {
      hookSingleHandlerClass(cfg.unsend.destroyMessageHandlerClass, cfg, lpparam);
    }
  }

  private void hookSingleHandlerClass(String className, LineVersion.Config cfg, LoadParam lpparam) {
    if (isBlank(className)) return;
    try {
      Class<?> handlerClass = Reflect.findClass(className, lpparam.classLoader);
      for (Class<?> c = handlerClass; c != null && c != Object.class; c = c.getSuperclass()) {
        for (Method m : c.getDeclaredMethods()) {
          if (m.getName().equals(cfg.unsend.methodDestroyHandler)
              && m.getParameterCount() == 3
              && !Modifier.isAbstract(m.getModifiers())) {
            m.setAccessible(true);
            Knot.module
                .hook(m)
                .intercept(
                    chain -> {
                      if (isEnabled()) {
                        try {
                          Object result = handleDestroyHandler(chain, cfg, lpparam.classLoader);
                          if (result != null) return result;
                        } catch (Throwable t) {
                          Knot.log("Knot: Handler unsend error: " + t);
                        }
                      }
                      return chain.proceed();
                    });
          }
        }
      }
    } catch (Throwable t) {
      Knot.log("Knot: Handler hook failed (" + className + "): " + t);
    }
  }

  private static Object handleDestroyHandler(
      XposedInterface.Chain chain, LineVersion.Config cfg, ClassLoader cl) {
    Object op = chain.getArg(1);
    if (op == null) return null;

    recordIfPresent(getMessageId(op, cfg), "handler");

    if (isUnsendOperation(op, cfg)) {
      sanitizeUnsendOperation(op, cfg);
    }

    if (!isBlank(cfg.unsend.handlerSuccessResultClass)) {
      try {
        Class<?> successClass = Reflect.findClass(cfg.unsend.handlerSuccessResultClass, cl);
        Constructor<?> ctor = successClass.getDeclaredConstructor(boolean.class);
        ctor.setAccessible(true);
        return ctor.newInstance(false);
      } catch (Throwable ignored) {
      }
    }
    return null;
  }

  private static boolean isUnsendOperation(Object op, LineVersion.Config cfg) {
    if (op == null) return false;
    try {
      Object type = Reflect.getObjectField(op, cfg.unsend.operationTypeField);
      if (type == null) return false;
      String typeStr = type.toString();
      return cfg.unsend.operationNotifiedUnsendName.equals(typeStr)
          || cfg.unsend.operationUnsendName.equals(typeStr);
    } catch (Throwable ignored) {
      return false;
    }
  }

  private static String getMessageId(Object op, LineVersion.Config cfg) {
    if (op == null) return null;
    try {
      return (String) Reflect.getObjectField(op, cfg.unsend.operationParam2Field);
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static void sanitizeUnsendOperation(Object op, LineVersion.Config cfg) {
    if (op == null) return;
    try {
      Object type = Reflect.getObjectField(op, cfg.unsend.operationTypeField);
      Object harmlessType = toHarmlessOperationType(type, cfg);
      if (harmlessType != null) {
        Reflect.setObjectField(op, cfg.unsend.operationTypeField, harmlessType);
      }
    } catch (Throwable t) {
      Knot.log("Knot: Failed to set op type: " + t);
    }

    try {
      Reflect.setObjectField(op, cfg.unsend.operationParam2Field, "");
    } catch (Throwable ignored) {
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Object toHarmlessOperationType(Object type, LineVersion.Config cfg) {
    if (type == null) return null;
    try {
      Class<?> typeClass = type.getClass();
      if (!isBlank(cfg.unsend.methodOperationTypeValueOf)) {
        Object mapped =
            Reflect.callStaticMethod(
                typeClass, cfg.unsend.methodOperationTypeValueOf, cfg.unsend.operationTypeDummy);
        if (mapped != null) return mapped;
      }
      if (typeClass.isEnum()) {
        try {
          return Enum.valueOf((Class<Enum>) typeClass, "SEND_CHAT_CHECKED");
        } catch (Throwable ignored) {
        }
        try {
          return Enum.valueOf((Class<Enum>) typeClass, "DUMMY");
        } catch (Throwable ignored) {
        }
      }
    } catch (Throwable ignored) {
    }
    return null;
  }

  private static void recordIfPresent(String msgId, String source) {
    if (!isBlank(msgId)) onUnsendDetected(msgId, source);
  }

  private static void onUnsendDetected(String msgId, String source) {
    if (!unsendEvents.containsKey(msgId)) {
      Knot.log("Knot: Blocked unsend (" + source + "), id=" + msgId);
      persistUnsendEvent(msgId, getFormattedTime());
    }
    TextView tsView = getTimestampView(msgId);
    if (tsView != null) applyUnsendIndicator(tsView, tsView.getContext(), msgId);
  }

  private static void initializeUnsendCache() {
    try {
      JSONObject json = SettingsStore.loadUnsendHistory();
      Iterator<String> keys = json.keys();
      while (keys.hasNext()) {
        String id = keys.next();
        unsendEvents.put(id, json.getString(id));
      }
    } catch (Exception e) {
      Knot.log("Knot: Unsend history load error: " + e);
    }
  }

  private static synchronized void persistUnsendEvent(String msgId, String timestamp) {
    unsendEvents.put(msgId, timestamp);
    try {
      JSONObject json = new JSONObject();
      for (Map.Entry<String, String> entry : unsendEvents.entrySet())
        json.put(entry.getKey(), entry.getValue());
      SettingsStore.saveUnsendHistory(json);
    } catch (Exception e) {
      Knot.log("Knot: Unsend history save error: " + e);
    }
  }

  private static String getFormattedTime() {
    return new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(new Date());
  }

  private void hookViewHolderBinding(LineVersion.Config cfg, LoadParam lpparam) {
    try {
      Knot.hookAll(
          Reflect.findClass(cfg.unsend.chatMessageViewHolderClass, lpparam.classLoader),
          cfg.unsend.methodBind,
          chain -> {
            Object result = chain.proceed();
            if (isEnabled()) {
              try {
                handleViewHolderBinding(chain);
              } catch (Exception e) {
                Knot.log("Knot: Bind error: " + e);
              }
            }
            return result;
          });
    } catch (Throwable t) {
      Knot.log("Knot: Bind hook failed: " + t);
    }
  }

  private static void handleViewHolderBinding(XposedInterface.Chain chain) throws Exception {
    LineVersion.Config cfg = LineVersion.get();
    Object viewData = chain.getArg(cfg.unsend.methodBindIndex);
    if (viewData == null) return;
    Object commonData = Reflect.callMethod(viewData, cfg.unsend.methodGetCommonData);
    if (commonData == null) return;

    String msgId = (String) Reflect.getObjectField(commonData, cfg.unsend.chatMessageIdField);
    if (isBlank(msgId)) {
      try {
        if (!isBlank(cfg.unsend.chatMessageServerIdLongField)) {
          long serverIdLong =
              Reflect.getLongField(commonData, cfg.unsend.chatMessageServerIdLongField);
          if (serverIdLong > 0) msgId = String.valueOf(serverIdLong);
        }
      } catch (Throwable ignored) {
      }
    }
    View root = (View) Reflect.callMethod(chain.getThisObject(), cfg.unsend.methodGetItemView);
    if (root == null) return;

    TextView tsView = (TextView) root.findViewById(cfg.res.idTimestamp);
    if (tsView == null) return;

    clearTimestampViewMapping(tsView);
    resetViewProperties(tsView);
    if (!isBlank(msgId)) {
      tsView.setTag(TIMESTAMP_MSG_ID_TAG, msgId);
      timestampViews.put(msgId, new WeakReference<>(tsView));
      if ((++timestampBindCount % TIMESTAMP_VIEW_CLEANUP_INTERVAL) == 0) {
        cleanupStaleTimestampViews();
      }
      if (unsendEvents.containsKey(msgId)) applyUnsendIndicator(tsView, root.getContext(), msgId);
    }
  }

  private static void applyUnsendIndicator(
      final TextView tsView, final Context context, final String msgId) {
    Context appContext = context.getApplicationContext();
    final Context toastContext = appContext != null ? appContext : context;
    float dens = tsView.getResources().getDisplayMetrics().density;
    final int targetPx = (int) (14 * dens);
    int padPx = (int) (3 * dens);

    Bitmap colored = resolveTintedIndicator(context, targetPx);
    if (colored == null) return;
    final BitmapDrawable draw = new BitmapDrawable(tsView.getResources(), colored);
    draw.setBounds(0, 0, targetPx, targetPx);

    tsView.post(
        () -> {
          if (!msgId.equals(tsView.getTag(TIMESTAMP_MSG_ID_TAG))) return;
          tsView.setCompoundDrawables(null, null, draw, null);
          tsView.setCompoundDrawablePadding(padPx);
          tsView.setOnClickListener(
              v -> {
                String time = unsendEvents.get(msgId);
                if (time != null) {
                  if (currentToast != null) currentToast.cancel();
                  currentToast =
                      Toast.makeText(
                          toastContext,
                          ModuleResources.get(R.string.unset_time_prefix) + time,
                          Toast.LENGTH_SHORT);
                  currentToast.show();
                }
              });
        });
  }

  private static TextView getTimestampView(String msgId) {
    WeakReference<TextView> ref = timestampViews.get(msgId);
    if (ref == null) return null;
    TextView view = ref.get();
    if (view == null || !msgId.equals(view.getTag(TIMESTAMP_MSG_ID_TAG))) {
      timestampViews.remove(msgId);
      return null;
    }
    return view;
  }

  private static void clearTimestampViewMapping(TextView view) {
    Object previousMsgId = view.getTag(TIMESTAMP_MSG_ID_TAG);
    if (previousMsgId instanceof String) {
      WeakReference<TextView> ref = timestampViews.get(previousMsgId);
      if (ref == null || ref.get() == view) timestampViews.remove(previousMsgId);
    }
    view.setTag(TIMESTAMP_MSG_ID_TAG, null);
  }

  private static void cleanupStaleTimestampViews() {
    for (Map.Entry<String, WeakReference<TextView>> entry : timestampViews.entrySet()) {
      TextView view = entry.getValue().get();
      if (view == null || !entry.getKey().equals(view.getTag(TIMESTAMP_MSG_ID_TAG))) {
        timestampViews.remove(entry.getKey());
      }
    }
  }

  private static void resetViewProperties(final TextView tsView) {
    tsView.post(
        () -> {
          tsView.setCompoundDrawables(null, null, null, null);
          tsView.setCompoundDrawablePadding(0);
          tsView.setOnClickListener(null);
          tsView.setClickable(false);
        });
  }

  // Rebuilt only on size change; otherwise every bind rescales and retints on the scroll path.
  private static Bitmap resolveTintedIndicator(Context ctx, int sizePx) {
    Bitmap cached = tintedIndicator;
    if (cached != null && tintedIndicatorPx == sizePx) return cached;

    Bitmap raw = resolveIndicatorIcon(ctx);
    if (raw == null) return null;
    Bitmap built = applyTint(Bitmap.createScaledBitmap(raw, sizePx, sizePx, true), Color.RED);
    tintedIndicatorPx = sizePx;
    tintedIndicator = built;
    return built;
  }

  private static Bitmap resolveIndicatorIcon(Context ctx) {
    if (indicatorIcon != null) return indicatorIcon;
    try {
      String pkg = ModuleResources.MODULE_PACKAGE;
      Context modCtx = ctx.createPackageContext(pkg, Context.CONTEXT_IGNORE_SECURITY);
      int resId = modCtx.getResources().getIdentifier("message_off", "drawable", pkg);
      if (resId != 0) indicatorIcon = BitmapFactory.decodeResource(modCtx.getResources(), resId);
    } catch (Exception ignored) {
    }
    return indicatorIcon;
  }

  private static Bitmap applyTint(Bitmap src, int color) {
    Bitmap out = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(out);
    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    p.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
    canvas.drawBitmap(src, 0, 0, p);
    return out;
  }
}
