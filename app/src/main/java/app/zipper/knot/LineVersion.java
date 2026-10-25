package app.zipper.knot;

import android.content.Context;
import app.zipper.knot.mapping.Mappings;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class LineVersion {

  // Absent from LINE before this version, so older versions don't count it as unresolved
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  public @interface Since {
    String value();
  }

  public static class Config {
    public String linePkg = "jp.naver.line.android";

    public Main main = new Main();
    public Settings settings = new Settings();
    public PlusMenu plusMenu = new PlusMenu();
    public ChatListMoreMenu chatListMoreMenu = new ChatListMoreMenu();
    public Unsend unsend = new Unsend();
    public Thrift thrift = new Thrift();
    public Tabs tabs = new Tabs();
    public Ads ads = new Ads();
    public Home home = new Home();
    public Chat chat = new Chat();
    public Res res = new Res();
    public ReadReceipt readReceipt = new ReadReceipt();
    public ChatHeader chatHeader = new ChatHeader();
    public Font font = new Font();
    public Notification notification = new Notification();
    public NotificationFix notificationFix = new NotificationFix();
    public ForegroundKeepAlive foregroundKeepAlive = new ForegroundKeepAlive();
    public TalkTabHeader talkTabHeader = new TalkTabHeader();
    public SearchBarAgentI searchBarAgentI = new SearchBarAgentI();
    public AgentIInChat agentIInChat = new AgentIInChat();
    public AiIcon aiIcon = new AiIcon();
    public ImageQuality imageQuality = new ImageQuality();
    public Media media = new Media();
    public Profile profile = new Profile();
    public ProfileTimestamps profileTimestamps = new ProfileTimestamps();
    public ChatJump chatJump = new ChatJump();
    public ChatTimestamp chatTimestamp = new ChatTimestamp();
    public ChatEditSelectAll chatEditSelectAll = new ChatEditSelectAll();
    public MessageEditHistory messageEditHistory = new MessageEditHistory();
    public Camera camera = new Camera();
    public CallTone callTone = new CallTone();
    public MuteMessage muteMessage = new MuteMessage();
    public Iab iab = new Iab();
    public HomeTab homeTab = new HomeTab();
    public NightMode nightMode = new NightMode();
    public Compose compose = new Compose();
    public Home26NavIcon home26NavIcon = new Home26NavIcon();
    public Kotlin kotlin = new Kotlin();
    public Androidx androidx = new Androidx();
    public Room room = new Room();
    public EventBus eventBus = new EventBus();
    public Services services = new Services();
    public ChatActivity chatActivity = new ChatActivity();
    public Message message = new Message();
    public KeywordAssist keywordAssist = new KeywordAssist();
    public PermanentHideChat permanentHideChat = new PermanentHideChat();

    public static class KeywordAssist {
      @Since("26.15.0")
      public String toggleViewClass = "";

      public String methodGetChecked = "getChecked";
      public String methodSetChecked = "setChecked";
      public String methodGetOnCheckedChange = "getOnCheckedChange";
      public String resSearchBarBg = "search_bar_bg";
      public String resSearchBarInput = "searchbar_input_text";
      public String resSearchBarCancel = "searchbar_cancel_button";
      public String resSearchBarIcon = "v2_common_search_icon";
      public String resOnColor = "primaryFill";
      public String resTitle = "search_keywordassist_title_keywordassist";
    }

    public static class Androidx {
      public String lifecycleEventClass = "";
      public String lifecycleOwnerClass = "";
      public String viewHolderClass = "";
      public String viewHolderViewField = "";
    }

    public static class EventBus {
      public String busClass = "";
      public String providerField = "";
      public String methodPost = "";
    }

    public static class Services {
      public String keyClass = "";
      public String locatorClass = "";
      public String methodLocate = "";
    }

    public static class ChatActivity {
      public String chatIdField = "";
      public String methodGetChatId = "";
    }

    public static class Message {
      public String processorClass = "";
      public String processorMetadataClass = "";
      public String dataManagerClass = "";
      public String selectionStateClass = "";
      public String selectionProviderClass = "";
    }

    public static class Room {
      public String migrationUtilClass = "";
      public String methodIsMigrationRequired = "";
    }

    public static class Kotlin {
      public String unitClass = "";
      public String fieldUnitInstance = "";
      public String continuationClass = "kotlin.coroutines.Continuation";
      public String continuationImplClass = "";
      public String stateFlowImplClass = "";
      public String function0Class = "";
      public String function1Class = "";
      public String function2Class = "";
    }

    public static class Compose {
      public String composerClass = "";
      public String composerImplClass = "";
      public String methodEndRestartGroup = "";
      public String clickableClass = "";
      public String methodClickable = "";
      public String methodCombinedClickable = "";
      public String onGloballyPositionedClass = "";
      public String methodOnGloballyPositioned = "";
      public String layoutCoordinatesClass = "";
      public String methodLocalToWindow = "";
      public String methodCoordinatesSize = "";
    }

    public static class Home26NavIcon {
      public String rendererClass = "";
      public String rendererMethod = "";
      public int agentDrawableId = 0;
      public int settingsDrawableId = 0;
    }

    public static class NightMode {
      public String nightModeConfiguratorClass = "";
      public String methodApplyNightMode = "";
      public String fieldSystemDarkMode = "";
      public String inputPassActivityClass = "com.linecorp.line.passlock.InputPassActivity";
      public String darkThemeManagerClass = "";
      public String methodIsDarkTheme = "";
      public String methodThemeMode = "";
      public String methodIsDefaultTheme = "";
    }

    public static class HomeTab {
      public String tabListProviderClass = "";
      public String methodBuildTabList = "";
      public String mainTabEnumClass = "";
    }

    public static class Camera {
      public String cameraModuleClass = "";
      public String methodUseExternalCamera = "";
      public String cameraLauncherClass = "";
      public String methodLaunchCamera = "";
      public String launchModeClass = "";
      public String launchSourceClass = "";
      public String launchCallbackClass = "";
      public String captureChoiceClass = "";
      public String captureChooserClass = "";
      public String methodShowCaptureChooser = "";
      public String schemeServiceActivity =
          "jp.naver.line.android.activity.schemeservice.LineSchemeServiceActivity";
    }

    public static class CallTone {
      public String toneSourceClass = "";
      public String uriToneSourceClass = "";
      public String methodToneUri = "";
      public String remoteRingbackClass = "";
      public String remoteRingbackContextField = "";
      public String remoteRingbackFallbackField = "";
      public String ringtoneWrapperClass = "";
    }

    public static class MuteMessage {
      public String labFeatureClass = "";
      public String methodIsFeatureEnabled = "";
      public String silentMessageFeatureClass = "";
      public String sendModeClass = "";
      public String methodSendMode = "";
      public String sendModeEnumClass = "";
    }

    public static class ChatTimestamp {
      public String displayTimeInterface = "";
      public String methodCreatedMillis = "";
    }

    public static class ChatEditSelectAll {
      public String methodGetSelectionState = "";
      public String methodGetItem = "";
      public String methodGetCount = "getCount";
      public String methodGetSelectedIds = "";
      public String methodToggleItem = "";
      public String methodIsItemSelected = "";
    }

    public static class MessageEditHistory {
      public String editRequestClass = "";
      public String editRequestIdField = "";
      public String editRequestTextField = "";
      public String menuListBuilderClass = "";
      public String menuListMethod = "";
      public String menuItemEnumClass = "";
      public String menuPresentationEnumClass = "";
      public String methodMenuLabel = "";
      public String methodMenuIcon = "";
      public String methodMenuActionAccessor = "";
      public String menuActionLambdaClass = "";
      public String menuContextMessageField = "";
      public String menuMessageDataField = "";
      public String menuMessageIdField = "";
      public String menuEditedFlagField = "";
    }

    public static class Iab {
      public String inAppBrowserActivityClass =
          "com.linecorp.line.iab.browser.impl.InAppBrowserActivity";
    }

    public static class ChatJump {
      public String requestClass = "com.linecorp.line.chat.request.ChatHistoryRequest";
      public String launchActivityClass =
          "jp.naver.line.android.activity.chathistory.ChatHistoryActivityLaunchActivity";
      public String requestExtraKey = "chatHistoryRequest";
      public String requestChatIdMethod = "getChatId";
    }

    public static class Media {
      public String videoDurationCheckClass = "";
      public String videoDurationCheckMethod = "";
      public String videoDurationCheckWithSizeMethod = "";
      public String videoDurationCheckWithoutSizeMethod = "";
      public String mediaPickerParamsClass = "";
      public String fieldMediaPickerMaxVideoDuration = "";
      public String droppedMediaPreprocessorClass = "";
      public String videoDurationSuccessClass = "";
      public String fieldVideoDurationSuccess = "";
      public String galleryViewClass = "";
      public String fieldGalleryDurationLimit = "";
      public String selectionValidatorClass = "";
      public String selectionValidatorMethod = "";
      public String selectionValidatorParamClass = "";
      public String videoProfileTrimmerActivityClass =
          "jp.naver.line.android.activity.setting.videoprofile.trim.VideoProfileTrimmerActivity";
      public String fieldVideoProfileTrimmerLimit = "";
    }

    public static class Profile {
      public String h13baClass = "";
      public String fieldH3 = "";
      public String methodGetProfile = "getProfile";
      public String fieldMid = "";
    }

    public static class ProfileTimestamps {
      public String activityClass = "com.linecorp.line.userprofile.impl.UserProfileActivity";
      public String midExtraKey = "USER_PROFILE_MID";
      public String resHeaderButtonContainer = "user_profile_header_button_binding";
      public String optionalButtonWidthDimen = "userprofile_optional_button_width";
      public String optionalButtonSpacingDimen = "userprofile_optional_button_spacing";
    }

    public static class ImageQuality {
      public String qualityProfileHighClass = "";
      public String qualityProfileMediumClass = "";
      public String methodGetMaxDimension = "";
      public String methodGetQuality = "";
      public String imageUtilClass = "";
    }

    public static class Main {
      public String mainActivity = "jp.naver.line.android.activity.main.MainActivity";
      public String baseMainTabFragment = "jp.naver.line.android.activity.main.BaseMainTabFragment";
      public String headerButton = "jp.naver.line.android.common.view.header.HeaderButton";
      public String headerButtonTypeClass = "";
      public String slotFarLeft = "FAR_LEFT";
      public String headerInterfaceA = "";
      public String fieldHeaderHelper = "";
      public String fieldChatActivity = "";
      public String methodSetHeaderButton = "";
      public String methodSetHeaderLabel = "";
      public String methodSetHeaderButtonVisibility = "";
      public String methodGetHeaderButtonView = "";
      public String methodSetButtonImageDrawable = "setButtonImageViewDrawable";
      public String methodSetHeaderOnClickListener = "";
      public String methodRefreshNavHeader = "";
      public String methodHeaderSetTitle = "setTitle";
      public String methodHeaderSetButtonVisibility = "";
      public String methodHeaderSetButtonListener = "";
    }

    public static class Settings {
      public String mainSettingsFragmentClass =
          "com.linecorp.line.settings.main.LineUserMainSettingsFragment";
      public String settingsAdapterClass = "";
      public String settingsItemClass = "";
      public String settingsBaseAdapterClass = "";
      public String settingsSearchHelperClass = "";
      public String settingsAdapterWrapperClass = "";
      public String methodSetItems = "";
      public String methodBindViewHolder = "";
      public String methodGetItem = "";
      public String fieldItemModel = "";
      public String fieldModelTag = "";
      public String fieldViewHolderView = "";
      public String settingsHeaderItemClass = "";
      public String settingsRowItemClass = "";
      public String settingsHandlerBaseClass = "";
      public String fieldIsVisible = "";
      public String fieldLayoutId = "";
      public String fieldActionHandler = "";
      public String fieldIconProvider = "";
      public String fieldDescriptionProvider = "";
      public String fieldSubActionHandler = "";
      public String fieldVisibilityFilter = "";
      public String fieldDefaultHandler = "";
      public String fieldCommonHandler = "";
      public String methodSetDescription = "";
      public String methodProxyGetItemType = "";
      public String methodSetTitleText = "setTitleText";
      public String methodSetChecked = "setChecked";
      public String methodSetItemType = "setItemType";
      public String methodSetSyncStatus = "setSyncStatus";
      public String methodSetDividerVisible = "setDividerVisible";

      public String textItemViewClass =
          "com.linecorp.line.settings.base.itemview.LineUserSettingTextItemView";
      public String methodRowSetTitleText = "setTitleText";
      public String methodRowSetDescription = "setDescriptionText";
      public String methodRowSetArrowVisible = "setArrowVisible";
      public String methodRowSetDividerVisible = "setDividerVisible";
      public String methodRowSetTitleColor = "setTitleTextColor";

      public String themeSettingItemId = "line-main-settings.themes";
    }

    public static class PlusMenu {
      public String plusMenuComponentClass = "";
      public String plusMenuCallbackClass = "";

      public String methodAddMenuItem = "";
      public String methodCreateMenu = "";

      public String editChatDrawable = "chat_tab_ui_header_plusmenu_edit_chat";
      public String targetPkg = "jp.naver.line.android";
    }

    public static class ChatListMoreMenu {
      public String popupListViewClass = "jp.naver.line.android.common.view.listview.PopupListView";
      public String fieldListView = "";
      public String popupListAdapterClass = "";
      public String fieldPopupItems = "";
      public String clickListenerClass = "";
      public String methodAddItem = "";
    }

    public static class ReadReceipt {
      public String readReceiptManagerClass = "";
      public String methodSendReadReceipt = "";
      public String methodExecuteReadReceiptAsync = "";
      public String methodReadAll = "";
      public String methodResolveReadTarget = "";
      public String operationNotifiedReadName = "NOTIFIED_READ_MESSAGE";
      public String longPressReadClass = "";
      public String reactClientClass =
          "jp.naver.line.android.thrift.client.impl.TalkServiceClientImpl";
      public String methodReact = "";
      public String reactRequestMessageIdField = "";
      public String reactSuccessResultClass = "";
    }

    public static class Unsend {
      public String notifiedReadMessageHandlerClass = "";
      public String notifiedSendReactionHandlerClass = "";
      public String chatMessageViewHolderClass = "";

      public String methodReadBuffer = "";
      public String methodBind = "";
      public String methodOperationTypeValueOf = "";
      public int methodBindIndex = 1;
      public String methodGetItemView = "";
      public String methodGetCommonData = "";
      public String carouselAdapterClass = "";
      public String methodCarouselBindViewHolder = "onBindViewHolder";
      public int operationTypeDummy = 40;

      public String chatServiceConfigClass = "";
      public String methodUnsendLimit = "";
      public String methodUnsendPremiumLimit = "";
      public String appInfoProviderClass = "";
      public String methodGetFullUserAgent = "";
      public String methodGetSimpleUserAgent = "";
      public String methodGetFullUserAgentWithContext = "";
      public String methodGetSimpleUserAgentWithContext = "";
      public String methodUnsendThrift = "unsendMessage";
      public String methodUnsendThriftSilent = "silentlyUnsendMessage";
      public String operationTypeField = "";
      public String operationParam1Field = "";
      public String operationParam2Field = "";
      public String operationParam3Field = "";
      public String operationCreatedTimeField = "";
      public String chatMessageIdField = "";
      public String chatMessageServerIdLongField = "";

      public String operationUnsendName = "DESTROY_MESSAGE";
      public String operationNotifiedUnsendName = "NOTIFIED_DESTROY_MESSAGE";
      public String unsendDestroyHandlerClass = "";
      public String destroyMessageHandlerClass = "";
      public String methodDestroyHandler = "";
      public String messageConverterClass = "";
      public String methodConvertMessage = "";
      public String incomingMessageIdField = "";
      public String incomingMessageTypeField = "";
      public String incomingMessageParamsField = "";
      public String incomingMessageParamsMapField = "";
      public String incomingMessageTypeEnumClass = "";
      public String handlerSuccessResultClass = "";
    }

    public static class Thrift {
      public String talkServiceClientImplClass =
          "jp.naver.line.android.thrift.client.impl.LegacyTalkServiceClientImpl";
      public String v1 = "";
      public String protocolClass = "";
      public String messageClass = "";
      public String methodWriteMessageBegin = "";
      public String methodReadMessageBegin = "";

      public String methodDestroyMessage = "destroyMessage";
      public String methodDestroyMessages = "destroyMessages";
    }

    public static class Tabs {
      public String bottomNavigationBarTextViewClass =
          "jp.naver.line.android.activity.main.bottomnavigationbar.BottomNavigationBarTextView";
      public String resNews = "bnb_news";
      public String resMini = "bnb_mini";
      public String resContainer = "main_tab_container";
      public String resBtnText = "bnb_button_text";
      public String resCall = "bnb_call";
      public String resCommerce = "bnb_commerce";
      public String resCommerceTw = "bnb_commerce_tw";
      public String resWallet = "bnb_wallet";
    }

    public static class Ads {
      public String ladAdView = "com.linecorp.line.ladsdk.ui.common.view.lifecycle.LadAdView";
      public String ladAdViewV2 = "com.linecorp.line.ladsdk.ui.v2.common.lifecycle.LyadAdView";
      public String smartChannel =
          "com.linecorp.line.admolin.smartch.v2.view.SmartChannelViewLayout";
      public String classAdSdkBase = "com.linecorp.line.ladsdk";
      public String classAdMolinBase = "com.linecorp.line.admolin";
    }

    public static class Home {
      public String resRecommendation = "home_tab_contents_recommendation_placement";
      public String resServiceCarouselId = "home_tab_service_carousel";
      public String resServiceTitleId = "home_tab_service_title";
      public String resNoServicesId = "home_tab_no_services_title";

      public String home26FeedTypePrefixes =
          "HomeFeed,HomeContentsRecommendation,GlobalHomePage,GlobalHomeDefault,AdModel,HomePerformanceAd";
      public String home26ServiceTypePrefixes = "HomeServiceList,GlobalHomeServiceSection";
      public String home26LoadingMoreDataClass = "";
      public String home26ModuleBodyField = "";
      public String home26ModuleTypeMethod = "getType";
    }

    public static class Chat {
      public String searchHeaderHelperClass = "";
      public String searchHeaderShowMethod = "";
      public String searchHeaderControllerField = "";
      public String searchHeaderEventBusField = "";
      public String searchHeaderViewField = "";
      public String searchControllerSearchBoxMethod = "";
      public String searchPresenterClass = "";
      public String searchKeywordTypeClass = "";
      public String searchKeywordTypeMethod = "";
      public String searchResultClass = "";
      public String searchResultCtorArgs = "chatId,keyword,idList,count";
      public String searchResultWrapperClass = "";
      public String searchBoxViewClass = "jp.naver.line.android.customview.SearchBoxView";
      public String searchBoxEditTextField = "";
      public String searchBoxIconField = "";
      public String searchBoxSetEditingLayoutMethod = "setEditingLayout";
      public String searchBoxGetTextMethod = "getSearchText";
      public String searchKeywordEventClass = "";
      public String searchKeywordEventKeywordField = "";
      public String searchPresenterKeywordChangedMethod =
          "onSearchInChatKeywordChangedEventReceived";
      public String searchPresenterKeywordSubjectField = "";
      public String searchKeywordSubjectValueMethod = "";
      public String searchResultWrapperResultOptionalField = "";
      public String searchResultCountField = "";
      public String searchResultTitleViewHolderClass = "";
      public String searchResultTitleBindMethod = "";
      public String searchResultTitleBindingField = "";
      public String searchResultTitleTextViewField = "";
      public String searchResultHeaderItemClass = "";
      public String searchResultHeaderTitleField = "";
      public String searchResultHeaderCountField = "";
      public String searchFtsInChatQueryClass = "";
      public String searchFtsQueryField = "";
      public String searchFtsChatIdField = "";
      public String searchFtsLimitField = "";
      public String searchFtsPrepareMethod = "";
      public String searchFtsBindTextMethod = "";
      public String searchFtsStepMethod = "";
    }

    public static class ChatHeader {
      public String chatHistoryActivity =
          "jp.naver.line.android.activity.chathistory.ChatHistoryActivity";
      public String controllerParam4Class = "";
      public String controllerParam5Class = "";
      public String controllerParam6Class = "";
      public String controllerParam8Class = "";
      public String controllerParam9Class = "";
      public String controllerParam10Class = "";
      public String headerController = "";
      public String headerHelper = "";
    }

    public static class Font {
      public String fontConfigClass = "";
      public String fontManagerClass = "";
      public String fontCallbackClass = "";
      public String methodGetFontConfig = "";
      public String methodGetFontSettings = "";
      public String methodOnFontChanged = "";
      public String fontRequestExecutorClass = "";
      public String fontCallbackWithHandlerClass = "";
    }

    public static class Res {
      public int idSettingList = 0;
      public int idPersonalInfo = 0;
      public int typeSection = 0;
      public int typeRow = 0;
      public int idIcon = 0;
      public int idDesc = 0;
      public int idMark = 0;
      public int idSeparator = 0;
      public int idArrow = 0;
      public int idNewMark = 0;
      public int idNoticeDot = 0;
      public int idTitle = 0;
      public int layoutCheckbox = 0;
      public int layoutSectionHeader = 0;
      public int layoutSettingsMain = 0;
      public int idHeader = 0;
      public int idTimestamp = 0;
      public String resSettingsHeaderBtn = "settings_header_button";
      public String resSettingsBtn = "settings_button";
      public String resTooltipBackground = "home_tooltip_background";
      public String resTooltipArrowUp = "home_tooltip_arrow_up";
    }

    public static class Notification {
      public String chatHistoryRequestClass = "com.linecorp.line.chat.request.ChatHistoryRequest";
      public String chatHistoryActivityLaunchActivityClass =
          "jp.naver.line.android.activity.chathistory.ChatHistoryActivityLaunchActivity";
      public String messageIdExtra = "line.message.id";
      public String messageNotificationTag = "NOTIFICATION_TAG_MESSAGE";
      public String chatNotificationTag = "jp.naver.line.android.notification.tag.chat";
      public String chatIdExtra = "line.chat.id";
      public String squareNotificationExtra = "line.square.notification";
      public String accessServiceClass =
          "jp.naver.line.android.access.remote.LineAccessServiceForNotification";
      public String accessServiceCallbackClass = "";
      public String accessServiceCallbackMethod = "";
      public String accessServiceChatIdExtra = "chatId";
      public String markAsReadAction =
          "jp.naver.line.android.access.remote.action.MARK_AS_READ_ACTION";
      public String markAsReadLabelRes = "chatlist_longtap_menu_label_markasread";
      public String decryptedResultClass = "";
      public String messageClass = "";
      public String messageServerIdField = "";
      public String messageContentTypeField = "";
      public String messageTextField = "";
      public String messageMetadataField = "";
      public String messageMetadataKeyClass = "";
      public String messageMetadataStringMethod = "";
      public String messageObsPopField = "OBS_POP";
      public String messageVisualBuilderClass = "";
      public String messageVisualEncryptionMethod = "";
      public String messageThumbnailRequestClass = "";
      public String messageObsEncryptionDataClass = "";
      public String messageSquareUtilsClass = "com.linecorp.square.chat.SquareChatUtils";
      public String messageSquareCheckMethod = "";
      public String messageProcessorMethod = "";
      public String messageProcessorContentTypeClass = "";
      public String messageProcessorAuxClass = "";
      public String messageContentCacheClass = "";
      public String messageContentCacheInitializeMethod = "";
      public String messageContentCacheKeyClass = "";
      public String messageThumbnailCacheFileMethod = "";
      public String glideClass = "";
      public String glideWithContextMethod = "";
      public String glideRetrieverMethod = "";
      public String glideRetrieverGetMethod = "";
      public String glideAsFileMethod = "";
      public String glideLoadMethod = "";
      public String glideSubmitMethod = "";
      public String glideClearMethod = "";
      public String stickerFileManagerClass = "";
      public String stickerInitializeMethod = "";
      public String stickerMainCacheFileMethod = "";
      public String stickerRequestFactoryClass = "";
      public String stickerRequestFactoryField = "";
      public String stickerResourceClass = "";
      public String stickerSecretClass = "";
      public String stickerOptionClass = "";
      public String stickerStaticOptionField = "STATIC";
      public String stickerMainRequestMethod = "";
      public String stickerRemoteFetcherClass = "";
      public String stickerRemoteCompleteMethod = "";
      public String stickerRemoteCallClass = "";
      public String stickerRemoteResponseClass = "";
      public String stickerRemoteTargetFileField = "";
      public String combinationStickerMetadataFileManagerClass = "";
      public String combinationStickerMetadataFileMethod = "";
      public String combinationStickerMetadataWriteMethod = "";
      public String combinationStickerMetadataResponseClass = "";
      public String sticonImageRepositoryClass = "";
      public String sticonImageCacheImplementationClass = "";
      public String sticonImageCachePutMethod = "";
      public String sticonImageRepositoryFactoryField = "";
      public String sticonImageRepositoryFactoryMethod = "";
      public String sticonImageRepositoryCacheMethod = "";
      public String sticonCoroutineRequestClass = "";
      public String sticonCoroutineBuildersClass = "";
      public String sticonCoroutineRunBlockingMethod = "";
      public String sticonCoroutineFunctionClass = "";
      public String sticonImageKeyClass = "";
      public String sticonPaidProductClass = "";
      public String sticonPaidClass = "";
      public String sticonOptionTypeClass = "";
      public String fileProviderHelperClass = "";
      public String fileProviderUriMethod = "";
    }

    public static class NotificationFix {
      public String lineFcmServiceClass =
          "jp.naver.line.android.service.fcm.LineFirebaseMessagingService";
      public String lineFcmDispatchMethod = "";
      public String lineFcmOwnershipMethod = "";
      public String lineFcmTokenMethod = "";
      public String lineFcmServiceBaseClass = "";
      public String firebaseRemoteMessageClass = "";
      public String firebaseReceiverClass = "com.google.firebase.iid.FirebaseInstanceIdReceiver";
      public String firebaseReceiverMethod = "";
      public String firebaseReceiverEnvelopeClass = "";
      public String firebaseReceiverIntentField = "";
      public String firebaseDispatcherClass = "";
      public String firebaseDispatcherSingletonField = "";
      public String firebaseDispatcherMethod = "";
      public String firebaseDispatcherContextField = "";
      public String firebaseDispatcherQueueField = "";
      public String firebaseBindDeliveryClass = "";
      public String firebaseBindDeliveryMethod = "";
      public String firebaseMessagingServiceClass =
          "com.google.firebase.messaging.FirebaseMessagingService";
      public String firebaseMessagingHandleMethod = "";
      public String firebaseWakefulStartClass = "";
      public String firebaseWakefulStartMethod = "";
      public String firebaseCompletedTaskClass = "";
      public String firebaseCompletedTaskMethod = "";
      public String firebaseMessagingClass = "com.google.firebase.messaging.FirebaseMessaging";
      public String firebaseMessagingGetTokenMethod = "";
      public String firebaseMessagingTokenFreshMethod = "";
      public String firebaseAppClass = "";
      public String firebaseAppGetInstanceMethod = "";
      public String legyStreamingStateClass = "";
      public String legyStreamingLifecycleClass = "";
      public String legyStreamingLifecycleMethod = "";
      public String legyStreamingManagerField = "";
      public String legyBackgroundStateField = "BACKGROUND";
      public String legyDisconnectRunnableClass = "";
      public String legyStateField = "";
      public String legyTimeoutField = "";
      public String legyBackgroundWorkerFlagField = "";
      public String legyHandlerField = "";
      public String legyRunnableField = "";
      public String fisCertDigestClass = "";
      public String fisCertDigestMethod = "";
      public String fisCertSha1 = "89396DC419292473972813922867E6973D6F5C50";
      public String gmsSignatureCheckClass = "";
      public String gmsSignatureCheckMethod = "";
      public String gmsAvailabilityClass = "";
      public String gmsAvailabilityMethod = "";
    }

    public static class ForegroundKeepAlive {
      public String serviceClass = "androidx.work.impl.foreground.SystemForegroundService";
    }

    public static class TalkTabHeader {
      public String chatTabHeaderStateClass = "";
      public String iconListStateField = "";
      public String buttonListStateField = "";
      public String iconTypeClass = "";
      public String iconTypeFieldInButton = "";
      public String subDeviceOpenChatButtonClass = "";
      public String subDeviceAlbumButtonClass = "";
    }

    public static class SearchBarAgentI {
      public String talkVisibleMethod = "";
      public String talkClickMethod = "";
      public String homeSearchBarClass = "";
      public String homeRefreshMethod = "";
      public String homeRootViewField = "";
      public String homeTabTypeField = "";
      public String homeTabName = "HOME";
      public String homeTabV2Name = "HOME_V2";
      public String chatTabName = "CHAT";
      public String newsTabName = "NEWS";
      public int homeAiContainerId = 0;
      public int homeGuidelineId = 0;
      public int homeGuidelineEndDp = 55;
      public String homeGuidelineClass = "androidx.constraintlayout.widget.Guideline";

      public String miniTabHeaderClass =
          "com.linecorp.line.wallet.impl.v3.view.WalletV3GrandDesignHeaderView";
      public String miniTabAgentMethod = "";

      public String commerceHeaderClass = "";
      public String commerceHeaderMethod = "";

      public String imageViewerAiButtonClass = "";

      @Since("26.15.0")
      public String chatImageAiButtonClass = "";

      @Since("26.15.0")
      public String chatImageAiButtonMethod = "";
    }

    public static class AgentIInChat {
      public String toggleComposableClass = "";
    }

    public static class AiIcon {
      public String repoClass = "";
      public String methodGetShownAfterMillis = "";
    }

    public static class PermanentHideChat {
      public String chatDaoClass = "";
      public String methodUpdateChatOnMessage = "";
      public String squareChatDaoClass = "";
      public String methodUpdateSquareChat = "";
      public String squareChatIdField = "";
      public String squareChatColumnClass = "";
      public String squareArchivedColumn = "ARCHIVED_TYPE";
      public String methodGetSquareChat = "";
      public String squareChatArchiveTypeField = "";
      public String methodSetSquareArchiveType = "";
      public String squareArchiveTypeClass = "";
      public String squareArchivedType = "ARCHIVED";
      public String squareNotArchivedType = "NON_ARCHIVED";
      public String chatOpenLoaderClass = "";
      public String methodChatOpenLoad = "";
      public String chatDataClass = "jp.naver.line.android.model.ChatData";
      public String methodChatDataArchived = "";
      public String updateViewEventClass = "";
      public String updateViewEventChatList = "CHAT_LIST";
      public String chatTabBadgeEventClass = "";
      public String chatTabBadgeEventField = "";
      public String methodUpdateArchived = "";
      public String unhideLabelRes = "hidden_restore";
      public String hideLabelRes = "chatlist_label_hide";
      public String hideConfirmMessageRes = "chatlist_hideconfirmdialog_message";
      public String hideConfirmSquareMessageRes = "openchat_chatlist_popup_hidechat";
      public String cancelLabelRes = "cancel";
      public String chatMenuHideItemClass = "";
      public String methodChatMenuItemLabel = "";
      public String chatPreviewItemClass = "";
      public String chatPreviewChatIdField = "";
      public String hidePopupClass = "";
      public String methodHidePopup = "";
      public String hidePopupRequestClass = "";
      public String hidePopupChatIdField = "";
      public String hidePopupLabelsClass = "";
      public String hidePopupConfirmLabelField = "";
      public String textButtonClass = "";
      public String methodTextButton = "";
      public String hidePopupMessageClass = "";
      public String stringResourceClass = "";
      public String methodStringResource = "";
      public String dialogBuilderClass = "";
      public String methodDialogMessage = "";
      public String methodDialogPositive = "";
      public String methodDialogNegative = "";
      public String methodDialogShow = "";
      public String chatListHideItemClass = "";
      public String methodChatListMenuItem = "";
      public String chatListHideItemField = "";
      public String chatListHideListenerField = "";
      public String methodChatListItemInfo = "";
      public String chatListItemChatIdField = "";
      public String chatListMenuItemClass = "";
      public String chatListMenuActionClass = "";
      public String chatListHideConfirmClass = "";
      public String methodConfirmHide = "";
      public String chatTabRowClass = "";
      public String methodChatTabRowKey = "getKey";
      public String chatTabRowDataClass = "";
      public String chatTabItemClass = "";
      public String methodChatTabItem = "";
      public String chatTabRowLayoutClass = "";
      public String methodChatTabRowLayout = "";
      public String alphaModifierClass = "";
      public String methodAlphaModifier = "";
      public String chatListRowBinderClass = "";
      public String methodChatListRowBind = "";
    }
  }

  private static volatile Config cachedConfig = null;

  public static Config get() {
    return cachedConfig;
  }

  public static Config load(Context context, ClassLoader cl) {
    if (cachedConfig == null) cachedConfig = Mappings.load(context, cl);
    return cachedConfig;
  }

  public static String getVersionName(Context context) {
    if (context == null) return null;
    try {
      return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
    } catch (Throwable t) {
      Knot.log("Knot: getVersionName failed: " + t.getMessage());
      return null;
    }
  }
}
