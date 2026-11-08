package app.zipper.knot.mapping;

import static app.zipper.knot.mapping.Finder.sig;

import app.zipper.knot.LineVersion;
import java.lang.reflect.Modifier;
import java.util.List;
import org.luckypray.dexkit.query.enums.StringMatchType;
import org.luckypray.dexkit.query.enums.UsingType;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

final class HeaderRules {

  private static final String HEADER = "jp.naver.line.android.common.view.header.Header";

  static final Mappings.Section[] SECTIONS = {
    HeaderRules::chatHeader,
    HeaderRules::talkTab,
    HeaderRules::home,
    HeaderRules::ads,
    HeaderRules::agentI
  };

  private HeaderRules() {}

  // The main tabs' header buttons use this helper too
  private static void chatHeader(Finder f, LineVersion.Config c) {
    c.chatHeader.headerController =
        f.classUsingIdIn(
            Finder.packageOf(
                f.classUsing(
                    "getUtsParams()Lcom/linecorp/line/chat/ui/dialog/model/BlockedContactDeleteSuggestDialogUtsParams;")),
            "dimen",
            "titlebar_text_size");
    String controller = c.chatHeader.headerController;
    List<String> args = f.ctorParams(controller, 11);
    c.chatHeader.controllerParam4Class = Finder.at(args, 4);
    c.chatHeader.controllerParam5Class = Finder.at(args, 5);
    c.chatHeader.controllerParam6Class = f.classUsing("SQUARE_OA_SHOP_CONFIRM_DIALOG_REQUEST_KEY");
    c.chatHeader.controllerParam8Class = Finder.at(args, 8);
    c.chatHeader.controllerParam9Class = Finder.at(args, 9);
    c.chatHeader.controllerParam10Class = Finder.at(args, 10);

    c.main.fieldChatActivity = f.fieldOf(controller, CoreRules.CHAT_ACTIVITY);
    c.main.methodSetHeaderButton =
        f.method(controller, MethodMatcher.create().usingEqStrings("99+"));
    c.main.headerInterfaceA = f.paramType(controller, c.main.methodSetHeaderButton, 1);
    c.main.fieldHeaderHelper = f.fieldReadIn(controller, c.main.methodSetHeaderButton);

    c.chatHeader.headerHelper = f.fieldType(controller, c.main.fieldHeaderHelper);
    String helper = c.chatHeader.headerHelper;
    c.main.headerButtonTypeClass = f.classUsing("INNER_RIGHT");
    c.main.methodGetHeaderButtonView =
        f.methodOf(
            helper,
            "jp.naver.line.android.common.view.header.HeaderButton",
            c.main.headerButtonTypeClass);
    c.main.methodSetHeaderButtonVisibility =
        f.method(
            helper,
            MethodMatcher.create()
                .addInvoke(
                    "Ljp/naver/line/android/common/view/header/HeaderButton;->setButtonVisibility(I)V"));
    c.main.methodSetHeaderLabel =
        f.method(
            helper,
            MethodMatcher.create()
                .addInvoke(
                    "Ljp/naver/line/android/common/view/header/HeaderButton;->setButtonContentDescription(Ljava/lang/String;)V"));
    c.main.methodSetHeaderOnClickListener =
        f.methodOf(
            helper, "void", c.main.headerButtonTypeClass, "android.view.View$OnClickListener");

    c.main.methodHeaderSetButtonListener =
        f.method(
            HEADER,
            MethodMatcher.create().name("setUpButtonOnClickListener$", StringMatchType.StartsWith));
    c.main.methodHeaderSetButtonVisibility =
        f.method(
            HEADER,
            MethodMatcher.create().name("setUpButtonVisibility$", StringMatchType.StartsWith));
    c.main.methodRefreshNavHeader =
        f.method(
            HEADER, MethodMatcher.create().addUsingField(FieldMatcher.create().name("RIGHT_ONLY")));
  }

