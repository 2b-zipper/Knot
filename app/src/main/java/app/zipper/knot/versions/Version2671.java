package app.zipper.knot.versions;

import app.zipper.knot.LineVersion;

public class Version2671 {
  public static LineVersion.Config create() {
    LineVersion.Config v = new LineVersion.Config();

    v.main.mainActivity = "jp.naver.line.android.activity.main.MainActivity";
    v.main.headerButton = "jp.naver.line.android.common.view.header.HeaderButton";
    v.main.headerButtonInnerField = "f186921a";
    v.main.headerButtonTypeClass = "fc8.d";
    v.main.slotFarLeft = "FAR_LEFT";
    v.main.headerInterfaceA = "jp.naver.line.android.common.view.header.a";
    v.main.fieldHeaderHelper = "f";
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
    v.settings.settingsAdapterClass = "j88.f";
    v.settings.settingsItemClass = "j88.f$c";
    v.settings.settingsBaseAdapterClass = "j88.f$b";
    v.settings.settingsSearchHelperClass = "jr4.b";
    v.settings.settingsAdapterWrapperClass = "tm4.a";
    v.settings.settingsHeaderItemClass = "um4.q";
    v.settings.settingsRowItemClass = "um4.s";
    v.settings.settingsHandlerBaseClass = "um4.y";
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
    v.settings.methodProxyGetItemType = "g";
    v.settings.methodSetTitleText = "setTitleText";
    v.settings.methodSetChecked = "setChecked";
    v.settings.methodSetItemType = "setItemType";
    v.settings.methodSetSyncStatus = "setSyncStatus";
    v.settings.methodSetDividerVisible = "setDividerVisible";

    v.plusMenu.plusMenuComponentClass = "kv0.n";
    v.plusMenu.plusMenuComposerClass = "z2.k";
    v.plusMenu.plusMenuComposerImplClass = "z2.l";
    v.plusMenu.plusMenuCallbackClass = "mj8.a";
    v.plusMenu.plusMenuOnClickItemClass = "mj8.l";
    v.plusMenu.methodAddMenuItem = "a";
    v.plusMenu.methodCreateMenu = "c";
    v.plusMenu.methodExecuteAction = "X";
    v.plusMenu.editChatDrawable = "chat_tab_ui_header_plusmenu_edit_chat";

    v.chatListMoreMenu.popupListViewClass =
        "jp.naver.line.android.common.view.listview.PopupListView";
    v.chatListMoreMenu.fieldListView = "a";
    v.chatListMoreMenu.popupListAdapterClass =
        "jp.naver.line.android.common.view.listview.PopupListView$b";
    v.chatListMoreMenu.fieldPopupItems = "a";
    v.chatListMoreMenu.clickListenerClass = "no1.a";
    v.chatListMoreMenu.methodAddItem = "a";

    v.readReceipt.readReceiptManagerClass = "du2.e";
    v.readReceipt.readReceiptQueueClass = "vf8.b";
    v.readReceipt.methodEnqueueReadReceipt = "c";
    v.readReceipt.methodSendReadReceipt = "d";
    v.readReceipt.methodExecuteReadReceiptAsync = "e";
    v.readReceipt.methodReadAll = "c";
    v.readReceipt.operationNotifiedReadName = "NOTIFIED_READ_MESSAGE";
    v.readReceipt.longPressReadClass = "aq1";

    v.unsend.notifiedReadMessageHandlerClass = "ti8.zd$a";
    v.unsend.notifiedSendReactionHandlerClass = "ti8.zd$a";
    v.unsend.notifiedDestroyMessageHandlerClass = "ti8.zd$a";
    v.unsend.chatMessageViewHolderClass = "ee1.i";
    v.unsend.methodReadBuffer = "b";
    v.unsend.methodBind = "V";
    v.unsend.methodOperationTypeValueOf = "a";
    v.unsend.methodBindIndex = 1;
    v.unsend.methodGetItemView = "c0";
    v.unsend.methodGetCommonData = "b";
    v.unsend.operationTypeDummy = 40;
    v.unsend.chatServiceConfigClass = "pl4.p";
    v.unsend.methodUnsendLimit = "i";
    v.unsend.methodUnsendPremiumLimit = "h";
    v.unsend.appInfoProviderClass = "zf8.c";
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
    v.unsend.unsendDestroyHandlerClass = "wg8.z0";
    v.unsend.operationClass = "ti8.zd";

    v.thrift.talkServiceClientImplClass =
        "jp.naver.line.android.thrift.client.impl.LegacyTalkServiceClientImpl";
    v.thrift.talkServiceClientInterface = "jp.naver.line.android.thrift.client.TalkServiceClient";
    v.thrift.v1 = "s1";
    v.thrift.protocolClass = "org.apache.thrift.o";
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
    v.home.lypRecommendationModuleArgClass = "fz1.r";
    v.home.lypRecommendationContextClass = "o12.h";
    v.home.lypRecommendationComposerClass = "z2.k";
    v.home.lypRecommendationModuleClass = "fz1.r$f0";
    v.home.lypRecommendationControllerClass = "i42.k";
    v.home.lypRecommendationSectionClass = "e12.e";

    v.chat.headerController = "v81.f1";
    v.chat.headerHelper = "jp.naver.line.android.common.view.header.b";
    v.chat.chatIdField = "j";
    v.chat.methodGetChatId = "P";

    v.chatHeader.chatHistoryActivity =
        "jp.naver.line.android.activity.chathistory.ChatHistoryActivity";
    v.chatHeader.fieldChatConfigChatId = "p41.a";
    v.chatHeader.fieldChatConfigIsMuted = "n41.a";
    v.chatHeader.fieldChatConfigCategory = "v01.g";
    v.chatHeader.fieldChatConfigType = "v81.r0";
    v.chatHeader.fieldAppInfoVersion = "yi1.n";
    v.chatHeader.fieldAppInfoPkg = "v01.a";
    v.chatHeader.fieldAppInfoId = "gm0.d";

    v.font.fontConfigClass = "q6.n";
    v.font.fontManagerClass = "q6.m";
    v.font.fontCallbackClass = "q6.n$c";
    v.font.fontInjectedClass = "f94.k";
    v.font.methodGetFontConfig = "a";
    v.font.methodInitializeFont = "b";
    v.font.methodGetFontSettings = "c";
    v.font.methodOnFontChanged = "b";
    v.font.fieldTypeface = "f266814a";
    v.font.fontRequestExecutorClass = "q6.p";
    v.font.fontCallbackWithHandlerClass = "q6.c";

    v.res.idSettingList = 0x7f0b2370;
    v.res.idPersonalInfo = 0x7f1536a6;
    v.res.typeSection = 0x7f0e0556;
    v.res.typeRow = 0x7f0e0559;
    v.res.idIcon = 0x7f0b2361;
    v.res.idDesc = 0x7f0b2353;
    v.res.idMark = 0x7f0b2374;
    v.res.idSeparator = 0x7f0b239a;
    v.res.idArrow = 0x7f0b233b;
    v.res.idNewMark = 0x7f0b19c6;
    v.res.idNoticeDot = 0x7f0b1a33;
    v.res.idTitle = 0x7f0b23a2;
    v.res.layoutCheckbox = 0x7f0e054a;
    v.res.layoutSectionHeader = 0x7f0e0556;
    v.res.layoutSettingsMain = 0x7f0e0550;
    v.res.idHeader = 0x7f0b1166;
    v.res.idStatusBarGuide = 0x7f0b2613;
    v.res.idTimestamp = 0x7f0b08b3;
    v.res.idChatMessageText = 0x7f0b07a9;
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
    v.notificationFix.lineFcmServiceBaseClass = "is.i";
    v.notificationFix.firebaseRemoteMessageClass = "is.k0";
    v.notificationFix.firebaseReceiverClass = "com.google.firebase.iid.FirebaseInstanceIdReceiver";
    v.notificationFix.firebaseReceiverMethod = "a";
    v.notificationFix.firebaseReceiverEnvelopeClass = "kk.a";
    v.notificationFix.firebaseReceiverIntentField = "f197382a";
    v.notificationFix.firebaseDispatcherClass = "is.n";
    v.notificationFix.firebaseDispatcherAccessorMethod = "a";
    v.notificationFix.firebaseDispatcherMethod = "b";
    v.notificationFix.firebaseDispatcherContextField = "f174156a";
    v.notificationFix.firebaseDispatcherQueueField = "f174155d";
    v.notificationFix.firebaseBindDeliveryClass = "is.c1";
    v.notificationFix.firebaseBindDeliveryMethod = "b";
    v.notificationFix.firebaseMessagingServiceClass =
        "com.google.firebase.messaging.FirebaseMessagingService";
    v.notificationFix.firebaseMessagingHandleMethod = "c";
    v.notificationFix.firebaseWakefulStartClass = "is.z0";
    v.notificationFix.firebaseWakefulStartMethod = "c";
    v.notificationFix.firebaseCompletedTaskClass = "tm.n";
    v.notificationFix.firebaseCompletedTaskMethod = "e";
    v.foregroundKeepAlive.serviceClass = "androidx.work.impl.foreground.SystemForegroundService";
    v.notificationFix.legyStreamingStateClass = "com.linecorp.legy.streaming.h$a";
    v.notificationFix.legyStreamingLifecycleClass = "com.linecorp.legy.streaming.h$d";
    v.notificationFix.legyStreamingLifecycleMethod = "a1";
    v.notificationFix.legyLifecycleOwnerClass = "androidx.lifecycle.u0";
    v.notificationFix.legyLifecycleEventClass = "androidx.lifecycle.e0$a";
    v.notificationFix.legyBackgroundStateField = "BACKGROUND";
    v.notificationFix.legyDisconnectRunnableClass = "v30.j";
    v.notificationFix.legyStateFieldCandidates = new String[] {"q", "f57738q"};
    v.notificationFix.legyTimeoutFieldCandidates = new String[] {"s", "f57740s"};
    v.notificationFix.legyBackgroundWorkerFlagFieldCandidates = new String[] {"u", "f57742u"};
    v.notificationFix.legyHandlerFieldCandidates = new String[] {"c", "f57724c"};
    v.notificationFix.legyRunnableFieldCandidates = new String[] {"t", "f57741t"};

    v.talkTabHeader.chatTabHeaderStateClass = "hq1.f";
    v.talkTabHeader.iconListStateField = "x";
    v.talkTabHeader.buttonListStateField = "C";
    v.talkTabHeader.iconTypeClass = "iv0.n";
    v.talkTabHeader.iconTypeFieldInButton = "a";
    v.talkTabHeader.subDeviceOpenChatButtonClass = "no1.c$e";
    v.talkTabHeader.subDeviceAlbumButtonClass = "no1.c$b";

    v.searchBarAgentI.talkVisibleMethod = "w";
    v.searchBarAgentI.talkClickMethod = "s";
    v.searchBarAgentI.homeSearchBarClass = "vj4.g";
    v.searchBarAgentI.homeRefreshMethod = "e";
    v.searchBarAgentI.homeRootViewField = "c";
    v.searchBarAgentI.homeTabTypeField = "b";
    v.searchBarAgentI.homeTabName = "HOME";
    v.searchBarAgentI.homeTabV2Name = "HOME_V2";
    v.searchBarAgentI.homeAiContainerId = 0x7f0b16da;
    v.searchBarAgentI.homeGuidelineId = 0x7f0b16dc;
    v.searchBarAgentI.homeGuidelineEndDp = 55;
    v.searchBarAgentI.homeGuidelineClass = "androidx.constraintlayout.widget.Guideline";

    v.agentIInChat.toggleComposableClass = "rb1.g";

    v.aiIcon.repoClass = "jx0.c";
    v.aiIcon.methodGetShownAfterMillis = "m";

    v.imageQuality.qualityProfileHighClass = "mg8.a$b$a";
    v.imageQuality.qualityProfileMediumClass = "mg8.a$b$b";
    v.imageQuality.methodGetMaxDimension = "a";
    v.imageQuality.methodGetQuality = "b";
    v.imageQuality.imageUtilClass = "jp.naver.line.android.util.y0";

    v.profile.g50fClass = "n50.f";
    v.profile.h13baClass = "k23.b$a";
    v.profile.fieldH3 = "c";
    v.profile.g50aClass = "n50.a";
    v.profile.methodGetProfile = "getProfile";
    v.profile.fieldMid = "b";

    v.profileTimestamps.activityClass = "com.linecorp.line.userprofile.impl.UserProfileActivity";
    v.profileTimestamps.midExtraKey = "USER_PROFILE_MID";
    v.profileTimestamps.resHeaderButtonContainer = "user_profile_header_button_binding";

    v.media.videoDurationCheckClass = "d41.b";
    v.media.videoDurationCheckMethod = "c";
    v.media.mediaPickerParamsClass = "com.linecorp.line.media.picker.b$i";
    v.media.fieldMediaPickerMaxVideoDuration = "y";
    v.media.droppedMediaPreprocessorClass = "ps0.b";
    v.media.videoDurationSuccessClass = "e41.a$c";
    v.media.fieldVideoDurationSuccess = "a";
    v.media.galleryViewClass = "bd1.f0";
    v.media.fieldGalleryDurationLimit = "U";
    v.media.selectionValidatorClass = "hw2.s";
    v.media.selectionValidatorMethod = "o";
    v.media.selectionValidatorParamClass = "yr1.b";
    v.media.videoProfileTrimmerActivityClass =
        "jp.naver.line.android.activity.setting.videoprofile.trim.VideoProfileTrimmerActivity";
    v.media.fieldVideoProfileTrimmerLimit = "M";

    v.chat.searchHeaderHelperClass = "wj1.h";
    v.chat.searchHeaderControllerField = "i";
    v.chat.searchHeaderEventBusField = "b";
    v.chat.searchControllerSearchBoxMethod = "D0";
    v.chat.searchPresenterClass = "ak1.r";
    v.chat.searchKeywordTypeClass = "xy0.a";
    v.chat.searchKeywordTypeMethod = "d";
    v.chat.searchResultClass = "xy0.f";
    v.chat.searchResultWrapperClass = "xy0.g";
    v.chat.searchBoxViewClass = "jp.naver.line.android.customview.SearchBoxView";
    v.chat.searchBoxEditTextField = "b";
    v.chat.searchKeywordEventClass = "vj1.b";
    v.chat.searchKeywordEventKeywordField = "a";
    v.chat.searchPresenterKeywordChangedMethod = "onSearchInChatKeywordChangedEventReceived";
    v.chat.searchPresenterKeywordSubjectField = "t";
    v.chat.searchResultWrapperResultOptionalField = "c";
    v.chat.searchResultCountField = "d";
    v.chat.searchResultTitleViewHolderClass = "dk1.k";
    v.chat.searchResultTitleBindMethod = "F0";
    v.chat.searchResultTitleBindingField = "x";
    v.chat.searchResultTitleTextViewField = "b";
    v.chat.searchFtsInChatQueryClass = "ty1.o";
    v.chat.searchFtsQueryField = "a";
    v.chat.searchFtsChatIdField = "b";
    v.chat.searchFtsLimitField = "c";

    v.announcementFix.formatterClass = "gg1.c";
    v.announcementFix.formatMethod = "a";
    v.announcementFix.nameResolverMethod = "b";
    v.announcementFix.announcementEventClass = "px0.h$c0";

    v.chatJump.requestClass = "com.linecorp.line.chat.request.ChatHistoryRequest";
    v.chatJump.launchActivityClass =
        "jp.naver.line.android.activity.chathistory.ChatHistoryActivityLaunchActivity";
    v.chatJump.requestExtraKey = "chatHistoryRequest";

    v.chatTimestamp.displayTimeInterface = "u21.f";
    v.chatTimestamp.methodCreatedMillis = "a";

    v.chatEditSelectAll.selectionProviderClass = "m21.c";
    v.chatEditSelectAll.selectionStateClass = "m21.d";
    v.chatEditSelectAll.methodGetSelectionState = "m";
    v.chatEditSelectAll.methodGetItem = "B";
    v.chatEditSelectAll.methodGetSelectedIds = "d";
    v.chatEditSelectAll.methodToggleItem = "g";
    v.chatEditSelectAll.methodIsItemSelected = "n";

    v.camera.cameraModuleClass = "wy1.h";
    v.camera.methodUseExternalCamera = "c";

    v.iab.inAppBrowserActivityClass = "com.linecorp.line.iab.browser.api.InAppBrowserActivity";

    return v;
  }
}
