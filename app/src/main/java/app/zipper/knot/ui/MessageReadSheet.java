package app.zipper.knot.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import app.zipper.knot.Knot;
import app.zipper.knot.LineVersion;
import app.zipper.knot.R;
import app.zipper.knot.Reflect;
import app.zipper.knot.SettingsStore;
import app.zipper.knot.utils.LineDBUtils;
import app.zipper.knot.utils.LineTheme;
import app.zipper.knot.utils.ModuleResources;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONObject;

public final class MessageReadSheet {

  private static final String LINE_PKG = "jp.naver.line.android";
  private static final String BOTTOM_SHEET_BEHAVIOR_CLASS =
      "com.google.android.material.bottomsheet.BottomSheetBehavior";
  private static final String NESTED_SCROLL_VIEW_CLASS = "androidx.core.widget.NestedScrollView";
  private static final String STORED_TIME_FMT = "yyyy-MM-dd HH:mm:ss";
  private static final int TIME_VIEW_ID = 0x64010020;

  private static final class StoredReader {
    final String mid;
    final String name;
    final String rawTime;

    StoredReader(String mid, String name, String rawTime) {
      this.mid = mid;
      this.name = name;
      this.rawTime = rawTime;
    }
  }

  private static final class ReaderRow {
    final String mid;
    final String name;
    final String picturePath;
    final String rawTime;
    final String displayTime;

    ReaderRow(String mid, String name, String picturePath, String rawTime, String displayTime) {
      this.mid = mid;
      this.name = name;
      this.picturePath = picturePath;
      this.rawTime = rawTime;
      this.displayTime = displayTime;
    }
  }

  private static final class AvatarBinder {
    private final Object binder;
    private final String bindMethod;
    private final Object friendListUsage;

    private AvatarBinder(Object binder, String bindMethod, Object friendListUsage) {
      this.binder = binder;
      this.bindMethod = bindMethod;
      this.friendListUsage = friendListUsage;
    }

    static AvatarBinder create(Context context, LineVersion.Config cfg) {
      if (cfg == null) return null;
      LineVersion.Config.ReadReceipt rr = cfg.readReceipt;
      if (rr.profileAvatarBinderClass.isEmpty()
          || rr.profileAvatarBinderMethod.isEmpty()
          || rr.profileAvatarUsageEnumClass.isEmpty()
          || rr.profileAvatarFacadeClass.isEmpty()
          || rr.profileAvatarFacadeKeyField.isEmpty()
          || rr.profileAvatarLoaderFactoryMethod.isEmpty()
          || cfg.services.locatorClass.isEmpty()
          || cfg.notification.glideClass.isEmpty()) {
        return null;
      }
      try {
        ClassLoader cl = context.getClassLoader();
        Object usage =
            Reflect.getStaticObjectField(
                Reflect.findClass(rr.profileAvatarUsageEnumClass, cl), "FRIEND_LIST");
        Class<?> facadeCls = Reflect.findClass(rr.profileAvatarFacadeClass, cl);
        Object key = Reflect.getStaticObjectField(facadeCls, rr.profileAvatarFacadeKeyField);
        Object facade =
            Reflect.callStaticMethod(
                Reflect.findClass(cfg.services.locatorClass, cl),
                cfg.services.methodLocate,
                context,
                key);
        if (usage == null || facade == null) return null;

        Class<?> glideCls = Reflect.findClass(cfg.notification.glideClass, cl);
        Object requestManager =
            Reflect.callStaticMethod(glideCls, cfg.notification.glideWithContextMethod, context);
        if (requestManager == null) return null;

        Object glideLoader =
            Reflect.callMethod(facade, rr.profileAvatarLoaderFactoryMethod, requestManager);
        if (glideLoader == null) return null;

        Class<?> binderCls = Reflect.findClass(rr.profileAvatarBinderClass, cl);
        Constructor<?> ctor = binderCls.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        return new AvatarBinder(ctor.newInstance(glideLoader), rr.profileAvatarBinderMethod, usage);
      } catch (Throwable t) {
        Knot.log("Knot: MessageReadSheet avatar binder init failed: " + t);
        return null;
      }
    }

