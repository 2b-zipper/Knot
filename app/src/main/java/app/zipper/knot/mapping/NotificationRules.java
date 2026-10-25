package app.zipper.knot.mapping;

import static app.zipper.knot.mapping.Finder.sig;

import app.zipper.knot.LineVersion;
import java.lang.reflect.Modifier;
import java.util.List;
import org.luckypray.dexkit.query.enums.StringMatchType;
import org.luckypray.dexkit.query.enums.UsingType;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

final class NotificationRules {

  static final Mappings.Section[] SECTIONS = {
    NotificationRules::accessService,
    NotificationRules::message,
    NotificationRules::messageMedia,
    NotificationRules::glide,
    NotificationRules::sticker,
    NotificationRules::sticon,
    NotificationRules::firebase,
    NotificationRules::legy
  };

  private NotificationRules() {}

  private static void accessService(Finder f, LineVersion.Config c) {
    c.notification.accessServiceCallbackClass =
        f.clsIn(
            "jp.naver.line.android.access.remote",
            ClassMatcher.create()
                .addMethod(
                    MethodMatcher.create()
                        .name("<init>")
                        .paramTypes(
                            "jp.naver.line.android.access.remote.LineAccessServiceForNotification",
                            "java.lang.String"))
                .addMethod(sig("void", "java.lang.Throwable")));
    c.notification.accessServiceCallbackMethod =
        f.methodOf(c.notification.accessServiceCallbackClass, "void");
  }

  private static void message(Finder f, LineVersion.Config c) {
    c.notification.messageClass = f.classUsing("relatedMessageServiceCode:");
    String message = c.notification.messageClass;
    c.notification.messageServerIdField = f.thriftField(message, "id");
    c.notification.messageTextField = f.thriftField(message, "text");
    c.notification.messageContentTypeField = f.thriftField(message, "contentType");
    c.notification.messageProcessorContentTypeClass =
        f.fieldType(message, c.notification.messageContentTypeField);
    c.notification.messageMetadataField = f.thriftField(message, "contentMetadata");
    c.notification.messageMetadataKeyClass =
        f.classUsing("KEY_PAYMENT_REQUESTED_REPRESENTATIVE_USER_ID");
    c.notification.messageObsEncryptionDataClass = f.classUsing("ObsEncryptionData(spaceId=");

    c.notification.decryptedResultClass = f.classUsing("Decrypted(decryptedMessage=");
    String processors = Finder.packageOf(c.notification.decryptedResultClass);
    // syncMessageContent builds the continuation of that name
    c.notification.messageProcessorMethod =
        f.method(
            c.message.processorClass,
            MethodMatcher.create()
                .addInvoke(
                    MethodMatcher.create()
                        .declaredClass(
                            f.classAnnotatedIn(
                                processors,
                                "jp.naver.line.android.talkop.processor.impl.RECEIVE_MESSAGE",
                                "syncMessageContent"))
                        .name("<init>")));
    // syncMessageContent(..., String, aux, Long, continuation), static in older builds
    c.notification.messageProcessorAuxClass =
        Finder.after(
            Finder.params(
                f.methodData(
                    c.message.processorClass,
                    MethodMatcher.create().name(c.notification.messageProcessorMethod))),
            "java.lang.String");

    // getString(key) returns null for a missing key; its sibling falls back to ""
    c.notification.messageMetadataStringMethod =
        f.method(
            c.message.processorMetadataClass,
            sig("java.lang.String", c.notification.messageMetadataKeyClass)
                .addNoneOf(MethodMatcher.create().usingEqStrings("")));

    c.notification.messageSquareCheckMethod =
        f.method(
            c.notification.messageSquareUtilsClass,
            sig("boolean", "java.lang.String")
                .addInvoke(
                    sig("com.linecorp.square.chat.SquareChatCategory", "java.lang.String")
                        .declaredClass(c.notification.messageSquareUtilsClass)));
    c.notification.messageVisualBuilderClass = f.classUsing("EXPIRED_MESSAGE");
    c.notification.messageVisualEncryptionMethod =
        f.method(
            c.notification.messageVisualBuilderClass,
            MethodMatcher.create()
                .addUsingField(
                    FieldMatcher.create().name("OBS_KEY_MATERIAL_FOR_ENCRYPTED_CONTENT")));
  }

