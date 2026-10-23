package app.zipper.knot.versions;

import app.zipper.knot.LineVersion;

public class Version26160 {
  public static LineVersion.Config create() {
    LineVersion.Config v = new LineVersion.Config();

    v.main.mainActivity = "jp.naver.line.android.activity.main.MainActivity";
    v.main.baseMainTabFragment = "jp.naver.line.android.activity.main.BaseMainTabFragment";
    v.main.headerButton = "jp.naver.line.android.common.view.header.HeaderButton";
    v.main.headerButtonTypeClass = "cm8.d";
    v.main.slotFarLeft = "FAR_LEFT";
    v.main.headerInterfaceA = "jp.naver.line.android.common.view.header.a";
    v.main.fieldHeaderHelper = "e";
    v.main.fieldChatActivity = "a";
    v.main.methodSetHeaderButton = "i";
    v.main.methodSetHeaderLabel = "k";
    v.main.methodSetHeaderButtonVisibility = "s";
    v.main.methodGetHeaderButtonView = "h";
    v.main.methodSetButtonImageDrawable = "setButtonImageViewDrawable";
    v.main.methodSetHeaderOnClickListener = "r";
    v.main.methodRefreshNavHeader = "a";
    v.main.methodHeaderSetTitle = "setTitle";
    v.main.methodHeaderSetButtonVisibility =
        "setUpButtonVisibility$LINE_Android_migrant_common_libs";
    v.main.methodHeaderSetButtonListener =
        "setUpButtonOnClickListener$LINE_Android_migrant_common_libs";

    v.settings.mainSettingsFragmentClass =
        "com.linecorp.line.settings.main.LineUserMainSettingsFragment";
    v.settings.settingsAdapterClass = "ji8.f";
    v.settings.settingsItemClass = "ji8.f$c";
    v.settings.settingsBaseAdapterClass = "ji8.f$b";
    v.settings.settingsSearchHelperClass = "eh5.b";
    v.settings.settingsAdapterWrapperClass = "dc5.a";
    v.settings.settingsHeaderItemClass = "ec5.x";
    v.settings.settingsRowItemClass = "ec5.b0";
    v.settings.settingsHandlerBaseClass = "ec5.k0";
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

    v.plusMenu.plusMenuComponentClass = "q61.n";
    v.plusMenu.plusMenuComposerImplClass = "j3.d1";
    v.plusMenu.plusMenuCallbackClass = "nt8.a";
    v.plusMenu.plusMenuOnClickItemClass = "nt8.l";
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
    v.chatListMoreMenu.clickListenerClass = "j32.a";
    v.chatListMoreMenu.methodAddItem = "a";

    v.readReceipt.readReceiptManagerClass = "qg3.e";
    v.readReceipt.methodSendReadReceipt = "d";
    v.readReceipt.methodExecuteReadReceiptAsync = "e";
    v.readReceipt.methodReadAll = "c";
    v.readReceipt.methodResolveReadTarget = "a";
    v.readReceipt.operationNotifiedReadName = "NOTIFIED_READ_MESSAGE";
    v.readReceipt.longPressReadClass = "w42";
    v.readReceipt.reactClientClass =
        "jp.naver.line.android.thrift.client.impl.TalkServiceClientImpl";
    v.readReceipt.methodReact = "p0";
    v.readReceipt.reactRequestMessageIdField = "b";
    v.readReceipt.reactSuccessResultClass = "gr8.a1$b";

    v.unsend.notifiedReadMessageHandlerClass = "uq8.z1";
    v.unsend.notifiedSendReactionHandlerClass = "uq8.k2";
    v.unsend.chatMessageViewHolderClass = "lq1.i";
    v.unsend.methodReadBuffer = "b";
    v.unsend.methodBind = "U";
    v.unsend.methodOperationTypeValueOf = "findByValue";
    v.unsend.methodBindIndex = 1;
    v.unsend.methodGetItemView = "c0";
    v.unsend.methodGetCommonData = "b";
    v.unsend.carouselAdapterClass = "qq1.a";
    v.unsend.methodCarouselBindViewHolder = "onBindViewHolder";
    v.unsend.operationTypeDummy = 40;
    v.unsend.chatServiceConfigClass = "ya5.u";
    v.unsend.methodUnsendLimit = "k";
    v.unsend.methodUnsendPremiumLimit = "j";
    v.unsend.appInfoProviderClass = "wp8.d";
    v.unsend.methodGetFullUserAgent = "h";
    v.unsend.methodGetSimpleUserAgent = "k";
    v.unsend.methodGetFullUserAgentWithContext = "i";
    v.unsend.methodGetSimpleUserAgentWithContext = "l";
    v.unsend.methodUnsendThrift = "unsendMessage";
    v.unsend.methodUnsendThriftSilent = "silentlyUnsendMessage";
    v.unsend.operationTypeField = "c";
    v.unsend.operationParam1Field = "g";
    v.unsend.operationParam2Field = "h";
    v.unsend.operationParam3Field = "i";
    v.unsend.operationCreatedTimeField = "b";
    v.unsend.chatMessageIdField = "d";
    v.unsend.chatMessageServerIdLongField = "c";
    v.unsend.operationUnsendName = "DESTROY_MESSAGE";
    v.unsend.operationNotifiedUnsendName = "NOTIFIED_DESTROY_MESSAGE";
    v.unsend.unsendDestroyHandlerClass = "uq8.z0";
    v.unsend.destroyMessageHandlerClass = "uq8.r";
    v.unsend.methodDestroyHandler = "b";
    v.unsend.messageConverterClass = "wk8.u2";
    v.unsend.methodConvertMessage = "a";
    v.unsend.incomingMessageIdField = "c";
    v.unsend.incomingMessageTypeField = "d";
    v.unsend.incomingMessageParamsField = "o";
    v.unsend.incomingMessageParamsMapField = "a";
    v.unsend.incomingMessageTypeEnumClass = "so8.g$b";
    v.unsend.handlerSuccessResultClass = "tq8.a$a$c";

    v.thrift.talkServiceClientImplClass =
        "jp.naver.line.android.thrift.client.impl.LegacyTalkServiceClientImpl";
    v.thrift.talkServiceClientInterface = "jp.naver.line.android.thrift.client.TalkServiceClient";
    v.thrift.v1 = "c1";
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

    v.home.home26FeedTypePrefixes =
        "HomeFeed,HomeContentsRecommendation,GlobalHomePage,GlobalHomeDefault,AdModel,HomePerformanceAd";
    v.home.home26ServiceTypePrefixes = "HomeServiceList,GlobalHomeServiceSection";
    v.home.home26LoadingMoreDataClass = "eh2.g$a";
    v.home.home26ModuleBodyField = "e";
    v.home.home26ModuleTypeMethod = "getType";

    v.chat.headerController = "wk1.t0";
    v.chat.headerHelper = "jp.naver.line.android.common.view.header.b";
    v.chat.chatIdField = "j";
    v.chat.methodGetChatId = "s";

    v.chatHeader.chatHistoryActivity =
        "jp.naver.line.android.activity.chathistory.ChatHistoryActivity";
    v.chatHeader.fieldChatConfigChatId = "dg1.a";
    v.chatHeader.fieldChatConfigIsMuted = "bg1.a";
    v.chatHeader.fieldChatConfigType = "wk1.k0";
    v.chatHeader.fieldAppInfoVersion =
        "com.linecorp.line.chat.ui.impl.officialaccount.OaChatStatusBarViewModel";
    v.chatHeader.fieldAppInfoPkg = "fc1.a";
    v.chatHeader.fieldAppInfoId = "rw0.d";

    v.font.fontConfigClass = "j7.l";
    v.font.fontManagerClass = "j7.k";
    v.font.fontCallbackClass = "j7.l$c";
    v.font.fontInjectedClass = "fx4.n";
    v.font.methodGetFontConfig = "a";
    v.font.methodGetFontSettings = "c";
    v.font.methodOnFontChanged = "b";
    v.font.fontRequestExecutorClass = "j7.n";
    v.font.fontCallbackWithHandlerClass = "j7.c";

    v.res.idSettingList = 0x7f0b2276;
    v.res.idPersonalInfo = 0x7f153aa6;
    v.res.typeSection = 0x7f0e0531;
    v.res.typeRow = 0x7f0e0534;
    v.res.idIcon = 0x7f0b2267;
    v.res.idDesc = 0x7f0b2259;
    v.res.idMark = 0x7f0b227a;
    v.res.idSeparator = 0x7f0b22a3;
    v.res.idArrow = 0x7f0b2241;
    v.res.idNewMark = 0x7f0b18e3;
    v.res.idNoticeDot = 0x7f0b1952;
    v.res.idTitle = 0x7f0b22ac;
    v.res.layoutCheckbox = 0x7f0e0525;
    v.res.layoutSectionHeader = 0x7f0e0531;
    v.res.layoutSettingsMain = 0x7f0e052b;
    v.res.idHeader = 0x7f0b10f8;
    v.res.idTimestamp = 0x7f0b0864;
    v.res.resSettingsHeaderBtn = "settings_header_button";
    v.res.resSettingsBtn = "settings_button";
    v.res.resTooltipBackground = "home_tooltip_background";
    v.res.resTooltipArrowUp = "home_tooltip_arrow_up";

    v.notification.accessServiceCallbackClass =
        "jp.naver.line.android.access.remote.LineAccessServiceForNotification$b";
    v.notification.accessServiceCallbackMethod = "g";
    v.notification.chatHistoryRequestClass = "com.linecorp.line.chat.request.ChatHistoryRequest";
    v.notification.chatHistoryActivityLaunchActivityClass =
        "jp.naver.line.android.activity.chathistory.ChatHistoryActivityLaunchActivity";

    v.notification.decryptedResultClass = "uq8.z2$b$a";
    v.notification.messageClass = "ts8.od";
    v.notification.messageServerIdField = "d";
    v.notification.messageContentTypeField = "j";
    v.notification.messageTextField = "g";
    v.notification.messageMetadataField = "k";
    v.notification.messageMetadataKeyClass = "pn8.b$c";
    v.notification.messageMetadataStringMethod = "u";
    v.notification.messageObsPopField = "OBS_POP";
    v.notification.messageVisualBuilderClass = "xk8.m";
    v.notification.messageVisualEncryptionMethod = "m";
    v.notification.messageThumbnailRequestClass = "e80.d";
    v.notification.messageObsEncryptionDataClass = "lg1.a$c";
    v.notification.messageSquareUtilsClass = "com.linecorp.square.chat.SquareChatUtils";
    v.notification.messageSquareCheckMethod = "b";
    v.notification.messageProcessorClass = "uq8.z2";
    v.notification.messageProcessorMethod = "g";
    v.notification.messageProcessorContentTypeClass = "ts8.q7";
    v.notification.messageProcessorMetadataClass = "pn8.b";
    v.notification.messageProcessorAuxClass = "so8.m";
    v.notification.messageProcessorContinuationClass = "dt8.d";
    v.notification.messageContentCacheClass = "l31.i";
    v.notification.messageContentCacheInitializeMethod = "F";
    v.notification.messageContentCacheKeyClass = "j31.c";
    v.notification.messageThumbnailCacheFileMethod = "e";
    v.notification.glideClass = "com.bumptech.glide.c";
    v.notification.glideWithContextMethod = "e";
    v.notification.glideRetrieverMethod = "c";
    v.notification.glideRetrieverGetMethod = "f";
    v.notification.glideAsFileMethod = "p";
    v.notification.glideLoadMethod = "b0";
    v.notification.glideSubmitMethod = "g0";
    v.notification.glideClearMethod = "n";
    v.notification.stickerFileManagerClass = "su5.f";
    v.notification.stickerInitializeMethod = "F";
    v.notification.stickerMainCacheFileMethod = "n";
    v.notification.stickerRequestFactoryClass = "dt5.a";
    v.notification.stickerRequestFactoryField = "a";
    v.notification.stickerServiceLocatorClass = "sc0.f";
    v.notification.stickerServiceLocatorMethod = "a";
    v.notification.stickerResourceClass = "zu5.d";
    v.notification.stickerSecretClass = "zu5.e";
    v.notification.stickerOptionClass = "zu5.s";
    v.notification.stickerStaticOptionField = "STATIC";
    v.notification.stickerMainRequestMethod = "i";
    v.notification.stickerRemoteFetcherClass = "di2.g";
    v.notification.stickerRemoteCompleteMethod = "f";
    v.notification.stickerRemoteCallClass = "vx8.f";
    v.notification.stickerRemoteResponseClass = "vx8.m0";
    v.notification.stickerRemoteTargetFileField = "c";
    v.notification.combinationStickerMetadataFileManagerClass = "bs5.e";
    v.notification.combinationStickerMetadataFileMethod = "b";
    v.notification.combinationStickerMetadataWriteMethod = "d";
    v.notification.combinationStickerMetadataResponseClass = "ds5.e";
    v.notification.sticonImageRepositoryClass = "ck7.c";
    v.notification.sticonImageCacheImplementationClass = "fv5.b";
    v.notification.sticonImageCachePutMethod = "a";
    v.notification.sticonImageRepositoryFactoryField = "a";
    v.notification.sticonImageRepositoryFactoryMethod = "a";
    v.notification.sticonImageRepositoryCacheMethod = "a";
    v.notification.sticonCoroutineRequestClass = "rh7.r";
    v.notification.sticonCoroutineBuildersClass = "rw8.h";
    v.notification.sticonCoroutineRunBlockingMethod = "b";
    v.notification.sticonCoroutineFunctionClass = "nt8.p";
    v.notification.sticonImageKeyClass = "pv5.g";
    v.notification.sticonPaidProductClass = "pv5.q$b";
    v.notification.sticonPaidClass = "pv5.d$d";
    v.notification.sticonOptionTypeClass = "pv5.i";
    v.notification.fileProviderHelperClass =
        "jp.naver.line.android.common.LineCommonFileProvider$a";
    v.notification.fileProviderUriMethod = "d";

    v.notificationFix.lineFcmServiceClass =
        "jp.naver.line.android.service.fcm.LineFirebaseMessagingService";
    v.notificationFix.lineFcmDispatchMethod = "d";
    v.notificationFix.lineFcmOwnershipMethod = "g";
    v.notificationFix.lineFcmTokenMethod = "e";
    v.notificationFix.lineFcmServiceBaseClass = "hz.i";
    v.notificationFix.firebaseRemoteMessageClass = "hz.t0";
    v.notificationFix.firebaseReceiverClass = "com.google.firebase.iid.FirebaseInstanceIdReceiver";
    v.notificationFix.firebaseReceiverMethod = "a";
    v.notificationFix.firebaseReceiverEnvelopeClass = "cr.a";
    v.notificationFix.firebaseReceiverIntentField = "a";
    v.notificationFix.firebaseDispatcherClass = "hz.n";
    v.notificationFix.firebaseDispatcherSingletonField = "d";
    v.notificationFix.firebaseDispatcherMethod = "b";
    v.notificationFix.firebaseDispatcherContextField = "a";
    v.notificationFix.firebaseDispatcherQueueField = "d";
    v.notificationFix.firebaseBindDeliveryClass = "hz.o1";
    v.notificationFix.firebaseBindDeliveryMethod = "b";
    v.notificationFix.firebaseMessagingServiceClass =
        "com.google.firebase.messaging.FirebaseMessagingService";
    v.notificationFix.firebaseMessagingHandleMethod = "c";
    v.notificationFix.firebaseWakefulStartClass = "hz.j1";
    v.notificationFix.firebaseWakefulStartMethod = "c";
    v.notificationFix.firebaseCompletedTaskClass = "pt.n";
    v.notificationFix.firebaseCompletedTaskMethod = "e";
    v.notificationFix.firebaseMessagingClass = "com.google.firebase.messaging.FirebaseMessaging";
    v.notificationFix.firebaseMessagingGetTokenMethod = "a";
    v.notificationFix.firebaseMessagingTokenFreshMethod = "i";
    v.notificationFix.firebaseAppClass = "tx.e";
    v.notificationFix.firebaseAppGetInstanceMethod = "c";
    v.foregroundKeepAlive.serviceClass = "androidx.work.impl.foreground.SystemForegroundService";
    v.notificationFix.legyStreamingStateClass = "com.linecorp.legy.streaming.h$a";
    v.notificationFix.legyStreamingLifecycleClass = "com.linecorp.legy.streaming.h$d";
    v.notificationFix.legyStreamingLifecycleMethod = "e1";
    v.notificationFix.legyStreamingManagerField = "a";
    v.notificationFix.legyLifecycleOwnerClass = "androidx.lifecycle.u0";
    v.notificationFix.legyLifecycleEventClass = "androidx.lifecycle.f0$a";
    v.notificationFix.legyBackgroundStateField = "BACKGROUND";
    v.notificationFix.legyDisconnectRunnableClass = "ta0.h";
    v.notificationFix.legyStateField = "q";
    v.notificationFix.legyTimeoutField = "s";
    v.notificationFix.legyBackgroundWorkerFlagField = "u";
    v.notificationFix.legyHandlerField = "c";
    v.notificationFix.legyRunnableField = "t";
    v.notificationFix.fisCertDigestClass = "or.a";
    v.notificationFix.fisCertDigestMethod = "a";
    v.notificationFix.fisCertSha1 = "89396DC419292473972813922867E6973D6F5C50";
    v.notificationFix.gmsSignatureCheckClass = "dr.k";
    v.notificationFix.gmsSignatureCheckMethod = "b";
    v.notificationFix.gmsAvailabilityClass = "dr.j";
    v.notificationFix.gmsAvailabilityMethod = "e";

    v.talkTabHeader.chatTabHeaderStateClass = "d52.f";
    v.talkTabHeader.iconListStateField = "y";
    v.talkTabHeader.buttonListStateField = "D";
    v.talkTabHeader.iconTypeClass = "o61.n";
    v.talkTabHeader.iconTypeFieldInButton = "a";
    v.talkTabHeader.subDeviceOpenChatButtonClass = "j32.c$f";
    v.talkTabHeader.subDeviceAlbumButtonClass = "j32.c$b";

    v.searchBarAgentI.talkVisibleMethod = "x";
    v.searchBarAgentI.talkClickMethod = "u";
    v.searchBarAgentI.homeSearchBarClass = "d95.j";
    v.searchBarAgentI.homeRefreshMethod = "e";
    v.searchBarAgentI.homeRootViewField = "c";
    v.searchBarAgentI.homeTabTypeField = "b";
    v.searchBarAgentI.homeTabName = "HOME";
    v.searchBarAgentI.homeTabV2Name = "HOME_V2";
    v.searchBarAgentI.chatTabName = "CHAT";
    v.searchBarAgentI.newsTabName = "NEWS";
    v.searchBarAgentI.homeAiContainerId = 0x7f0b1627;
    v.searchBarAgentI.homeGuidelineId = 0x7f0b1629;
    v.searchBarAgentI.homeGuidelineEndDp = 55;
    v.searchBarAgentI.homeGuidelineClass = "androidx.constraintlayout.widget.Guideline";
    v.searchBarAgentI.miniTabHeaderClass =
        "com.linecorp.line.wallet.impl.v3.view.WalletV3GrandDesignHeaderView";
    v.searchBarAgentI.miniTabAgentMethod = "o";
    v.searchBarAgentI.commerceHeaderClass = "com.linecorp.line.commerce.impl.c";
    v.searchBarAgentI.commerceHeaderMethod = "d";
    v.searchBarAgentI.imageViewerAiButtonClass = "hc8.s0";
    v.searchBarAgentI.chatImageAiButtonClass = "zr1.d";
    v.searchBarAgentI.chatImageAiButtonMethod = "a";
    v.home26NavIcon.rendererClass = "fm2.n";
    v.home26NavIcon.rendererMethod = "b";
    v.home26NavIcon.agentDrawableId = 0x7f080b92;
    v.home26NavIcon.settingsDrawableId = 0x7f081291;

    v.compose.composerClass = "j3.r";
    v.compose.clickableClass = "u1.m0";
    v.compose.methodClickable = "a";
    v.compose.methodCombinedClickable = "d";
    v.compose.onGloballyPositionedClass = "z4.x1";
    v.compose.methodOnGloballyPositioned = "a";
    v.compose.layoutCoordinatesClass = "z4.b0";
    v.compose.methodLocalToWindow = "k";
    v.compose.methodCoordinatesSize = "b";

    v.kotlin.unitClass = "vs8.i0";
    v.kotlin.fieldUnitInstance = "a";
    v.kotlin.continuationClass = "kotlin.coroutines.Continuation";

    v.agentIInChat.toggleComposableClass = "vn1.h";

    v.aiIcon.repoClass = "s81.c";
    v.aiIcon.methodGetShownAfterMillis = "v";

    v.imageQuality.qualityProfileHighClass = "jq8.a$b$a";
    v.imageQuality.qualityProfileMediumClass = "jq8.a$b$b";
    v.imageQuality.methodGetMaxDimension = "a";
    v.imageQuality.methodGetQuality = "b";
    v.imageQuality.imageUtilClass = "jp.naver.line.android.util.g1";

    v.profile.g50fClass = "sc0.f";
    v.profile.h13baClass = "ap3.b";
    v.profile.fieldH3 = "k0";
    v.profile.g50aClass = "sc0.a";
    v.profile.serviceLocatorMethod = "a";
    v.profile.methodGetProfile = "getProfile";
    v.profile.fieldMid = "b";

    v.profileTimestamps.activityClass = "com.linecorp.line.userprofile.impl.UserProfileActivity";
    v.profileTimestamps.midExtraKey = "USER_PROFILE_MID";
    v.profileTimestamps.resHeaderButtonContainer = "user_profile_header_button_binding";

    v.media.videoDurationCheckClass = "rf1.b";
    v.media.videoDurationCheckMethod = "c";
    v.media.videoDurationCheckWithSizeMethod = "a";
    v.media.videoDurationCheckWithoutSizeMethod = "b";
    v.media.mediaPickerParamsClass = "com.linecorp.line.media.picker.b$i";
    v.media.fieldMediaPickerMaxVideoDuration = "y";
    v.media.droppedMediaPreprocessorClass = "e31.b";
    v.media.videoDurationSuccessClass = "sf1.a$c";
    v.media.fieldVideoDurationSuccess = "a";
    v.media.galleryViewClass = "hp1.c0";
    v.media.fieldGalleryDurationLimit = "Y";
    v.media.selectionValidatorClass = "vi3.r";
    v.media.selectionValidatorMethod = "n";
    v.media.selectionValidatorParamClass = "x62.b";
    v.media.videoProfileTrimmerActivityClass =
        "jp.naver.line.android.activity.setting.videoprofile.trim.VideoProfileTrimmerActivity";
    v.media.fieldVideoProfileTrimmerLimit = "O";

    v.chat.searchHeaderHelperClass = "hy1.v";
    v.chat.searchHeaderShowMethod = "b";
    v.chat.searchHeaderControllerField = "n";
    v.chat.searchHeaderEventBusField = "c";
    v.chat.searchHeaderViewField = "a";
    v.chat.searchEventBusPostMethod = "b";
    v.chat.searchControllerSearchBoxMethod = "d";
    v.chat.searchPresenterClass = "ly1.o";
    v.chat.searchKeywordTypeClass = "ia1.c";
    v.chat.searchKeywordTypeMethod = "shouldTriggerSearch";
    v.chat.searchResultClass = "ia1.h";
    v.chat.searchResultCtorArgs = "chatId,keyword,idList,count";
    v.chat.searchResultWrapperClass = "ia1.i";
    v.chat.searchBoxViewClass = "jp.naver.line.android.customview.SearchBoxView";
    v.chat.searchBoxEditTextField = "c";
    v.chat.searchBoxIconField = "e";
    v.chat.searchBoxSetEditingLayoutMethod = "setEditingLayout";
    v.chat.searchBoxGetTextMethod = "getSearchText";
    v.chat.searchKeywordEventClass = "gy1.b";
    v.chat.searchKeywordEventKeywordField = "a";
    v.chat.searchPresenterKeywordChangedMethod = "onSearchInChatKeywordChangedEventReceived";
    v.chat.searchPresenterKeywordSubjectField = "A";
    v.chat.searchKeywordSubjectValueMethod = "w";
    v.chat.searchResultWrapperResultOptionalField = "c";
    v.chat.searchResultCountField = "d";
    v.chat.searchResultTitleViewHolderClass = "oy1.k";
    v.chat.searchResultTitleBindMethod = "H0";
    v.chat.searchResultTitleBindingField = "x";
    v.chat.searchResultTitleTextViewField = "b";
    v.chat.searchResultHeaderItemClass = "fa1.c$e";
    v.chat.searchResultHeaderTitleField = "a";
    v.chat.searchResultHeaderCountField = "b";
    v.chat.searchFtsInChatQueryClass = "xd2.p";
    v.chat.searchFtsQueryField = "a";
    v.chat.searchFtsChatIdField = "b";
    v.chat.searchFtsLimitField = "c";
    v.chat.searchFtsPrepareMethod = "prepare";
    v.chat.searchFtsBindTextMethod = "bindText";
    v.chat.searchFtsStepMethod = "step";

    v.chatJump.requestClass = "com.linecorp.line.chat.request.ChatHistoryRequest";
    v.chatJump.launchActivityClass =
        "jp.naver.line.android.activity.chathistory.ChatHistoryActivityLaunchActivity";
    v.chatJump.requestExtraKey = "chatHistoryRequest";
    v.chatJump.requestChatIdMethod = "getChatId";

    v.chatTimestamp.displayTimeInterface = "he1.f";
    v.chatTimestamp.methodCreatedMillis = "a";

    v.chatEditSelectAll.selectionProviderClass = "zd1.c";
    v.chatEditSelectAll.selectionStateClass = "zd1.d";
    v.chatEditSelectAll.methodGetSelectionState = "n";
    v.chatEditSelectAll.methodGetItem = "q";
    v.chatEditSelectAll.methodGetSelectedIds = "d";
    v.chatEditSelectAll.methodToggleItem = "k";
    v.chatEditSelectAll.methodIsItemSelected = "l";

    v.messageEditHistory.editRequestClass = "yk8.h";
    v.messageEditHistory.editRequestIdField = "b";
    v.messageEditHistory.editRequestTextField = "d";
    v.messageEditHistory.menuListBuilderClass = "fm1.f2";
    v.messageEditHistory.menuListMethod = "a";
    v.messageEditHistory.menuItemEnumClass = "xc1.c";
    v.messageEditHistory.menuPresentationEnumClass = "fm1.d1";
    v.messageEditHistory.methodMenuLabel = "getContextMenuButtonText";
    v.messageEditHistory.methodMenuIcon = "getContextIconRes";
    v.messageEditHistory.methodMenuActionAccessor = "getButtonAction";
    v.messageEditHistory.menuActionLambdaClass = "wc1.f$b";
    v.messageEditHistory.menuContextMessageField = "b";
    v.messageEditHistory.menuMessageDataField = "b";
    v.messageEditHistory.menuMessageIdField = "c";
    v.messageEditHistory.menuEditedFlagField = "x";

    v.camera.cameraModuleClass = "ae2.j";
    v.camera.methodUseExternalCamera = "c";
    v.camera.cameraLauncherClass = "com.linecorp.line.media.picker.b";
    v.camera.methodLaunchCamera = "b";
    v.camera.launchModeClass = "com.linecorp.line.media.picker.b$l";
    v.camera.launchSourceClass = "com.linecorp.line.media.picker.b$k";
    v.camera.launchCallbackClass = "com.linecorp.line.media.picker.b$h";
    v.camera.captureChoiceClass = "com.linecorp.line.media.picker.a";
    v.camera.captureChooserClass = "ny0.y";
    v.camera.methodShowCaptureChooser = "a";
    v.camera.schemeServiceActivity =
        "jp.naver.line.android.activity.schemeservice.LineSchemeServiceActivity";

    v.callTone.toneSourceClass = "a40.g";
    v.callTone.uriToneSourceClass = "a40.b";
    v.callTone.methodToneUri = "a";
    v.callTone.remoteRingbackClass = "sv7.b";
    v.callTone.remoteRingbackContextField = "b";
    v.callTone.remoteRingbackFallbackField = "d";
    v.callTone.ringtoneWrapperClass = "sv7.c";

    v.muteMessage.labFeatureClass = "vh8.k";
    v.muteMessage.methodIsFeatureEnabled = "c";
    v.muteMessage.silentMessageFeatureClass = "vh8.i0";
    v.muteMessage.sendModeClass = "mt1.e";
    v.muteMessage.methodSendMode = "a";
    v.muteMessage.sendModeEnumClass = "m02.a";
    v.muteMessage.silentFlagWriterClass = "tq8.p1";
    v.muteMessage.methodWriteSilentFlag = "a";

    v.iab.inAppBrowserActivityClass = "com.linecorp.line.iab.browser.impl.InAppBrowserActivity";

    v.homeTab.tabListProviderClass = "mg8.d";
    v.homeTab.methodBuildTabList = "a";
    v.homeTab.mainTabEnumClass = "jp.naver.line.android.activity.main.a";

    v.nightMode.nightModeConfiguratorClass = "z60.a";
    v.nightMode.methodApplyNightMode = "a";
    v.nightMode.fieldSystemDarkMode = "a";
    v.nightMode.inputPassActivityClass = "com.linecorp.line.passlock.InputPassActivity";
    v.nightMode.darkThemeManagerClass = "ih6.j";
    v.nightMode.methodIsDarkTheme = "l";
    v.nightMode.methodThemeMode = "A";
    v.nightMode.methodIsDefaultTheme = "B";

    v.room.migrationUtilClass = "sd.h";
    v.room.methodIsMigrationRequired = "b";

    v.keywordAssist.toggleViewClass = "hy1.c";
    v.keywordAssist.methodGetChecked = "getChecked";
    v.keywordAssist.methodSetChecked = "setChecked";
    v.keywordAssist.methodGetOnCheckedChange = "getOnCheckedChange";
    v.keywordAssist.resSearchBarBg = "search_bar_bg";
    v.keywordAssist.resSearchBarInput = "searchbar_input_text";
    v.keywordAssist.resSearchBarCancel = "searchbar_cancel_button";
    v.keywordAssist.resSearchBarIcon = "v2_common_search_icon";
    v.keywordAssist.resOnColor = "primaryFill";
    v.keywordAssist.resTitle = "search_keywordassist_title_keywordassist";

    v.permanentHideChat.chatDaoClass = "ze3.s";
    v.permanentHideChat.methodUpdateChatOnMessage = "h";
    v.permanentHideChat.squareChatDaoClass = "f76.n";
    v.permanentHideChat.methodUpdateSquareChat = "i";
    v.permanentHideChat.squareChatIdField = "a";
    v.permanentHideChat.squareChatColumnClass = "b66.c";
    v.permanentHideChat.squareArchivedColumn = "ARCHIVED_TYPE";
    v.permanentHideChat.methodGetSquareChat = "a";
    v.permanentHideChat.squareChatArchiveTypeField = "g";
    v.permanentHideChat.methodSetSquareArchiveType = "h";
    v.permanentHideChat.squareArchiveTypeClass = "b66.b";
    v.permanentHideChat.squareArchivedType = "ARCHIVED";
    v.permanentHideChat.squareNotArchivedType = "NON_ARCHIVED";
    v.permanentHideChat.chatOpenLoaderClass = "ld8.w";
    v.permanentHideChat.methodChatOpenLoad = "e";
    v.permanentHideChat.chatDataClass = "jp.naver.line.android.model.ChatData";
    v.permanentHideChat.methodChatDataArchived = "g";
    v.permanentHideChat.eventBusClass = "com.linecorp.rxeventbus.d";
    v.permanentHideChat.eventBusProviderField = "a";
    v.permanentHideChat.serviceLocatorClass = "sc0.f";
    v.permanentHideChat.serviceLocatorMethod = "a";
    v.permanentHideChat.methodEventBusPost = "b";
    v.permanentHideChat.updateViewEventClass = "xe8.b";
    v.permanentHideChat.updateViewEventChatList = "CHAT_LIST";
    v.permanentHideChat.chatTabBadgeEventClass = "xe8.a";
    v.permanentHideChat.chatTabBadgeEventField = "a";
    v.permanentHideChat.messageDataManagerClass = "wk8.l0";
    v.permanentHideChat.methodUpdateArchived = "F";
    v.permanentHideChat.chatMenuHideItemClass = "a71.h";
    v.permanentHideChat.methodChatMenuItemLabel = "a";
    v.permanentHideChat.chatPreviewItemClass = "pi1.b";
    v.permanentHideChat.chatPreviewChatIdField = "a";
    v.permanentHideChat.hidePopupClass = "b71.m0";
    v.permanentHideChat.methodHidePopup = "a";
    v.permanentHideChat.hidePopupRequestClass = "b71.n0";
    v.permanentHideChat.hidePopupChatIdField = "a";
    v.permanentHideChat.hidePopupLabelsClass = "b71.r1";
    v.permanentHideChat.hidePopupConfirmLabelField = "a";
    v.permanentHideChat.textButtonClass = "z82.i";
    v.permanentHideChat.methodTextButton = "a";
    v.permanentHideChat.hidePopupMessageClass = "b71.j0";
    v.permanentHideChat.stringResourceClass = "h5.h";
    v.permanentHideChat.methodStringResource = "d";
    v.permanentHideChat.dialogBuilderClass = "ml8.f$a";
    v.permanentHideChat.methodDialogMessage = "i";
    v.permanentHideChat.methodDialogPositive = "o";
    v.permanentHideChat.methodDialogNegative = "k";
    v.permanentHideChat.methodDialogShow = "t";
    v.permanentHideChat.chatListHideItemClass = "h32.g$a";
    v.permanentHideChat.methodChatListMenuItem = "a";
    v.permanentHideChat.chatListHideItemField = "a";
    v.permanentHideChat.chatListHideListenerField = "b";
    v.permanentHideChat.methodChatListItemInfo = "f";
    v.permanentHideChat.chatListItemChatIdField = "a";
    v.permanentHideChat.chatListMenuItemClass = "ml8.i";
    v.permanentHideChat.chatListMenuActionClass = "nt8.a";
    v.permanentHideChat.chatListHideConfirmClass = "d42.a";
    v.permanentHideChat.methodConfirmHide = "y5";
    v.permanentHideChat.chatTabRowClass = "c71.d0";
    v.permanentHideChat.methodChatTabRowKey = "getKey";
    v.permanentHideChat.chatTabRowDataClass = "c71.i";
    v.permanentHideChat.chatTabItemClass = "z61.t1";
    v.permanentHideChat.methodChatTabItem = "b";
    v.permanentHideChat.chatTabRowLayoutClass = "c71.l1";
    v.permanentHideChat.methodChatTabRowLayout = "a";
    v.permanentHideChat.alphaModifierClass = "f4.a";
    v.permanentHideChat.methodAlphaModifier = "a";
    v.permanentHideChat.chatListRowBinderClass = "k32.b$a";
    v.permanentHideChat.methodChatListRowBind = "b";
    v.permanentHideChat.viewHolderClass = "androidx.recyclerview.widget.RecyclerView$e0";
    v.permanentHideChat.viewHolderViewField = "a";

    return v;
  }
}
