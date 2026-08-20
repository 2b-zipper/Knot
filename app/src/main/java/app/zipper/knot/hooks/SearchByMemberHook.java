package app.zipper.knot.hooks;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Reflect;
import app.zipper.knot.SettingsStore;
import app.zipper.knot.utils.LineDBUtils;
import app.zipper.knot.utils.LineTheme;
import app.zipper.knot.utils.ModuleStrings;
import app.zipper.knot.utils.SearchResultLayout;
import io.github.libxposed.api.XposedInterface;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class SearchByMemberHook implements BaseHook {

  private static final String MEMBER_ICON_TAG = "knot_search_member_icon";
  private static final String TOOLTIP_KEY = "knot_search_member_tooltip_shown";
  private static final String LINE_PKG = "jp.naver.line.android";
  private static final String LINE_DB = "naver_line";
  private static final int FILTER_ACTIVE_TINT = 0xFF2196F3;

  private static final Map<String, String> chatMemberFilter = new ConcurrentHashMap<>();
  private static final Map<String, String> chatMemberFilterName = new ConcurrentHashMap<>();
  private static final WeakHashMap<Activity, Object> searchHeaderMap = new WeakHashMap<>();
  private static final Set<String> pendingFetchAll = ConcurrentHashMap.newKeySet();
  private static final Map<String, List<Long>> cachedResults = new ConcurrentHashMap<>();
  private static final Map<String, String> cachedKeywords = new ConcurrentHashMap<>();
  private static final Map<String, Long> forcedKeywordRefreshes = new ConcurrentHashMap<>();
  private static final WeakHashMap<EditText, Boolean> watchedEditTexts = new WeakHashMap<>();
  private static final ThreadLocal<Boolean> creatingResult = new ThreadLocal<>();
  private static final AtomicLong showAllNonce = new AtomicLong();

  private static volatile PopupWindow activeTooltip = null;
  private static ClassLoader appClassLoader;
  private static String searchKeywordEventClass;
  private static LineVersion.Config lineVersionConfig;

  public static boolean hasActiveFilter() {
    return !chatMemberFilter.isEmpty();
  }

  static String getActiveFilterMid(String chatId) {
    return chatId != null ? chatMemberFilter.get(chatId) : null;
  }

  @Override
  public void hook(KnotConfig options, LoadParam lpparam) throws Throwable {
    if (!options.searchByMember.enabled) return;

    appClassLoader = lpparam.classLoader;
    LineVersion.Config config = LineVersion.get();
    if (config == null || !isValidConfig(config)) return;

    lineVersionConfig = config;
    searchKeywordEventClass = config.chat.searchKeywordEventClass;

    setupHeaderHook(config, lpparam.classLoader);
    setupSearchBoxHook(config, lpparam.classLoader);
    setupSearchPresenterHook(config, lpparam.classLoader);
    setupSearchResultHook(config, lpparam.classLoader);
    setupSearchResultWrapperHook(config, lpparam.classLoader);
    setupKeywordTypeHook(config, lpparam.classLoader);
  }

  private boolean isValidConfig(LineVersion.Config config) {
    return !config.chat.searchHeaderHelperClass.isEmpty()
        && !config.chat.searchHeaderControllerField.isEmpty()
        && !config.chat.searchHeaderEventBusField.isEmpty()
        && !config.chat.searchControllerSearchBoxMethod.isEmpty()
        && !config.chat.searchPresenterClass.isEmpty()
        && !config.chat.searchResultClass.isEmpty()
        && !config.chat.searchResultWrapperClass.isEmpty()
        && !config.chat.searchBoxViewClass.isEmpty()
        && !config.chat.searchBoxEditTextField.isEmpty()
        && !config.chat.searchKeywordEventClass.isEmpty()
        && !config.chat.searchPresenterKeywordChangedMethod.isEmpty()
        && !config.chat.searchPresenterKeywordSubjectField.isEmpty()
        && !config.chat.searchKeywordEventKeywordField.isEmpty()
        && !config.chat.searchResultWrapperResultOptionalField.isEmpty();
  }

  private void setupHeaderHook(LineVersion.Config config, ClassLoader classLoader) {
    try {
      Class<?> helperCls = Reflect.findClass(config.chat.searchHeaderHelperClass, classLoader);
      Knot.module
          .hook(Reflect.findMethodExact(helperCls, "a"))
          .intercept(
              chain -> {
                Object result = chain.proceed();
                handleHeaderCreated(chain.getThisObject(), config);
                return result;
              });
    } catch (Throwable t) {
      Knot.log("Knot: SearchByMember setupHeaderHook error: " + t);
    }
  }

  private void handleHeaderCreated(Object helper, LineVersion.Config config) {
    try {
      View headerView = (View) Reflect.getObjectField(helper, "a");
      if (headerView == null) return;

      Activity activity = unwrapActivity(headerView.getContext());
      if (activity == null) return;

      searchHeaderMap.put(activity, helper);
      setupTextWatcherIfNeeded(activity, config);
      injectMemberSearchIcon(activity, headerView, config);
      reapplyActiveFilterOnOpen(activity, config);
    } catch (Throwable t) {
      Knot.log("Knot: handleHeaderCreated error: " + t);
    }
  }

  private void reapplyActiveFilterOnOpen(Activity activity, LineVersion.Config config) {
    String chatId = resolveChatId(activity, config);
    if (chatId == null || !chatMemberFilter.containsKey(chatId)) return;
    EditText et = getSearchEditText(activity);
    if (et == null) return;
    et.post(
        () -> {
          if (chatMemberFilter.containsKey(chatId)) triggerReSearch(activity, chatId);
        });
  }

  private void setupTextWatcherIfNeeded(Activity activity, LineVersion.Config config) {
    EditText et = getSearchEditText(activity);
    if (et == null) return;
    synchronized (watchedEditTexts) {
      if (!watchedEditTexts.containsKey(et)) {
        watchedEditTexts.put(et, Boolean.TRUE);
        addShowAllTextWatcher(et, activity, config);
      }
    }
  }

  private void injectMemberSearchIcon(
      Activity activity, View headerView, LineVersion.Config config) {
    int containerId =
        activity
            .getResources()
            .getIdentifier("chat_ui_header_search_box_container", "id", LINE_PKG);
    if (containerId == 0) return;

    // 26.13.0 swapped this container from FrameLayout to LinearLayout.
    View container = headerView.findViewById(containerId);
    if (!(container instanceof ViewGroup) || ((ViewGroup) container).getChildCount() == 0) return;

    View searchBoxView = ((ViewGroup) container).getChildAt(0);
    int iconId = activity.getResources().getIdentifier("v2_common_search_icon", "id", LINE_PKG);
    if (iconId == 0) return;

    ImageView icon = searchBoxView.findViewById(iconId);
    if (icon == null || MEMBER_ICON_TAG.equals(icon.getTag())) return;

    icon.setTag(MEMBER_ICON_TAG);
    icon.setEnabled(true);
    icon.setOnClickListener(v -> onMemberIconClicked(activity, config, icon));

    updateIconVisualState(activity, config, icon);
    showSearchMemberTooltip(activity, icon);
  }

  private void onMemberIconClicked(Activity activity, LineVersion.Config config, ImageView icon) {
    dismissTooltip(true);
    try {
      String chatId = resolveChatId(activity, config);
      if (chatId == null || chatId.isEmpty()) return;

      if (chatMemberFilter.containsKey(chatId)) {
        clearMemberFilter(activity, chatId, icon);
      } else {
        List<LineDBUtils.MemberInfo> members = LineDBUtils.getChatMembers(chatId);
        if (members.isEmpty()) {
          Toast.makeText(activity, ModuleStrings.SEARCH_BY_MEMBER_EMPTY, Toast.LENGTH_SHORT).show();
          return;
        }
        showMemberPicker(activity, chatId, members, icon);
      }
    } catch (Throwable t) {
      Knot.log("Knot: onMemberIconClicked error: " + t);
    }
  }

  private void updateIconVisualState(Activity activity, LineVersion.Config config, ImageView icon) {
    String chatId = resolveChatId(activity, config);
    if (chatId == null || !chatMemberFilter.containsKey(chatId)) return;
    tintFilterActive(icon);
    String name = chatMemberFilterName.get(chatId);
    if (name != null) setFilteringHint(activity, name);
  }

  private void setupSearchBoxHook(LineVersion.Config config, ClassLoader classLoader) {
    try {
      Class<?> sbCls = Reflect.findClass(config.chat.searchBoxViewClass, classLoader);
      Knot.module
          .hook(Reflect.findMethodExact(sbCls, "setEditingLayout", boolean.class))
          .intercept(
              chain -> {
                Object result = chain.proceed();
                ImageView icon = (ImageView) Reflect.getObjectField(chain.getThisObject(), "d");
                if (icon != null && MEMBER_ICON_TAG.equals(icon.getTag())) icon.setEnabled(true);
                return result;
              });
    } catch (Throwable t) {
      Knot.log("Knot: setupSearchBoxHook error: " + t);
    }
  }

  private void setupSearchPresenterHook(LineVersion.Config config, ClassLoader classLoader) {
    try {
      Class<?> presenterCls = Reflect.findClass(config.chat.searchPresenterClass, classLoader);
      Class<?> eventCls = Reflect.findClass(config.chat.searchKeywordEventClass, classLoader);
      Knot.module
          .hook(
              Reflect.findMethodExact(
                  presenterCls, config.chat.searchPresenterKeywordChangedMethod, eventCls))
          .intercept(
              chain -> {
                handleForcedKeywordRefresh(chain, config);
                return chain.proceed();
              });
    } catch (Throwable t) {
      Knot.log("Knot: setupSearchPresenterHook error: " + t);
    }
  }

  private void setupKeywordTypeHook(LineVersion.Config config, ClassLoader classLoader) {
    if (config.chat.searchKeywordTypeClass.isEmpty()
        || config.chat.searchKeywordTypeMethod.isEmpty()) return;
    try {
      Knot.module
          .hook(
              Reflect.findMethodExact(
                  config.chat.searchKeywordTypeClass,
                  classLoader,
                  config.chat.searchKeywordTypeMethod,
                  String.class))
          .intercept(
              chain -> {
                if (!hasMeaningfulKeyword((String) chain.getArg(0)) && !pendingFetchAll.isEmpty())
                  return Boolean.TRUE;
                return chain.proceed();
              });
    } catch (Throwable t) {
      Knot.log("Knot: SearchByMember setupKeywordTypeHook error: " + t);
    }
  }

  private void setupSearchResultHook(LineVersion.Config config, ClassLoader classLoader) {
    try {
      SearchResultLayout layout = SearchResultLayout.of(config.chat, classLoader);
      Knot.module
          .hook(layout.constructor)
          .intercept(
              chain -> {
                Object[] args = chain.getArgs().toArray();
                handleSearchResultCreation(args, layout);
                return chain.proceed(args);
              });
    } catch (Throwable t) {
      Knot.log("Knot: setupSearchResultHook error: " + t);
    }
  }

  @SuppressWarnings("unchecked")
  private void handleSearchResultCreation(Object[] args, SearchResultLayout layout) {
    try {
      if (Boolean.TRUE.equals(creatingResult.get())) return;

      String chatId = (String) args[layout.chatId];
      if (chatId == null) return;

      String senderMid = chatMemberFilter.get(chatId);
      if (senderMid == null) return;

      String keyword = (String) args[layout.keyword];
      List<Long> ids = (List<Long>) args[layout.idList];
      if (ids == null || ids.isEmpty()) {
        handleEmptyIds(chatId, senderMid, keyword, args, layout);
      } else {
        List<Long> filtered = filterLocalIdsByMid(ids, senderMid);
        cacheResults(chatId, filtered, keyword);
        args[layout.idList] = filtered;
        args[layout.count] = filtered.size();
      }
    } catch (Throwable t) {
      Knot.log("Knot: handleSearchResultCreation error: " + t);
    }
  }

  private void handleEmptyIds(
      String chatId, String senderMid, String keyword, Object[] args, SearchResultLayout layout) {
    if (pendingFetchAll.remove(chatId) || !hasMeaningfulKeyword(keyword)) {
      List<Long> allIds = fetchAllMemberLocalIds(chatId, senderMid);
      cacheResults(chatId, allIds, keyword);
      args[layout.idList] = allIds;
      args[layout.count] = allIds.size();
    } else {
      cachedResults.remove(chatId);
      cachedKeywords.put(chatId, normalizeKeyword(keyword));
    }
  }

  private void setupSearchResultWrapperHook(LineVersion.Config config, ClassLoader classLoader) {
    try {
      SearchResultLayout layout = SearchResultLayout.of(config.chat, classLoader);
      Knot.module
          .hook(
              Reflect.findConstructorExact(
                  config.chat.searchResultWrapperClass, classLoader, String.class, String.class))
          .intercept(
              chain -> {
                Object result = chain.proceed();
                handleSearchResultWrapperCreated(chain, layout);
                return result;
              });

      Knot.module
          .hook(
              Reflect.findMethodExact(
                  config.chat.searchResultWrapperClass, classLoader, "equals", Object.class))
          .intercept(chain -> Boolean.FALSE);
    } catch (Throwable t) {
      Knot.log("Knot: setupSearchResultWrapperHook error: " + t);
    }
  }

  private void handleSearchResultWrapperCreated(
      XposedInterface.Chain chain, SearchResultLayout layout) {
    try {
      String chatId = (String) chain.getArg(0);
      if (chatId == null) return;
      String senderMid = chatMemberFilter.get(chatId);
      if (senderMid == null) return;

      String keyword = (String) chain.getArg(1);
      String resultKeyword = keyword != null ? keyword : "";
      String normalizedKeyword = normalizeKeyword(resultKeyword);

      List<Long> ids = null;
      if (pendingFetchAll.remove(chatId)) {
        ids = fetchAllMemberLocalIds(chatId, senderMid);
        cacheResults(chatId, ids, resultKeyword);
      } else if (Objects.equals(cachedKeywords.get(chatId), normalizedKeyword)) {
        ids = cachedResults.get(chatId);
      }

      if (ids == null) {
        Knot.log("Knot: SearchByMember wrapperCreated resolved no ids, skipping injection");
        return;
      }

      creatingResult.set(Boolean.TRUE);
      try {
        String injectKeyword = distinctifyShowAllKeyword(resultKeyword);
        Object result = layout.newInstance(chatId, injectKeyword, ids.size(), ids);
        Reflect.setObjectField(
            chain.getThisObject(),
            lineVersionConfig.chat.searchResultWrapperResultOptionalField,
            Optional.of(result));
        Knot.log("Knot: SearchByMember injected wrapperCreated ids=" + ids.size());
      } finally {
        creatingResult.remove();
      }
    } catch (Throwable t) {
      Knot.log("Knot: handleSearchResultWrapperCreated error: " + t);
    }
  }

  private static String distinctifyShowAllKeyword(String keyword) {
    if (keyword == null || !keyword.isEmpty()) return keyword;
    return (showAllNonce.getAndIncrement() & 1L) == 0 ? "" : "\u200B";
  }

  private static List<Long> filterLocalIdsByMid(List<Long> ids, String senderMid) {
    File dbFile = lineDatabaseFile();
    if (dbFile == null) return ids;
    try (SQLiteDatabase db =
        SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY)) {
      StringBuilder sb = new StringBuilder("SELECT id FROM chat_history WHERE id IN (");
      for (int i = 0; i < ids.size(); i++) {
        if (i > 0) sb.append(",");
        sb.append("?");
      }
      sb.append(") AND ").append(senderMidPredicate(isOwnMid(senderMid)));

      String[] args = new String[ids.size() + 1];
      for (int i = 0; i < ids.size(); i++) args[i] = String.valueOf(ids.get(i));
      args[ids.size()] = senderMid;

      Set<Long> kept = new HashSet<>();
      try (Cursor cursor = db.rawQuery(sb.toString(), args)) {
        while (cursor.moveToNext()) kept.add(cursor.getLong(0));
      }

      List<Long> result = new ArrayList<>();
      for (Long id : ids) if (kept.contains(id)) result.add(id);
      return result;
    } catch (Throwable t) {
      Knot.log("Knot: filterLocalIdsByMid error: " + t);
      return ids;
    }
  }

  private static List<Long> fetchAllMemberLocalIds(String chatId, String senderMid) {
    File dbFile = lineDatabaseFile();
    if (dbFile == null) return new ArrayList<>();
    try {
      String sql =
          "SELECT id FROM chat_history WHERE chat_id = ? AND "
              + senderMidPredicate(isOwnMid(senderMid))
              + " ORDER BY id DESC";
      try (SQLiteDatabase db =
              SQLiteDatabase.openDatabase(
                  dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
          Cursor cursor = db.rawQuery(sql, new String[] {chatId, senderMid})) {
        List<Long> result = new ArrayList<>();
        while (cursor.moveToNext()) result.add(cursor.getLong(0));
        return result;
      }
    } catch (Throwable t) {
      Knot.log("Knot: fetchAllMemberLocalIds error: " + t);
      return new ArrayList<>();
    }
  }

  private static File lineDatabaseFile() {
    Application app = Knot.currentApplication();
    if (app == null) return null;
    File dbFile = app.getDatabasePath(LINE_DB);
    return dbFile.exists() ? dbFile : null;
  }

  private static boolean isOwnMid(String senderMid) {
    return senderMid.equals(LineDBUtils.getMyMid());
  }

  private static String senderMidPredicate(boolean isOwn) {
    return isOwn ? "(from_mid = ? OR from_mid IS NULL)" : "from_mid = ?";
  }

  private static void cacheResults(String chatId, List<Long> ids, String keyword) {
    cachedResults.put(chatId, ids);
    cachedKeywords.put(chatId, normalizeKeyword(keyword));
  }

  private static void clearFilterCaches(String chatId) {
    pendingFetchAll.remove(chatId);
    cachedResults.remove(chatId);
    cachedKeywords.remove(chatId);
  }

  private static void tintFilterActive(ImageView icon) {
    icon.setColorFilter(FILTER_ACTIVE_TINT, PorterDuff.Mode.SRC_ATOP);
  }

  private void setFilteringHint(Activity activity, String name) {
    EditText et = getSearchEditText(activity);
    if (et != null) et.setHint(ModuleStrings.SEARCH_BY_MEMBER_FILTERING + name);
  }

  private void showMemberPicker(
      Activity activity, String chatId, List<LineDBUtils.MemberInfo> members, ImageView icon) {
    LineTheme.invalidate();
    String[] names = members.stream().map(m -> m.name).toArray(String[]::new);
    int themeId = LineTheme.dialogTheme(activity);
    LineTheme.applyDialogColors(
        new AlertDialog.Builder(activity, themeId)
            .setTitle(ModuleStrings.SEARCH_BY_MEMBER_TITLE)
            .setItems(
                names,
                (d, i) -> {
                  LineDBUtils.MemberInfo member = members.get(i);
                  chatMemberFilter.put(chatId, member.mid);
                  chatMemberFilterName.put(chatId, member.name);
                  clearFilterCaches(chatId);
                  tintFilterActive(icon);
                  setFilteringHint(activity, member.name);
                  triggerReSearch(activity, chatId);
                })
            .setNegativeButton(android.R.string.cancel, null)
            .show(),
        activity);
  }

  private void clearMemberFilter(Activity activity, String chatId, ImageView icon) {
    clearFilterCaches(chatId);
    chatMemberFilterName.remove(chatId);
    chatMemberFilter.remove(chatId);
    icon.clearColorFilter();
    EditText et = getSearchEditText(activity);
    if (et != null) et.setHint("");
    triggerReSearch(activity, chatId);
    Toast.makeText(activity, ModuleStrings.SEARCH_BY_MEMBER_FILTER_CLEARED, Toast.LENGTH_SHORT)
        .show();
  }

  private EditText getSearchEditText(Activity activity) {
    try {
      Object helper = searchHeaderMap.get(activity);
      if (helper == null) return null;
      Object controller = getSearchController(helper);
      if (controller == null) return null;
      Object sbView = getSearchBoxView(controller);
      if (sbView == null) return null;
      return (EditText)
          Reflect.getObjectField(sbView, lineVersionConfig.chat.searchBoxEditTextField);
    } catch (Throwable t) {
      Knot.log("Knot: getSearchEditText error: " + t);
      return null;
    }
  }

  private void addShowAllTextWatcher(EditText et, Activity activity, LineVersion.Config config) {
    et.addTextChangedListener(
        new android.text.TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int st, int c, int a) {}

          @Override
          public void onTextChanged(CharSequence s, int st, int b, int c) {}

          @Override
          public void afterTextChanged(android.text.Editable s) {
            if (s.length() != 0) return;
            String chatId = resolveChatId(activity, config);
            if (chatId != null && chatMemberFilter.containsKey(chatId))
              triggerReSearch(activity, chatId);
          }
        });
  }

  private void triggerReSearch(Activity activity, String chatId) {
    try {
      Object helper = searchHeaderMap.get(activity);
      if (helper == null) return;

      Object controller = getSearchController(helper);
      if (controller == null) {
        Knot.log(
            "Knot: triggerReSearch aborted, no search controller in field ."
                + lineVersionConfig.chat.searchHeaderControllerField);
        return;
      }
      Object sbView = getSearchBoxView(controller);
      if (sbView == null) {
        Knot.log(
            "Knot: triggerReSearch aborted, no search box from ."
                + lineVersionConfig.chat.searchControllerSearchBoxMethod
                + "()");
        return;
      }
      String currentText = (String) Reflect.callMethod(sbView, "getSearchText");
      if (currentText == null) currentText = "";

      boolean filterActive = chatId != null && chatMemberFilter.containsKey(chatId);
      if (filterActive && !hasMeaningfulKeyword(currentText)) {
        pendingFetchAll.add(chatId);
      } else if (chatId != null) {
        pendingFetchAll.remove(chatId);
      }

      if (!postSearchKeywordEvent(helper, currentText)) {
        Knot.log("Knot: triggerReSearch failed to dispatch keyword");
      }
    } catch (Throwable t) {
      Knot.log("Knot: triggerReSearch error: " + t);
    }
  }

  private boolean postSearchKeywordEvent(Object helper, String keyword) {
    try {
      Object eventBus =
          Reflect.getObjectField(helper, lineVersionConfig.chat.searchHeaderEventBusField);
      if (eventBus == null) return false;

      Class<?> eventClass = Reflect.findClass(searchKeywordEventClass, appClassLoader);
      Object event = Reflect.newInstance(eventClass, keyword);
      requestForcedKeywordRefresh(keyword);
      Reflect.callMethod(eventBus, "b", event);
      return true;
    } catch (Throwable t) {
      forcedKeywordRefreshes.remove(keyword != null ? keyword : "");
      Knot.log("Knot: postSearchKeywordEvent error: " + t);
      return false;
    }
  }

  private void requestForcedKeywordRefresh(String keyword) {
    forcedKeywordRefreshes.put(keyword != null ? keyword : "", System.nanoTime());
  }

  private void handleForcedKeywordRefresh(XposedInterface.Chain chain, LineVersion.Config config) {
    try {
      Object event = chain.getArg(0);
      String keyword =
          (String) Reflect.getObjectField(event, config.chat.searchKeywordEventKeywordField);
      if (!consumeForcedKeywordRefresh(keyword)) return;

      Object keywordSubject =
          Reflect.getObjectField(
              chain.getThisObject(), config.chat.searchPresenterKeywordSubjectField);
      AtomicReference<Object> currentValueRef = getBehaviorSubjectCurrentValueRef(keywordSubject);
      if (currentValueRef == null) {
        Knot.log(
            "Knot: SearchByMember could not reach the keyword subject value, re-search deduped");
        return;
      }

      currentValueRef.lazySet("__knot_force_refresh__" + System.nanoTime());
    } catch (Throwable t) {
      Knot.log("Knot: handleForcedKeywordRefresh error: " + t);
    }
  }

  private boolean consumeForcedKeywordRefresh(String keyword) {
    String key = keyword != null ? keyword : "";
    Long requestedAt = forcedKeywordRefreshes.get(key);
    if (requestedAt == null) return false;
    long ageNanos = System.nanoTime() - requestedAt;
    if (ageNanos > 5_000_000_000L) {
      forcedKeywordRefreshes.remove(key, requestedAt);
      return false;
    }
    return forcedKeywordRefreshes.remove(key, requestedAt);
  }

  private static Object readBehaviorSubjectValue(Object subject) throws Throwable {
    String name = lineVersionConfig.chat.searchKeywordSubjectValueMethod;
    if (name.isEmpty()) return null;
    Method getter = Reflect.findMethodExact(subject.getClass(), name);
    // A stale mapping can land on the subject's static create(), which returns a fresh empty
    // subject instead of throwing.
    if (Modifier.isStatic(getter.getModifiers())) return null;
    return getter.invoke(subject);
  }

  @SuppressWarnings("unchecked")
  private AtomicReference<Object> getBehaviorSubjectCurrentValueRef(Object subject) {
    if (subject == null) return null;
    try {
      Object currentValue = readBehaviorSubjectValue(subject);
      for (Field field : subject.getClass().getDeclaredFields()) {
        if (!AtomicReference.class.isAssignableFrom(field.getType())) continue;
        field.setAccessible(true);
        Object ref = field.get(subject);
        if (!(ref instanceof AtomicReference<?>)) continue;
        AtomicReference<?> atomicRef = (AtomicReference<?>) ref;
        if (Objects.equals(atomicRef.get(), currentValue)) return (AtomicReference<Object>) ref;
      }
    } catch (Throwable t) {
      Knot.log("Knot: getBehaviorSubjectCurrentValueRef error: " + t);
    }
    return null;
  }

  private Object getSearchController(Object helper) {
    return Reflect.getObjectField(helper, lineVersionConfig.chat.searchHeaderControllerField);
  }

  private Object getSearchBoxView(Object controller) {
    return Reflect.callMethod(controller, lineVersionConfig.chat.searchControllerSearchBoxMethod);
  }

  private static boolean hasMeaningfulKeyword(String keyword) {
    if (keyword == null || keyword.isEmpty()) return false;

    for (int i = 0; i < keyword.length(); ) {
      int codePoint = keyword.codePointAt(i);
      i += Character.charCount(codePoint);

      if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)) continue;
      if (Character.isISOControl(codePoint)) continue;
      if (Character.getType(codePoint) == Character.FORMAT) continue;
      return true;
    }
    return false;
  }

  private static String normalizeKeyword(String keyword) {
    return hasMeaningfulKeyword(keyword) ? keyword : "";
  }

  private static void showSearchMemberTooltip(Activity activity, View anchor) {
    if (SettingsStore.get(TOOLTIP_KEY, false)) return;
    if (activeTooltip != null && activeTooltip.isShowing()) return;

    anchor.post(
        () -> {
          if (anchor.getWidth() == 0 || !anchor.isShown()) return;
          if (SettingsStore.get(TOOLTIP_KEY, false)) return;
          try {
            LineVersion.Config cfg = LineVersion.get();
            if (cfg == null) return;
            String pkg = cfg.linePkg;
            float dp = activity.getResources().getDisplayMetrics().density;

            ImageView arrowView = new ImageView(activity);
            int arrowId =
                activity.getResources().getIdentifier(cfg.res.resTooltipArrowUp, "drawable", pkg);
            if (arrowId != 0) arrowView.setImageResource(arrowId);
            arrowView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

            LinearLayout.LayoutParams arrowLp =
                new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            arrowLp.gravity = Gravity.START;
            arrowLp.leftMargin =
                Math.max(0, anchor.getWidth() / 2 - arrowView.getMeasuredWidth() / 2);

            TextView text = new TextView(activity);
            text.setText(ModuleStrings.TOOLTIP_SEARCH_BY_MEMBER);
            text.setTextColor(Color.WHITE);
            text.setTextSize(13f);
            int ph = (int) (12 * dp), pv = (int) (7 * dp);
            text.setPadding(ph, pv, ph, pv);
            int bgId =
                activity
                    .getResources()
                    .getIdentifier(cfg.res.resTooltipBackground, "drawable", pkg);
            if (bgId != 0) text.setBackgroundResource(bgId);

            LinearLayout container = new LinearLayout(activity);
            container.setOrientation(LinearLayout.VERTICAL);
            container.addView(arrowView, arrowLp);
            container.addView(
                text,
                new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            container.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

            PopupWindow popup =
                new PopupWindow(
                    container,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            popup.setOutsideTouchable(true);
            popup.setFocusable(false);
            popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            container.setOnClickListener(v -> dismissTooltip(true));
            popup.showAsDropDown(anchor, 0, 0);
            activeTooltip = popup;
          } catch (Throwable t) {
            Knot.log("Knot: SearchByMember tooltip error: " + t);
          }
        });
  }

  private static void dismissTooltip(boolean markShown) {
    PopupWindow p = activeTooltip;
    activeTooltip = null;
    if (p != null)
      try {
        p.dismiss();
      } catch (Throwable ignored) {
      }
    if (markShown) SettingsStore.save(TOOLTIP_KEY, true);
  }

  private Activity unwrapActivity(Context ctx) {
    while (ctx instanceof ContextWrapper) {
      if (ctx instanceof Activity) return (Activity) ctx;
      ctx = ((ContextWrapper) ctx).getBaseContext();
    }
    return null;
  }

  private String resolveChatId(Activity activity, LineVersion.Config config) {
    try {
      android.os.Bundle extras = activity.getIntent().getExtras();
      if (extras != null) {
        android.os.Parcelable req = extras.getParcelable("chat-history-request");
        if (req != null) {
          Object id = Reflect.callMethod(req, "getChatId");
          if (id instanceof String && !((String) id).isEmpty()) return (String) id;
        }
      }
    } catch (Throwable ignored) {
    }
    for (String key : new String[] {"chatId", "chat_id", "extra-chat-id"}) {
      String id = activity.getIntent().getStringExtra(key);
      if (id != null && !id.isEmpty()) return id;
    }
    if (config != null) {
      try {
        Object field = Reflect.getObjectField(activity, config.chat.chatIdField);
        if (field != null) return (String) Reflect.callMethod(field, config.chat.methodGetChatId);
      } catch (Throwable ignored) {
      }
    }
    return null;
  }
}