  private static void talkTab(Finder f, LineVersion.Config c) {
    c.talkTabHeader.chatTabHeaderStateClass =
        f.classUsing(
            "createEndButtons(Ljava/util/List;Lcom/linecorp/line/chatlist/repository/model/AiFriendsChatTabEntrySetting;Ljava/util/Set;)Ljava/util/List;");
    String state = c.talkTabHeader.chatTabHeaderStateClass;
    c.talkTabHeader.iconListStateField = f.ctorField(state, 24, c.kotlin.stateFlowImplClass);
    c.talkTabHeader.buttonListStateField = f.ctorField(state, 29, c.kotlin.stateFlowImplClass);
    c.talkTabHeader.iconTypeClass =
        f.classUsing(
            "clickAIFriends(Lcom/linecorp/line/chat/tab/ui/header/ChatTabSubTabType;)Ljp/naver/line/android/analytics/tracking/request/LogEventRequest$Click;");
    c.talkTabHeader.iconTypeFieldInButton =
        f.fieldOf(f.classUsing("ChatTabHeaderIcon(type="), c.talkTabHeader.iconTypeClass);
    c.talkTabHeader.subDeviceAlbumButtonClass = f.classUsing("AllAlbumsButtonStatus(visibility=");
    c.talkTabHeader.subDeviceOpenChatButtonClass = f.classUsing("OpenChatButtonStatus(visibility=");
  }

  private static void home(Finder f, LineVersion.Config c) {
    c.home.home26LoadingMoreDataClass = f.classUsing(", isPullToRefreshLoading=");
    c.home.home26ModuleBodyField =
        f.field(
            Finder.typeArgument(
                f.fieldData(
                    c.home.home26LoadingMoreDataClass,
                    FieldMatcher.create().type("java.util.List"))),
            FieldMatcher.create()
                .type(
                    ClassMatcher.create()
                        .addMethod(
                            MethodMatcher.create()
                                .name(c.home.home26ModuleTypeMethod)
                                .returnType("java.lang.String"))));
    c.home.homePageWithModulesClass = f.classUsing("GcsPageWithModules(pageId=");
    c.home.home26ActivityCardClass = f.classUsing("HomeActivityCard(contentList=");
    c.home.home26HybridCardClass = f.classUsing("GcsHomeActivityHybridContentCard(contentList=");
    c.home.home26FixedServiceCardClass = f.classUsing("FixedService(id=");
    // GLOBALHOME has cards with the same toString
    String cardBase = f.superOf(c.home.home26FixedServiceCardClass);
    c.home.home26RecentServiceCardClass =
        f.classUsing(ClassMatcher.create().superClass(cardBase), "RecentlyUsedService(id=");
    c.home.home26CalendarCardClass =
        f.classUsing(ClassMatcher.create().superClass(cardBase), "Calendar(id=");

    c.home26NavIcon.agentDrawableId = f.id("drawable", "home26_navi_top_agent");
    c.home26NavIcon.settingsDrawableId = f.id("drawable", "navi_top_setting");
    String subTabs = Finder.packageOf(f.classUsing("UnselectedSubTabButtonScope"));
    // The renderer's class also builds the NEW badge
    c.home26NavIcon.rendererClass =
        Finder.declaring(
            f.creatorIn(
                subTabs,
                f.classUsingIdIn(subTabs, "drawable", "grand_ui_top_navigation_badge_new")));
    // (drawableId, text, onClick, modifier, ..., composer, changed, defaults)
    c.home26NavIcon.rendererMethod =
        f.staticMethodOf(
            c.home26NavIcon.rendererClass,
            "void",
            "int",
            "java.lang.String",
            c.kotlin.function0Class,
            null,
            null,
            null,
            null,
            c.compose.composerClass,
            "int",
            "int");

    c.homeTab.mainTabEnumClass = f.classUsing("commercetwtab", "chatlist");
    c.homeTab.tabListProviderClass =
        f.clsIn(
            Finder.packageOf(f.classUsing("NEW_SQUARE_JOIN_REQUEST")),
            ClassMatcher.create().addMethod(sig("int", c.homeTab.mainTabEnumClass)));
    c.homeTab.methodBuildTabList =
        f.method(
            c.homeTab.tabListProviderClass,
            MethodMatcher.create().addUsingField(FieldMatcher.create().name("LIMITED_NETWORK")));
  }

