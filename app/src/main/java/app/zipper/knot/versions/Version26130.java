package app.zipper.knot.versions;

import app.zipper.knot.LineVersion;

public class Version26130 {
  public static LineVersion.Config create() {
    LineVersion.Config v = new LineVersion.Config();

    v.main.mainActivity = "jp.naver.line.android.activity.main.MainActivity";
    v.main.baseMainTabFragment = "jp.naver.line.android.activity.main.BaseMainTabFragment";
    v.main.headerButton = "jp.naver.line.android.common.view.header.HeaderButton";
    v.main.headerButtonTypeClass = "ba8.d";
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
    v.settings.settingsAdapterClass = "g68.f";
    v.settings.settingsItemClass = "g68.f$c";
    v.settings.settingsBaseAdapterClass = "g68.f$b";
    v.settings.settingsSearchHelperClass = "i85.b";
    v.settings.settingsAdapterWrapperClass = "j35.a";
    v.settings.settingsHeaderItemClass = "k35.s";
    v.settings.settingsRowItemClass = "k35.v";
    v.settings.settingsHandlerBaseClass = "k35.a0";
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
    v.settings.methodProxyGetItemType = "h";
    v.settings.methodSetTitleText = "setTitleText";
    v.settings.methodSetChecked = "setChecked";
    v.settings.methodSetItemType = "setItemType";
    v.settings.methodSetSyncStatus = "setSyncStatus";
    v.settings.methodSetDividerVisible = "setDividerVisible";

    v.plusMenu.plusMenuComponentClass = "y01.l";
    v.plusMenu.plusMenuComposerImplClass = "h3.d1";
    v.plusMenu.plusMenuCallbackClass = "kh8.a";
    v.plusMenu.plusMenuOnClickItemClass = "kh8.l";
    v.plusMenu.methodAddMenuItem = "a";
    v.plusMenu.methodCreateMenu = "c";
    v.plusMenu.methodExecuteAction = "Z";
    v.plusMenu.editChatDrawable = "chat_tab_ui_header_plusmenu_edit_chat";

    v.chatListMoreMenu.popupListViewClass =
        "jp.naver.line.android.common.view.listview.PopupListView";
    v.chatListMoreMenu.fieldListView = "a";
    v.chatListMoreMenu.popupListAdapterClass =
        "jp.naver.line.android.common.view.listview.PopupListView$b";
    v.chatListMoreMenu.fieldPopupItems = "a";
    v.chatListMoreMenu.clickListenerClass = "qw1.a";
    v.chatListMoreMenu.methodAddItem = "a";

    v.readReceipt.readReceiptManagerClass = "u83.e";
    v.readReceipt.methodSendReadReceipt = "d";
    v.readReceipt.methodExecuteReadReceiptAsync = "e";
    v.readReceipt.methodReadAll = "c";
    v.readReceipt.methodResolveReadTarget = "a";
    v.readReceipt.operationNotifiedReadName = "NOTIFIED_READ_MESSAGE";
    v.readReceipt.longPressReadClass = "dy1";

    v.unsend.notifiedReadMessageHandlerClass = "te8.y1";
    v.unsend.notifiedSendReactionHandlerClass = "te8.i2";
    v.unsend.notifiedDestroyMessageHandlerClass = "te8.b1";
    v.unsend.chatMessageViewHolderClass = "sk1.f";
    v.unsend.methodReadBuffer = "b";
    v.unsend.methodBind = "K";
    v.unsend.methodOperationTypeValueOf = "a";
    v.unsend.methodBindIndex = 1;
    v.unsend.methodGetItemView = "d0";
    v.unsend.methodGetCommonData = "b";
    v.unsend.operationTypeDummy = 40;
    v.unsend.chatServiceConfigClass = "e25.r";
    v.unsend.methodUnsendLimit = "j";
    v.unsend.methodUnsendPremiumLimit = "i";
    v.unsend.appInfoProviderClass = "vd8.d";
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
    v.unsend.unsendDestroyHandlerClass = "te8.b1";
    v.unsend.operationClass = "rg8.de";

    v.thrift.talkServiceClientImplClass =
        "jp.naver.line.android.thrift.client.impl.LegacyTalkServiceClientImpl";
    v.thrift.talkServiceClientInterface = "jp.naver.line.android.thrift.client.TalkServiceClient";
    v.thrift.v1 = "Y0";
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
    v.home.lypRecommendationModuleArgClass = "s72.k0";
    v.home.lypRecommendationContextClass = "da2.r";
    v.home.lypRecommendationModuleClass = "s72.k0$q0";
    v.home.lypRecommendationControllerClass = "wg2.j";
    v.home.lypRecommendationSectionClass = "t92.g";

    v.home.home26FeedTypePrefixes =
        "HomeFeed,HomeContentsRecommendation,GlobalHomePage,GlobalHomeDefault,AdModel,HomePerformanceAd";
    v.home.home26ServiceTypePrefixes = "HomeServiceList,GlobalHomeServiceSection";
    v.home.home26LoadingMoreDataClass = "fa2.h$a";
    v.home.home26ModuleBodyField = "e";

    v.chat.headerController = "gf1.k1";
    v.chat.headerHelper = "jp.naver.line.android.common.view.header.b";
    v.chat.chatIdField = "j";
    v.chat.methodGetChatId = "t";

    v.chatHeader.chatHistoryActivity =
        "jp.naver.line.android.activity.chathistory.ChatHistoryActivity";
    v.chatHeader.fieldChatConfigChatId = "oa1.a";
    v.chatHeader.fieldChatConfigIsMuted = "ma1.a";
    v.chatHeader.fieldChatConfigType = "gf1.a1";
    v.chatHeader.fieldAppInfoVersion = "iq1.n";
    v.chatHeader.fieldAppInfoPkg = "q61.a";
    v.chatHeader.fieldAppInfoId = "yq0.d";

    v.font.fontConfigClass = "f7.m";
    v.font.fontManagerClass = "f7.l";
    v.font.fontCallbackClass = "f7.m$c";
    v.font.fontInjectedClass = "qo4.m";
    v.font.methodGetFontConfig = "a";
    v.font.methodGetFontSettings = "c";
    v.font.methodOnFontChanged = "b";
    v.font.fontRequestExecutorClass = "f7.o";
    v.font.fontCallbackWithHandlerClass = "f7.c";

    v.res.idSettingList = 0x7f0b229e;
    v.res.idPersonalInfo = 0x7f153941;
    v.res.typeSection = 0x7f0e0545;
    v.res.typeRow = 0x7f0e0548;
    v.res.idIcon = 0x7f0b2290;
    v.res.idDesc = 0x7f0b2282;
    v.res.idMark = 0x7f0b22a2;
    v.res.idSeparator = 0x7f0b22cb;
    v.res.idArrow = 0x7f0b226a;
    v.res.idNewMark = 0x7f0b1905;
    v.res.idNoticeDot = 0x7f0b1972;
    v.res.idTitle = 0x7f0b22d3;
    v.res.layoutCheckbox = 0x7f0e0539;
    v.res.layoutSectionHeader = 0x7f0e0545;
    v.res.layoutSettingsMain = 0x7f0e053f;
    v.res.idHeader = 0x7f0b1115;
    v.res.idTimestamp = 0x7f0b088f;
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
    v.notificationFix.lineFcmOwnershipMethod = "g";
    v.notificationFix.lineFcmTokenMethod = "e";
    v.notificationFix.lineFcmServiceBaseClass = "bu.i";
    v.notificationFix.firebaseRemoteMessageClass = "bu.r0";
    v.notificationFix.firebaseReceiverClass = "com.google.firebase.iid.FirebaseInstanceIdReceiver";
    v.notificationFix.firebaseReceiverMethod = "a";
    v.notificationFix.firebaseReceiverEnvelopeClass = "xl.a";
    v.notificationFix.firebaseReceiverIntentField = "a";
    v.notificationFix.firebaseDispatcherClass = "bu.n";
    v.notificationFix.firebaseDispatcherSingletonField = "d";
    v.notificationFix.firebaseDispatcherMethod = "b";
    v.notificationFix.firebaseDispatcherContextField = "a";
    v.notificationFix.firebaseDispatcherQueueField = "d";
    v.notificationFix.firebaseBindDeliveryClass = "bu.m1";
    v.notificationFix.firebaseBindDeliveryMethod = "b";
    v.notificationFix.firebaseMessagingServiceClass =
        "com.google.firebase.messaging.FirebaseMessagingService";
    v.notificationFix.firebaseMessagingHandleMethod = "c";
    v.notificationFix.firebaseWakefulStartClass = "bu.h1";
    v.notificationFix.firebaseWakefulStartMethod = "c";
    v.notificationFix.firebaseCompletedTaskClass = "ko.n";
    v.notificationFix.firebaseCompletedTaskMethod = "e";
    v.notificationFix.firebaseMessagingClass = "com.google.firebase.messaging.FirebaseMessaging";
    v.notificationFix.firebaseMessagingGetTokenMethod = "a";
    v.notificationFix.firebaseMessagingTokenFreshMethod = "i";
    v.notificationFix.firebaseAppClass = "ns.e";
    v.notificationFix.firebaseAppGetInstanceMethod = "c";
    v.foregroundKeepAlive.serviceClass = "androidx.work.impl.foreground.SystemForegroundService";
    v.notificationFix.legyStreamingStateClass = "com.linecorp.legy.streaming.h$a";
    v.notificationFix.legyStreamingLifecycleClass = "com.linecorp.legy.streaming.h$d";
    v.notificationFix.legyStreamingLifecycleMethod = "e1";
    v.notificationFix.legyLifecycleOwnerClass = "androidx.lifecycle.u0";
    v.notificationFix.legyLifecycleEventClass = "androidx.lifecycle.f0$a";
    v.notificationFix.legyBackgroundStateField = "BACKGROUND";
    v.notificationFix.legyDisconnectRunnableClass = "s50.k";
    v.notificationFix.legyStateField = "q";
    v.notificationFix.legyTimeoutField = "s";
    v.notificationFix.legyBackgroundWorkerFlagField = "u";
    v.notificationFix.legyHandlerField = "c";
    v.notificationFix.legyRunnableField = "t";
    v.notificationFix.fisCertDigestClass = "jm.a";
    v.notificationFix.fisCertDigestMethod = "a";
    v.notificationFix.fisCertSha1 = "89396DC419292473972813922867E6973D6F5C50";
    v.notificationFix.gmsSignatureCheckClass = "yl.k";
    v.notificationFix.gmsSignatureCheckMethod = "b";
    v.notificationFix.gmsAvailabilityClass = "yl.j";
    v.notificationFix.gmsAvailabilityMethod = "d";

    v.talkTabHeader.chatTabHeaderStateClass = "ky1.d";
    v.talkTabHeader.iconListStateField = "y";
    v.talkTabHeader.buttonListStateField = "D";
    v.talkTabHeader.iconTypeClass = "w01.p";
    v.talkTabHeader.iconTypeFieldInButton = "a";
    v.talkTabHeader.subDeviceOpenChatButtonClass = "qw1.c$f";
    v.talkTabHeader.subDeviceAlbumButtonClass = "qw1.c$b";

    v.searchBarAgentI.talkVisibleMethod = "x";
    v.searchBarAgentI.talkClickMethod = "t";
    v.searchBarAgentI.homeSearchBarClass = "k05.j";
    v.searchBarAgentI.homeRefreshMethod = "e";
    v.searchBarAgentI.homeRootViewField = "c";
    v.searchBarAgentI.homeTabTypeField = "b";
    v.searchBarAgentI.homeTabName = "HOME";
    v.searchBarAgentI.homeTabV2Name = "HOME_V2";
    v.searchBarAgentI.chatTabName = "CHAT";
    v.searchBarAgentI.newsTabName = "NEWS";
    v.searchBarAgentI.homeAiContainerId = 0x7f0b163e;
    v.searchBarAgentI.homeGuidelineId = 0x7f0b1640;
    v.searchBarAgentI.homeGuidelineEndDp = 55;
    v.searchBarAgentI.homeGuidelineClass = "androidx.constraintlayout.widget.Guideline";
    v.searchBarAgentI.miniTabHeaderClass =
        "com.linecorp.line.wallet.impl.v3.view.WalletV3GrandDesignHeaderView";
    v.searchBarAgentI.miniTabAgentMethod = "o";
    v.searchBarAgentI.commerceHeaderClass = "bz1.y";
    v.searchBarAgentI.commerceHeaderMethod = "e";
    v.home26NavIcon.rendererClass = "ve2.l";
    v.home26NavIcon.rendererMethod = "b";
    v.home26NavIcon.agentDrawableId = 0x7f080b88;
    v.home26NavIcon.settingsDrawableId = 0x7f081240;

    v.compose.composerClass = "h3.r";
    v.compose.clickableClass = "u1.k0";
    v.compose.methodClickable = "a";
    v.compose.methodCombinedClickable = "d";
    v.compose.onGloballyPositionedClass = "x4.y1";
    v.compose.methodOnGloballyPositioned = "a";
    v.compose.layoutCoordinatesClass = "x4.b0";
    v.compose.methodLocalToWindow = "m";
    v.compose.methodCoordinatesSize = "a";

    v.agentIInChat.toggleComposableClass = "ei1.j";

    v.aiIcon.repoClass = "b31.c";
    v.aiIcon.methodGetShownAfterMillis = "t";

    v.imageQuality.qualityProfileHighClass = "ie8.a$b$a";
    v.imageQuality.qualityProfileMediumClass = "ie8.a$b$b";
    v.imageQuality.methodGetMaxDimension = "a";
    v.imageQuality.methodGetQuality = "b";
    v.imageQuality.imageUtilClass = "jp.naver.line.android.util.f1";

    v.profile.g50fClass = "s70.g";
    v.profile.h13baClass = "dh3.b";
    v.profile.fieldH3 = "R1";
    v.profile.g50aClass = "s70.a";
    v.profile.methodGetProfile = "getProfile";
    v.profile.fieldMid = "b";

    v.profileTimestamps.activityClass = "com.linecorp.line.userprofile.impl.UserProfileActivity";
    v.profileTimestamps.midExtraKey = "USER_PROFILE_MID";
    v.profileTimestamps.resHeaderButtonContainer = "user_profile_header_button_binding";

    v.media.videoDurationCheckClass = "ca1.b";
    v.media.videoDurationCheckMethod = "c";
    v.media.mediaPickerParamsClass = "com.linecorp.line.media.picker.b$i";
    v.media.fieldMediaPickerMaxVideoDuration = "y";
    v.media.droppedMediaPreprocessorClass = "ox0.b";
    v.media.videoDurationSuccessClass = "da1.a$c";
    v.media.fieldVideoDurationSuccess = "a";
    v.media.galleryViewClass = "pj1.v";
    v.media.fieldGalleryDurationLimit = "Y";
    v.media.selectionValidatorClass = "za3.p";
    v.media.selectionValidatorMethod = "o";
    v.media.selectionValidatorParamClass = "c02.c";
    v.media.videoProfileTrimmerActivityClass =
        "jp.naver.line.android.activity.setting.videoprofile.trim.VideoProfileTrimmerActivity";
    v.media.fieldVideoProfileTrimmerLimit = "M";

    v.chat.searchHeaderHelperClass = "sr1.g";
    v.chat.searchHeaderControllerField = "l";
    v.chat.searchHeaderEventBusField = "c";
    v.chat.searchControllerSearchBoxMethod = "d";
    v.chat.searchPresenterClass = "wr1.m";
    v.chat.searchKeywordTypeClass = "r41.c";
    v.chat.searchKeywordTypeMethod = "e";
    v.chat.searchResultClass = "r41.h";
    v.chat.searchResultCtorArgs = "chatId,keyword,idList,count";
    v.chat.searchResultWrapperClass = "r41.i";
    v.chat.searchBoxViewClass = "jp.naver.line.android.customview.SearchBoxView";
    v.chat.searchBoxEditTextField = "b";
    v.chat.searchKeywordEventClass = "rr1.b";
    v.chat.searchKeywordEventKeywordField = "a";
    v.chat.searchPresenterKeywordChangedMethod = "onSearchInChatKeywordChangedEventReceived";
    v.chat.searchPresenterKeywordSubjectField = "z";
    v.chat.searchKeywordSubjectValueMethod = "w";
    v.chat.searchResultWrapperResultOptionalField = "c";
    v.chat.searchResultCountField = "d";
    v.chat.searchResultTitleViewHolderClass = "zr1.h";
    v.chat.searchResultTitleBindMethod = "H0";
    v.chat.searchResultTitleBindingField = "x";
    v.chat.searchResultTitleTextViewField = "b";
    v.chat.searchFtsInChatQueryClass = "a72.r";
    v.chat.searchFtsQueryField = "a";
    v.chat.searchFtsChatIdField = "b";
    v.chat.searchFtsLimitField = "c";

    v.announcementFix.formatterClass = "ym1.a";
    v.announcementFix.formatMethod = "a";
    v.announcementFix.nameResolverMethod = "b";
    v.announcementFix.announcementEventClass = "h31.h$d0";

    v.chatJump.requestClass = "com.linecorp.line.chat.request.ChatHistoryRequest";
    v.chatJump.launchActivityClass =
        "jp.naver.line.android.activity.chathistory.ChatHistoryActivityLaunchActivity";
    v.chatJump.requestExtraKey = "chatHistoryRequest";

    v.chatTimestamp.displayTimeInterface = "s81.f";
    v.chatTimestamp.methodCreatedMillis = "a";

    v.chatEditSelectAll.selectionProviderClass = "k81.c";
    v.chatEditSelectAll.selectionStateClass = "k81.d";
    v.chatEditSelectAll.methodGetSelectionState = "d0";
    v.chatEditSelectAll.methodGetItem = "g0";
    v.chatEditSelectAll.methodGetSelectedIds = "f";
    v.chatEditSelectAll.methodToggleItem = "a";
    v.chatEditSelectAll.methodIsItemSelected = "e";

    v.messageEditHistory.editRequestClass = "x88.h";
    v.messageEditHistory.editRequestIdField = "b";
    v.messageEditHistory.editRequestTextField = "d";
    v.messageEditHistory.menuListBuilderClass = "pg1.x1";
    v.messageEditHistory.menuListMethod = "a";
    v.messageEditHistory.menuItemEnumClass = "i71.c";
    v.messageEditHistory.menuPresentationEnumClass = "pg1.x0";
    v.messageEditHistory.methodMenuLabel = "h";
    v.messageEditHistory.methodMenuIcon = "f";
    v.messageEditHistory.methodMenuActionAccessor = "e";
    v.messageEditHistory.menuActionLambdaClass = "h71.f$b";
    v.messageEditHistory.menuContextMessageField = "b";
    v.messageEditHistory.menuMessageDataField = "b";
    v.messageEditHistory.menuMessageIdField = "c";
    v.messageEditHistory.menuEditedFlagField = "x";

    v.camera.cameraModuleClass = "d72.j";
    v.camera.methodUseExternalCamera = "d";

    v.muteMessage.labFeatureClass = "s58.d";
    v.muteMessage.methodIsFeatureEnabled = "c";
    v.muteMessage.silentMessageFeatureClass = "s58.w";
    v.muteMessage.sendModeClass = "sn1.e";
    v.muteMessage.methodSendMode = "a";
    v.muteMessage.sendModeEnumClass = "xt1.a";
    v.muteMessage.silentFlagWriterClass = "se8.f1";
    v.muteMessage.methodWriteSilentFlag = "a";

    v.iab.inAppBrowserActivityClass = "com.linecorp.line.iab.browser.impl.InAppBrowserActivity";

    v.homeTab.tabListProviderClass = "l48.e";
    v.homeTab.methodBuildTabList = "a";
    v.homeTab.mainTabEnumClass = "jp.naver.line.android.activity.main.a";

    v.nightMode.nightModeConfiguratorClass = "t10.a";
    v.nightMode.methodApplyNightMode = "a";
    v.nightMode.fieldSystemDarkMode = "a";
    v.nightMode.inputPassActivityClass = "com.linecorp.line.passlock.InputPassActivity";
    v.nightMode.darkThemeManagerClass = "o76.j";
    v.nightMode.methodIsDarkTheme = "j";
    v.nightMode.methodThemeMode = "t";
    v.nightMode.methodIsDefaultTheme = "v";

    return v;
  }
}