  private static void messageMedia(Finder f, LineVersion.Config c) {
    c.notification.messageContentCacheClass = f.classUsing("message-content-temp-");
    c.notification.messageContentCacheInitializeMethod =
        f.methodOf(c.notification.messageContentCacheClass, "void", "android.content.Context");
    c.notification.messageContentCacheKeyClass = f.classUsing("MessageContentKey(chatId=");
    c.notification.messageThumbnailCacheFileMethod =
        f.method(
            c.notification.messageContentCacheClass,
            sig("java.io.File", c.notification.messageContentCacheKeyClass)
                .addInvoke("Ljava/lang/Object;->getClass()Ljava/lang/Class;"));
    c.notification.messageThumbnailRequestClass = f.classUsing("GlideChatThumbnailRequest(chatId=");

    String provider = ".line.common.FileProvider";
    c.notification.fileProviderHelperClass = f.classUsing(provider);
    c.notification.fileProviderUriMethod =
        f.method(
            c.notification.fileProviderHelperClass,
            MethodMatcher.create().usingEqStrings(provider));
  }

  private static void glide(Finder f, LineVersion.Config c) {
    c.notification.glideClass =
        f.classUsing(
            "GeneratedAppGlideModuleImpl is implemented incorrectly. If you've manually implemented this class, remove your implementation. The Annotation processor will generate a correct implementation.");
    String glide = c.notification.glideClass;
    c.notification.glideRetrieverMethod =
        f.method(
            glide,
            MethodMatcher.create()
                .usingEqStrings(
                    "You cannot start a load on a not yet attached View or a Fragment where getActivity() returns null (which usually occurs when getActivity() is called before the Fragment is attached or after the Fragment is destroyed)."));
    String retriever = f.returnType(glide, c.notification.glideRetrieverMethod);
    c.notification.glideRetrieverGetMethod =
        f.method(
            retriever,
            MethodMatcher.create().usingEqStrings("You cannot start a load on a null Context"));
    c.notification.glideWithContextMethod =
        f.method(
            glide,
            sig(null, "android.content.Context")
                .modifiers(Modifier.STATIC)
                .addInvoke(
                    MethodMatcher.create()
                        .declaredClass(retriever)
                        .name(c.notification.glideRetrieverGetMethod)));

    String manager = f.returnType(glide, c.notification.glideWithContextMethod);
    // DOWNLOAD_ONLY_OPTIONS is the last option the static initializer builds
    List<String> options = Finder.writes(f.initializer(manager), manager);
    c.notification.glideAsFileMethod =
        f.method(
            manager,
            MethodMatcher.create()
                .paramCount(0)
                .addUsingField(
                    FieldMatcher.create()
                        .declaredClass(manager)
                        .name(Finder.at(options, options.size() - 1)),
                    UsingType.Read));
    c.notification.glideClearMethod =
        f.method(
            manager,
            sig("void", (String) null)
                .addInvoke("Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;"));

    String builder = f.returnType(manager, c.notification.glideAsFileMethod);
    // load(Object) only forwards to loadModel, which stores the model
    c.notification.glideLoadMethod =
        f.method(
            builder,
            sig(builder, "java.lang.Object")
                .addNoneOf(
                    MethodMatcher.create()
                        .addUsingField(
                            FieldMatcher.create().declaredClass(builder), UsingType.Write))
                .addInvoke(
                    MethodMatcher.create()
                        .declaredClass(builder)
                        .paramTypes("java.lang.Object")
                        .addUsingField(
                            FieldMatcher.create().declaredClass(builder), UsingType.Write)));
    c.notification.glideSubmitMethod =
        f.method(builder, MethodMatcher.create().usingNumbers(Integer.MIN_VALUE));
  }