  // HomeTabConfiguration and ChatTabConfiguration each have their own isAdRcEnabled
  private static void ads(Finder f, LineVersion.Config c) {
    c.ads.homeTabConfigClass = f.classUsing(", isAdRcEnabled=", ", isMinorRegionHomeTab=");
    c.ads.homeTabAdEnabledMethod =
        f.getter(
            c.ads.homeTabConfigClass, f.propertyField(c.ads.homeTabConfigClass, "isAdRcEnabled"));
    c.ads.chatTabConfigClass = f.classUsing(", isAdRcEnabled=", ", isSmartChLogSamplingPicked=");
    c.ads.chatTabAdEnabledMethod =
        f.getter(
            c.ads.chatTabConfigClass, f.propertyField(c.ads.chatTabConfigClass, "isAdRcEnabled"));

    String keyboardRepoIface =
        Finder.outer(
            f.classUsing(
                "com.linecorp.line.shopdata.keyboard.repository.ShopKeyboardBannerRepositoryFactory"));
    c.ads.keyboardBannerRepoClass =
        f.clsIn(
            Finder.packageOf(keyboardRepoIface),
            ClassMatcher.create().addInterface(keyboardRepoIface));
    c.ads.keyboardBannerListMethod = f.methodOf(c.ads.keyboardBannerRepoClass, null, (String) null);
    c.ads.keyboardBannerListParam =
        f.paramType(c.ads.keyboardBannerRepoClass, c.ads.keyboardBannerListMethod, 0);

    String bannerVm = Finder.at(f.ctorParams(f.classUsing("line-chat-entry"), 7), 0);
    MethodData entryBannerTypes =
        f.methodDataIn(
            Finder.packageOf(Finder.at(f.ctorParams(bannerVm, 4), 0)),
            sig("java.util.Set").addUsingField(FieldMatcher.create().name("POST_NOTIFICATION")));
    c.ads.entryBannerRepoClass = Finder.declaring(entryBannerTypes);
    c.ads.entryBannerTypesMethod = Finder.name(entryBannerTypes);
  }

