package app.zipper.knot.ui.settings;

import static app.zipper.knot.ui.settings.SettingsViews.dp;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.Main;
import app.zipper.knot.R;
import app.zipper.knot.Reflect;
import app.zipper.knot.SettingsStore;
import app.zipper.knot.hooks.BackupRestoreHook;
import app.zipper.knot.hooks.FcmFixHook;
import app.zipper.knot.hooks.HomeTabTypeHook;
import app.zipper.knot.utils.LineTheme;
import app.zipper.knot.utils.ModuleResources;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class SettingsPage {

  private static final KnotConfig.Category[] DISPLAY_CATEGORIES = {
    KnotConfig.Category.PRIVACY,
    KnotConfig.Category.CHAT,
    KnotConfig.Category.DISPLAY,
    KnotConfig.Category.NOTIFICATION,
    KnotConfig.Category.SYSTEM
  };

  private static volatile Object toggleType = null;
  private static volatile Object statusEnum = null;

  private final Context ctx;
  private final KnotSettingsDialog dialog;
  private final LinearLayout list;

  private SettingsPage(Context ctx, KnotSettingsDialog dialog) {
    this.ctx = ctx;
    this.dialog = dialog;
    this.list = new LinearLayout(ctx);
  }

  public static View build(Context ctx, KnotSettingsDialog dialog, KnotConfig.Category category) {
    return new SettingsPage(ctx, dialog).render(category, false);
  }

  public static View buildAllCategories(Context ctx, KnotSettingsDialog dialog) {
    return new SettingsPage(ctx, dialog).render(null, true);
  }

  public static void filter(View pageRoot, String query) {
    ViewGroup list;
    if (pageRoot instanceof ScrollView) {
      list = (ViewGroup) ((ScrollView) pageRoot).getChildAt(0);
    } else if (pageRoot instanceof ViewGroup) {
      list = (ViewGroup) pageRoot;
    } else {
      return;
    }
    if (list == null) return;

    boolean isSearching = !query.isEmpty();
    View lastHeader = null;
    int itemsInCurrentSection = 0;

    for (int i = 0; i < list.getChildCount(); i++) {
      View child = list.getChildAt(i);
      Object tag = child.getTag();

      if (SettingsViews.TAG_SECTION_HEADER.equals(tag)) {
        if (lastHeader != null) {
          lastHeader.setVisibility(
              itemsInCurrentSection > 0 || !isSearching ? View.VISIBLE : View.GONE);
        }
        lastHeader = child;
        itemsInCurrentSection = 0;
        continue;
      }

      if (!isSearching) {
        child.setVisibility(View.VISIBLE);
        continue;
      }

      if (tag instanceof String) {
        boolean matches = ((String) tag).contains(query);
        child.setVisibility(matches ? View.VISIBLE : View.GONE);
        if (matches) itemsInCurrentSection++;
      }
    }

    if (lastHeader != null) {
      lastHeader.setVisibility(
          itemsInCurrentSection > 0 || !isSearching ? View.VISIBLE : View.GONE);
    }
  }

  private View render(KnotConfig.Category category, boolean expandAll) {
    int bgColor = LineTheme.backgroundColor(ctx);
    list.setOrientation(LinearLayout.VERTICAL);
    list.setBackgroundColor(bgColor);
    list.setPadding(0, 0, 0, dp(ctx, 64));

    if (category != null) {
      addCategoryItems(category);
    } else {
      addStorageSection();

      if (expandAll) {
        for (KnotConfig.Category cat : DISPLAY_CATEGORIES) {
          sectionHeader(cat.label());
          addCategoryItems(cat);
        }
      } else {
        sectionHeader(ModuleResources.get(R.string.settings_title));
        for (KnotConfig.Category cat : DISPLAY_CATEGORIES) {
          row().title(cat.label()).onClick(v -> dialog.openCategory(ctx, cat)).add();
        }
      }

      addBackupSection();
      addOtherSection();
    }

    ScrollView scroller = new ScrollView(ctx);
    scroller.setBackgroundColor(bgColor);
    scroller.addView(list);
    return scroller;
  }

  private SettingsViews.Row row() {
    return SettingsViews.row(ctx, list);
  }

  private void sectionHeader(String text) {
    SettingsViews.sectionHeader(ctx, list, text);
  }

  private void addStorageSection() {
    sectionHeader(ModuleResources.get(R.string.cat_storage));

    String activePath = SettingsStore.getSettingsDir();
    row()
        .title(
            activePath == null
                ? ModuleResources.get(R.string.settings_path_picker_hint)
                : activePath)
        .description(ModuleResources.get(R.string.desc_path_row))
        .titleColor(activePath == null ? Color.RED : LineTheme.accentGreen(ctx))
        .searchTag(
            SettingsViews.searchTag(
                ModuleResources.get(R.string.cat_storage),
                ModuleResources.get(R.string.desc_path_row)))
        .onClick(v -> SettingsFilePickers.openFolderPicker(ctx))
        .add();
  }

  private void addBackupSection() {
    sectionHeader(ModuleResources.get(R.string.cat_backup));

    row()
        .title(ModuleResources.get(R.string.opt_backup_label))
        .description(ModuleResources.get(R.string.opt_backup_desc))
        .onClick(v -> BackupRestoreHook.runBackup(ctx))
        .add();

    row()
        .title(ModuleResources.get(R.string.opt_restore_label))
        .description(ModuleResources.get(R.string.opt_restore_desc))
        .onClick(v -> SettingsFilePickers.openRestorePicker(ctx))
        .add();
  }

  private void addOtherSection() {
    sectionHeader(ModuleResources.get(R.string.cat_other));

    row()
        .title(ModuleResources.get(R.string.opt_language_label))
        .description(ModuleResources.get(R.string.opt_language_desc))
        .value(languageLabel())
        .onClick(v -> openLanguagePicker())
        .add();

    row()
        .title(ModuleResources.get(R.string.opt_about_label))
        .description(ModuleResources.get(R.string.opt_about_desc))
        .onClick(v -> dialog.openAbout(dialog.dialogContext()))
        .add();

    row()
        .title(ModuleResources.get(R.string.settings_reset))
        .description(ModuleResources.get(R.string.desc_reset_row))
        .titleColor(Color.RED)
        .noArrow()
        .onClick(v -> confirmReset())
        .add();
  }

  private void confirmReset() {
    Context dialogCtx = dialog.dialogContext();
    LineTheme.applyDialogColors(
        new AlertDialog.Builder(dialogCtx, LineTheme.dialogTheme(dialogCtx))
            .setTitle(ModuleResources.get(R.string.settings_reset))
            .setMessage(ModuleResources.get(R.string.settings_reset_confirm))
            .setPositiveButton(
                ModuleResources.get(R.string.settings_reset_ok),
                (d, w) -> {
                  SettingsStore.reset();
                  SettingsStore.load(Main.options);
                  ModuleResources.invalidate();
                  dialog.onConfigChanged();
                })
            .setNegativeButton(ModuleResources.get(R.string.settings_cancel), null)
            .show(),
        dialogCtx);
  }

  private void addCategoryItems(KnotConfig.Category category) {
    int lastSectionRes = 0;
    for (KnotConfig.Item item : Main.options.items) {
      if (item.category != category) continue;
      if (item.sectionRes != 0 && item.sectionRes != lastSectionRes) {
        sectionHeader(item.section());
        lastSectionRes = item.sectionRes;
      }
      addItemRow(item);
    }
  }

  private void addItemRow(KnotConfig.Item item) {
    try {
      switch (item.key) {
        case "custom_font_path":
          addPickerRow(
              item,
              SettingsFilePickers.currentFontName(),
              v -> SettingsFilePickers.openFontPicker(ctx));
          break;
        case "home_tab_type":
          String homeType = SettingsStore.getString(item.key, "");
          addPickerRow(
              item,
              homeType.isEmpty() ? ModuleResources.get(R.string.home_type_default) : homeType,
              v -> openHomeTypePicker(item));
          break;
        case "fcm_fix_mode":
          addPickerRow(item, fcmFixModeLabel(), v -> openFcmFixModePicker(item));
          break;
        case "fcm_force_registration":
          addPickerRow(item, v -> forceFcmRegistration());
          break;
        default:
          addToggleRow(item);
      }
    } catch (Throwable ignored) {
    }
  }

  private void addPickerRow(KnotConfig.Item item, View.OnClickListener onClick) {
    addPickerRow(item, null, onClick);
  }

  private void addPickerRow(
      KnotConfig.Item item, CharSequence value, View.OnClickListener onClick) {
    row().title(item.label()).description(item.description()).value(value).onClick(onClick).add();
  }

  private void addToggleRow(KnotConfig.Item item) {
    LineVersion.Config cfg = LineVersion.get();
    View row = LayoutInflater.from(ctx).inflate(cfg.res.layoutCheckbox, list, false);

    Reflect.callMethod(row, cfg.settings.methodSetTitleText, item.label());
    Reflect.callMethod(row, cfg.settings.methodSetDescription, item.description(), null, null);

    cacheToggleConstants(cfg);
    if (toggleType != null) Reflect.callMethod(row, cfg.settings.methodSetItemType, toggleType);
    if (statusEnum != null) Reflect.callMethod(row, cfg.settings.methodSetSyncStatus, statusEnum);

    Reflect.callMethod(
        row, cfg.settings.methodSetChecked, SettingsStore.get(item.key, item.enabled));
    Reflect.callMethod(row, cfg.settings.methodSetDividerVisible, true);

    Runnable refreshState = () -> applyDisabledState(row, item, cfg);
    dialog.addStateRefresher(refreshState);
    refreshState.run();

    row.setOnClickListener(v -> toggleItem(row, item, cfg));
    row.setTag(SettingsViews.searchTag(item.label(), item.description()));
    list.addView(row);
  }

  private void cacheToggleConstants(LineVersion.Config cfg) {
    if (toggleType != null && statusEnum != null) return;
    try {
      View probe = LayoutInflater.from(ctx).inflate(cfg.res.layoutCheckbox, null, false);
      for (Method m : probe.getClass().getMethods()) {
        if (m.getParameterCount() != 1) continue;
        Class<?> p = m.getParameterTypes()[0];
        if (!p.isEnum()) continue;
        if ("setItemType".equals(m.getName())) {
          for (Object c : p.getEnumConstants()) if ("TOGGLE".equals(c.toString())) toggleType = c;
        } else if ("setSyncStatus".equals(m.getName())) {
          for (Object c : p.getEnumConstants()) if ("SUCCESS".equals(c.toString())) statusEnum = c;
        }
      }
    } catch (Throwable ignored) {
    }
  }

  private void applyDisabledState(View row, KnotConfig.Item item, LineVersion.Config cfg) {
    if (item.disabledWhenEnabledKey == null) return;
    boolean isDisabled = SettingsStore.get(item.disabledWhenEnabledKey, false);
    row.setAlpha(isDisabled ? 0.5f : 1.0f);
    if (!isDisabled) return;

    Reflect.callMethod(row, cfg.settings.methodSetChecked, false);
    if (!SettingsStore.get(item.key, false)) return;
    saveItem(item.key, false);
  }

  private void toggleItem(View row, KnotConfig.Item item, LineVersion.Config cfg) {
    if (item.disabledWhenEnabledKey != null
        && SettingsStore.get(item.disabledWhenEnabledKey, false)) {
      Reflect.callMethod(row, cfg.settings.methodSetChecked, false);
      return;
    }
    boolean newState = !SettingsStore.get(item.key, item.enabled);
    Reflect.callMethod(row, cfg.settings.methodSetChecked, newState);
    saveItem(item.key, newState);
    dialog.onItemToggled();
  }

  private static void saveItem(String key, boolean enabled) {
    SettingsStore.save(key, enabled);
    KnotConfig.Item registered = Main.options.find(key);
    if (registered != null) registered.enabled = enabled;
  }

  private static void saveItem(String key, String value) {
    SettingsStore.save(key, value);
    KnotConfig.Item registered = Main.options.find(key);
    if (registered != null) registered.value = value;
  }

  private void forceFcmRegistration() {
    if (!SettingsStore.get("experimental_fcm_fix", false)) {
      toast(ModuleResources.get(R.string.fcm_force_registration_needs_fix));
      return;
    }
    toast(
        FcmFixHook.requestFcmTokenRefresh(ctx.getClassLoader())
            ? ModuleResources.get(R.string.fcm_force_registration_started)
            : ModuleResources.get(R.string.fcm_force_registration_failed));
  }

  private void toast(String message) {
    Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show();
  }

  private static String languageLabel() {
    String tag = ModuleResources.language();
    return tag.isEmpty()
        ? ModuleResources.get(R.string.language_system)
        : ModuleResources.displayName(tag);
  }

  private void openLanguagePicker() {
    List<String> values = new ArrayList<>();
    List<String> labels = new ArrayList<>();
    values.add(ModuleResources.LANGUAGE_SYSTEM);
    labels.add(ModuleResources.get(R.string.language_system));
    for (String tag : ModuleResources.SUPPORTED_LANGUAGES) {
      values.add(tag);
      labels.add(ModuleResources.displayName(tag));
    }

    Context dialogCtx = dialog.dialogContext();
    LineTheme.applyDialogColors(
        new AlertDialog.Builder(dialogCtx, LineTheme.dialogTheme(dialogCtx))
            .setTitle(ModuleResources.get(R.string.opt_language_label))
            .setSingleChoiceItems(
                labels.toArray(new String[0]),
                Math.max(values.indexOf(ModuleResources.language()), 0),
                (d, which) -> {
                  ModuleResources.setLanguage(values.get(which));
                  d.dismiss();
                  dialog.onLanguageChanged();
                })
            .setNegativeButton(ModuleResources.get(R.string.settings_cancel), null)
            .show(),
        dialogCtx);
  }

  private static String fcmFixMode() {
    return FcmFixHook.normalizeMode(SettingsStore.getString("fcm_fix_mode", FcmFixHook.MODE_LEGY));
  }

  private static String fcmFixModeLabel() {
    return ModuleResources.get(
        FcmFixHook.MODE_FIS.equals(fcmFixMode())
            ? R.string.fcm_fix_mode_fis
            : R.string.fcm_fix_mode_legy);
  }

  private void openFcmFixModePicker(KnotConfig.Item item) {
    List<String> values = new ArrayList<>();
    values.add(FcmFixHook.MODE_LEGY);
    values.add(FcmFixHook.MODE_FIS);

    String[] labels = {
      ModuleResources.get(R.string.fcm_fix_mode_legy),
      ModuleResources.get(R.string.fcm_fix_mode_fis)
    };

    showChoicePicker(item, values, labels, fcmFixMode());
  }

  private void openHomeTypePicker(KnotConfig.Item item) {
    List<String> values = new ArrayList<>();
    values.add("");
    values.addAll(HomeTabTypeHook.availableHomeTypes(ctx.getClassLoader()));

    String[] labels = values.toArray(new String[0]);
    labels[0] = ModuleResources.get(R.string.home_type_default);

    showChoicePicker(item, values, labels, SettingsStore.getString(item.key, ""));
  }

  private void showChoicePicker(
      KnotConfig.Item item, List<String> values, String[] labels, String current) {
    LineTheme.applyDialogColors(
        new AlertDialog.Builder(ctx, LineTheme.dialogTheme(ctx))
            .setTitle(item.label())
            .setSingleChoiceItems(
                labels,
                Math.max(values.indexOf(current), 0),
                (d, which) -> {
                  saveItem(item.key, values.get(which));
                  d.dismiss();
                  dialog.onConfigChanged();
                })
            .setNegativeButton(ModuleResources.get(R.string.settings_cancel), null)
            .show(),
        ctx);
  }
}
