package app.zipper.knot.versions;

import app.zipper.knot.LineVersion;

public class Version2661 {
  public static LineVersion.Config create() {
    LineVersion.Config v = new LineVersion.Config();

    v.main.mainActivity = "jp.naver.line.android.activity.main.MainActivity";
    v.main.headerButton = "jp.naver.line.android.common.view.header.HeaderButton";
    v.main.headerButtonInnerField = "f192961a";
    v.main.headerButtonTypeClass = "qa8.d";
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
    v.settings.settingsAdapterClass = "u68.f";
    v.settings.settingsItemClass = "u68.f$c";
    v.settings.settingsBaseAdapterClass = "u68.f$b";
    v.settings.settingsSearchHelperClass = "pp4.b";
    v.settings.settingsAdapterWrapperClass = "jl4.a";
    v.settings.settingsHeaderItemClass = "kl4.q";
    v.settings.settingsRowItemClass = "kl4.t";
    v.settings.settingsHandlerBaseClass = "kl4.y";
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

    v.plusMenu.plusMenuComponentClass = "av0.t";
    v.plusMenu.plusMenuComposerClass = "t2.k";
    v.plusMenu.plusMenuComposerImplClass = "t2.l";
    v.plusMenu.plusMenuCallbackClass = "xh8.a";
    v.plusMenu.plusMenuOnClickItemClass = "xh8.l";
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
    v.chatListMoreMenu.clickListenerClass = "vn1.a";
    v.chatListMoreMenu.methodAddItem = "a";

    v.readReceipt.readReceiptManagerClass = "at2.e";
    v.readReceipt.readReceiptQueueClass = "ge8.b";
    v.readReceipt.methodEnqueueReadReceipt = "c";
    v.readReceipt.methodSendReadReceipt = "d";
    v.readReceipt.methodExecuteReadReceiptAsync = "e";
    v.readReceipt.methodReadAll = "c";
    v.readReceipt.operationNotifiedReadName = "NOTIFIED_READ_MESSAGE";
    v.readReceipt.longPressReadClass = "ip1";

    v.unsend.notifiedReadMessageHandlerClass = "eh8.ae$a";
    v.unsend.notifiedSendReactionHandlerClass = "eh8.ae$a";
    v.unsend.notifiedDestroyMessageHandlerClass = "eh8.ae$a";
    v.unsend.chatMessageViewHolderClass = "nd1.h";
    v.unsend.methodReadBuffer = "a";
    v.unsend.methodBind = "N";
    v.unsend.methodOperationTypeValueOf = "a";
    v.unsend.methodBindIndex = 1;
    v.unsend.methodGetItemView = "c0";
    v.unsend.methodGetCommonData = "b";
    v.unsend.operationTypeDummy = 40;
    v.unsend.chatServiceConfigClass = "fk4.p";
    v.unsend.methodUnsendLimit = "i";
    v.unsend.methodUnsendPremiumLimit = "h";
    v.unsend.appInfoProviderClass = "ke8.d";
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
    v.unsend.unsendDestroyHandlerClass = "hf8.z0";
    v.unsend.operationClass = "eh8.ae";

    v.thrift.talkServiceClientImplClass =
        "jp.naver.line.android.thrift.client.impl.LegacyTalkServiceClientImpl";
    v.thrift.talkServiceClientInterface = "jp.naver.line.android.thrift.client.TalkServiceClient";
    v.thrift.v1 = "d1";
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
    v.home.lypRecommendationModuleArgClass = "my1.r";
    v.home.lypRecommendationContextClass = "v02.g";
    v.home.lypRecommendationComposerClass = "t2.k";
    v.home.lypRecommendationModuleClass = "my1.r$d0";
    v.home.lypRecommendationControllerClass = "p32.j";
    v.home.lypRecommendationSectionClass = "l02.e";

    v.chat.headerController = "h81.o1";
    v.chat.headerHelper = "jp.naver.line.android.common.view.header.b";
    v.chat.chatIdField = "j";
    v.chat.methodGetChatId = "P";

    v.chatHeader.chatHistoryActivity =
        "jp.naver.line.android.activity.chathistory.ChatHistoryActivity";
    v.chatHeader.fieldChatConfigChatId = "b41.a";
    v.chatHeader.fieldChatConfigIsMuted = "z31.a";
    v.chatHeader.fieldChatConfigCategory = "h01.g";
    v.chatHeader.fieldChatConfigType = "h81.y0";
    v.chatHeader.fieldAppInfoVersion = "hi1.n";
    v.chatHeader.fieldAppInfoPkg = "h01.a";
    v.chatHeader.fieldAppInfoId = "wl0.c";

    v.font.fontConfigClass = "k6.n";
    v.font.fontManagerClass = "k6.m";
    v.font.fontCallbackClass = "k6.n$c";
    v.font.fontInjectedClass = "x74.j";
    v.font.methodGetFontConfig = "a";
    v.font.methodInitializeFont = "b";
    v.font.methodGetFontSettings = "g";
    v.font.methodOnFontChanged = "b";
    v.font.fieldTypeface = "f199739a";
    v.font.fontRequestExecutorClass = "k6.p";
    v.font.fontCallbackWithHandlerClass = "k6.c";

    v.res.idSettingList = 0x7f0b2373;
    v.res.idPersonalInfo = 0x7f15365b;
    v.res.typeSection = 0x7f0e0559;
    v.res.typeRow = 0x7f0e055c;
    v.res.idIcon = 0x7f0b2364;
    v.res.idDesc = 0x7f0b2356;
    v.res.idMark = 0x7f0b2377;
    v.res.idSeparator = 0x7f0b239d;
    v.res.idArrow = 0x7f0b233e;
    v.res.idNewMark = 0x7f0b19c9;
    v.res.idNoticeDot = 0x7f0b1a36;
    v.res.idTitle = 0x7f0b23a5;
    v.res.layoutCheckbox = 0x7f0e054e;
    v.res.layoutSectionHeader = 0x7f0e0559;
    v.res.layoutSettingsMain = 0x7f0e0553;
    v.res.idHeader = 0x7f0b1161;
    v.res.idStatusBarGuide = 0x7f0b2616;
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
    v.notificationFix.lineFcmServiceBaseClass = "es.i";
    v.notificationFix.firebaseRemoteMessageClass = "es.k0";
    v.notificationFix.firebaseReceiverClass = "com.google.firebase.iid.FirebaseInstanceIdReceiver";
    v.notificationFix.firebaseReceiverMethod = "a";
    v.notificationFix.firebaseReceiverEnvelopeClass = "ek.a";
    v.notificationFix.firebaseReceiverIntentField = "f120495a";
    v.notificationFix.firebaseDispatcherClass = "es.n";
    v.notificationFix.firebaseDispatcherAccessorMethod = "a";
    v.notificationFix.firebaseDispatcherMethod = "b";
    v.notificationFix.firebaseDispatcherContextField = "f124189a";
    v.notificationFix.firebaseDispatcherQueueField = "f124188d";
    v.notificationFix.firebaseBindDeliveryClass = "es.e1";
    v.notificationFix.firebaseBindDeliveryMethod = "b";
    v.notificationFix.firebaseMessagingServiceClass =
        "com.google.firebase.messaging.FirebaseMessagingService";
    v.notificationFix.firebaseMessagingHandleMethod = "c";
    v.notificationFix.firebaseWakefulStartClass = "es.z0";
    v.notificationFix.firebaseWakefulStartMethod = "c";
    v.notificationFix.firebaseCompletedTaskClass = "nm.n";
    v.notificationFix.firebaseCompletedTaskMethod = "e";
    v.foregroundKeepAlive.serviceClass = "androidx.work.impl.foreground.SystemForegroundService";
    v.notificationFix.legyStreamingStateClass = "com.linecorp.legy.streaming.h$a";
    v.notificationFix.legyStreamingLifecycleClass = "com.linecorp.legy.streaming.h$d";
    v.notificationFix.legyStreamingLifecycleMethod = "Z0";
    v.notificationFix.legyLifecycleOwnerClass = "androidx.lifecycle.u0";
    v.notificationFix.legyLifecycleEventClass = "androidx.lifecycle.e0$a";
    v.notificationFix.legyBackgroundStateField = "BACKGROUND";
    v.notificationFix.legyDisconnectRunnableClass = "o30.i";
    v.notificationFix.legyStateFieldCandidates = new String[] {"q", "f235489q"};
    v.notificationFix.legyTimeoutFieldCandidates = new String[] {"s", "f235491s"};
    v.notificationFix.legyBackgroundWorkerFlagFieldCandidates = new String[] {"u", "f235493u"};
    v.notificationFix.legyHandlerFieldCandidates = new String[] {"c", "f235475c"};
    v.notificationFix.legyRunnableFieldCandidates = new String[] {"t", "f235492t"};

    v.talkTabHeader.chatTabHeaderStateClass = "pp1.f";
    v.talkTabHeader.iconListStateField = "x";
    v.talkTabHeader.buttonListStateField = "C";
    v.talkTabHeader.iconTypeClass = "yu0.n";
    v.talkTabHeader.iconTypeFieldInButton = "a";
    v.talkTabHeader.subDeviceOpenChatButtonClass = "vn1.c$e";
    v.talkTabHeader.subDeviceAlbumButtonClass = "vn1.c$b";

    v.searchBarAgentI.talkVisibleMethod = "w";
    v.searchBarAgentI.talkClickMethod = "r";
    v.searchBarAgentI.homeSearchBarClass = "ni4.i";
    v.searchBarAgentI.homeRefreshMethod = "e";
    v.searchBarAgentI.homeRootViewField = "c";
    v.searchBarAgentI.homeTabTypeField = "b";
    v.searchBarAgentI.homeTabName = "HOME";
    v.searchBarAgentI.homeTabV2Name = "HOME_V2";
    v.searchBarAgentI.homeAiContainerId = 0x7f0b16db;
    v.searchBarAgentI.homeGuidelineId = 0x7f0b16dd;
    v.searchBarAgentI.homeGuidelineEndDp = 55;
    v.searchBarAgentI.homeGuidelineClass = "androidx.constraintlayout.widget.Guideline";

    v.agentIInChat.toggleComposableClass = "za1.g";

    v.aiIcon.repoClass = "yw0.c";
    v.aiIcon.methodGetShownAfterMillis = "i";

    v.imageQuality.qualityProfileHighClass = "xe8.a$b$a";
    v.imageQuality.qualityProfileMediumClass = "xe8.a$b$b";
    v.imageQuality.methodGetMaxDimension = "a";
    v.imageQuality.methodGetQuality = "b";
    v.imageQuality.imageUtilClass = "jp.naver.line.android.util.a1";

    v.profile.g50fClass = "g50.f";
    v.profile.h13baClass = "h13.b$a";
    v.profile.fieldH3 = "c";
    v.profile.g50aClass = "g50.a";
    v.profile.methodGetProfile = "getProfile";
    v.profile.fieldMid = "b";

    v.profileTimestamps.activityClass = "com.linecorp.line.userprofile.impl.UserProfileActivity";
    v.profileTimestamps.midExtraKey = "USER_PROFILE_MID";
    v.profileTimestamps.resHeaderButtonContainer = "user_profile_header_button_binding";

    v.media.videoDurationCheckClass = "p31.b";
    v.media.videoDurationCheckMethod = "c";
    v.media.mediaPickerParamsClass = "com.linecorp.line.media.picker.b$i";
    v.media.fieldMediaPickerMaxVideoDuration = "y";
    v.media.droppedMediaPreprocessorClass = "fs0.b";
    v.media.videoDurationSuccessClass = "q31.a$c";
    v.media.fieldVideoDurationSuccess = "a";
    v.media.galleryViewClass = "kc1.a0";
    v.media.fieldGalleryDurationLimit = "U";
    v.media.selectionValidatorClass = "ev2.s";
    v.media.selectionValidatorMethod = "o";
    v.media.selectionValidatorParamClass = "gr1.c";
    v.media.videoProfileTrimmerActivityClass =
        "jp.naver.line.android.activity.setting.videoprofile.trim.VideoProfileTrimmerActivity";
    v.media.fieldVideoProfileTrimmerLimit = "M";

    v.chat.searchHeaderHelperClass = "fj1.h";
    v.chat.searchHeaderControllerField = "i";
    v.chat.searchHeaderEventBusField = "b";
    v.chat.searchControllerSearchBoxMethod = "d";
    v.chat.searchPresenterClass = "jj1.n";
    v.chat.searchKeywordTypeClass = "ky0.a";
    v.chat.searchKeywordTypeMethod = "e";
    v.chat.searchResultClass = "ky0.f";
    v.chat.searchResultWrapperClass = "ky0.g";
    v.chat.searchBoxViewClass = "jp.naver.line.android.customview.SearchBoxView";
    v.chat.searchBoxEditTextField = "b";
    v.chat.searchKeywordEventClass = "ej1.b";
    v.chat.searchKeywordEventKeywordField = "a";
    v.chat.searchPresenterKeywordChangedMethod = "onSearchInChatKeywordChangedEventReceived";
    v.chat.searchPresenterKeywordSubjectField = "t";
    v.chat.searchResultWrapperResultOptionalField = "c";
    v.chat.searchResultCountField = "d";
    v.chat.searchResultTitleViewHolderClass = "mj1.j";
    v.chat.searchResultTitleBindMethod = "F0";
    v.chat.searchResultTitleBindingField = "x";
    v.chat.searchResultTitleTextViewField = "b";
    v.chat.searchFtsInChatQueryClass = "ay1.p";
    v.chat.searchFtsQueryField = "a";
    v.chat.searchFtsChatIdField = "b";
    v.chat.searchFtsLimitField = "c";

    v.announcementFix.formatterClass = "pf1.c";
    v.announcementFix.formatMethod = "a";
    v.announcementFix.nameResolverMethod = "b";
    v.announcementFix.announcementEventClass = "ex0.h$c0";

    v.chatJump.requestClass = "com.linecorp.line.chat.request.ChatHistoryRequest";
    v.chatJump.launchActivityClass =
        "jp.naver.line.android.activity.chathistory.ChatHistoryActivityLaunchActivity";
    v.chatJump.requestExtraKey = "chatHistoryRequest";

    v.chatTimestamp.displayTimeInterface = "g21.f";
    v.chatTimestamp.methodCreatedMillis = "a";

    v.chatEditSelectAll.selectionProviderClass = "y11.c";
    v.chatEditSelectAll.selectionStateClass = "y11.d";
    v.chatEditSelectAll.methodGetSelectionState = "m";
    v.chatEditSelectAll.methodGetItem = "A";
    v.chatEditSelectAll.methodGetSelectedIds = "d";
    v.chatEditSelectAll.methodToggleItem = "h";
    v.chatEditSelectAll.methodIsItemSelected = "l";

    v.camera.cameraModuleClass = "dy1.h";
    v.camera.methodUseExternalCamera = "c";

    v.iab.inAppBrowserActivityClass = "jp.naver.line.android.activity.iab.InAppBrowserActivity";

    return v;
  }
}