  private static void sticker(Finder f, LineVersion.Config c) {
    c.notification.stickerFileManagerClass = f.classUsing("message_overlay/");
    c.notification.stickerInitializeMethod =
        f.methodOf(c.notification.stickerFileManagerClass, "void", "android.content.Context");
    c.notification.stickerMainCacheFileMethod =
        f.method(
            c.notification.stickerFileManagerClass,
            sig("java.io.File", "long", "long")
                .addInvoke("Ljava/lang/String;->valueOf(J)Ljava/lang/String;"));

    String combination = "combination_sticker";
    c.notification.combinationStickerMetadataFileManagerClass = f.classUsing(combination);
    c.notification.combinationStickerMetadataFileMethod =
        f.method(
            c.notification.combinationStickerMetadataFileManagerClass,
            MethodMatcher.create().usingEqStrings(combination));
    c.notification.combinationStickerMetadataResponseClass =
        f.classUsing("CombinationStickerMetadata(version=");
    c.notification.combinationStickerMetadataWriteMethod =
        f.staticMethodOf(
            c.notification.combinationStickerMetadataFileManagerClass,
            "boolean",
            "java.io.File",
            c.notification.combinationStickerMetadataResponseClass);

    c.notification.stickerResourceClass = f.classUsing("DeprecatedStickerResourceData(packageId=");
    c.notification.stickerSecretClass =
        f.classUsing("DeprecatedStickerResourceSecretData(stickerHash=");
    c.notification.stickerOptionClass = f.classUsing("ANIMATION_SOUND_TYPE");
    c.notification.stickerRequestFactoryClass =
        f.clsIn(
            Finder.packageOf(
                f.classUsing(
                    "com.linecorp.line.shopdata.glide.request.GlideStickerRequestFactoryInstanceFactory")),
            ClassMatcher.create()
                .addMethod(
                    sig(
                        null,
                        c.notification.stickerResourceClass,
                        c.notification.stickerOptionClass,
                        "boolean"))
                .methodCount(23)
                .fieldCount(1));
    c.notification.stickerRequestFactoryField =
        f.field(
            c.notification.stickerRequestFactoryClass,
            FieldMatcher.create().modifiers(Modifier.STATIC));
    c.notification.stickerMainRequestMethod =
        f.methodOf(
            c.notification.stickerRequestFactoryClass,
            null,
            c.notification.stickerResourceClass,
            c.notification.stickerOptionClass,
            "boolean");

    // Three classes report Glide progress; the fetcher downloads into a File
    c.notification.stickerRemoteFetcherClass =
        f.classUsing(
            ClassMatcher.create()
                .addMethod(
                    MethodMatcher.create()
                        .name("<init>")
                        .paramTypes(
                            Finder.leading(
                                11,
                                "android.content.Context",
                                null,
                                "android.os.Handler",
                                "java.io.File",
                                "java.lang.String"))),
            "com.linecorp.glide.GlideRequestProgressListener");
    String fetcher = c.notification.stickerRemoteFetcherClass;
    c.notification.stickerRemoteTargetFileField = f.fieldOf(fetcher, "java.io.File");
    c.notification.stickerRemoteResponseClass = f.classUsing("Response{protocol=");
    c.notification.stickerRemoteCompleteMethod =
        f.method(fetcher, MethodMatcher.create().addInvoke("Ljava/io/Closeable;->close()V"));
    c.notification.stickerRemoteCallClass =
        f.paramType(fetcher, c.notification.stickerRemoteCompleteMethod, 0);
  }