    boolean bind(ImageView avatar, String mid, String picturePath) {
      try {
        Reflect.callMethod(
            binder,
            bindMethod,
            avatar,
            mid != null ? mid : "",
            picturePath != null ? picturePath : "",
            friendListUsage);
        return true;
      } catch (Throwable t) {
        Knot.log("Knot: MessageReadSheet avatar bind failed: " + t);
        return false;
      }
    }
  }

  private final Context context;
  private final LayoutInflater inflater;
  private final Dialog dialog;
  private final boolean materialSheet;
  private final int bgColor;
  private final int primaryColor;
  private final int secondaryColor;
  private final int itemLayoutId;
  private final int avatarId;
  private final int nameId;
  private final int reactionId;
  private final int peekHeight;
  private final int topMargin;
  private final int titleHeight;
  private final AvatarBinder avatarBinder;
  private View listContainer;
  private int insetTop;
  private int insetBottom;

  public static void show(Context context, String chatId, String msgId) {
    if (context == null || chatId == null || msgId == null) return;
    try {
      LineTheme.invalidate();
      new MessageReadSheet(context).present(loadReaders(chatId, msgId));
    } catch (Throwable t) {
      Knot.log("Knot: MessageReadSheet show failed: " + t);
    }
  }

  private MessageReadSheet(Context context) {
    this.context = context;
    this.inflater = LayoutInflater.from(context);
    LineVersion.Config cfg = LineVersion.get();
    Dialog sheetDialog = createMaterialBottomSheet(cfg);
    this.materialSheet = sheetDialog != null;
    this.dialog = sheetDialog != null ? sheetDialog : createFallbackDialog();
    this.bgColor = LineTheme.backgroundColor(context);
    this.primaryColor = LineTheme.primaryTextColor(context);
    this.secondaryColor = LineTheme.secondaryTextColor(context);
    this.itemLayoutId = resId("chat_ui_reaction_sheet_item", "layout");
    this.avatarId = resId("chat_ui_reactionsheet_item_avatar", "id");
    this.nameId = resId("chat_ui_reactionsheet_item_name", "id");
    this.reactionId = resId("chat_ui_reactionsheet_item_reaction", "id");
    this.peekHeight = dimenPx("chat_ui_reactionsheet_peek_height", dp(336));
    this.topMargin = dimenPx("chat_ui_reactionsheet_expanded_top_margin", dp(24));
    this.titleHeight = dimenPx("chat_ui_reactionsheet_title_height", dp(55));
    this.avatarBinder = AvatarBinder.create(context, cfg);
  }

