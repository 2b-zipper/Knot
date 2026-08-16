package app.zipper.knot.versions;

import app.zipper.knot.LineVersion;

public class Version26110 {
  public static LineVersion.Config create() {
    LineVersion.Config v = new LineVersion.Config();

    v.main.mainActivity = "jp.naver.line.android.activity.main.MainActivity";
    v.main.baseMainTabFragment = "jp.naver.line.android.activity.main.BaseMainTabFragment";
    v.main.headerButton = "jp.naver.line.android.common.view.header.HeaderButton";
    v.main.headerButtonTypeClass = "m48.d";
    v.main.slotFarLeft = "FAR_LEFT";
    v.main.headerInterfaceA = "jp.naver.line.android.common.view.header.a";
    v.main.fieldHeaderHelper = "e";
    v.main.fieldChatActivity = "a";
    v.main.methodSetHeaderButton = "i";
    v.main.methodSetHeaderLabel = "k";
    v.main.methodSetHeaderButtonVisibility = "s";
    v.main.methodGetHeaderButtonView = "h";
    v.main.methodSetHeaderOnClickListener = "r";
    v.main.methodRefreshNavHeader = "a";
    v.main.methodHeaderSetTitle = "setTitle";
    v.main.methodHeaderSetButtonVisibility = "setUpButtonVisibility$common_libs";
    v.main.methodHeaderSetButtonListener = "setUpButtonOnClickListener$common_libs";

    v.settings.mainSettingsFragmentClass =
        "com.linecorp.line.settings.main.LineUserMainSettingsFragment";
    v.settings.settingsAdapterClass = "r08.f";
    v.settings.settingsItemClass = "r08.f$c";
    v.settings.settingsBaseAdapterClass = "r08.f$b";
    v.settings.settingsSearchHelperClass = "j25.b";
    v.settings.settingsAdapterWrapperClass = "ox4.a";
    v.settings.settingsHeaderItemClass = "px4.s";
    v.settings.settingsRowItemClass = "px4.v";
    v.settings.settingsHandlerBaseClass = "px4.b0";
    v.settings.methodSetItems = "n";
    v.settings.methodBindViewHolder = "r";
    v.settings.methodGetItem = "q";
    v.settings.fieldItemModel = "a";
    v.settings.fieldModelTag = "a";
    v.settings.fieldViewHolderView = "a";
    v.settings.fieldIsVisible = "k";
    v.settings.fieldLayoutId = "b";
    v.settings.fieldActionHandler = "d";
    v.settings.fieldIconProvider = "f";
    v.settings.fieldDescriptionProvider = "g";
    v.settings.fieldSubActionHandler = "h";
    v.settings.fieldVisibilityFilter = "j";
    v.settings.fieldDefaultHandler = "p";
    v.settings.fieldCommonHandler = "m";
    v.settings.methodSetDescription = "b";
    v.settings.methodProxyGetItemType = "f";
    v.settings.methodSetTitleText = "setTitleText";
    v.settings.methodSetChecked = "setChecked";
    v.settings.methodSetItemType = "setItemType";
    v.settings.methodSetSyncStatus = "setSyncStatus";
    v.settings.methodSetDividerVisible = "setDividerVisible";

    v.plusMenu.plusMenuComponentClass = "cz0.s";
    v.plusMenu.plusMenuComposerImplClass = "h3.f1";
    v.plusMenu.plusMenuCallbackClass = "vb8.a";
    v.plusMenu.plusMenuOnClickItemClass = "vb8.l";
    v.plusMenu.methodAddMenuItem = "a";
    v.plusMenu.methodCreateMenu = "c";
    v.plusMenu.methodExecuteAction = "Y";
    v.plusMenu.editChatDrawable = "chat_tab_ui_header_plusmenu_edit_chat";

    v.chatListMoreMenu.popupListViewClass =
        "jp.naver.line.android.common.view.listview.PopupListView";
    v.chatListMoreMenu.fieldListView = "a";
    v.chatListMoreMenu.popupListAdapterClass =
        "jp.naver.line.android.common.view.listview.PopupListView$b";
    v.chatListMoreMenu.fieldPopupItems = "a";
    v.chatListMoreMenu.clickListenerClass = "mu1.a";
    v.chatListMoreMenu.methodAddItem = "a";

    v.readReceipt.readReceiptManagerClass = "q33.e";
    v.readReceipt.methodSendReadReceipt = "d";
    v.readReceipt.methodExecuteReadReceiptAsync = "e";
    v.readReceipt.methodReadAll = "c";
    v.readReceipt.methodResolveReadTarget = "a";
    v.readReceipt.operationNotifiedReadName = "NOTIFIED_READ_MESSAGE";
    v.readReceipt.longPressReadClass = "zv1";

    v.unsend.notifiedReadMessageHandlerClass = "e98.e2";
    v.unsend.notifiedSendReactionHandlerClass = "e98.p2";
    v.unsend.notifiedDestroyMessageHandlerClass = "e98.c1";
    v.unsend.chatMessageViewHolderClass = "wi1.h";
    v.unsend.methodReadBuffer = "b";
    v.unsend.methodBind = "B";
    v.unsend.methodOperationTypeValueOf = "a";
    v.unsend.methodBindIndex = 1;
    v.unsend.methodGetItemView = "d0";
    v.unsend.methodGetCommonData = "b";
    v.unsend.operationTypeDummy = 40;
    v.unsend.chatServiceConfigClass = "jw4.r";
    v.unsend.methodUnsendLimit = "j";
    v.unsend.methodUnsendPremiumLimit = "i";
    v.unsend.appInfoProviderClass = "g88.d";
    v.unsend.methodGetFullUserAgent = "h";
    v.unsend.methodGetSimpleUserAgent = "k";
    v.unsend.methodGetFullUserAgentWithContext = "i";
    v.unsend.methodGetSimpleUserAgentWithContext = "l";
    v.unsend.methodUnsendThrift = "unsendMessage";
    v.unsend.methodUnsendThriftSilent = "silentlyUnsendMessage";
    v.unsend.methodUnsendAnnouncement = "unsendChatRoomAnnouncement";
    v.unsend.operationTypeField = "c";
    v.unsend.operationParam1Field = "g";
    v.unsend.operationParam2Field = "h";
    v.unsend.operationParam3Field = "i";
    v.unsend.operationCreatedTimeField = "b";
    v.unsend.chatMessageIdField = "d";
    v.unsend.operationUnsendName = "DESTROY_MESSAGE";
    v.unsend.operationNotifiedUnsendName = "NOTIFIED_DESTROY_MESSAGE";
    v.unsend.unsendDestroyHandlerClass = "e98.c1";
    v.unsend.operationClass = "cb8.de";

    v.thrift.talkServiceClientImplClass =
        "jp.naver.line.android.thrift.client.impl.LegacyTalkServiceClientImpl";
    v.thrift.talkServiceClientInterface = "jp.naver.line.android.thrift.client.TalkServiceClient";
    v.thrift.v1 = "j1";
    v.thrift.protocolClass = "org.apache.thrift.o";
    v.thrift.messageClass = "org.apache.thrift.e";
    v.thrift.methodWriteMessageBegin = "b";
    v.thrift.methodReadMessageBegin = "a";
    v.thrift.methodDestroyMessage = "destroyMessage";
    v.thrift.methodDestroyMessages = "destroyMessages";

    v.tabs.bottomNavigationBarTextViewClass =
        "jp.naver.line.android.activity.main.bottomnavigationbar.BottomNavigationBarTextView";

    v.ads.classAdSdkBase = "com.linecorp.line.ladsdk";
    v.ads.classAdMolinBase = "com.linecorp.line.admolin";
    v.ads.ladAdView = v.ads.classAdSdkBase + ".ui.common.view.lifecycle.LadAdView";
    v.ads.ladAdViewV2 = v.ads.classAdSdkBase + ".ui.v2.common.lifecycle.LyadAdView";
    v.ads.smartChannel = v.ads.classAdMolinBase + ".smartch.v2.view.SmartChannelViewLayout";

    v.home.resRecommendation = "home_tab_contents_recommendation_placement";
    v.home.resServiceCarouselId = "home_tab_service_carousel";
    v.home.resServiceTitleId = "home_tab_service_title";
    v.home.resNoServicesId = "home_tab_no_services_title";
    v.home.lypRecommendationModuleArgClass = "m52.a0";
    v.home.lypRecommendationContextClass = "v72.l";
    v.home.lypRecommendationModuleClass = "m52.a0$n0";
    v.home.lypRecommendationControllerClass = "ac2.k";
    v.home.lypRecommendationSectionClass = "l72.e";

    v.home.home26FeedTypePrefixes =
        "HomeFeed,HomeContentsRecommendation,GlobalHomePage,GlobalHomeDefault,AdModel,HomePerformanceAd,FLEX";
    v.home.home26ServiceTypePrefixes = "HomeServiceList,GlobalHomeServiceSection";
    v.home.home26LoadingMoreDataClass = "x72.h$a";
    v.home.home26ModuleBodyField = "e";

    v.chat.headerController = "ed1.s1";
    v.chat.headerHelper = "jp.naver.line.android.common.view.header.b";
    v.chat.chatIdField = "j";
    v.chat.methodGetChatId = "u";

    v.chatHeader.chatHistoryActivity =
        "jp.naver.line.android.activity.chathistory.ChatHistoryActivity";
    v.chatHeader.fieldChatConfigChatId = "o81.a";
    v.chatHeader.fieldChatConfigIsMuted = "m81.a";
    v.chatHeader.fieldChatConfigType = "ed1.c1";
    v.chatHeader.fieldAppInfoVersion = "jo1.n";
    v.chatHeader.fieldAppInfoPkg = "s41.a";
    v.chatHeader.fieldAppInfoId = "jp0.d";

    v.font.fontConfigClass = "e7.m";
    v.font.fontManagerClass = "e7.l";
    v.font.fontCallbackClass = "e7.m$c";
    v.font.fontInjectedClass = "fj4.p";
    v.font.methodGetFontConfig = "a";
    v.font.methodGetFontSettings = "c";
    v.font.methodOnFontChanged = "b";
    v.font.fontRequestExecutorClass = "e7.o";
    v.font.fontCallbackWithHandlerClass = "e7.c";

    v.res.idSettingList = 0x7f0b22df;
    v.res.idPersonalInfo = 0x7f1537ed;
    v.res.typeSection = 0x7f0e055e;
    v.res.typeRow = 0x7f0e0561;
    v.res.idIcon = 0x7f0b22d0;
    v.res.idDesc = 0x7f0b22c2;
    v.res.idMark = 0x7f0b22e3;
    v.res.idSeparator = 0x7f0b230b;
    v.res.idArrow = 0x7f0b22aa;
    v.res.idNewMark = 0x7f0b1957;
    v.res.idNoticeDot = 0x7f0b19c4;
    v.res.idTitle = 0x7f0b2313;
    v.res.layoutCheckbox = 0x7f0e0552;
    v.res.layoutSectionHeader = 0x7f0e055e;
    v.res.layoutSettingsMain = 0x7f0e0558;
    v.res.idHeader = 0x7f0b1131;
    v.res.idStatusBarGuide = 0x7f0b2579;
    v.res.idTimestamp = 0x7f0b08a6;
    v.res.idChatMessageText = 0x7f0b0799;
    v.res.resSettingsHeaderBtn = "settings_header_button";
    v.res.resSettingsBtn = "settings_button";
    v.res.resTooltipBackground = "home_tooltip_background";
    v.res.resTooltipArrowUp = "home_tooltip_arrow_up";

    v.notification.chatHistoryRequestClass = "com.linecorp.line.chat.request.ChatHistoryRequest";
    v.notification.chatHistoryActivityLaunchActivityClass =
        "jp.naver.line.android.activity.chathistory.ChatHistoryActivityLaunchActivity";

    v.notificationFix.lineFcmServiceClass =
        "jp.naver.line.android.service.fcm.LineFirebaseMessagingService";
    v.notificationFix.lineFcmDispatchMethod = "d";
    v.notificationFix.lineFcmOwnershipMethod = "f";
    v.notificationFix.lineFcmTokenMethod = "e";
    v.notificationFix.lineFcmServiceBaseClass = "ft.i";
    v.notificationFix.firebaseRemoteMessageClass = "ft.j0";
    v.notificationFix.firebaseReceiverClass = "com.google.firebase.iid.FirebaseInstanceIdReceiver";
    v.notificationFix.firebaseReceiverMethod = "a";
    v.notificationFix.firebaseReceiverEnvelopeClass = "fl.a";
    v.notificationFix.firebaseReceiverIntentField = "a";
    v.notificationFix.firebaseDispatcherClass = "ft.n";
    v.notificationFix.firebaseDispatcherSingletonField = "d";
    v.notificationFix.firebaseDispatcherMethod = "b";
    v.notificationFix.firebaseDispatcherContextField = "a";
    v.notificationFix.firebaseDispatcherQueueField = "d";
    v.notificationFix.firebaseBindDeliveryClass = "ft.d1";
    v.notificationFix.firebaseBindDeliveryMethod = "b";
    v.notificationFix.firebaseMessagingServiceClass =
        "com.google.firebase.messaging.FirebaseMessagingService";
    v.notificationFix.firebaseMessagingHandleMethod = "c";
    v.notificationFix.firebaseWakefulStartClass = "ft.y0";
    v.notificationFix.firebaseWakefulStartMethod = "c";
    v.notificationFix.firebaseCompletedTaskClass = "qn.n";
    v.notificationFix.firebaseCompletedTaskMethod = "e";
    v.foregroundKeepAlive.serviceClass = "androidx.work.impl.foreground.SystemForegroundService";
    v.notificationFix.legyStreamingStateClass = "com.linecorp.legy.streaming.h$a";
    v.notificationFix.legyStreamingLifecycleClass = "com.linecorp.legy.streaming.h$d";
    v.notificationFix.legyStreamingLifecycleMethod = "e1";
    v.notificationFix.legyLifecycleOwnerClass = "androidx.lifecycle.u0";
    v.notificationFix.legyLifecycleEventClass = "androidx.lifecycle.e0$a";
    v.notificationFix.legyBackgroundStateField = "BACKGROUND";
    v.notificationFix.legyDisconnectRunnableClass = "y40.j";
    v.notificationFix.legyStateField = "q";
    v.notificationFix.legyTimeoutField = "s";
    v.notificationFix.legyBackgroundWorkerFlagField = "u";
    v.notificationFix.legyHandlerField = "c";
    v.notificationFix.legyRunnableField = "t";
    v.notificationFix.fisCertDigestClass = "rl.a";
    v.notificationFix.fisCertDigestMethod = "a";
    v.notificationFix.fisCertSha1 = "89396DC419292473972813922867E6973D6F5C50";
    v.notificationFix.gmsSignatureCheckClass = "gl.k";
    v.notificationFix.gmsSignatureCheckMethod = "b";
    v.notificationFix.gmsAvailabilityClass = "gl.j";
    v.notificationFix.gmsAvailabilityMethod = "d";

    v.talkTabHeader.chatTabHeaderStateClass = "gw1.f";
    v.talkTabHeader.iconListStateField = "y";
    v.talkTabHeader.buttonListStateField = "D";
    v.talkTabHeader.iconTypeClass = "az0.q";
    v.talkTabHeader.iconTypeFieldInButton = "a";
    v.talkTabHeader.subDeviceOpenChatButtonClass = "mu1.c$f";
    v.talkTabHeader.subDeviceAlbumButtonClass = "mu1.c$b";

    v.searchBarAgentI.talkVisibleMethod = "x";
    v.searchBarAgentI.talkClickMethod = "u";
    v.searchBarAgentI.homeSearchBarClass = "pu4.i";
    v.searchBarAgentI.homeRefreshMethod = "e";
    v.searchBarAgentI.homeRootViewField = "c";
    v.searchBarAgentI.homeTabTypeField = "b";
    v.searchBarAgentI.homeTabName = "HOME";
    v.searchBarAgentI.homeTabV2Name = "HOME_V2";
    v.searchBarAgentI.chatTabName = "CHAT";
    v.searchBarAgentI.newsTabName = "NEWS";
    v.searchBarAgentI.homeAiContainerId = 0x7f0b1688;
    v.searchBarAgentI.homeGuidelineId = 0x7f0b168a;
    v.searchBarAgentI.homeGuidelineEndDp = 55;
    v.searchBarAgentI.homeGuidelineClass = "androidx.constraintlayout.widget.Guideline";
    v.searchBarAgentI.miniTabHeaderClass =
        "com.linecorp.line.wallet.impl.v3.view.WalletV3GrandDesignHeaderView";
    v.searchBarAgentI.miniTabAgentMethod = "o";
    v.home26NavIcon.rendererClass = "oa2.m";
    v.home26NavIcon.rendererMethod = "b";
    v.home26NavIcon.agentDrawableId = 0x7f080b83;
    v.home26NavIcon.settingsDrawableId = 0x7f08124a;

    v.compose.composerClass = "h3.t";
    v.compose.clickableClass = "u1.l0";
    v.compose.methodClickable = "a";
    v.compose.methodCombinedClickable = "d";
    v.compose.onGloballyPositionedClass = "w4.y1";
    v.compose.methodOnGloballyPositioned = "a";
    v.compose.layoutCoordinatesClass = "w4.b0";
    v.compose.methodLocalToWindow = "k";
    v.compose.methodCoordinatesSize = "a";

    v.agentIInChat.toggleComposableClass = "ig1.i";

    v.aiIcon.repoClass = "f11.c";
    v.aiIcon.methodGetShownAfterMillis = "q";

    v.imageQuality.qualityProfileHighClass = "t88.a$b$a";
    v.imageQuality.qualityProfileMediumClass = "t88.a$b$b";
    v.imageQuality.methodGetMaxDimension = "a";
    v.imageQuality.methodGetQuality = "b";
    v.imageQuality.imageUtilClass = "jp.naver.line.android.util.f1";

    v.profile.g50fClass = "y60.g";
    v.profile.h13baClass = "ac3.b";
    v.profile.fieldH3 = "U";
    v.profile.g50aClass = "y60.a";
    v.profile.methodGetProfile = "getProfile";
    v.profile.fieldMid = "b";

    v.profileTimestamps.activityClass = "com.linecorp.line.userprofile.impl.UserProfileActivity";
    v.profileTimestamps.midExtraKey = "USER_PROFILE_MID";
    v.profileTimestamps.resHeaderButtonContainer = "user_profile_header_button_binding";

    v.media.videoDurationCheckClass = "c81.b";
    v.media.videoDurationCheckMethod = "c";
    v.media.mediaPickerParamsClass = "com.linecorp.line.media.picker.b$i";
    v.media.fieldMediaPickerMaxVideoDuration = "y";
    v.media.droppedMediaPreprocessorClass = "xv0.b";
    v.media.videoDurationSuccessClass = "d81.a$c";
    v.media.fieldVideoDurationSuccess = "a";
    v.media.galleryViewClass = "th1.t";
    v.media.fieldGalleryDurationLimit = "U";
    v.media.selectionValidatorClass = "v53.r";
    v.media.selectionValidatorMethod = "o";
    v.media.selectionValidatorParamClass = "yx1.c";
    v.media.videoProfileTrimmerActivityClass =
        "jp.naver.line.android.activity.setting.videoprofile.trim.VideoProfileTrimmerActivity";
    v.media.fieldVideoProfileTrimmerLimit = "M";

    v.chat.searchHeaderHelperClass = "np1.h";
    v.chat.searchHeaderControllerField = "i";
    v.chat.searchHeaderEventBusField = "b";
    v.chat.searchControllerSearchBoxMethod = "d";
    v.chat.searchPresenterClass = "rp1.m";
    v.chat.searchKeywordTypeClass = "t21.a";
    v.chat.searchKeywordTypeMethod = "e";
    v.chat.searchResultClass = "t21.f";
    v.chat.searchResultWrapperClass = "t21.g";
    v.chat.searchBoxViewClass = "jp.naver.line.android.customview.SearchBoxView";
    v.chat.searchBoxEditTextField = "b";
    v.chat.searchKeywordEventClass = "mp1.b";
    v.chat.searchKeywordEventKeywordField = "a";
    v.chat.searchPresenterKeywordChangedMethod = "onSearchInChatKeywordChangedEventReceived";
    v.chat.searchPresenterKeywordSubjectField = "t";
    v.chat.searchResultWrapperResultOptionalField = "c";
    v.chat.searchResultCountField = "d";
    v.chat.searchResultTitleViewHolderClass = "up1.k";
    v.chat.searchResultTitleBindMethod = "G0";
    v.chat.searchResultTitleBindingField = "x";
    v.chat.searchResultTitleTextViewField = "b";
    v.chat.searchFtsInChatQueryClass = "u42.k";
    v.chat.searchFtsQueryField = "a";
    v.chat.searchFtsChatIdField = "b";
    v.chat.searchFtsLimitField = "c";

    v.announcementFix.formatterClass = "cl1.c";
    v.announcementFix.formatMethod = "a";
    v.announcementFix.nameResolverMethod = "b";
    v.announcementFix.announcementEventClass = "l11.h$d0";

    v.chatJump.requestClass = "com.linecorp.line.chat.request.ChatHistoryRequest";
    v.chatJump.launchActivityClass =
        "jp.naver.line.android.activity.chathistory.ChatHistoryActivityLaunchActivity";
    v.chatJump.requestExtraKey = "chatHistoryRequest";

    v.chatTimestamp.displayTimeInterface = "s61.f";
    v.chatTimestamp.methodCreatedMillis = "a";

    v.chatEditSelectAll.selectionProviderClass = "k61.c";
    v.chatEditSelectAll.selectionStateClass = "k61.d";
    v.chatEditSelectAll.methodGetSelectionState = "d0";
    v.chatEditSelectAll.methodGetItem = "g0";
    v.chatEditSelectAll.methodGetSelectedIds = "d";
    v.chatEditSelectAll.methodToggleItem = "h";
    v.chatEditSelectAll.methodIsItemSelected = "k";


    v.messageEditHistory.editRequestClass = "i38.h";
    v.messageEditHistory.editRequestIdField = "b";
    v.messageEditHistory.editRequestTextField = "d";
    v.messageEditHistory.menuListBuilderClass = "ne1.z1";
    v.messageEditHistory.menuListMethod = "a";
    v.messageEditHistory.menuItemEnumClass = "j51.c";
    v.messageEditHistory.menuPresentationEnumClass = "ne1.x0";
    v.messageEditHistory.methodMenuLabel = "h";
    v.messageEditHistory.methodMenuIcon = "f";
    v.messageEditHistory.methodMenuActionAccessor = "e";
    v.messageEditHistory.menuActionLambdaClass = "i51.f$b";
    v.messageEditHistory.menuContextMessageField = "b";
    v.messageEditHistory.menuMessageDataField = "b";
    v.messageEditHistory.menuMessageIdField = "c";
    v.messageEditHistory.menuEditedFlagField = "x";

    v.camera.cameraModuleClass = "x42.g";
    v.camera.methodUseExternalCamera = "d";

    v.iab.inAppBrowserActivityClass = "com.linecorp.line.iab.browser.impl.InAppBrowserActivity";

    v.homeTab.tabListProviderClass = "wy7.b";
    v.homeTab.methodBuildTabList = "a";
    v.homeTab.mainTabEnumClass = "jp.naver.line.android.activity.main.a";

    v.nightMode.nightModeConfiguratorClass = "z00.a";
    v.nightMode.methodApplyNightMode = "a";
    v.nightMode.fieldSystemDarkMode = "a";
    v.nightMode.darkThemeManagerClass = "o16.j";
    v.nightMode.methodIsDarkTheme = "n";
    v.nightMode.methodThemeMode = "z";

    return v;
  }
}
