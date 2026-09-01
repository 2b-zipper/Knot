package app.zipper.knot.versions;

import app.zipper.knot.LineVersion;

public class Version26100 {
  public static LineVersion.Config create() {
    LineVersion.Config v = new LineVersion.Config();

    v.main.mainActivity = "jp.naver.line.android.activity.main.MainActivity";
    v.main.baseMainTabFragment = "jp.naver.line.android.activity.main.BaseMainTabFragment";
    v.main.headerButton = "jp.naver.line.android.common.view.header.HeaderButton";
    v.main.headerButtonTypeClass = "x08.d";
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
    v.settings.settingsAdapterClass = "cx7.f";
    v.settings.settingsItemClass = "cx7.f$c";
    v.settings.settingsBaseAdapterClass = "cx7.f$b";
    v.settings.settingsSearchHelperClass = "pz4.b";
    v.settings.settingsAdapterWrapperClass = "uu4.a";
    v.settings.settingsHeaderItemClass = "vu4.r";
    v.settings.settingsRowItemClass = "vu4.t";
    v.settings.settingsHandlerBaseClass = "vu4.v";
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

    v.plusMenu.plusMenuComponentClass = "ey0.s";
    v.plusMenu.plusMenuComposerImplClass = "h3.c1";
    v.plusMenu.plusMenuCallbackClass = "f88.a";
    v.plusMenu.plusMenuOnClickItemClass = "f88.l";
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
    v.chatListMoreMenu.clickListenerClass = "bt1.a";
    v.chatListMoreMenu.methodAddItem = "a";

    v.readReceipt.readReceiptManagerClass = "n13.e";
    v.readReceipt.methodSendReadReceipt = "d";
    v.readReceipt.methodExecuteReadReceiptAsync = "e";
    v.readReceipt.methodReadAll = "c";
    v.readReceipt.methodResolveReadTarget = "a";
    v.readReceipt.operationNotifiedReadName = "NOTIFIED_READ_MESSAGE";
    v.readReceipt.longPressReadClass = "ou1";

    v.unsend.notifiedReadMessageHandlerClass = "p58.c2";
    v.unsend.notifiedSendReactionHandlerClass = "p58.m2";
    v.unsend.notifiedDestroyMessageHandlerClass = "p58.b1";
    v.unsend.chatMessageViewHolderClass = "sh1.f";
    v.unsend.methodReadBuffer = "b";
    v.unsend.methodBind = "g0";
    v.unsend.methodOperationTypeValueOf = "a";
    v.unsend.methodBindIndex = 1;
    v.unsend.methodGetItemView = "c0";
    v.unsend.methodGetCommonData = "b";
    v.unsend.operationTypeDummy = 40;
    v.unsend.chatServiceConfigClass = "pt4.r";
    v.unsend.methodUnsendLimit = "j";
    v.unsend.methodUnsendPremiumLimit = "i";
    v.unsend.appInfoProviderClass = "r48.c";
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
    v.unsend.unsendDestroyHandlerClass = "p58.b1";
    v.unsend.operationClass = "m78.ae";

    v.thrift.talkServiceClientImplClass =
        "jp.naver.line.android.thrift.client.impl.LegacyTalkServiceClientImpl";
    v.thrift.talkServiceClientInterface = "jp.naver.line.android.thrift.client.TalkServiceClient";
    v.thrift.v1 = "g1";
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
    v.home.lypRecommendationModuleArgClass = "a42.x";
    v.home.lypRecommendationContextClass = "k62.o";
    v.home.lypRecommendationModuleClass = "a42.x$l0";
    v.home.lypRecommendationControllerClass = "ja2.k";
    v.home.lypRecommendationSectionClass = "z52.e";

    v.home.home26FeedTypePrefixes =
        "HomeFeed,HomeContentsRecommendation,GlobalHomePage,GlobalHomeDefault,AdModel,HomePerformanceAd";
    v.home.home26ServiceTypePrefixes = "HomeServiceList,GlobalHomeServiceSection";
    v.home.home26LoadingMoreDataClass = "m62.h$a";
    v.home.home26ModuleBodyField = "e";

    v.chat.headerController = "ac1.r1";
    v.chat.headerHelper = "jp.naver.line.android.common.view.header.b";
    v.chat.chatIdField = "j";
    v.chat.methodGetChatId = "r";

    v.chatHeader.chatHistoryActivity =
        "jp.naver.line.android.activity.chathistory.ChatHistoryActivity";
    v.chatHeader.fieldChatConfigChatId = "n71.a";
    v.chatHeader.fieldChatConfigIsMuted = "l71.a";
    v.chatHeader.fieldChatConfigType = "ac1.b1";
    v.chatHeader.fieldAppInfoVersion = "cn1.n";
    v.chatHeader.fieldAppInfoPkg = "r31.a";
    v.chatHeader.fieldAppInfoId = "oo0.d";

    v.font.fontConfigClass = "e7.m";
    v.font.fontManagerClass = "e7.l";
    v.font.fontCallbackClass = "e7.m$c";
    v.font.fontInjectedClass = "zg4.k";
    v.font.methodGetFontConfig = "a";
    v.font.methodGetFontSettings = "c";
    v.font.methodOnFontChanged = "b";
    v.font.fontRequestExecutorClass = "e7.o";
    v.font.fontCallbackWithHandlerClass = "e7.c";

    v.res.idSettingList = 0x7f0b22c0;
    v.res.idPersonalInfo = 0x7f1537da;
    v.res.typeSection = 0x7f0e055e;
    v.res.typeRow = 0x7f0e0561;
    v.res.idIcon = 0x7f0b22b1;
    v.res.idDesc = 0x7f0b22a3;
    v.res.idMark = 0x7f0b22c4;
    v.res.idSeparator = 0x7f0b22ec;
    v.res.idArrow = 0x7f0b228b;
    v.res.idNewMark = 0x7f0b193d;
    v.res.idNoticeDot = 0x7f0b19a9;
    v.res.idTitle = 0x7f0b22f4;
    v.res.layoutCheckbox = 0x7f0e0552;
    v.res.layoutSectionHeader = 0x7f0e055e;
    v.res.layoutSettingsMain = 0x7f0e0558;
    v.res.idHeader = 0x7f0b1114;
    v.res.idTimestamp = 0x7f0b08a2;
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
    v.notificationFix.lineFcmServiceBaseClass = "ht.i";
    v.notificationFix.firebaseRemoteMessageClass = "ht.k0";
    v.notificationFix.firebaseReceiverClass = "com.google.firebase.iid.FirebaseInstanceIdReceiver";
    v.notificationFix.firebaseReceiverMethod = "a";
    v.notificationFix.firebaseReceiverEnvelopeClass = "hl.a";
    v.notificationFix.firebaseReceiverIntentField = "a";
    v.notificationFix.firebaseDispatcherClass = "ht.n";
    v.notificationFix.firebaseDispatcherSingletonField = "d";
    v.notificationFix.firebaseDispatcherMethod = "b";
    v.notificationFix.firebaseDispatcherContextField = "a";
    v.notificationFix.firebaseDispatcherQueueField = "d";
    v.notificationFix.firebaseBindDeliveryClass = "ht.e1";
    v.notificationFix.firebaseBindDeliveryMethod = "b";
    v.notificationFix.firebaseMessagingServiceClass =
        "com.google.firebase.messaging.FirebaseMessagingService";
    v.notificationFix.firebaseMessagingHandleMethod = "c";
    v.notificationFix.firebaseWakefulStartClass = "ht.z0";
    v.notificationFix.firebaseWakefulStartMethod = "c";
    v.notificationFix.firebaseCompletedTaskClass = "sn.n";
    v.notificationFix.firebaseCompletedTaskMethod = "e";
    v.notificationFix.firebaseMessagingClass = "com.google.firebase.messaging.FirebaseMessaging";
    v.notificationFix.firebaseMessagingGetTokenMethod = "a";
    v.notificationFix.firebaseMessagingTokenFreshMethod = "i";
    v.notificationFix.firebaseAppClass = "ur.e";
    v.notificationFix.firebaseAppGetInstanceMethod = "c";
    v.foregroundKeepAlive.serviceClass = "androidx.work.impl.foreground.SystemForegroundService";
    v.notificationFix.legyStreamingStateClass = "com.linecorp.legy.streaming.h$a";
    v.notificationFix.legyStreamingLifecycleClass = "com.linecorp.legy.streaming.h$d";
    v.notificationFix.legyStreamingLifecycleMethod = "a1";
    v.notificationFix.legyLifecycleOwnerClass = "androidx.lifecycle.u0";
    v.notificationFix.legyLifecycleEventClass = "androidx.lifecycle.e0$a";
    v.notificationFix.legyBackgroundStateField = "BACKGROUND";
    v.notificationFix.legyDisconnectRunnableClass = "y40.j";
    v.notificationFix.legyStateField = "q";
    v.notificationFix.legyTimeoutField = "s";
    v.notificationFix.legyBackgroundWorkerFlagField = "u";
    v.notificationFix.legyHandlerField = "c";
    v.notificationFix.legyRunnableField = "t";
    v.notificationFix.fisCertDigestClass = "tl.a";
    v.notificationFix.fisCertDigestMethod = "a";
    v.notificationFix.fisCertSha1 = "89396DC419292473972813922867E6973D6F5C50";
    v.notificationFix.gmsSignatureCheckClass = "il.k";
    v.notificationFix.gmsSignatureCheckMethod = "b";
    v.notificationFix.gmsAvailabilityClass = "il.j";
    v.notificationFix.gmsAvailabilityMethod = "d";

    v.talkTabHeader.chatTabHeaderStateClass = "vu1.e";
    v.talkTabHeader.iconListStateField = "y";
    v.talkTabHeader.buttonListStateField = "D";
    v.talkTabHeader.iconTypeClass = "cy0.q";
    v.talkTabHeader.iconTypeFieldInButton = "a";
    v.talkTabHeader.subDeviceOpenChatButtonClass = "bt1.c$f";
    v.talkTabHeader.subDeviceAlbumButtonClass = "bt1.c$b";

    v.searchBarAgentI.talkVisibleMethod = "x";
    v.searchBarAgentI.talkClickMethod = "u";
    v.searchBarAgentI.homeSearchBarClass = "vr4.i";
    v.searchBarAgentI.homeRefreshMethod = "e";
    v.searchBarAgentI.homeRootViewField = "c";
    v.searchBarAgentI.homeTabTypeField = "b";
    v.searchBarAgentI.homeTabName = "HOME";
    v.searchBarAgentI.homeTabV2Name = "HOME_V2";
    v.searchBarAgentI.chatTabName = "CHAT";
    v.searchBarAgentI.newsTabName = "NEWS";
    v.searchBarAgentI.homeAiContainerId = 0x7f0b1673;
    v.searchBarAgentI.homeGuidelineId = 0x7f0b1675;
    v.searchBarAgentI.homeGuidelineEndDp = 55;
    v.searchBarAgentI.homeGuidelineClass = "androidx.constraintlayout.widget.Guideline";
    v.searchBarAgentI.miniTabHeaderClass =
        "com.linecorp.line.wallet.impl.v3.view.WalletV3GrandDesignHeaderView";
    v.searchBarAgentI.miniTabAgentMethod = "o";
    v.searchBarAgentI.commerceHeaderClass = "lv1.v";
    v.searchBarAgentI.commerceHeaderMethod = "e";
    v.home26NavIcon.rendererClass = "x82.m";
    v.home26NavIcon.rendererMethod = "b";
    v.home26NavIcon.agentDrawableId = 0x7f080b6f;
    v.home26NavIcon.settingsDrawableId = 0x7f081239;

    v.compose.composerClass = "h3.s";
    v.compose.clickableClass = "u1.o0";
    v.compose.methodClickable = "a";
    v.compose.methodCombinedClickable = "d";
    v.compose.onGloballyPositionedClass = "w4.x1";
    v.compose.methodOnGloballyPositioned = "a";
    v.compose.layoutCoordinatesClass = "w4.b0";
    v.compose.methodLocalToWindow = "k";
    v.compose.methodCoordinatesSize = "a";

    v.agentIInChat.toggleComposableClass = "ef1.i";

    v.aiIcon.repoClass = "e01.c";
    v.aiIcon.methodGetShownAfterMillis = "k";

    v.imageQuality.qualityProfileHighClass = "e58.a$b$a";
    v.imageQuality.qualityProfileMediumClass = "e58.a$b$b";
    v.imageQuality.methodGetMaxDimension = "a";
    v.imageQuality.methodGetQuality = "b";
    v.imageQuality.imageUtilClass = "jp.naver.line.android.util.e1";

    v.profile.g50fClass = "q60.g";
    v.profile.h13baClass = "v93.b";
    v.profile.fieldH3 = "Ia";
    v.profile.g50aClass = "q60.a";
    v.profile.methodGetProfile = "getProfile";
    v.profile.fieldMid = "b";

    v.profileTimestamps.activityClass = "com.linecorp.line.userprofile.impl.UserProfileActivity";
    v.profileTimestamps.midExtraKey = "USER_PROFILE_MID";
    v.profileTimestamps.resHeaderButtonContainer = "user_profile_header_button_binding";

    v.media.videoDurationCheckClass = "b71.b";
    v.media.videoDurationCheckMethod = "c";
    v.media.mediaPickerParamsClass = "com.linecorp.line.media.picker.b$i";
    v.media.fieldMediaPickerMaxVideoDuration = "y";
    v.media.droppedMediaPreprocessorClass = "gv0.b";
    v.media.videoDurationSuccessClass = "c71.a$c";
    v.media.fieldVideoDurationSuccess = "a";
    v.media.galleryViewClass = "pg1.z";
    v.media.fieldGalleryDurationLimit = "U";
    v.media.selectionValidatorClass = "s33.r";
    v.media.selectionValidatorMethod = "o";
    v.media.selectionValidatorParamClass = "mw1.b";
    v.media.videoProfileTrimmerActivityClass =
        "jp.naver.line.android.activity.setting.videoprofile.trim.VideoProfileTrimmerActivity";
    v.media.fieldVideoProfileTrimmerLimit = "M";

    v.chat.searchHeaderHelperClass = "fo1.i";
    v.chat.searchHeaderControllerField = "i";
    v.chat.searchHeaderEventBusField = "b";
    v.chat.searchControllerSearchBoxMethod = "C0";
    v.chat.searchPresenterClass = "jo1.l";
    v.chat.searchKeywordTypeClass = "s11.a";
    v.chat.searchKeywordTypeMethod = "d";
    v.chat.searchResultClass = "s11.f";
    v.chat.searchResultCtorArgs = "chatId,count,keyword,idList";
    v.chat.searchResultWrapperClass = "s11.g";
    v.chat.searchBoxViewClass = "jp.naver.line.android.customview.SearchBoxView";
    v.chat.searchBoxEditTextField = "b";
    v.chat.searchKeywordEventClass = "eo1.b";
    v.chat.searchKeywordEventKeywordField = "a";
    v.chat.searchPresenterKeywordChangedMethod = "onSearchInChatKeywordChangedEventReceived";
    v.chat.searchPresenterKeywordSubjectField = "t";
    v.chat.searchKeywordSubjectValueMethod = "v";
    v.chat.searchResultWrapperResultOptionalField = "c";
    v.chat.searchResultCountField = "d";
    v.chat.searchResultTitleViewHolderClass = "mo1.h";
    v.chat.searchResultTitleBindMethod = "F0";
    v.chat.searchResultTitleBindingField = "x";
    v.chat.searchResultTitleTextViewField = "b";
    v.chat.searchFtsInChatQueryClass = "i32.n";
    v.chat.searchFtsQueryField = "a";
    v.chat.searchFtsChatIdField = "b";
    v.chat.searchFtsLimitField = "c";

    v.announcementFix.formatterClass = "xj1.c";
    v.announcementFix.formatMethod = "a";
    v.announcementFix.nameResolverMethod = "b";
    v.announcementFix.announcementEventClass = "k01.g$d0";

    v.chatJump.requestClass = "com.linecorp.line.chat.request.ChatHistoryRequest";
    v.chatJump.launchActivityClass =
        "jp.naver.line.android.activity.chathistory.ChatHistoryActivityLaunchActivity";
    v.chatJump.requestExtraKey = "chatHistoryRequest";

    v.chatTimestamp.displayTimeInterface = "r51.f";
    v.chatTimestamp.methodCreatedMillis = "a";

    v.chatEditSelectAll.selectionProviderClass = "j51.c";
    v.chatEditSelectAll.selectionStateClass = "j51.d";
    v.chatEditSelectAll.methodGetSelectionState = "d0";
    v.chatEditSelectAll.methodGetItem = "g0";
    v.chatEditSelectAll.methodGetSelectedIds = "f";
    v.chatEditSelectAll.methodToggleItem = "b";
    v.chatEditSelectAll.methodIsItemSelected = "l";

    v.messageEditHistory.editRequestClass = "tz7.h";
    v.messageEditHistory.editRequestIdField = "b";
    v.messageEditHistory.editRequestTextField = "d";
    v.messageEditHistory.menuListBuilderClass = "jd1.u1";
    v.messageEditHistory.menuListMethod = "a";
    v.messageEditHistory.menuItemEnumClass = "i41.c";
    v.messageEditHistory.menuPresentationEnumClass = "jd1.a1";
    v.messageEditHistory.methodMenuLabel = "g";
    v.messageEditHistory.methodMenuIcon = "e";
    v.messageEditHistory.methodMenuActionAccessor = "d";
    v.messageEditHistory.menuActionLambdaClass = "h41.f$b";
    v.messageEditHistory.menuContextMessageField = "b";
    v.messageEditHistory.menuMessageDataField = "b";
    v.messageEditHistory.menuMessageIdField = "c";
    v.messageEditHistory.menuEditedFlagField = "x";

    v.camera.cameraModuleClass = "l32.h";
    v.camera.methodUseExternalCamera = "c";

    v.muteMessage.labFeatureClass = "pw7.b";
    v.muteMessage.methodIsFeatureEnabled = "c";
    v.muteMessage.silentMessageFeatureClass = "pw7.j";

    v.iab.inAppBrowserActivityClass = "com.linecorp.line.iab.browser.impl.InAppBrowserActivity";

    v.homeTab.tabListProviderClass = "iv7.c";
    v.homeTab.methodBuildTabList = "a";
    v.homeTab.mainTabEnumClass = "jp.naver.line.android.activity.main.a";

    v.nightMode.nightModeConfiguratorClass = "a10.a";
    v.nightMode.methodApplyNightMode = "a";
    v.nightMode.fieldSystemDarkMode = "a";
    v.nightMode.inputPassActivityClass = "com.linecorp.line.passlock.InputPassActivity";
    v.nightMode.darkThemeManagerClass = "py5.k";
    v.nightMode.methodIsDarkTheme = "o";
    v.nightMode.methodThemeMode = "B";
    v.nightMode.methodIsDefaultTheme = "C";

    return v;
  }
}