  private void present(List<ReaderRow> readers) {
    View sheet = buildSheetView(readers);
    if (sheet == null) return;
    int maxHeight = Math.max(peekHeight, usableDisplayHeight() - topMargin);
    int sheetHeight = computeSheetHeight(maxHeight, 0, readers.size());
    dialog.setContentView(
        sheet, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, sheetHeight));
    configureBottomSheet(sheet, readers.size());
    dialog.show();
  }

  private Dialog createMaterialBottomSheet(LineVersion.Config cfg) {
    if (cfg == null || cfg.readReceipt.bottomSheetDialogClass.isEmpty()) return null;
    try {
      int themeId = resId("chat_ui_message_reaction_sheet", "style");
      if (themeId == 0) themeId = resId("BottomSheetDialog", "style");
      Class<?> cls =
          Reflect.findClass(cfg.readReceipt.bottomSheetDialogClass, context.getClassLoader());
      return (Dialog)
          Reflect.findConstructorExact(cls, Context.class, int.class).newInstance(context, themeId);
    } catch (Throwable t) {
      Knot.log("Knot: BottomSheetDialog creation fell back to Dialog: " + t);
      return null;
    }
  }

  private Dialog createFallbackDialog() {
    Dialog fallback = new Dialog(context);
    fallback.requestWindowFeature(Window.FEATURE_NO_TITLE);
    return fallback;
  }

  private View buildSheetView(List<ReaderRow> readers) {
    int sheetLayoutId = resId("chat_ui_reaction_sheet", "layout");
    if (sheetLayoutId == 0) return null;

    View root = inflater.inflate(sheetLayoutId, null);
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(bgColor);
    float radius = dp(14);
    bg.setCornerRadii(new float[] {radius, radius, radius, radius, 0f, 0f, 0f, 0f});
    root.setBackground(bg);

    TextView title = root.findViewById(resId("chat_ui_reactionsheet_title", "id"));
    if (title != null) {
      title.setText(ModuleResources.get(R.string.read_receipt_sheet_title, readers.size()));
      title.setTextColor(primaryColor);
    }

    ImageView close = root.findViewById(resId("chat_ui_reactionsheet_close", "id"));
    if (close != null) {
      close.setColorFilter(primaryColor);
      close.setOnClickListener(v -> dialog.dismiss());
    }

    View recycler = root.findViewById(resId("chat_ui_reactionsheet_recycler_view", "id"));
    if (recycler != null && recycler.getParent() instanceof ViewGroup) {
      ViewGroup parent = (ViewGroup) recycler.getParent();
      int index = parent.indexOfChild(recycler);
      ViewGroup.LayoutParams lp = recycler.getLayoutParams();
      parent.removeViewAt(index);
      parent.addView(readers.isEmpty() ? buildEmptyView() : buildReaderList(readers), index, lp);
    }
    return root;
  }

  private TextView buildEmptyView() {
    TextView empty = new TextView(context);
    empty.setText(ModuleResources.get(R.string.read_receipt_sheet_empty));
    empty.setTextColor(secondaryColor);
    empty.setTextSize(14);
    empty.setGravity(Gravity.CENTER);
    return empty;
  }

  // NestedScrollView implements AndroidX NestedScrollingChild3, unlike framework ListView,
  // so Material BottomSheetBehavior coordinates expand/collapse and vertical scrolling natively.
  private ViewGroup buildReaderList(List<ReaderRow> readers) {
    ViewGroup scrollView = createScrollContainer();
    LinearLayout list = new LinearLayout(context);
    list.setOrientation(LinearLayout.VERTICAL);
    list.setPadding(0, 0, 0, dp(16));
    listContainer = list;
    for (ReaderRow reader : readers) {
      list.addView(createReaderRow(list, reader));
    }
    scrollView.addView(
        list,
        new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    return scrollView;
  }

  private ViewGroup createScrollContainer() {
    try {
      Class<?> cls = Reflect.findClass(NESTED_SCROLL_VIEW_CLASS, context.getClassLoader());
      return (ViewGroup) Reflect.findConstructorExact(cls, Context.class).newInstance(context);
    } catch (Throwable ignored) {
      return new ScrollView(context);
    }
  }

  private View createReaderRow(ViewGroup parent, ReaderRow reader) {
    View row = inflater.inflate(itemLayoutId, parent, false);
    View reactionView = row.findViewById(reactionId);
    if (reactionView != null) reactionView.setVisibility(View.GONE);

    TextView nameView = row.findViewById(nameId);
    if (nameView != null) {
      nameView.setText(reader.name);
      nameView.setTextColor(primaryColor);
    }

    if (row instanceof RelativeLayout) {
      attachTimeView((RelativeLayout) row, nameView).setText(reader.displayTime);
    }

    ImageView avatar = row.findViewById(avatarId);
    if (avatar != null) bindAvatar(avatar, reader);

    return row;
  }

  private TextView attachTimeView(RelativeLayout row, View nameView) {
    TextView timeView = new TextView(context);
    timeView.setId(TIME_VIEW_ID);
    timeView.setTextSize(12);
    timeView.setTextColor(secondaryColor);
    timeView.setSingleLine(true);

    RelativeLayout.LayoutParams timeLp =
        new RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    timeLp.addRule(RelativeLayout.ALIGN_PARENT_END);
    timeLp.addRule(RelativeLayout.CENTER_VERTICAL);
    timeLp.setMarginStart(dp(12));
    row.addView(timeView, timeLp);

    if (nameView != null && nameView.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
      RelativeLayout.LayoutParams nameLp = (RelativeLayout.LayoutParams) nameView.getLayoutParams();
      nameLp.addRule(RelativeLayout.START_OF, TIME_VIEW_ID);
      nameLp.addRule(RelativeLayout.LEFT_OF, TIME_VIEW_ID);
      nameView.setLayoutParams(nameLp);
    }
    return timeView;
  }

  private void bindAvatar(ImageView avatar, ReaderRow reader) {
    if (avatarBinder != null && avatarBinder.bind(avatar, reader.mid, reader.picturePath)) {
      return;
    }
    GradientDrawable circle = new GradientDrawable();
    circle.setShape(GradientDrawable.OVAL);
    circle.setColor(LineTheme.cardColor(context));
    avatar.setImageDrawable(circle);
  }

  private int usableDisplayHeight() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
      if (wm != null) {
        WindowMetrics metrics = wm.getCurrentWindowMetrics();
        Insets bars =
            metrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
        return Math.max(0, metrics.getBounds().height() - bars.top - bars.bottom);
      }
    }
    return context.getResources().getDisplayMetrics().heightPixels;
  }

  private int computeSheetHeight(int maxHeight, int bottomInset, int readerCount) {
    int minHeight = Math.min(maxHeight, peekHeight + bottomInset);
    if (readerCount <= 0) return minHeight;
    int estimated = titleHeight + readerCount * dp(56) + dp(24) + bottomInset;
    return Math.min(maxHeight, Math.max(minHeight, estimated));
  }

  private void configureBottomSheet(View sheet, int readerCount) {
    Window window = dialog.getWindow();
    if (window != null) {
      window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
      window.setDimAmount(0.4f);
      if (!materialSheet) {
        window.setGravity(Gravity.BOTTOM);
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
      }
    }

    dialog.setOnShowListener(
        d -> {
          try {
            int sheetId = resId("design_bottom_sheet", "id");
            if (sheetId == 0) return;
            View bottomSheet = dialog.findViewById(sheetId);
            if (bottomSheet == null) return;
            bottomSheet.setBackgroundColor(Color.TRANSPARENT);
            Class<?> behaviorCls =
                Reflect.findClass(BOTTOM_SHEET_BEHAVIOR_CLASS, context.getClassLoader());
            Object behavior =
                Reflect.findMethodExact(behaviorCls, "from", View.class).invoke(null, bottomSheet);
            Runnable syncBounds = () -> syncSheetBounds(sheet, bottomSheet, behavior, readerCount);

            sheet.setOnApplyWindowInsetsListener(
                (v, insets) -> {
                  readSystemInsets(insets);
                  syncBounds.run();
                  return insets;
                });
            if (bottomSheet.getParent() instanceof View) {
              ((View) bottomSheet.getParent())
                  .addOnLayoutChangeListener(
                      (v, l, t, r, b, ol, ot, or_, ob) -> {
                        if ((b - t) != (ob - ot)) syncBounds.run();
                      });
            }
            sheet.requestApplyInsets();
            syncBounds.run();
          } catch (Throwable ignored) {
          }
        });
  }

  @SuppressWarnings("deprecation")
  private void readSystemInsets(WindowInsets insets) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
      insetTop = bars.top;
      insetBottom = bars.bottom;
    } else {
      insetTop = insets.getSystemWindowInsetTop();
      insetBottom = insets.getSystemWindowInsetBottom();
    }
  }

  private void syncSheetBounds(View sheet, View bottomSheet, Object behavior, int readerCount) {
    if (listContainer != null) {
      int bottomPad = dp(16) + insetBottom;
      if (listContainer.getPaddingBottom() != bottomPad) {
        listContainer.setPadding(0, 0, 0, bottomPad);
      }
    }

    int parentHeight =
        bottomSheet.getParent() instanceof View ? ((View) bottomSheet.getParent()).getHeight() : 0;
    int maxHeight =
        parentHeight > 0
            ? Math.max(dp(160), parentHeight - insetTop - topMargin)
            : Math.max(peekHeight, usableDisplayHeight() - topMargin);
    int targetHeight = computeSheetHeight(maxHeight, insetBottom, readerCount);
    ViewGroup.LayoutParams lp = sheet.getLayoutParams();
    if (lp != null && lp.height != targetHeight) {
      lp.height = targetHeight;
      sheet.setLayoutParams(lp);
    }
    if (behavior != null) {
      try {
        Reflect.callMethod(
            behavior, "setPeekHeight", Math.min(peekHeight + insetBottom, maxHeight));
      } catch (Throwable ignored) {
      }
    }
  }

  private int resId(String name, String type) {
    return context.getResources().getIdentifier(name, type, LINE_PKG);
  }

  private int dimenPx(String name, int fallback) {
    try {
      int id = resId(name, "dimen");
      if (id != 0) return context.getResources().getDimensionPixelSize(id);
    } catch (Resources.NotFoundException ignored) {
    }
    return fallback;
  }

  private int dp(int value) {
    return Math.round(value * context.getResources().getDisplayMetrics().density);
  }

  private static List<ReaderRow> loadReaders(String chatId, String msgId) {
    List<StoredReader> stored = readStoredReaders(chatId, msgId);
    if (stored.isEmpty()) return Collections.emptyList();

    List<String> mids = new ArrayList<>(stored.size());
    for (StoredReader reader : stored) mids.add(reader.mid);
    Map<String, LineDBUtils.MemberProfile> profiles = LineDBUtils.resolveMemberProfiles(mids);

    Locale locale = Locale.getDefault();
    SimpleDateFormat parser = new SimpleDateFormat(STORED_TIME_FMT, locale);
    SimpleDateFormat todayFmt = new SimpleDateFormat("HH:mm:ss", locale);
    SimpleDateFormat yearFmt = new SimpleDateFormat("MM/dd HH:mm:ss", locale);
    SimpleDateFormat fullFmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", locale);
    Calendar today = Calendar.getInstance();

    List<ReaderRow> rows = new ArrayList<>(stored.size());
    for (StoredReader reader : stored) {
      LineDBUtils.MemberProfile profile = profiles.get(reader.mid);
      String name =
          profile != null && profile.name != null && !profile.name.isEmpty()
              ? profile.name
              : reader.name;
      String picturePath = profile != null ? profile.picturePath : null;
      String displayTime =
          formatDisplayTime(reader.rawTime, parser, todayFmt, yearFmt, fullFmt, today);
      rows.add(new ReaderRow(reader.mid, name, picturePath, reader.rawTime, displayTime));
    }
    Collections.sort(rows, (a, b) -> a.rawTime.compareTo(b.rawTime));
    return rows;
  }

  private static List<StoredReader> readStoredReaders(String chatId, String msgId) {
    List<StoredReader> list = new ArrayList<>();
    try {
      synchronized (SettingsStore.class) {
        JSONObject root = SettingsStore.loadReadHistory();
        JSONObject chats = root.optJSONObject("c");
        if (chats == null) return list;
        JSONObject chatObj = chats.optJSONObject(chatId);
        if (chatObj == null) return list;
        JSONObject messages = chatObj.optJSONObject("m");
        if (messages == null) return list;
        JSONObject msgObj = messages.optJSONObject(msgId);
        if (msgObj == null) return list;
        JSONObject readers = msgObj.optJSONObject("r");
        if (readers == null) return list;

        Iterator<String> keys = readers.keys();
        while (keys.hasNext()) {
          String mid = keys.next();
          JSONObject entry = readers.optJSONObject(mid);
          if (entry == null) continue;
          String name = entry.optString("n", mid);
          String time = entry.optString("t", "");
          list.add(new StoredReader(mid, name, time));
        }
      }
    } catch (Throwable t) {
      Knot.log("Knot: MessageReadSheet read history parse failed: " + t);
    }
    return list;
  }

  private static String formatDisplayTime(
      String raw,
      SimpleDateFormat parser,
      SimpleDateFormat todayFmt,
      SimpleDateFormat yearFmt,
      SimpleDateFormat fullFmt,
      Calendar today) {
    if (raw == null || raw.isEmpty()) return "";
    try {
      Date date = parser.parse(raw);
      if (date == null) return raw;
      Calendar cal = Calendar.getInstance();
      cal.setTime(date);
      if (cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
          && cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
        return todayFmt.format(date);
      }
      if (cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
        return yearFmt.format(date);
      }
      return fullFmt.format(date);
    } catch (Throwable ignored) {
      return raw;
    }
  }
}