  private static void sticon(Finder f, LineVersion.Config c) {
    // runBlocking(context, block) parks with Long.MAX_VALUE; runBlocking(block) forwards to it
    MethodData run =
        f.methodDataAnywhere(
            MethodMatcher.create()
                .modifiers(Modifier.STATIC)
                .returnType("java.lang.Object")
                .paramTypes(null, c.kotlin.function2Class)
                .usingNumbers(Long.MAX_VALUE));
    c.notification.sticonCoroutineBuildersClass = Finder.declaring(run);
    c.notification.sticonCoroutineRunBlockingMethod =
        f.method(
            c.notification.sticonCoroutineBuildersClass,
            sig("java.lang.Object", c.kotlin.function2Class)
                .modifiers(Modifier.STATIC)
                .addInvoke(
                    MethodMatcher.create()
                        .declaredClass(c.notification.sticonCoroutineBuildersClass)
                        .name(Finder.name(run))));
    c.notification.sticonCoroutineFunctionClass = c.kotlin.function2Class;
    c.notification.sticonCoroutineRequestClass =
        f.classAnnotatedIn(
            Finder.packageOf(
                f.classUsing(
                    "decodeWithApngDrawable(Ljava/io/File;)Landroid/graphics/drawable/Drawable;")),
            "com.linecorp.shop.impl.sticon.imagestorage.SticonImageRepositoryImpl$retrieveDrawable$2");

    c.notification.sticonImageKeyClass = f.classUsing(", useContentImageResource=");
    String key = c.notification.sticonImageKeyClass;
    c.notification.sticonOptionTypeClass =
        f.clsIn(
            Finder.packageOf(key),
            ClassMatcher.create()
                .superClass("java.lang.Enum")
                .addField(FieldMatcher.create().name("hasAnimation")));
    c.notification.sticonPaidClass = f.classUsing("Paid(product=");
    c.notification.sticonPaidProductClass = f.classUsing("Paid(id=");

    // The implementation sizes its tiers with Math.pow(i / 4f, 1.75)
    c.notification.sticonImageCacheImplementationClass =
        f.clsIn(
            Finder.packageOf(
                f.classUsing("com.linecorp.line.shopdata.sticon.cache.SticonImageCacheFactory")),
            ClassMatcher.create()
                .addMethod(MethodMatcher.create().usingNumbers(Double.doubleToRawLongBits(1.75))));
    c.notification.sticonImageCachePutMethod =
        f.methodOf(
            c.notification.sticonImageCacheImplementationClass,
            "void",
            key,
            "android.graphics.drawable.Drawable");

    String factory =
        "com.linecorp.shop.impl.sticon.imagestorage.DelegatedSticonImageRepositoryFactory";
    c.notification.sticonImageRepositoryClass =
        f.clsIn(
            Finder.packageOf(f.classUsing(factory)),
            ClassMatcher.create()
                .addMethod(sig("android.graphics.drawable.Drawable", key))
                .methodCount(5)
                .fieldCount(1));
    String repository = c.notification.sticonImageRepositoryClass;
    c.notification.sticonImageRepositoryCacheMethod =
        f.methodOf(repository, "android.graphics.drawable.Drawable", key);
    c.notification.sticonImageRepositoryFactoryField =
        f.field(repository, FieldMatcher.create().modifiers(Modifier.STATIC));
    c.notification.sticonImageRepositoryFactoryMethod =
        f.method(
            f.fieldType(repository, c.notification.sticonImageRepositoryFactoryField),
            MethodMatcher.create().usingEqStrings(factory));
  }

