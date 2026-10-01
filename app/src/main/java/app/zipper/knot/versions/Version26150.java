package app.zipper.knot.versions;

import app.zipper.knot.LineVersion;

public class Version26150 {
  public static LineVersion.Config create() {
    LineVersion.Config v = new LineVersion.Config();

    v.main.mainActivity = "jp.naver.line.android.activity.main.MainActivity";
    v.main.baseMainTabFragment = "jp.naver.line.android.activity.main.BaseMainTabFragment";
    v.main.headerButton = "jp.naver.line.android.common.view.header.HeaderButton";
    v.main.headerButtonTypeClass = "qi8.d";
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
    v.main.methodHeaderSetButtonVisibility =
        "setUpButtonVisibility$LINE_Android_migrant_common_libs";
    v.main.methodHeaderSetButtonListener =
        "setUpButtonOnClickListener$LINE_Android_migrant_common_libs";

    v.settings.mainSettingsFragmentClass =
        "com.linecorp.line.settings.main.LineUserMainSettingsFragment";
    v.settings.settingsAdapterClass = "xe8.f";
    v.settings.settingsItemClass = "xe8.f$c";
    v.settings.settingsBaseAdapterClass = "xe8.f$b";
    v.settings.settingsSearchHelperClass = "xg5.b";
    v.settings.settingsAdapterWrapperClass = "yb5.a";
    v.settings.settingsHeaderItemClass = "zb5.r";
    v.settings.settingsRowItemClass = "zb5.u";
    v.settings.settingsHandlerBaseClass = "zb5.z";
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

    v.plusMenu.plusMenuComponentClass = "c71.t";
    v.plusMenu.plusMenuComposerImplClass = "j3.b1";
    v.plusMenu.plusMenuCallbackClass = "aq8.a";
    v.plusMenu.plusMenuOnClickItemClass = "aq8.l";
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
    v.chatListMoreMenu.clickListenerClass = "s32.a";
    v.chatListMoreMenu.methodAddItem = "a";

    v.readReceipt.readReceiptManagerClass = "pg3.e";
    v.readReceipt.methodSendReadReceipt = "d";
    v.readReceipt.methodExecuteReadReceiptAsync = "e";
    v.readReceipt.methodReadAll = "c";
    v.readReceipt.methodResolveReadTarget = "a";
    v.readReceipt.operationNotifiedReadName = "NOTIFIED_READ_MESSAGE";
    v.readReceipt.longPressReadClass = "f52";
    v.readReceipt.reactClientClass =
        "jp.naver.line.android.thrift.client.impl.TalkServiceClientImpl";
    v.readReceipt.methodReact = "T";
    v.readReceipt.reactRequestMessageIdField = "b";
    v.readReceipt.reactSuccessResultClass = "tn8.t0$b";

    v.unsend.notifiedReadMessageHandlerClass = "in8.a2";
    v.unsend.notifiedSendReactionHandlerClass = "in8.l2";
    v.unsend.chatMessageViewHolderClass = "wq1.f";
    v.unsend.methodReadBuffer = "b";
    v.unsend.methodBind = "i0";
    v.unsend.methodOperationTypeValueOf = "findByValue";
    v.unsend.methodBindIndex = 1;
    v.unsend.methodGetItemView = "a0";
    v.unsend.methodGetCommonData = "b";
    v.unsend.operationTypeDummy = 40;
    v.unsend.chatServiceConfigClass = "ta5.u";
    v.unsend.methodUnsendLimit = "j";
    v.unsend.methodUnsendPremiumLimit = "i";
    v.unsend.appInfoProviderClass = "km8.d";
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
    v.unsend.chatMessageServerIdLongField = "c";
    v.unsend.operationUnsendName = "DESTROY_MESSAGE";
    v.unsend.operationNotifiedUnsendName = "NOTIFIED_DESTROY_MESSAGE";
    v.unsend.unsendDestroyHandlerClass = "in8.a1";
    v.unsend.destroyMessageHandlerClass = "in8.r";
    v.unsend.methodDestroyHandler = "b";
    v.unsend.messageConverterClass = "kh8.x2";
    v.unsend.methodConvertMessage = "a";
    v.unsend.incomingMessageIdField = "c";
    v.unsend.incomingMessageTypeField = "d";
    v.unsend.incomingMessageParamsField = "o";
    v.unsend.incomingMessageParamsMapField = "a";
    v.unsend.incomingMessageTypeEnumClass = "gl8.i$b";
    v.unsend.handlerSuccessResultClass = "hn8.a$a$c";

    v.thrift.talkServiceClientImplClass =
        "jp.naver.line.android.thrift.client.impl.LegacyTalkServiceClientImpl";
    v.thrift.talkServiceClientInterface = "jp.naver.line.android.thrift.client.TalkServiceClient";
    v.thrift.v1 = "W0";
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
    v.home.lypRecommendationModuleArgClass = "xe2.n0";
    v.home.lypRecommendationContextClass = "ih2.q";
    v.home.lypRecommendationModuleClass = "xe2.n0$q0";
    v.home.lypRecommendationControllerClass = "com.linecorp.line.home.ui.impl.lyprecommendation.b";
    v.home.lypRecommendationSectionClass = "yg2.g";

    v.home.home26FeedTypePrefixes =
        "HomeFeed,HomeContentsRecommendation,GlobalHomePage,GlobalHomeDefault,AdModel,HomePerformanceAd";
    v.home.home26ServiceTypePrefixes = "HomeServiceList,GlobalHomeServiceSection";
    v.home.home26LoadingMoreDataClass = "kh2.h$a";
    v.home.home26ModuleBodyField = "e";

    v.chat.headerController = "il1.c1";
    v.chat.headerHelper = "jp.naver.line.android.common.view.header.b";
    v.chat.chatIdField = "j";
    v.chat.methodGetChatId = "s";

    v.chatHeader.chatHistoryActivity =
        "jp.naver.line.android.activity.chathistory.ChatHistoryActivity";
    v.chatHeader.fieldChatConfigChatId = "pg1.a";
    v.chatHeader.fieldChatConfigIsMuted = "ng1.a";
    v.chatHeader.fieldChatConfigType = "il1.p0";
    v.chatHeader.fieldAppInfoVersion =
        "com.linecorp.line.chat.ui.impl.officialaccount.OaChatStatusBarViewModel";
    v.chatHeader.fieldAppInfoPkg = "rc1.a";
    v.chatHeader.fieldAppInfoId = "vw0.d";

    v.font.fontConfigClass = "j7.l";
    v.font.fontManagerClass = "j7.k";
    v.font.fontCallbackClass = "j7.l$c";
    v.font.fontInjectedClass = "dx4.p";
    v.font.methodGetFontConfig = "a";
    v.font.methodGetFontSettings = "c";
    v.font.methodOnFontChanged = "b";
    v.font.fontRequestExecutorClass = "j7.n";
    v.font.fontCallbackWithHandlerClass = "j7.c";

    v.res.idSettingList = 0x7f0b229e;
    v.res.idPersonalInfo = 0x7f1539ca;
    v.res.typeSection = 0x7f0e0543;
    v.res.typeRow = 0x7f0e0546;
    v.res.idIcon = 0x7f0b2290;
    v.res.idDesc = 0x7f0b2282;
    v.res.idMark = 0x7f0b22a2;
    v.res.idSeparator = 0x7f0b22cb;
    v.res.idArrow = 0x7f0b226a;
    v.res.idNewMark = 0x7f0b1916;
    v.res.idNoticeDot = 0x7f0b1983;
    v.res.idTitle = 0x7f0b22d4;
    v.res.layoutCheckbox = 0x7f0e0537;
    v.res.layoutSectionHeader = 0x7f0e0543;
    v.res.layoutSettingsMain = 0x7f0e053d;
    v.res.idHeader = 0x7f0b111c;
    v.res.idTimestamp = 0x7f0b0888;
    v.res.resSettingsHeaderBtn = "settings_header_button";
    v.res.resSettingsBtn = "settings_button";
    v.res.resTooltipBackground = "home_tooltip_background";
    v.res.resTooltipArrowUp = "home_tooltip_arrow_up";

    v.notification.chatHistoryRequestClass = "com.linecorp.line.chat.request.ChatHistoryRequest";
    v.notification.chatHistoryActivityLaunchActivityClass =
        "jp.naver.line.android.activity.chathistory.ChatHistoryActivityLaunchActivity";

    v.notification.decryptedResultClass = "in8.c3$b$a";
    v.notification.messageClass = "gp8.od";
    v.notification.messageServerIdField = "d";
    v.notification.messageContentTypeField = "j";
    v.notification.messageTextField = "g";
    v.notification.messageMetadataField = "k";
    v.notification.messageMetadataKeyClass = "dk8.b$c";
    v.notification.messageMetadataStringMethod = "u";
    v.notification.messageObsPopField = "OBS_POP";
    v.notification.messageVisualBuilderClass = "lh8.t";
    v.notification.messageVisualEncryptionMethod = "m";
    v.notification.messageThumbnailRequestClass = "e80.d";
    v.notification.messageObsEncryptionDataClass = "xg1.a$c";
    v.notification.messageSquareUtilsClass = "com.linecorp.square.chat.SquareChatUtils";
    v.notification.messageSquareCheckMethod = "b";
    v.notification.messageProcessorClass = "in8.c3";
    v.notification.messageProcessorMethod = "g";
    v.notification.messageProcessorContentTypeClass = "gp8.q7";
    v.notification.messageProcessorMetadataClass = "dk8.b";
    v.notification.messageProcessorAuxClass = "gl8.o";
    v.notification.messageProcessorContinuationClass = "qp8.d";
    v.notification.messageContentCacheClass = "y31.j";
    v.notification.messageContentCacheInitializeMethod = "E";
    v.notification.messageContentCacheKeyClass = "w31.c";
    v.notification.messageThumbnailCacheFileMethod = "e";
    v.notification.glideClass = "com.bumptech.glide.c";
    v.notification.glideWithContextMethod = "e";
    v.notification.glideRetrieverMethod = "c";
    v.notification.glideRetrieverGetMethod = "f";
    v.notification.glideAsFileMethod = "p";
    v.notification.glideLoadMethod = "b0";
    v.notification.glideSubmitMethod = "g0";
    v.notification.glideClearMethod = "n";
    v.notification.stickerFileManagerClass = "ku5.f";
    v.notification.stickerInitializeMethod = "E";
    v.notification.stickerMainCacheFileMethod = "p";
    v.notification.stickerRequestFactoryClass = "vs5.a";
    v.notification.stickerRequestFactoryField = "a";
    v.notification.stickerServiceLocatorClass = "sc0.f";
    v.notification.stickerServiceLocatorMethod = "a";
    v.notification.stickerResourceClass = "ru5.d";
    v.notification.stickerSecretClass = "ru5.e";
    v.notification.stickerOptionClass = "ru5.s";
    v.notification.stickerStaticOptionField = "STATIC";
    v.notification.stickerMainRequestMethod = "l";
    v.notification.stickerRemoteFetcherClass = "ji2.g";
    v.notification.stickerRemoteCompleteMethod = "c";
    v.notification.stickerRemoteCallClass = "iu8.f";
    v.notification.stickerRemoteResponseClass = "iu8.p0";
    v.notification.stickerRemoteTargetFileField = "c";
    v.notification.combinationStickerMetadataFileManagerClass = "tr5.e";
    v.notification.combinationStickerMetadataFileMethod = "b";
    v.notification.combinationStickerMetadataWriteMethod = "d";
    v.notification.combinationStickerMetadataResponseClass = "vr5.e";
    v.notification.sticonImageRepositoryClass = "gj7.b";
    v.notification.sticonImageCacheImplementationClass = "xu5.b";
    v.notification.sticonImageCachePutMethod = "b";
    v.notification.sticonImageRepositoryFactoryField = "a";
    v.notification.sticonImageRepositoryFactoryMethod = "a";
    v.notification.sticonImageRepositoryCacheMethod = "a";
    v.notification.sticonCoroutineRequestClass = "vg7.r";
    v.notification.sticonCoroutineBuildersClass = "et8.i";
    v.notification.sticonCoroutineRunBlockingMethod = "b";
    v.notification.sticonCoroutineFunctionClass = "aq8.p";
    v.notification.sticonImageKeyClass = "hv5.g";
    v.notification.sticonPaidProductClass = "hv5.q$b";
    v.notification.sticonPaidClass = "hv5.d$d";
    v.notification.sticonOptionTypeClass = "hv5.i";
    v.notification.fileProviderHelperClass =
        "jp.naver.line.android.common.LineCommonFileProvider$a";
    v.notification.fileProviderUriMethod = "d";

    v.notificationFix.lineFcmServiceClass =
        "jp.naver.line.android.service.fcm.LineFirebaseMessagingService";
    v.notificationFix.lineFcmDispatchMethod = "d";
    v.notificationFix.lineFcmOwnershipMethod = "g";
    v.notificationFix.lineFcmTokenMethod = "e";
    v.notificationFix.lineFcmServiceBaseClass = "iz.i";
    v.notificationFix.firebaseRemoteMessageClass = "iz.t0";
    v.notificationFix.firebaseReceiverClass = "com.google.firebase.iid.FirebaseInstanceIdReceiver";
    v.notificationFix.firebaseReceiverMethod = "a";
    v.notificationFix.firebaseReceiverEnvelopeClass = "dr.a";
    v.notificationFix.firebaseReceiverIntentField = "a";
    v.notificationFix.firebaseDispatcherClass = "iz.n";
    v.notificationFix.firebaseDispatcherSingletonField = "d";
    v.notificationFix.firebaseDispatcherMethod = "b";
    v.notificationFix.firebaseDispatcherContextField = "a";
    v.notificationFix.firebaseDispatcherQueueField = "d";
    v.notificationFix.firebaseBindDeliveryClass = "iz.o1";
    v.notificationFix.firebaseBindDeliveryMethod = "b";
    v.notificationFix.firebaseMessagingServiceClass =
        "com.google.firebase.messaging.FirebaseMessagingService";
    v.notificationFix.firebaseMessagingHandleMethod = "c";
    v.notificationFix.firebaseWakefulStartClass = "iz.j1";
    v.notificationFix.firebaseWakefulStartMethod = "c";
    v.notificationFix.firebaseCompletedTaskClass = "qt.n";
    v.notificationFix.firebaseCompletedTaskMethod = "e";
    v.notificationFix.firebaseMessagingClass = "com.google.firebase.messaging.FirebaseMessaging";
    v.notificationFix.firebaseMessagingGetTokenMethod = "a";
    v.notificationFix.firebaseMessagingTokenFreshMethod = "i";
    v.notificationFix.firebaseAppClass = "ux.e";
    v.notificationFix.firebaseAppGetInstanceMethod = "c";
    v.foregroundKeepAlive.serviceClass = "androidx.work.impl.foreground.SystemForegroundService";
    v.notificationFix.legyStreamingStateClass = "com.linecorp.legy.streaming.h$a";
    v.notificationFix.legyStreamingLifecycleClass = "com.linecorp.legy.streaming.h$d";
    v.notificationFix.legyStreamingLifecycleMethod = "e1";
    v.notificationFix.legyLifecycleOwnerClass = "androidx.lifecycle.u0";
    v.notificationFix.legyLifecycleEventClass = "androidx.lifecycle.f0$a";
    v.notificationFix.legyBackgroundStateField = "BACKGROUND";
    v.notificationFix.legyDisconnectRunnableClass = "ta0.j";
    v.notificationFix.legyStateField = "q";
    v.notificationFix.legyTimeoutField = "s";
    v.notificationFix.legyBackgroundWorkerFlagField = "u";
    v.notificationFix.legyHandlerField = "c";
    v.notificationFix.legyRunnableField = "t";
    v.notificationFix.fisCertDigestClass = "pr.a";
    v.notificationFix.fisCertDigestMethod = "a";
    v.notificationFix.fisCertSha1 = "89396DC419292473972813922867E6973D6F5C50";
    v.notificationFix.gmsSignatureCheckClass = "er.k";
    v.notificationFix.gmsSignatureCheckMethod = "b";
    v.notificationFix.gmsAvailabilityClass = "er.j";
    v.notificationFix.gmsAvailabilityMethod = "e";

    v.talkTabHeader.chatTabHeaderStateClass = "m52.f";
    v.talkTabHeader.iconListStateField = "y";
    v.talkTabHeader.buttonListStateField = "D";
    v.talkTabHeader.iconTypeClass = "a71.q";
    v.talkTabHeader.iconTypeFieldInButton = "a";
    v.talkTabHeader.subDeviceOpenChatButtonClass = "s32.c$f";
    v.talkTabHeader.subDeviceAlbumButtonClass = "s32.c$b";

    v.searchBarAgentI.talkVisibleMethod = "x";
    v.searchBarAgentI.talkClickMethod = "u";
    v.searchBarAgentI.homeSearchBarClass = "y85.i";
    v.searchBarAgentI.homeRefreshMethod = "e";
    v.searchBarAgentI.homeRootViewField = "c";
    v.searchBarAgentI.homeTabTypeField = "b";
    v.searchBarAgentI.homeTabName = "HOME";
    v.searchBarAgentI.homeTabV2Name = "HOME_V2";
    v.searchBarAgentI.chatTabName = "CHAT";
    v.searchBarAgentI.newsTabName = "NEWS";
    v.searchBarAgentI.homeAiContainerId = 0x7f0b164c;
    v.searchBarAgentI.homeGuidelineId = 0x7f0b164e;
    v.searchBarAgentI.homeGuidelineEndDp = 55;
    v.searchBarAgentI.homeGuidelineClass = "androidx.constraintlayout.widget.Guideline";
    v.searchBarAgentI.miniTabHeaderClass =
        "com.linecorp.line.wallet.impl.v3.view.WalletV3GrandDesignHeaderView";
    v.searchBarAgentI.miniTabAgentMethod = "o";
    v.searchBarAgentI.commerceHeaderClass = "com.linecorp.line.commerce.impl.c";
    v.searchBarAgentI.commerceHeaderMethod = "d";
    v.searchBarAgentI.imageViewerAiButtonClass = "v88.f0";
    v.home26NavIcon.rendererClass = "lm2.n";
    v.home26NavIcon.rendererMethod = "b";
    v.home26NavIcon.agentDrawableId = 0x7f080b9f;
    v.home26NavIcon.settingsDrawableId = 0x7f081298;

    v.compose.composerClass = "j3.r";
    v.compose.clickableClass = "u1.h0";
    v.compose.methodClickable = "a";
    v.compose.methodCombinedClickable = "d";
    v.compose.onGloballyPositionedClass = "z4.y1";
    v.compose.methodOnGloballyPositioned = "a";
    v.compose.layoutCoordinatesClass = "z4.b0";
    v.compose.methodLocalToWindow = "k";
    v.compose.methodCoordinatesSize = "a";

    v.kotlin.unitClass = "ip8.i0";
    v.kotlin.fieldUnitInstance = "a";

    v.agentIInChat.toggleComposableClass = "ho1.k";

    v.aiIcon.repoClass = "e91.c";
    v.aiIcon.methodGetShownAfterMillis = "x";

    v.imageQuality.qualityProfileHighClass = "xm8.a$b$a";
    v.imageQuality.qualityProfileMediumClass = "xm8.a$b$b";
    v.imageQuality.methodGetMaxDimension = "a";
    v.imageQuality.methodGetQuality = "b";
    v.imageQuality.imageUtilClass = "jp.naver.line.android.util.y0";

    v.profile.g50fClass = "sc0.f";
    v.profile.h13baClass = "zo3.b";
    v.profile.fieldH3 = "sd";
    v.profile.g50aClass = "sc0.a";
    v.profile.methodGetProfile = "getProfile";
    v.profile.fieldMid = "b";

    v.profileTimestamps.activityClass = "com.linecorp.line.userprofile.impl.UserProfileActivity";
    v.profileTimestamps.midExtraKey = "USER_PROFILE_MID";
    v.profileTimestamps.resHeaderButtonContainer = "user_profile_header_button_binding";

    v.media.videoDurationCheckClass = "dg1.b";
    v.media.videoDurationCheckMethod = "c";
    v.media.mediaPickerParamsClass = "com.linecorp.line.media.picker.b$i";
    v.media.fieldMediaPickerMaxVideoDuration = "y";
    v.media.droppedMediaPreprocessorClass = "r31.b";
    v.media.videoDurationSuccessClass = "eg1.a$c";
    v.media.fieldVideoDurationSuccess = "a";
    v.media.galleryViewClass = "sp1.u";
    v.media.fieldGalleryDurationLimit = "Y";
    v.media.selectionValidatorClass = "ui3.s";
    v.media.selectionValidatorMethod = "n";
    v.media.selectionValidatorParamClass = "f72.c";
    v.media.videoProfileTrimmerActivityClass =
        "jp.naver.line.android.activity.setting.videoprofile.trim.VideoProfileTrimmerActivity";
    v.media.fieldVideoProfileTrimmerLimit = "O";

    v.chat.searchHeaderHelperClass = "ry1.w";
    v.chat.searchHeaderShowMethod = "b";
    v.chat.searchHeaderControllerField = "n";
    v.chat.searchHeaderEventBusField = "c";
    v.chat.searchControllerSearchBoxMethod = "d";
    v.chat.searchPresenterClass = "vy1.n";
    v.chat.searchKeywordTypeClass = "ua1.c";
    v.chat.searchKeywordTypeMethod = "shouldTriggerSearch";
    v.chat.searchResultClass = "ua1.h";
    v.chat.searchResultCtorArgs = "chatId,keyword,idList,count";
    v.chat.searchResultWrapperClass = "ua1.i";
    v.chat.searchBoxViewClass = "jp.naver.line.android.customview.SearchBoxView";
    v.chat.searchBoxEditTextField = "c";
    v.chat.searchBoxIconField = "e";
    v.chat.searchKeywordEventClass = "qy1.b";
    v.chat.searchKeywordEventKeywordField = "a";
    v.chat.searchPresenterKeywordChangedMethod = "onSearchInChatKeywordChangedEventReceived";
    v.chat.searchPresenterKeywordSubjectField = "A";
    v.chat.searchKeywordSubjectValueMethod = "w";
    v.chat.searchResultWrapperResultOptionalField = "c";
    v.chat.searchResultCountField = "d";
    v.chat.searchResultTitleViewHolderClass = "yy1.h";
    v.chat.searchResultTitleBindMethod = "H0";
    v.chat.searchResultTitleBindingField = "x";
    v.chat.searchResultTitleTextViewField = "b";
    v.chat.searchFtsInChatQueryClass = "fe2.r";
    v.chat.searchFtsQueryField = "a";
    v.chat.searchFtsChatIdField = "b";
    v.chat.searchFtsLimitField = "c";
    v.chat.searchFtsPrepareMethod = "r2";
    v.chat.searchFtsBindTextMethod = "J2";
    v.chat.searchFtsStepMethod = "n2";

    v.chatJump.requestClass = "com.linecorp.line.chat.request.ChatHistoryRequest";
    v.chatJump.launchActivityClass =
        "jp.naver.line.android.activity.chathistory.ChatHistoryActivityLaunchActivity";
    v.chatJump.requestExtraKey = "chatHistoryRequest";

    v.chatTimestamp.displayTimeInterface = "te1.f";
    v.chatTimestamp.methodCreatedMillis = "a";

    v.chatEditSelectAll.selectionProviderClass = "le1.c";
    v.chatEditSelectAll.selectionStateClass = "le1.d";
    v.chatEditSelectAll.methodGetSelectionState = "e0";
    v.chatEditSelectAll.methodGetItem = "h0";
    v.chatEditSelectAll.methodGetSelectedIds = "d";
    v.chatEditSelectAll.methodToggleItem = "g";
    v.chatEditSelectAll.methodIsItemSelected = "e";

    v.messageEditHistory.editRequestClass = "mh8.h";
    v.messageEditHistory.editRequestIdField = "b";
    v.messageEditHistory.editRequestTextField = "d";
    v.messageEditHistory.menuListBuilderClass = "rm1.b2";
    v.messageEditHistory.menuListMethod = "a";
    v.messageEditHistory.menuItemEnumClass = "jd1.c";
    v.messageEditHistory.menuPresentationEnumClass = "rm1.c1";
    v.messageEditHistory.methodMenuLabel = "getContextMenuButtonText";
    v.messageEditHistory.methodMenuIcon = "getContextIconRes";
    v.messageEditHistory.methodMenuActionAccessor = "getButtonAction";
    v.messageEditHistory.menuActionLambdaClass = "id1.f$b";
    v.messageEditHistory.menuContextMessageField = "b";
    v.messageEditHistory.menuMessageDataField = "b";
    v.messageEditHistory.menuMessageIdField = "c";
    v.messageEditHistory.menuEditedFlagField = "x";

    v.camera.cameraModuleClass = "ie2.g";
    v.camera.methodUseExternalCamera = "d";
    v.camera.cameraLauncherClass = "com.linecorp.line.media.picker.b";
    v.camera.methodLaunchCamera = "b";
    v.camera.launchModeClass = "com.linecorp.line.media.picker.b$l";
    v.camera.launchSourceClass = "com.linecorp.line.media.picker.b$k";
    v.camera.launchCallbackClass = "com.linecorp.line.media.picker.b$h";
    v.camera.captureChoiceClass = "com.linecorp.line.media.picker.a";
    v.camera.captureChooserClass = "ry0.d0";
    v.camera.methodShowCaptureChooser = "a";
    v.camera.schemeServiceActivity =
        "jp.naver.line.android.activity.schemeservice.LineSchemeServiceActivity";

    v.callTone.toneSourceClass = "a40.g";
    v.callTone.uriToneSourceClass = "a40.b";
    v.callTone.methodToneUri = "a";
    v.callTone.remoteRingbackClass = "qs7.b";
    v.callTone.remoteRingbackContextField = "b";
    v.callTone.remoteRingbackFallbackField = "d";
    v.callTone.ringtoneWrapperClass = "qs7.c";

    v.muteMessage.labFeatureClass = "je8.d";
    v.muteMessage.methodIsFeatureEnabled = "c";
    v.muteMessage.silentMessageFeatureClass = "je8.j0";
    v.muteMessage.sendModeClass = "xt1.c";
    v.muteMessage.methodSendMode = "a";
    v.muteMessage.sendModeEnumClass = "w02.a";
    v.muteMessage.silentFlagWriterClass = "hn8.i1";
    v.muteMessage.methodWriteSilentFlag = "a";

    v.iab.inAppBrowserActivityClass = "com.linecorp.line.iab.browser.impl.InAppBrowserActivity";

    v.homeTab.tabListProviderClass = "ad8.g";
    v.homeTab.methodBuildTabList = "a";
    v.homeTab.mainTabEnumClass = "jp.naver.line.android.activity.main.a";

    v.nightMode.nightModeConfiguratorClass = "z60.a";
    v.nightMode.methodApplyNightMode = "a";
    v.nightMode.fieldSystemDarkMode = "a";
    v.nightMode.inputPassActivityClass = "com.linecorp.line.passlock.InputPassActivity";
    v.nightMode.darkThemeManagerClass = "rg6.k";
    v.nightMode.methodIsDarkTheme = "m";
    v.nightMode.methodThemeMode = "A";
    v.nightMode.methodIsDefaultTheme = "B";

    return v;
  }
}
