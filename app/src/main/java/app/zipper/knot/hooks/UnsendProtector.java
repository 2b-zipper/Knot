package app.zipper.knot.hooks;

import static app.zipper.knot.ui.settings.SettingsViews.dp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONObject;

public class UnsendProtector implements BaseHook {

  private static final int TIMESTAMP_MSG_ID_TAG = 0x7f7a0001;
  private static final int CHILD_IDS_TAG = 0x7f7a0002;
  private static final int BADGE_TAG = 0x7f7a0003;
  private static final int CAROUSEL_POSITION_TAG = 0x7f7a0004;
  private static final int TIMESTAMP_VIEW_CLEANUP_INTERVAL = 64;
  private static final String GRID_CELL_ID_PREFIX = "chat_ui_image_item_";
  private static final String CAROUSEL_LIST_ID = "chat_ui_row_carousel_image_recycler_view";
  private static final String CAROUSEL_FRAME_ID = "chat_ui_row_content_rounded_frame_layout";
  private static final int INDICATOR_DP = 14;
  private static final int BADGE_PADDING_DP = 4;
  private static final int BADGE_MARGIN_DP = 6;
  private static final int BADGE_COLOR = 0x80000000;
  private static final Map<String, String> unsendEvents = new ConcurrentHashMap<>();
  private static final Map<String, WeakReference<TextView>> timestampViews =
      new ConcurrentHashMap<>();
  private static final Map<Class<?>, List<Field>> listFieldsByClass = new ConcurrentHashMap<>();
  private static final Map<Class<?>, Field> itemViewFields = new ConcurrentHashMap<>();
  private static volatile int[] gridCellIds;
  private static volatile int carouselListId;
  private static volatile int carouselFrameId;
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

  // Messages kept while the option was on stay in the chat after it is turned off
  private static boolean shouldMark() {
    return isEnabled() || !unsendEvents.isEmpty();
  }

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    initializeUnsendCache();

    LineVersion.Config cfg = LineVersion.get();