  private static void firebase(Finder f, LineVersion.Config c) {
    String initialize = ". Make sure to call FirebaseApp.initializeApp(Context) first.";
    c.notificationFix.firebaseAppClass = f.classUsing(initialize);
    c.notificationFix.firebaseAppGetInstanceMethod =
        f.method(
            c.notificationFix.firebaseAppClass, MethodMatcher.create().usingEqStrings(initialize));

    c.notificationFix.firebaseBindDeliveryClass =
        f.classUsing("Firebase-FirebaseInstanceIdServiceConnection");
    c.notificationFix.firebaseBindDeliveryMethod =
        f.methodOf(c.notificationFix.firebaseBindDeliveryClass, null, "android.content.Intent");
    c.notificationFix.firebaseCompletedTaskClass =
        f.classUsing("Must not be called on GoogleApiHandler thread.");
    c.notificationFix.firebaseCompletedTaskMethod =
        f.staticMethodOf(c.notificationFix.firebaseCompletedTaskClass, null, "java.lang.Object");

    String rawData = "gcm.rawData64";
    c.notificationFix.firebaseDispatcherClass = f.classUsing(rawData);
    String dispatcher = c.notificationFix.firebaseDispatcherClass;
    c.notificationFix.firebaseDispatcherContextField =
        f.fieldOf(dispatcher, "android.content.Context");
    c.notificationFix.firebaseDispatcherMethod =
        f.method(dispatcher, MethodMatcher.create().usingEqStrings(rawData));
    c.notificationFix.firebaseDispatcherSingletonField =
        f.fieldOf(dispatcher, c.notificationFix.firebaseBindDeliveryClass);
    c.notificationFix.firebaseDispatcherQueueField =
        f.fieldOf(
            f.fieldType(dispatcher, c.notificationFix.firebaseDispatcherSingletonField),
            "java.util.ArrayDeque");
    c.notificationFix.firebaseReceiverMethod =
        f.method(
            c.notificationFix.firebaseReceiverClass,
            MethodMatcher.create()
                .addInvoke(
                    MethodMatcher.create()
                        .declaredClass(dispatcher)
                        .name(c.notificationFix.firebaseDispatcherMethod)));
    c.notificationFix.firebaseReceiverEnvelopeClass =
        f.paramType(
            c.notificationFix.firebaseReceiverClass, c.notificationFix.firebaseReceiverMethod, 1);
    c.notificationFix.firebaseReceiverIntentField =
        f.fieldOf(c.notificationFix.firebaseReceiverEnvelopeClass, "android.content.Intent");

    c.notificationFix.firebaseMessagingGetTokenMethod =
        f.method(
            c.notificationFix.firebaseMessagingClass,
            MethodMatcher.create().usingEqStrings("FCM Registration failed!"));
    c.notificationFix.firebaseMessagingTokenFreshMethod =
        f.methodOf(c.notificationFix.firebaseMessagingClass, "boolean", (String) null);
    c.notificationFix.firebaseMessagingHandleMethod =
        f.method(
            c.notificationFix.firebaseMessagingServiceClass,
            MethodMatcher.create()
                .usingEqStrings("com.google.firebase.messaging.RECEIVE_DIRECT_BOOT"));
    c.notificationFix.firebaseRemoteMessageClass = f.classUsing("google.");
    c.notificationFix.firebaseWakefulStartClass =
        f.clsIn(
            Finder.packageOf(c.notificationFix.firebaseRemoteMessageClass),
            ClassMatcher.create()
                .addMethod(
                    sig(
                        "android.content.ComponentName",
                        "android.content.Context",
                        "android.content.Intent")));
    c.notificationFix.firebaseWakefulStartMethod =
        f.staticMethodOf(
            c.notificationFix.firebaseWakefulStartClass,
            "android.content.ComponentName",
            "android.content.Context",
            "android.content.Intent");

    c.notificationFix.fisCertDigestClass =
        f.clsIn(
            Finder.packageOf(
                f.classUsing("context.getSystemService(Context.APP_OPS_SERVICE) is null")),
            ClassMatcher.create()
                .addMethod(sig("byte[]", "android.content.Context", "java.lang.String")));
    c.notificationFix.fisCertDigestMethod =
        f.method(
            c.notificationFix.fisCertDigestClass, MethodMatcher.create().usingEqStrings("SHA1"));
    String mismatch =
        " requires Google Play Store, but its signature doesn't match that of Google Play services.";
    c.notificationFix.gmsAvailabilityClass = f.classUsing(mismatch);
    c.notificationFix.gmsAvailabilityMethod =
        f.method(
            c.notificationFix.gmsAvailabilityClass,
            MethodMatcher.create().usingEqStrings(mismatch));
    c.notificationFix.gmsSignatureCheckClass =
        f.classUsing("Unable to obtain package certificate history.");
    c.notificationFix.gmsSignatureCheckMethod =
        f.method(
            c.notificationFix.gmsSignatureCheckClass,
            MethodMatcher.create()
                .usingEqStrings("cannot store more than Integer.MAX_VALUE elements"));

    c.notificationFix.lineFcmServiceBaseClass = f.classUsing("Firebase-Messaging-Intent-Handle");
    c.notificationFix.lineFcmDispatchMethod =
        f.method(
            c.notificationFix.lineFcmServiceClass,
            MethodMatcher.create().usingEqStrings("android.intent.action.BATTERY_CHANGED"));
    c.notificationFix.lineFcmOwnershipMethod =
        f.method(
            c.notificationFix.lineFcmServiceClass,
            MethodMatcher.create()
                .usingEqStrings("call to 'resume' before 'invoke' with coroutine"));
    // The deprecated (String) callback LINE overrides to register the token
    c.notificationFix.lineFcmTokenMethod =
        f.annotatedMethodOf(
            c.notificationFix.firebaseMessagingServiceClass,
            "java.lang.Deprecated",
            "void",
            "java.lang.String");
  }

