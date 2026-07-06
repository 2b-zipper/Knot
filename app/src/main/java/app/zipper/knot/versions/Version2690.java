package app.zipper.knot.versions;

import app.zipper.knot.LineVersion;

public class Version2690 {
  public static LineVersion.Config create() {
    LineVersion.Config v = new LineVersion.Config();

    v.main.mainActivity = "jp.naver.line.android.activity.main.MainActivity";
    v.main.headerButton = "jp.naver.line.android.common.view.header.HeaderButton";
    v.main.headerButtonInnerField = "f193742a";
    v.main.headerButtonTypeClass = "ow7.d";
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
    v.settings.settingsAdapterClass = "ss7.f";
    v.settings.settingsItemClass = "ss7.f$c";
    v.settings.settingsBaseAdapterClass = "ss7.f$b";
    v.settings.settingsSearchHelperClass = "aw4.b";
    v.settings.settingsAdapterWrapperClass = "jr4.a";
    v.settings.settingsHeaderItemClass = "kr4.s";
    v.settings.settingsRowItemClass = "kr4.v";
    v.settings.settingsHandlerBaseClass = "kr4.a0";
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

    v.plusMenu.plusMenuComponentClass = "bx0.t";
    v.plusMenu.plusMenuComposerClass = "h3.s";
    v.plusMenu.plusMenuComposerImplClass = "h3.e1";
    v.plusMenu.plusMenuCallbackClass = "v38.a";
    v.plusMenu.plusMenuOnClickItemClass = "v38.l";
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
    v.chatListMoreMenu.clickListenerClass = "ar1.a";
    v.chatListMoreMenu.methodAddItem = "a";

    v.readReceipt.readReceiptManagerClass = "ly2.e";
    v.readReceipt.methodSendReadReceipt = "d";
    v.readReceipt.methodExecuteReadReceiptAsync = "e";
    v.readReceipt.methodReadAll = "c";
    v.readReceipt.operationNotifiedReadName = "NOTIFIED_READ_MESSAGE";
    v.readReceipt.longPressReadClass = "ns1";

    v.unsend.notifiedReadMessageHandlerClass = "f18.e2";
    v.unsend.notifiedSendReactionHandlerClass = "f18.p2";
    v.unsend.notifiedDestroyMessageHandlerClass = "f18.c1";
    v.unsend.chatMessageViewHolderClass = "fg1.i";
    v.unsend.methodReadBuffer = "b";
    v.unsend.methodBind = "l0";
    v.unsend.methodOperationTypeValueOf = "a";
    v.unsend.methodBindIndex = 1;
    v.unsend.methodGetItemView = "c0";
    v.unsend.methodGetCommonData = "b";
    v.unsend.operationTypeDummy = 40;
    v.unsend.chatServiceConfigClass = "fq4.q";
    v.unsend.methodUnsendLimit = "i";
    v.unsend.methodUnsendPremiumLimit = "h";
    v.unsend.appInfoProviderClass = "i08.c";
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
    v.unsend.unsendDestroyHandlerClass = "f18.c1";
    v.unsend.operationClass = "c38.be";

    v.thrift.talkServiceClientImplClass =
        "jp.naver.line.android.thrift.client.impl.LegacyTalkServiceClientImpl";
    v.thrift.talkServiceClientInterface = "jp.naver.line.android.thrift.client.TalkServiceClient";
    v.thrift.v1 = "r1";
    v.thrift.protocolClass = "org.apache.thrift.n";
    v.thrift.messageClass = "org.apache.thrift.e";
    v.thrift.methodWriteMessageBegin = "b";
    v.thrift.methodReadMessageBegin = "a";
    v.thrift.methodDestroyMessage = "destroyMessage";
    v.thrift.methodDestroyMessages = "destroyMessages";

    v.tabs.bottomNavigationBarTextViewClass =
        "jp.naver.line.android.activity.main.bottomnavigationbar.BottomNavigationBarTextView";
    v.tabs.resVoom = "bnb_timeline";
    v.tabs.resNews = "bnb_news";
    v.tabs.resMini = "bnb_mini";
    v.tabs.resContainer = "main_tab_container";
    v.tabs.resBtnText = "bnb_button_text";

    v.ads.classAdSdkBase = "com.linecorp.line.ladsdk";
    v.ads.classAdMolinBase = "com.linecorp.line.admolin";
    v.ads.ladAdView = v.ads.classAdSdkBase + ".ui.common.view.lifecycle.LadAdView";
    v.ads.ladAdViewV2 = v.ads.classAdSdkBase + ".ui.v2.common.lifecycle.LyadAdView";
    v.ads.smartChannel = v.ads.classAdMolinBase + ".smartch.v2.view.SmartChannelViewLayout";

    v.home.resRecommendation = "home_tab_contents_recommendation_placement";
    v.home.resServiceCarouselId = "home_tab_service_carousel";
    v.home.resServiceTitleId = "home_tab_service_title";
    v.home.resNoServicesId = "home_tab_no_services_title";
    v.home.lypRecommendationModuleArgClass = "z12.w";
    v.home.lypRecommendationContextClass = "i42.m";
    v.home.lypRecommendationComposerClass = "h3.s";
    v.home.lypRecommendationModuleClass = "z12.w$h0";
    v.home.lypRecommendationControllerClass = "c72.k";
    v.home.lypRecommendationSectionClass = "y32.e";

    v.chat.headerController = "ta1.h1";
    v.chat.headerHelper = "jp.naver.line.android.common.view.header.b";
    v.chat.chatIdField = "j";
    v.chat.methodGetChatId = "s";

    v.chatHeader.chatHistoryActivity =
        "jp.naver.line.android.activity.chathistory.ChatHistoryActivity";
    v.chatHeader.fieldChatConfigChatId = "k61.a";
    v.chatHeader.fieldChatConfigIsMuted = "i61.a";
    v.chatHeader.fieldChatConfigType = "ta1.s0";
    v.chatHeader.fieldAppInfoVersion = "hl1.n";
    v.chatHeader.fieldAppInfoPkg = "p21.a";
    v.chatHeader.fieldAppInfoId = "qn0.d";

    v.font.fontConfigClass = "e7.n";
    v.font.fontManagerClass = "e7.m";
    v.font.fontCallbackClass = "e7.n$c";
    v.font.fontInjectedClass = "td4.j";
    v.font.methodGetFontConfig = "a";
    v.font.methodInitializeFont = "b";
    v.font.methodGetFontSettings = "c";
    v.font.methodOnFontChanged = "b";
    v.font.fieldTypeface = "f116276a";
    v.font.fontRequestExecutorClass = "e7.p";
    v.font.fontCallbackWithHandlerClass = "e7.c";

    v.res.idSettingList = 0x7f0b229d;
    v.res.idPersonalInfo = 0x7f153785;
    v.res.typeSection = 0x7f0e0559;
    v.res.typeRow = 0x7f0e055c;
    v.res.idIcon = 0x7f0b228e;
    v.res.idDesc = 0x7f0b2280;
    v.res.idMark = 0x7f0b22a1;
    v.res.idSeparator = 0x7f0b22c7;
    v.res.idArrow = 0x7f0b2268;
    v.res.idNewMark = 0x7f0b1921;
    v.res.idNoticeDot = 0x7f0b198b;
    v.res.idTitle = 0x7f0b22cf;
    v.res.layoutCheckbox = 0x7f0e054d;
    v.res.layoutSectionHeader = 0x7f0e0559;
    v.res.layoutSettingsMain = 0x7f0e0553;
    v.res.idHeader = 0x7f0b10f1;
    v.res.idStatusBarGuide = 0x7f0b2532;
    v.res.idTimestamp = 0x7f0b08a0;
    v.res.idChatMessageText = 0x7f0b0797;
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
    v.notificationFix.firebaseRemoteMessageClass = "ht.j0";
    v.notificationFix.firebaseReceiverClass = "com.google.firebase.iid.FirebaseInstanceIdReceiver";
    v.notificationFix.firebaseReceiverMethod = "a";
    v.notificationFix.firebaseReceiverEnvelopeClass = "hl.a";
    v.notificationFix.firebaseReceiverIntentField = "a";
    v.notificationFix.firebaseDispatcherClass = "ht.n";
    v.notificationFix.firebaseDispatcherSingletonField = "d";
    v.notificationFix.firebaseDispatcherMethod = "b";
    v.notificationFix.firebaseDispatcherContextField = "a";
    v.notificationFix.firebaseDispatcherQueueField = "d";
    v.notificationFix.firebaseBindDeliveryClass = "ht.b1";
    v.notificationFix.firebaseBindDeliveryMethod = "b";
    v.notificationFix.firebaseMessagingServiceClass =
        "com.google.firebase.messaging.FirebaseMessagingService";
    v.notificationFix.firebaseMessagingHandleMethod = "c";
    v.notificationFix.firebaseWakefulStartClass = "ht.x0";
    v.notificationFix.firebaseWakefulStartMethod = "c";
    v.notificationFix.firebaseCompletedTaskClass = "sn.n";
    v.notificationFix.firebaseCompletedTaskMethod = "e";
    v.foregroundKeepAlive.serviceClass = "androidx.work.impl.foreground.SystemForegroundService";
    v.notificationFix.legyStreamingStateClass = "com.linecorp.legy.streaming.h$a";
    v.notificationFix.legyStreamingLifecycleClass = "com.linecorp.legy.streaming.h$d";
    v.notificationFix.legyStreamingLifecycleMethod = "d1";
    v.notificationFix.legyLifecycleOwnerClass = "androidx.lifecycle.u0";
    v.notificationFix.legyLifecycleEventClass = "androidx.lifecycle.e0$a";
    v.notificationFix.legyBackgroundStateField = "BACKGROUND";
    v.notificationFix.legyDisconnectRunnableClass = "u40.k";
    v.notificationFix.legyStateFieldCandidates = new String[] {"q", "f61396q"};
    v.notificationFix.legyTimeoutFieldCandidates = new String[] {"s", "f61398s"};
    v.notificationFix.legyBackgroundWorkerFlagFieldCandidates = new String[] {"u", "f61400u"};
    v.notificationFix.legyHandlerFieldCandidates = new String[] {"c", "f61382c"};
    v.notificationFix.legyRunnableFieldCandidates = new String[] {"t", "f61399t"};

    v.talkTabHeader.chatTabHeaderStateClass = "us1.c";
    v.talkTabHeader.iconListStateField = "x";
    v.talkTabHeader.buttonListStateField = "C";
    v.talkTabHeader.iconTypeClass = "zw0.l";
    v.talkTabHeader.iconTypeFieldInButton = "a";
    v.talkTabHeader.subDeviceOpenChatButtonClass = "ar1.c$f";
    v.talkTabHeader.subDeviceAlbumButtonClass = "ar1.c$b";

    v.searchBarAgentI.talkVisibleMethod = "x";
    v.searchBarAgentI.talkClickMethod = "t";
    v.searchBarAgentI.homeSearchBarClass = "lo4.j";
    v.searchBarAgentI.homeRefreshMethod = "e";
    v.searchBarAgentI.homeRootViewField = "c";
    v.searchBarAgentI.homeTabTypeField = "b";
    v.searchBarAgentI.homeTabName = "HOME";
    v.searchBarAgentI.homeTabV2Name = "HOME_V2";
    v.searchBarAgentI.chatTabName = "CHAT";
    v.searchBarAgentI.homeAiContainerId = 0x7f0b1658;
    v.searchBarAgentI.homeGuidelineId = 0x7f0b165a;
    v.searchBarAgentI.homeGuidelineEndDp = 55;
    v.searchBarAgentI.homeGuidelineClass = "androidx.constraintlayout.widget.Guideline";

    v.agentIInChat.toggleComposableClass = "rd1.j";

    v.aiIcon.repoClass = "cz0.c";
    v.aiIcon.methodGetShownAfterMillis = "k";

    v.imageQuality.qualityProfileHighClass = "v08.a$b$a";
    v.imageQuality.qualityProfileMediumClass = "v08.a$b$b";
    v.imageQuality.methodGetMaxDimension = "a";
    v.imageQuality.methodGetQuality = "b";
    v.imageQuality.imageUtilClass = "jp.naver.line.android.util.c1";

    v.profile.g50fClass = "m60.g";
    v.profile.h13baClass = "t63.b";
    v.profile.fieldH3 = "e9";
    v.profile.g50aClass = "m60.a";
    v.profile.methodGetProfile = "getProfile";
    v.profile.fieldMid = "b";

    v.profileTimestamps.activityClass = "com.linecorp.line.userprofile.impl.UserProfileActivity";
    v.profileTimestamps.midExtraKey = "USER_PROFILE_MID";
    v.profileTimestamps.resHeaderButtonContainer = "user_profile_header_button_binding";

    v.media.videoDurationCheckClass = "y51.b";
    v.media.videoDurationCheckMethod = "c";
    v.media.mediaPickerParamsClass = "com.linecorp.line.media.picker.b$i";
    v.media.fieldMediaPickerMaxVideoDuration = "y";
    v.media.droppedMediaPreprocessorClass = "gu0.b";
    v.media.videoDurationSuccessClass = "z51.a$c";
    v.media.fieldVideoDurationSuccess = "a";
    v.media.galleryViewClass = "cf1.e0";
    v.media.fieldGalleryDurationLimit = "U";
    v.media.selectionValidatorClass = "q03.r";
    v.media.selectionValidatorMethod = "o";
    v.media.selectionValidatorParamClass = "lu1.b";
    v.media.videoProfileTrimmerActivityClass =
        "jp.naver.line.android.activity.setting.videoprofile.trim.VideoProfileTrimmerActivity";
    v.media.fieldVideoProfileTrimmerLimit = "M";

    v.chat.searchHeaderHelperClass = "gm1.i";
    v.chat.searchHeaderControllerField = "i";
    v.chat.searchHeaderEventBusField = "b";
    v.chat.searchControllerSearchBoxMethod = "C0";
    v.chat.searchPresenterClass = "km1.p";
    v.chat.searchKeywordTypeClass = "q01.a";
    v.chat.searchKeywordTypeMethod = "d";
    v.chat.searchResultClass = "q01.f";
    v.chat.searchResultWrapperClass = "q01.g";
    v.chat.searchBoxViewClass = "jp.naver.line.android.customview.SearchBoxView";
    v.chat.searchBoxEditTextField = "b";
    v.chat.searchKeywordEventClass = "fm1.b";
    v.chat.searchKeywordEventKeywordField = "a";
    v.chat.searchPresenterKeywordChangedMethod = "onSearchInChatKeywordChangedEventReceived";
    v.chat.searchPresenterKeywordSubjectField = "t";
    v.chat.searchResultWrapperResultOptionalField = "c";
    v.chat.searchResultCountField = "d";
    v.chat.searchResultTitleViewHolderClass = "nm1.g";
    v.chat.searchResultTitleBindMethod = "F0";
    v.chat.searchResultTitleBindingField = "x";
    v.chat.searchResultTitleTextViewField = "b";
    v.chat.searchFtsInChatQueryClass = "h12.o";
    v.chat.searchFtsQueryField = "a";
    v.chat.searchFtsChatIdField = "b";
    v.chat.searchFtsLimitField = "c";

    v.announcementFix.formatterClass = "hi1.c";
    v.announcementFix.formatMethod = "a";
    v.announcementFix.nameResolverMethod = "b";
    v.announcementFix.announcementEventClass = "iz0.h$d0";

    v.chatJump.requestClass = "com.linecorp.line.chat.request.ChatHistoryRequest";
    v.chatJump.launchActivityClass =
        "jp.naver.line.android.activity.chathistory.ChatHistoryActivityLaunchActivity";
    v.chatJump.requestExtraKey = "chatHistoryRequest";

    v.chatTimestamp.displayTimeInterface = "p41.f";
    v.chatTimestamp.methodCreatedMillis = "a";

    v.chatEditSelectAll.selectionProviderClass = "h41.c";
    v.chatEditSelectAll.selectionStateClass = "h41.d";
    v.chatEditSelectAll.methodGetSelectionState = "m";
    v.chatEditSelectAll.methodGetItem = "B";
    v.chatEditSelectAll.methodGetSelectedIds = "e";
    v.chatEditSelectAll.methodToggleItem = "j";
    v.chatEditSelectAll.methodIsItemSelected = "n";

    v.camera.cameraModuleClass = "k12.g";
    v.camera.methodUseExternalCamera = "d";

    v.iab.inAppBrowserActivityClass = "com.linecorp.line.iab.browser.InAppBrowserActivity";

    return v;
  }
}