    hookMessageConverter(cfg, lpparam);
    hookOperationHandler(cfg, lpparam);
    hookViewHolderBinding(cfg, lpparam);
    hookCarouselBinding(cfg, lpparam);
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
    if (tsView != null) tsView.post(() -> refreshUnsendMarks(tsView, msgId));
  }

  private static void refreshUnsendMarks(TextView tsView, String msgId) {
    if (!isBoundTo(tsView, msgId)) return;
    View row = imageRow(tsView);
    if (row == null) {
      applyUnsendIndicator(tsView, tsView.getContext(), msgId);
      return;
    }
    updateImageBadges(row, (List<?>) row.getTag(CHILD_IDS_TAG));
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
            if (shouldMark()) {
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

  // Carousel pages are bound by their own adapter, so a row bind misses pages scrolled in later
  private void hookCarouselBinding(LineVersion.Config cfg, LoadParam lpparam) {
    if (isBlank(cfg.unsend.carouselAdapterClass)) return;
    try {
      Knot.hookAll(
          Reflect.findClass(cfg.unsend.carouselAdapterClass, lpparam.classLoader),
          cfg.unsend.methodCarouselBindViewHolder,
          chain -> {
            Object result = chain.proceed();
            if (shouldMark()) {
              try {
                handleCarouselBinding(chain.getArg(0), (Integer) chain.getArg(1));
              } catch (Exception e) {
                Knot.log("Knot: Carousel bind error: " + e);
              }
            }
            return result;
          });
    } catch (Throwable t) {
      Knot.log("Knot: Carousel bind hook failed: " + t);
    }
  }

  private static void handleCarouselBinding(Object holder, int position) throws Exception {
    View page = itemViewOf(holder);
    if (page == null) return;
    page.setTag(CAROUSEL_POSITION_TAG, position);
    // Pages are bound before they are attached, and the row's ids are reachable only afterwards
    page.post(
        () -> {
          View row = imageRow(page);
          if (row != null) updateCarouselBadge(page, (List<?>) row.getTag(CHILD_IDS_TAG));
        });
  }

  private static View itemViewOf(Object holder) throws IllegalAccessException {
    Field field =
        itemViewFields.computeIfAbsent(holder.getClass(), UnsendProtector::findItemViewField);
    return field == null ? null : (View) field.get(holder);
  }

  // RecyclerView.ViewHolder keeps itemView as its only field typed exactly View
  private static Field findItemViewField(Class<?> holderClass) {
    Class<?> base = holderClass;
    while (base.getSuperclass() != null && base.getSuperclass() != Object.class) {
      base = base.getSuperclass();
    }
    for (Field f : base.getDeclaredFields()) {
      if (!Modifier.isStatic(f.getModifiers()) && f.getType() == View.class) {
        f.setAccessible(true);
        return f;
      }
    }
    return null;
  }

  private static void handleViewHolderBinding(XposedInterface.Chain chain) throws Exception {
    LineVersion.Config cfg = LineVersion.get();
    Object viewData = chain.getArg(cfg.unsend.methodBindIndex);
    if (viewData == null) return;
    Object commonData = Reflect.callMethod(viewData, cfg.unsend.methodGetCommonData);
    if (commonData == null) return;

    // A multiple image row carries one anchor image's id, but each image is unsent on its own
    List<String> childIds = childMessageIds(viewData, commonData.getClass(), cfg);
    Set<String> msgIds = new LinkedHashSet<>();
    String rowId = messageId(commonData, cfg);
    if (rowId != null) msgIds.add(rowId);
    for (String childId : childIds) {
      if (childId != null) msgIds.add(childId);
    }

    View root = (View) Reflect.callMethod(chain.getThisObject(), cfg.unsend.methodGetItemView);
    if (root == null) return;

    root.setTag(CHILD_IDS_TAG, childIds.isEmpty() ? null : childIds);
    if (!childIds.isEmpty()) updateImageBadges(root, childIds);

    TextView tsView = (TextView) root.findViewById(cfg.res.idTimestamp);
    if (tsView == null) return;

    clearTimestampViewMapping(tsView);
    resetViewProperties(tsView);
    if (msgIds.isEmpty()) return;

    tsView.setTag(TIMESTAMP_MSG_ID_TAG, msgIds);
    for (String msgId : msgIds) timestampViews.put(msgId, new WeakReference<>(tsView));
    if ((++timestampBindCount % TIMESTAMP_VIEW_CLEANUP_INTERVAL) == 0) {
      cleanupStaleTimestampViews();
    }
    // Multiple images are marked on each image instead of the timestamp
    if (childIds.isEmpty() && rowId != null && unsendEvents.containsKey(rowId)) {
      applyUnsendIndicator(tsView, root.getContext(), rowId);
    }
  }

  private static String messageId(Object commonData, LineVersion.Config cfg) {
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
    return isBlank(msgId) ? null : msgId;
  }

  // In the order of the grid cells and carousel pages, with null where an image has no id yet
  private static List<String> childMessageIds(
      Object viewData, Class<?> commonClass, LineVersion.Config cfg) {
    for (Field field : listFields(viewData.getClass())) {
      Object value;
      try {
        value = field.get(viewData);
      } catch (IllegalAccessException e) {
        continue;
      }
      if (!(value instanceof List) || ((List<?>) value).isEmpty()) continue;
      List<?> children = (List<?>) value;
      Object first = children.get(0);
      if (first == null || !sameFamily(viewData.getClass(), first.getClass())) continue;

      boolean hasAnyId = false;
      List<String> ids = new ArrayList<>(children.size());
      for (Object child : children) {
        String childId = null;
        try {
          Object childCommon = Reflect.callMethod(child, cfg.unsend.methodGetCommonData);
          if (commonClass.isInstance(childCommon)) childId = messageId(childCommon, cfg);
        } catch (Throwable ignored) {
        }
        if (childId != null) hasAnyId = true;
        ids.add(childId);
      }
      if (hasAnyId) return ids;
    }
    return Collections.emptyList();
  }

  private static List<Field> listFields(Class<?> cls) {
    return listFieldsByClass.computeIfAbsent(
        cls,
        c -> {
          List<Field> out = new ArrayList<>();
          for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
            for (Field f : k.getDeclaredFields()) {
              if (!Modifier.isStatic(f.getModifiers())
                  && List.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                out.add(f);
              }
            }
          }
          return out;
        });
  }

  private static boolean sameFamily(Class<?> a, Class<?> b) {
    for (Class<?> k = a.getSuperclass(); k != null && k != Object.class; k = k.getSuperclass()) {
      if (k.isAssignableFrom(b)) return true;
    }
    return false;
  }

  private static View imageRow(View view) {
    for (ViewParent p = view.getParent(); p instanceof View; p = p.getParent()) {
      if (((View) p).getTag(CHILD_IDS_TAG) instanceof List) return (View) p;
    }
    return null;
  }

  private static void updateImageBadges(View row, List<?> childIds) {
    resolveImageViewIds(row.getContext());
    int[] cellIds = gridCellIds;
    for (int i = 0; i < cellIds.length; i++) {
      View cell = row.findViewById(cellIds[i]);
      if (cell instanceof FrameLayout) setBadge((FrameLayout) cell, idAt(childIds, i));
    }
    View list = carouselListId == 0 ? null : row.findViewById(carouselListId);
    if (list instanceof ViewGroup) {
      ViewGroup pages = (ViewGroup) list;
      for (int i = 0; i < pages.getChildCount(); i++) {
        updateCarouselBadge(pages.getChildAt(i), childIds);
      }
    }
  }

  private static void updateCarouselBadge(View page, List<?> childIds) {
    Object position = page.getTag(CAROUSEL_POSITION_TAG);
    if (!(position instanceof Integer) || childIds == null) return;
    resolveImageViewIds(page.getContext());
    View frame = carouselFrameId == 0 ? null : page.findViewById(carouselFrameId);
    if (frame instanceof FrameLayout) {
      setBadge((FrameLayout) frame, idAt(childIds, (Integer) position));
    }
  }

  private static Object idAt(List<?> ids, int index) {
    return ids != null && index >= 0 && index < ids.size() ? ids.get(index) : null;
  }

  private static void resolveImageViewIds(Context context) {
    if (gridCellIds != null) return;
    List<Integer> cells = new ArrayList<>();
    for (int i = 0; ; i++) {
      int id = lineId(context, GRID_CELL_ID_PREFIX + i);
      if (id == 0) break;
      cells.add(id);
    }
    carouselListId = lineId(context, CAROUSEL_LIST_ID);
    carouselFrameId = lineId(context, CAROUSEL_FRAME_ID);
    gridCellIds = cells.stream().mapToInt(Integer::intValue).toArray();
  }

  private static int lineId(Context context, String name) {
    return context.getResources().getIdentifier(name, "id", context.getPackageName());
  }

  private static void setBadge(FrameLayout frame, Object id) {
    View badge = (View) frame.getTag(BADGE_TAG);
    if (!(id instanceof String) || !unsendEvents.containsKey(id)) {
      if (badge != null) badge.setVisibility(View.GONE);
      return;
    }
    if (badge == null) {
      badge = createBadge(frame);
      if (badge == null) return;
      frame.setTag(BADGE_TAG, badge);
    }
    String msgId = (String) id;
    badge.setVisibility(View.VISIBLE);
    badge.setOnClickListener(v -> showUnsendTime(v.getContext(), msgId));
  }

  private static View createBadge(FrameLayout frame) {
    Context ctx = frame.getContext();
    int iconPx = dp(ctx, INDICATOR_DP);
    Bitmap colored = resolveTintedIndicator(iconPx);
    if (colored == null) return null;

    int padPx = dp(ctx, BADGE_PADDING_DP);
    int sizePx = iconPx + padPx * 2;
    GradientDrawable background = new GradientDrawable();
    background.setShape(GradientDrawable.OVAL);
    background.setColor(BADGE_COLOR);

    ImageView badge = new ImageView(ctx);
    badge.setImageDrawable(new BitmapDrawable(ctx.getResources(), colored));
    badge.setBackground(background);
    badge.setPadding(padPx, padPx, padPx, padPx);
    FrameLayout.LayoutParams lp =
        new FrameLayout.LayoutParams(sizePx, sizePx, Gravity.TOP | Gravity.START);
    int marginPx = dp(ctx, BADGE_MARGIN_DP);
    lp.topMargin = marginPx;
    lp.setMarginStart(marginPx);
    frame.addView(badge, lp);
    return badge;
  }

  private static void showUnsendTime(Context context, String msgId) {
    String time = unsendEvents.get(msgId);
    if (time == null) return;
    Context appContext = context.getApplicationContext();
    if (currentToast != null) currentToast.cancel();
    currentToast =
        Toast.makeText(
            appContext != null ? appContext : context,
            ModuleResources.get(R.string.unset_time_prefix) + time,
            Toast.LENGTH_SHORT);
    currentToast.show();
  }

  private static boolean isBoundTo(View view, String msgId) {
    Object ids = view.getTag(TIMESTAMP_MSG_ID_TAG);
    return ids instanceof Set && ((Set<?>) ids).contains(msgId);
  }

  private static void applyUnsendIndicator(
      final TextView tsView, final Context context, final String msgId) {
    final int targetPx = dp(context, INDICATOR_DP);
    int padPx = dp(context, 3);

    Bitmap colored = resolveTintedIndicator(targetPx);
    if (colored == null) return;
    final BitmapDrawable draw = new BitmapDrawable(tsView.getResources(), colored);
    draw.setBounds(0, 0, targetPx, targetPx);

    tsView.post(
        () -> {
          if (!isBoundTo(tsView, msgId)) return;
          tsView.setCompoundDrawables(null, null, draw, null);
          tsView.setCompoundDrawablePadding(padPx);
          tsView.setOnClickListener(v -> showUnsendTime(context, msgId));
        });
  }

  private static TextView getTimestampView(String msgId) {
    WeakReference<TextView> ref = timestampViews.get(msgId);
    if (ref == null) return null;
    TextView view = ref.get();
    if (view == null || !isBoundTo(view, msgId)) {
      timestampViews.remove(msgId);
      return null;
    }
    return view;
  }

  private static void clearTimestampViewMapping(TextView view) {
    Object previousMsgIds = view.getTag(TIMESTAMP_MSG_ID_TAG);
    if (previousMsgIds instanceof Set) {
      for (Object msgId : (Set<?>) previousMsgIds) {
        WeakReference<TextView> ref = timestampViews.get(msgId);
        if (ref == null || ref.get() == view) timestampViews.remove(msgId);
      }
    }
    view.setTag(TIMESTAMP_MSG_ID_TAG, null);
  }

  private static void cleanupStaleTimestampViews() {
    for (Map.Entry<String, WeakReference<TextView>> entry : timestampViews.entrySet()) {
      TextView view = entry.getValue().get();
      if (view == null || !isBoundTo(view, entry.getKey())) {
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
  private static Bitmap resolveTintedIndicator(int sizePx) {
    Bitmap cached = tintedIndicator;
    if (cached != null && tintedIndicatorPx == sizePx) return cached;

    Bitmap raw = ModuleResources.bitmap("message_off", sizePx);
    if (raw == null) return null;
    Bitmap built = applyTint(raw, Color.RED);
    tintedIndicatorPx = sizePx;
    tintedIndicator = built;
    return built;
  }

  private static Bitmap applyTint(Bitmap src, int color) {
    Bitmap out = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
    out.setDensity(src.getDensity());
    Canvas canvas = new Canvas(out);
    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    p.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
    canvas.drawBitmap(src, 0, 0, p);
    return out;
  }
}