  // The lifecycle observer arms the disconnect timeout in the background
  private static void legy(Finder f, LineVersion.Config c) {
    c.notificationFix.legyStreamingStateClass =
        f.clsIn(
            "com.linecorp.legy.streaming",
            ClassMatcher.create()
                .superClass("java.lang.Enum")
                .addField(FieldMatcher.create().name("BACKGROUND")));
    String manager = Finder.outer(c.notificationFix.legyStreamingStateClass);
    c.notificationFix.legyHandlerField = f.fieldOf(manager, "android.os.Handler");
    c.notificationFix.legyStateField = f.ctorField(manager, 16, c.kotlin.stateFlowImplClass);
    c.notificationFix.legyRunnableField =
        f.ctorField(manager, 17, ClassMatcher.create().addInterface("java.lang.Runnable"));
    c.notificationFix.legyDisconnectRunnableClass =
        f.fieldType(manager, c.notificationFix.legyRunnableField);

    MethodMatcher onStateChanged =
        sig("void", c.androidx.lifecycleOwnerClass, c.androidx.lifecycleEventClass);
    c.notificationFix.legyStreamingLifecycleClass =
        f.clsIn(
            "com.linecorp.legy.streaming",
            ClassMatcher.create()
                .addField(FieldMatcher.create().type(manager))
                .addMethod(onStateChanged));
    String observer = c.notificationFix.legyStreamingLifecycleClass;
    c.notificationFix.legyStreamingLifecycleMethod =
        f.method(
            observer,
            MethodMatcher.create().addUsingField(FieldMatcher.create().name("FOREGROUND")));
    c.notificationFix.legyStreamingManagerField =
        f.fieldReadIn(observer, c.notificationFix.legyStreamingLifecycleMethod);

    for (String field : Finder.writes(f.methodData(observer, onStateChanged), manager)) {
      if (f.fieldType(manager, field).equals("long")) c.notificationFix.legyTimeoutField = field;
    }
    MethodData worker =
        f.methodData(
            manager,
            sig("boolean")
                .addInvoke(
                    MethodMatcher.create()
                        .declaredClass(
                            "com.linecorp.legy.streaming.BackgroundStreamingWorker",
                            StringMatchType.StartsWith)));
    for (String field : Finder.reads(worker, manager)) {
      if (f.fieldType(manager, field).equals("boolean")) {
        c.notificationFix.legyBackgroundWorkerFlagField = field;
      }
    }
  }
}
