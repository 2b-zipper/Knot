package app.zipper.knot.versions;

import app.zipper.knot.LineVersion;

public class Version26140 {
  public static LineVersion.Config create() {
    LineVersion.Config v = new LineVersion.Config();

    v.main.mainActivity = "jp.naver.line.android.activity.main.MainActivity";
    v.main.baseMainTabFragment = "jp.naver.line.android.activity.main.BaseMainTabFragment";
    v.main.headerButton = "jp.naver.line.android.common.view.header.HeaderButton";
    v.main.headerButtonTypeClass = "rb8.d";
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
    v.settings.settingsAdapterClass = "y78.f";
    v.settings.settingsItemClass = "y78.f$c";
    v.settings.settingsBaseAdapterClass = "y78.f$b";
    v.settings.settingsSearchHelperClass = "ka5.b";
    v.settings.settingsAdapterWrapperClass = "l55.a";
    v.settings.settingsHeaderItemClass = "m55.t";
    v.settings.settingsRowItemClass = "m55.v";
    v.settings.settingsHandlerBaseClass = "m55.z";
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

    v.plusMenu.plusMenuComponentClass = "s11.o";
    v.plusMenu.plusMenuComposerImplClass = "h3.d1";
    v.plusMenu.plusMenuCallbackClass = "aj8.a";
    v.plusMenu.plusMenuOnClickItemClass = "aj8.l";
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
    v.chatListMoreMenu.clickListenerClass = "wx1.a";
    v.chatListMoreMenu.methodAddItem = "a";

    v.readReceipt.readReceiptManagerClass = "na3.e";
    v.readReceipt.methodSendReadReceipt = "d";
    v.readReceipt.methodExecuteReadReceiptAsync = "e";
    v.readReceipt.methodReadAll = "c";
    v.readReceipt.methodResolveReadTarget = "a";
    v.readReceipt.operationNotifiedReadName = "NOTIFIED_READ_MESSAGE";
    v.readReceipt.longPressReadClass = "jz1";

    v.unsend.notifiedReadMessageHandlerClass = "jg8.y1";
    v.unsend.notifiedSendReactionHandlerClass = "jg8.j2";
    v.unsend.notifiedDestroyMessageHandlerClass = "jg8.a1";
    v.unsend.chatMessageViewHolderClass = "nl1.g";
    v.unsend.methodReadBuffer = "b";
    v.unsend.methodBind = "V";
    v.unsend.methodOperationTypeValueOf = "a";
    v.unsend.methodBindIndex = 1;
    v.unsend.methodGetItemView = "a0";
    v.unsend.methodGetCommonData = "b";
    v.unsend.operationTypeDummy = 40;
    v.unsend.chatServiceConfigClass = "g45.s";
    v.unsend.methodUnsendLimit = "j";
    v.unsend.methodUnsendPremiumLimit = "i";
    v.unsend.appInfoProviderClass = "lf8.d";
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
    v.unsend.unsendDestroyHandlerClass = "jg8.a1";
    v.unsend.operationClass = "hi8.de";

    v.thrift.talkServiceClientImplClass =
        "jp.naver.line.android.thrift.client.impl.LegacyTalkServiceClientImpl";
    v.thrift.talkServiceClientInterface = "jp.naver.line.android.thrift.client.TalkServiceClient";
    v.thrift.v1 = "c1";
    v.thrift.protocolClass = "org.apache.thrift.n";
    v.thrift.messageClass = "org.apache.thrift.d";
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
    v.home.lypRecommendationModuleArgClass = "y82.k0";
    v.home.lypRecommendationContextClass = "jb2.q";
    v.home.lypRecommendationModuleClass = "y82.k0$q0";
    v.home.lypRecommendationControllerClass = "pi2.k";
    v.home.lypRecommendationSectionClass = "za2.h";

    v.home.home26FeedTypePrefixes =
        "HomeFeed,HomeContentsRecommendation,GlobalHomePage,GlobalHomeDefault,AdModel,HomePerformanceAd";
    v.home.home26ServiceTypePrefixes = "HomeServiceList,GlobalHomeServiceSection";
    v.home.home26LoadingMoreDataClass = "lb2.g$a";
    v.home.home26ModuleBodyField = "e";

    v.chat.headerController = "ag1.t1";
    v.chat.headerHelper = "jp.naver.line.android.common.view.header.b";
    v.chat.chatIdField = "j";
    v.chat.methodGetChatId = "t";

    v.chatHeader.chatHistoryActivity =
        "jp.naver.line.android.activity.chathistory.ChatHistoryActivity";
    v.chatHeader.fieldChatConfigChatId = "ib1.a";
    v.chatHeader.fieldChatConfigIsMuted = "gb1.a";
    v.chatHeader.fieldChatConfigType = "ag1.e1";
    v.chatHeader.fieldAppInfoVersion = "fr1.n";
    v.chatHeader.fieldAppInfoPkg = "k71.a";
    v.chatHeader.fieldAppInfoId = "or0.d";

    v.font.fontConfigClass = "f7.l";
    v.font.fontManagerClass = "f7.k";
    v.font.fontCallbackClass = "f7.l$c";
    v.font.fontInjectedClass = "rq4.k";
    v.font.methodGetFontConfig = "a";
    v.font.methodGetFontSettings = "c";
    v.font.methodOnFontChanged = "b";
    v.font.fontRequestExecutorClass = "f7.n";
    v.font.fontCallbackWithHandlerClass = "f7.c";

    v.res.idSettingList = 0x7f0b2283;
    v.res.idPersonalInfo = 0x7f153993;
    v.res.typeSection = 0x7f0e053f;
    v.res.typeRow = 0x7f0e0542;
    v.res.idIcon = 0x7f0b2275;
    v.res.idDesc = 0x7f0b2267;
    v.res.idMark = 0x7f0b2287;
    v.res.idSeparator = 0x7f0b22b0;
    v.res.idArrow = 0x7f0b224f;
    v.res.idNewMark = 0x7f0b18fa;
    v.res.idNoticeDot = 0x7f0b1967;
    v.res.idTitle = 0x7f0b22b8;
    v.res.layoutCheckbox = 0x7f0e0533;
    v.res.layoutSectionHeader = 0x7f0e053f;
    v.res.layoutSettingsMain = 0x7f0e0539;
    v.res.idHeader = 0x7f0b110b;
    v.res.idTimestamp = 0x7f0b0888;
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
    v.notificationFix.lineFcmServiceBaseClass = "eu.i";
    v.notificationFix.firebaseRemoteMessageClass = "eu.s0";
    v.notificationFix.firebaseReceiverClass = "com.google.firebase.iid.FirebaseInstanceIdReceiver";
    v.notificationFix.firebaseReceiverMethod = "a";
    v.notificationFix.firebaseReceiverEnvelopeClass = "zl.a";
    v.notificationFix.firebaseReceiverIntentField = "a";
    v.notificationFix.firebaseDispatcherClass = "eu.n";
    v.notificationFix.firebaseDispatcherSingletonField = "d";
    v.notificationFix.firebaseDispatcherMethod = "b";
    v.notificationFix.firebaseDispatcherContextField = "a";
    v.notificationFix.firebaseDispatcherQueueField = "d";
    v.notificationFix.firebaseBindDeliveryClass = "eu.n1";
    v.notificationFix.firebaseBindDeliveryMethod = "b";
    v.notificationFix.firebaseMessagingServiceClass =
        "com.google.firebase.messaging.FirebaseMessagingService";
    v.notificationFix.firebaseMessagingHandleMethod = "c";
    v.notificationFix.firebaseWakefulStartClass = "eu.i1";
    v.notificationFix.firebaseWakefulStartMethod = "c";
    v.notificationFix.firebaseCompletedTaskClass = "mo.n";
    v.notificationFix.firebaseCompletedTaskMethod = "e";
    v.notificationFix.firebaseMessagingClass = "com.google.firebase.messaging.FirebaseMessaging";
    v.notificationFix.firebaseMessagingGetTokenMethod = "a";
    v.notificationFix.firebaseMessagingTokenFreshMethod = "i";
    v.notificationFix.firebaseAppClass = "qs.e";
    v.notificationFix.firebaseAppGetInstanceMethod = "c";
    v.foregroundKeepAlive.serviceClass = "androidx.work.impl.foreground.SystemForegroundService";
    v.notificationFix.legyStreamingStateClass = "com.linecorp.legy.streaming.h$a";
    v.notificationFix.legyStreamingLifecycleClass = "com.linecorp.legy.streaming.h$d";
    v.notificationFix.legyStreamingLifecycleMethod = "g1";
    v.notificationFix.legyLifecycleOwnerClass = "androidx.lifecycle.u0";
    v.notificationFix.legyLifecycleEventClass = "androidx.lifecycle.f0$a";
    v.notificationFix.legyBackgroundStateField = "BACKGROUND";
    v.notificationFix.legyDisconnectRunnableClass = "q50.k";
    v.notificationFix.legyStateField = "q";
    v.notificationFix.legyTimeoutField = "s";
    v.notificationFix.legyBackgroundWorkerFlagField = "u";
    v.notificationFix.legyHandlerField = "c";
    v.notificationFix.legyRunnableField = "t";
    v.notificationFix.fisCertDigestClass = "lm.a";
    v.notificationFix.fisCertDigestMethod = "a";
    v.notificationFix.fisCertSha1 = "89396DC419292473972813922867E6973D6F5C50";
    v.notificationFix.gmsSignatureCheckClass = "am.k";
    v.notificationFix.gmsSignatureCheckMethod = "b";
    v.notificationFix.gmsAvailabilityClass = "am.j";
    v.notificationFix.gmsAvailabilityMethod = "e";

    v.talkTabHeader.chatTabHeaderStateClass = "qz1.f";
    v.talkTabHeader.iconListStateField = "y";
    v.talkTabHeader.buttonListStateField = "D";
    v.talkTabHeader.iconTypeClass = "q11.n";
    v.talkTabHeader.iconTypeFieldInButton = "a";
    v.talkTabHeader.subDeviceOpenChatButtonClass = "wx1.c$f";
    v.talkTabHeader.subDeviceAlbumButtonClass = "wx1.c$b";

    v.searchBarAgentI.talkVisibleMethod = "x";
    v.searchBarAgentI.talkClickMethod = "t";
    v.searchBarAgentI.homeSearchBarClass = "m25.i";
    v.searchBarAgentI.homeRefreshMethod = "e";
    v.searchBarAgentI.homeRootViewField = "c";
    v.searchBarAgentI.homeTabTypeField = "b";
    v.searchBarAgentI.homeTabName = "HOME";
    v.searchBarAgentI.homeTabV2Name = "HOME_V2";
    v.searchBarAgentI.chatTabName = "CHAT";
    v.searchBarAgentI.newsTabName = "NEWS";
    v.searchBarAgentI.homeAiContainerId = 0x7f0b1630;
    v.searchBarAgentI.homeGuidelineId = 0x7f0b1632;
    v.searchBarAgentI.homeGuidelineEndDp = 55;
    v.searchBarAgentI.homeGuidelineClass = "androidx.constraintlayout.widget.Guideline";
    v.searchBarAgentI.miniTabHeaderClass =
        "com.linecorp.line.wallet.impl.v3.view.WalletV3GrandDesignHeaderView";
    v.searchBarAgentI.miniTabAgentMethod = "o";
    v.searchBarAgentI.commerceHeaderClass = "h02.u";
    v.searchBarAgentI.commerceHeaderMethod = "e";
    v.home26NavIcon.rendererClass = "ng2.n";
    v.home26NavIcon.rendererMethod = "b";
    v.home26NavIcon.agentDrawableId = 0x7f080b87;
    v.home26NavIcon.settingsDrawableId = 0x7f081278;

    v.compose.composerClass = "h3.s";
    v.compose.clickableClass = "u1.l0";
    v.compose.methodClickable = "a";
    v.compose.methodCombinedClickable = "d";
    v.compose.onGloballyPositionedClass = "x4.y1";
    v.compose.methodOnGloballyPositioned = "a";
    v.compose.layoutCoordinatesClass = "x4.b0";
    v.compose.methodLocalToWindow = "l";
    v.compose.methodCoordinatesSize = "a";

    v.agentIInChat.toggleComposableClass = "zi1.i";

    v.aiIcon.repoClass = "v31.c";
    v.aiIcon.methodGetShownAfterMillis = "s";

    v.imageQuality.qualityProfileHighClass = "yf8.a$b$a";
    v.imageQuality.qualityProfileMediumClass = "yf8.a$b$b";
    v.imageQuality.methodGetMaxDimension = "a";
    v.imageQuality.methodGetQuality = "b";
    v.imageQuality.imageUtilClass = "jp.naver.line.android.util.g1";

    v.profile.g50fClass = "o70.e";
    v.profile.h13baClass = "wi3.b";
    v.profile.fieldH3 = "ac";
    v.profile.g50aClass = "o70.a";
    v.profile.methodGetProfile = "getProfile";
    v.profile.fieldMid = "b";

    v.profileTimestamps.activityClass = "com.linecorp.line.userprofile.impl.UserProfileActivity";
    v.profileTimestamps.midExtraKey = "USER_PROFILE_MID";
    v.profileTimestamps.resHeaderButtonContainer = "user_profile_header_button_binding";

    v.media.videoDurationCheckClass = "wa1.b";
    v.media.videoDurationCheckMethod = "c";
    v.media.mediaPickerParamsClass = "com.linecorp.line.media.picker.b$i";
    v.media.fieldMediaPickerMaxVideoDuration = "y";
    v.media.droppedMediaPreprocessorClass = "hy0.b";
    v.media.videoDurationSuccessClass = "xa1.a$c";
    v.media.fieldVideoDurationSuccess = "a";
    v.media.galleryViewClass = "kk1.y";
    v.media.fieldGalleryDurationLimit = "Y";
    v.media.selectionValidatorClass = "sc3.r";
    v.media.selectionValidatorMethod = "n";
    v.media.selectionValidatorParamClass = "i12.b";
    v.media.videoProfileTrimmerActivityClass =
        "jp.naver.line.android.activity.setting.videoprofile.trim.VideoProfileTrimmerActivity";
    v.media.fieldVideoProfileTrimmerLimit = "M";

    v.chat.searchHeaderHelperClass = "xs1.g";
    v.chat.searchHeaderControllerField = "l";
    v.chat.searchHeaderEventBusField = "c";
    v.chat.searchControllerSearchBoxMethod = "n0";
    v.chat.searchPresenterClass = "bt1.t";
    v.chat.searchKeywordTypeClass = "l51.c";
    v.chat.searchKeywordTypeMethod = "d";
    v.chat.searchResultClass = "l51.h";
    v.chat.searchResultCtorArgs = "chatId,keyword,idList,count";
    v.chat.searchResultWrapperClass = "l51.i";
    v.chat.searchBoxViewClass = "jp.naver.line.android.customview.SearchBoxView";
    v.chat.searchBoxEditTextField = "b";
    v.chat.searchKeywordEventClass = "ws1.b";
    v.chat.searchKeywordEventKeywordField = "a";
    v.chat.searchPresenterKeywordChangedMethod = "onSearchInChatKeywordChangedEventReceived";
    v.chat.searchPresenterKeywordSubjectField = "z";
    v.chat.searchKeywordSubjectValueMethod = "w";
    v.chat.searchResultWrapperResultOptionalField = "c";
    v.chat.searchResultCountField = "d";
    v.chat.searchResultTitleViewHolderClass = "et1.m";
    v.chat.searchResultTitleBindMethod = "H0";
    v.chat.searchResultTitleBindingField = "x";
    v.chat.searchResultTitleTextViewField = "b";
    v.chat.searchFtsInChatQueryClass = "g82.p";
    v.chat.searchFtsQueryField = "a";
    v.chat.searchFtsChatIdField = "b";
    v.chat.searchFtsLimitField = "c";

    v.announcementFix.formatterClass = "tn1.a";
    v.announcementFix.formatMethod = "a";
    v.announcementFix.nameResolverMethod = "b";
    v.announcementFix.announcementEventClass = "b41.g$d0";

    v.chatJump.requestClass = "com.linecorp.line.chat.request.ChatHistoryRequest";
    v.chatJump.launchActivityClass =
        "jp.naver.line.android.activity.chathistory.ChatHistoryActivityLaunchActivity";
    v.chatJump.requestExtraKey = "chatHistoryRequest";

    v.chatTimestamp.displayTimeInterface = "m91.f";
    v.chatTimestamp.methodCreatedMillis = "a";

    v.chatEditSelectAll.selectionProviderClass = "e91.c";
    v.chatEditSelectAll.selectionStateClass = "e91.d";
    v.chatEditSelectAll.methodGetSelectionState = "e0";
    v.chatEditSelectAll.methodGetItem = "h0";
    v.chatEditSelectAll.methodGetSelectedIds = "d";
    v.chatEditSelectAll.methodToggleItem = "n";
    v.chatEditSelectAll.methodIsItemSelected = "h";

    v.messageEditHistory.editRequestClass = "na8.h";
    v.messageEditHistory.editRequestIdField = "b";
    v.messageEditHistory.editRequestTextField = "d";
    v.messageEditHistory.menuListBuilderClass = "kh1.y1";
    v.messageEditHistory.menuListMethod = "a";
    v.messageEditHistory.menuItemEnumClass = "c81.c";
    v.messageEditHistory.menuPresentationEnumClass = "kh1.w0";
    v.messageEditHistory.methodMenuLabel = "g";
    v.messageEditHistory.methodMenuIcon = "f";
    v.messageEditHistory.methodMenuActionAccessor = "d";
    v.messageEditHistory.menuActionLambdaClass = "b81.f$b";
    v.messageEditHistory.menuContextMessageField = "b";
    v.messageEditHistory.menuMessageDataField = "b";
    v.messageEditHistory.menuMessageIdField = "c";
    v.messageEditHistory.menuEditedFlagField = "x";

    v.camera.cameraModuleClass = "j82.j";
    v.camera.methodUseExternalCamera = "d";

    v.muteMessage.labFeatureClass = "k78.d";
    v.muteMessage.methodIsFeatureEnabled = "c";
    v.muteMessage.silentMessageFeatureClass = "k78.c0";
    v.muteMessage.sendModeClass = "no1.d";
    v.muteMessage.methodSendMode = "a";
    v.muteMessage.sendModeEnumClass = "cv1.a";
    v.muteMessage.silentFlagWriterClass = "ig8.e1";
    v.muteMessage.methodWriteSilentFlag = "a";

    v.iab.inAppBrowserActivityClass = "com.linecorp.line.iab.browser.impl.InAppBrowserActivity";

    v.homeTab.tabListProviderClass = "b68.g";
    v.homeTab.methodBuildTabList = "a";
    v.homeTab.mainTabEnumClass = "jp.naver.line.android.activity.main.a";

    v.nightMode.nightModeConfiguratorClass = "u10.a";
    v.nightMode.methodApplyNightMode = "b";
    v.nightMode.fieldSystemDarkMode = "a";
    v.nightMode.inputPassActivityClass = "com.linecorp.line.passlock.InputPassActivity";
    v.nightMode.darkThemeManagerClass = "q96.k";
    v.nightMode.methodIsDarkTheme = "i";
    v.nightMode.methodThemeMode = "w";
    v.nightMode.methodIsDefaultTheme = "y";

    return v;
  }
}
