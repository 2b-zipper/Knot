package app.zipper.knot.mapping;

import app.zipper.knot.LineVersion;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;

final class SettingsRules {

  static final Mappings.Section[] SECTIONS = {
    SettingsRules::resources, SettingsRules::items, SettingsRules::list
  };

  private SettingsRules() {}

  private static void resources(Finder f, LineVersion.Config c) {
    c.res.idArrow = f.id("id", "setting_arrow");
    c.res.idDesc = f.id("id", "setting_description");
    c.res.idHeader = f.id("id", "header");
    c.res.idIcon = f.id("id", "setting_icon");
    c.res.idMark = f.id("id", "setting_mark");
    c.res.idNewMark = f.id("id", "new_mark");
    c.res.idNoticeDot = f.id("id", "notice_dot");
    c.res.idPersonalInfo = f.id("string", "settings_main_index_personalinfo");
    c.res.idSeparator = f.id("id", "setting_separator");
    c.res.idSettingList = f.id("id", "setting_list");
    c.res.idTimestamp = f.id("id", "chat_ui_row_timestamp");
    c.res.idTitle = f.id("id", "setting_title");
    c.res.layoutCheckbox = f.id("layout", "line_user_settings_checkbox_item");
    c.res.layoutSectionHeader = f.id("layout", "line_user_settings_group_header_item");
    c.res.layoutSettingsMain = f.id("layout", "line_user_settings_fragment");
    c.res.typeRow = f.id("layout", "line_user_settings_icon_and_text_item");
    c.res.typeSection = c.res.layoutSectionHeader;

    c.settings.methodSetDescription =
        f.methodOf(
            "com.linecorp.line.settings.base.itemview.LineUserSettingSwitchItemView",
            "void",
            "java.lang.CharSequence",
            "android.text.method.MovementMethod",
            null);
  }

  private static void items(Finder f, LineVersion.Config c) {
    c.settings.settingsHandlerBaseClass =
        Finder.outer(
            f.classAnnotatedIn(
                Finder.packageOf(f.classUsing("LineUserSettingNoticeData(shouldHideFeedback=")),
                "com.linecorp.line.settings.base.model.LineUserSettingItem$Companion$ALWAYS_FALSE$1"));
    String item = c.settings.settingsHandlerBaseClass;
    String items = Finder.packageOf(item);

    c.settings.settingsHeaderItemClass =
        f.clsIn(
            items,
            ClassMatcher.create()
                .superClass(item)
                .addMethod(MethodMatcher.create().usingNumbers(c.res.layoutSectionHeader)));
    c.settings.settingsRowItemClass =
        f.clsIn(
            items,
            ClassMatcher.create()
                .superClass(item)
                .addMethod(MethodMatcher.create().usingNumbers(c.res.typeRow)));

    c.settings.fieldActionHandler = f.ctorField(item, 3, c.kotlin.function1Class);
    c.settings.fieldCommonHandler =
        f.fieldOf(
            item,
            f.classAnnotatedIn(
                items,
                "com.linecorp.line.settings.base.model.LineUserSettingItem$Companion$ALWAYS_TRUE$1"));
    // The action handler's default comes first
    c.settings.fieldDefaultHandler = f.ctorRead(item, 0);
    c.settings.fieldDescriptionProvider = f.ctorField(item, 6, c.kotlin.function2Class);
    c.settings.fieldIconProvider = f.ctorField(item, 5, c.kotlin.function1Class);
    c.settings.fieldIsVisible = f.fieldOf(item, "boolean");
    c.settings.fieldLayoutId = f.fieldOf(item, "int");
    c.settings.fieldModelTag = f.fieldOf(item, "java.lang.String");
    c.settings.fieldSubActionHandler = f.ctorField(item, 7, c.kotlin.function1Class);
    c.settings.fieldVisibilityFilter = f.ctorField(item, 9, c.kotlin.function2Class);
  }

  private static void list(Finder f, LineVersion.Config c) {
    c.settings.settingsSearchHelperClass =
        f.classUsingIdIn(
            Finder.packageOf(
                f.classUsing(
                    "createLypPremiumItemDescription(Landroid/content/Context;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;")),
            "color",
            "line_user_settings_item_large_screen_pressed_color_semantic");
    String helper = c.settings.settingsSearchHelperClass;
    c.settings.methodBindViewHolder =
        f.method(
            helper,
            MethodMatcher.create()
                .addInvoke(
                    MethodMatcher.create()
                        .declaredClass(c.services.locatorClass)
                        .name(c.services.methodLocate)));
    c.settings.methodGetItem =
        f.method(
            helper,
            MethodMatcher.create().addInvoke("Ljava/util/ArrayList;->get(I)Ljava/lang/Object;"));

    c.settings.settingsBaseAdapterClass = f.paramType(helper, c.settings.methodBindViewHolder, 0);
    c.settings.settingsAdapterClass = Finder.outer(c.settings.settingsBaseAdapterClass);
    c.settings.fieldViewHolderView =
        f.fieldOf(c.settings.settingsBaseAdapterClass, "android.view.View");
    c.settings.methodSetItems =
        f.methodOf(c.settings.settingsAdapterClass, "void", "java.util.Collection");

    c.settings.settingsItemClass = f.returnType(helper, c.settings.methodGetItem);
    c.settings.methodProxyGetItemType = f.methodOf(c.settings.settingsItemClass, "int");
    c.settings.settingsAdapterWrapperClass =
        f.cls(
            ClassMatcher.create()
                .addInterface(c.settings.settingsItemClass)
                .addMethod(
                    MethodMatcher.create()
                        .name("<init>")
                        .paramTypes(c.settings.settingsHandlerBaseClass)));
    c.settings.fieldItemModel =
        f.fieldOf(c.settings.settingsAdapterWrapperClass, c.settings.settingsHandlerBaseClass);
  }
}