  private static void agentI(Finder f, LineVersion.Config c) {
    c.agentIInChat.toggleComposableClass =
        f.classUsingId(
            MethodMatcher.create().modifiers(Modifier.STATIC).returnType("void").paramCount(5),
            "string",
            "chat_agentiinchat_toggle_hideagentiinchat");

    // From 26.17.0 every reader goes through one getAiButtonShownAfterMillis function reference
    String aiButtonRef = f.classUsing("getAiButtonShownAfterMillis()J");
    if (!aiButtonRef.isEmpty()) {
      c.aiIcon.repoClass =
          f.clsIn(
              Finder.packageOf(aiButtonRef),
              ClassMatcher.create()
                  .addInterface(c.kotlin.function0Class)
                  .addMethod(
                      MethodMatcher.create()
                          .addUsingField(
                              FieldMatcher.create()
                                  .name("CHATROOM_AI_BUTTON_SHOWN_AFTER_MILLIS"))));
    } else {
      String chipsVm =
          Finder.param(
              f.methodData(
                  c.agentIInChat.toggleComposableClass,
                  sig("void", null, c.kotlin.function1Class, null, null, "int")),
              2);
      String repo = Finder.at(f.ctorParams(chipsVm, 3), 1);
      c.aiIcon.repoClass =
          f.clsIn(
              Finder.packageOf(repo),
              ClassMatcher.create()
                  .addMethod(
                      MethodMatcher.create()
                          .addUsingField(
                              FieldMatcher.create().name("CHATROOM_AI_BUTTON_SHOWN_AFTER_MILLIS"),
                              UsingType.Read))
                  .addMethod(
                      MethodMatcher.create()
                          .addUsingField(
                              FieldMatcher.create().name("CHATROOM_IS_EVENT_REMINDER_DIALOG_SHOWN"),
                              UsingType.Read)));
    }
    c.aiIcon.methodGetShownAfterMillis =
        f.method(
            c.aiIcon.repoClass,
            MethodMatcher.create()
                .paramCount(0)
                .addUsingField(
                    FieldMatcher.create().name("CHATROOM_AI_BUTTON_SHOWN_AFTER_MILLIS")));

    String shareLayout = Finder.outer(f.classUsing("SHARE_CAROUSEL_BUTTON_LAYOUT"));
    if (f.id("dimen", "chat_ui_row_ai_image_edit_side_button_min_image_height") != 0) {
      c.searchBarAgentI.chatImageAiButtonClass =
          f.fieldTypeWhere(
              shareLayout,
              ClassMatcher.create()
                  .addMethod(
                      MethodMatcher.create()
                          .name("<init>")
                          .paramTypes(Finder.leading(7, "android.widget.ImageView"))));
    }
    c.searchBarAgentI.chatImageAiButtonMethod =
        f.methodOf(c.searchBarAgentI.chatImageAiButtonClass, "void", "java.lang.Integer");

    MethodMatcher commerceHeader =
        MethodMatcher.create().addUsingField(FieldMatcher.create().name("FALSE"));
    c.searchBarAgentI.commerceHeaderClass =
        f.classUsing(
            ClassMatcher.create().addMethod(commerceHeader),
            "onEvent(Lcom/linecorp/line/commerce/impl/CommerceTabUiEvent;)V");
    c.searchBarAgentI.commerceHeaderMethod =
        f.method(c.searchBarAgentI.commerceHeaderClass, commerceHeader);

    c.searchBarAgentI.homeAiContainerId = f.id("id", "main_tab_ai_entry_icon_container");
    c.searchBarAgentI.homeGuidelineId = f.id("id", "main_tab_guideline");
    c.searchBarAgentI.homeSearchBarClass =
        f.classUsing("onChangeSearchKeyword(Ljava/lang/String;I)V");
    String searchBar = c.searchBarAgentI.homeSearchBarClass;
    c.searchBarAgentI.homeTabTypeField =
        f.ctorField(
            searchBar,
            1,
            ClassMatcher.create()
                .superClass("java.lang.Enum")
                .addField(FieldMatcher.create().name("typeStr")));
    c.searchBarAgentI.homeRootViewField = f.ctorField(searchBar, 2, "android.view.View");
    c.searchBarAgentI.homeRefreshMethod =
        f.method(
            c.searchBarAgentI.homeSearchBarClass,
            MethodMatcher.create().addUsingField(FieldMatcher.create().name("YAHOO_2")));

    c.searchBarAgentI.imageViewerAiButtonClass =
        f.classUsingIdIn(
            Finder.packageOf(f.classUsing("VideoPlaybackSyncEvent(localMessageId=")),
            "id",
            "chat_gallery_line_ai_button");
    c.searchBarAgentI.miniTabAgentMethod =
        f.method(
            c.searchBarAgentI.miniTabHeaderClass,
            MethodMatcher.create().usingEqStrings("onAgentIButtonClick()V"));

    c.searchBarAgentI.talkClickMethod =
        f.method(
            c.talkTabHeader.chatTabHeaderStateClass,
            MethodMatcher.create().addUsingField(FieldMatcher.create().name("AI_BUTTON")));
    c.searchBarAgentI.talkVisibleMethod =
        f.method(c.talkTabHeader.chatTabHeaderStateClass, sig("boolean").usingNumbers(0, 1));
  }
}
