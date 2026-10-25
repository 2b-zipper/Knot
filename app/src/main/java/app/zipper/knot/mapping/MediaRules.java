package app.zipper.knot.mapping;

import static app.zipper.knot.mapping.Finder.sig;

import app.zipper.knot.LineVersion;
import java.lang.reflect.Modifier;
import java.util.List;
import org.luckypray.dexkit.query.enums.UsingType;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;

final class MediaRules {

  static final Mappings.Section[] SECTIONS = {
    MediaRules::callTone,
    MediaRules::camera,
    MediaRules::font,
    MediaRules::imageQuality,
    MediaRules::media,
    MediaRules::videoDuration,
    MediaRules::nightMode,
    MediaRules::profile
  };

  private MediaRules() {}

  private static void callTone(Finder f, LineVersion.Config c) {
    String player = f.classUsing("android.resource", "vibrator", "audio");
    c.callTone.toneSourceClass =
        Finder.param(
            f.methodData(
                player, sig("void", "android.content.Context", null, null, "int", "boolean")),
            2);
    String source = c.callTone.toneSourceClass;
    c.callTone.uriToneSourceClass =
        f.clsIn(
            Finder.packageOf(source),
            ClassMatcher.create()
                .superClass(source)
                .addMethod(MethodMatcher.create().name("<init>").paramTypes("android.net.Uri")));

    c.callTone.remoteRingbackClass =
        f.cls(
            ClassMatcher.create()
                .superClass(source)
                .addMethod(
                    MethodMatcher.create()
                        .name("<init>")
                        .paramTypes("android.content.Context", "java.lang.String", null)));
    String ringback = c.callTone.remoteRingbackClass;
    c.callTone.remoteRingbackContextField = f.fieldOf(ringback, "android.content.Context");
    c.callTone.remoteRingbackFallbackField =
        f.ctorField(ringback, 2, ClassMatcher.create().addInterface(c.kotlin.function1Class));
    c.callTone.methodToneUri =
        f.method(
            ringback,
            MethodMatcher.create()
                .addInvoke("Ljava/io/FileOutputStream;-><init>(Ljava/io/File;)V"));
    c.callTone.ringtoneWrapperClass =
        f.clsIn(
            Finder.packageOf(ringback),
            ClassMatcher.create()
                .addMethod(
                    MethodMatcher.create()
                        .name("<init>")
                        .paramTypes("android.content.Context", source)));
  }

  private static void camera(Finder f, LineVersion.Config c) {
    c.camera.cameraModuleClass = f.classUsing("nelo_enable");
    String module = c.camera.cameraModuleClass;
    // A Lazy by its shape, as R8 may rename kotlin.Lazy and narrow the field to its implementation
    ClassMatcher lazy =
        ClassMatcher.create().addMethod(sig("java.lang.Object")).addMethod(sig("boolean"));
    c.camera.methodUseExternalCamera = f.getter(module, f.ctorField(module, 2, lazy));

    c.camera.cameraLauncherClass =
        f.classUsingIdIn("com.linecorp.line.media.picker", "string", "gallery_camera_error_access");
    c.camera.methodLaunchCamera =
        f.method(
            c.camera.cameraLauncherClass,
            MethodMatcher.create().addUsingField(FieldMatcher.create().name("URL_SCHEME")));
    c.camera.launchModeClass =
        f.paramType(c.camera.cameraLauncherClass, c.camera.methodLaunchCamera, 1);
    c.camera.launchCallbackClass =
        f.paramType(c.camera.cameraLauncherClass, c.camera.methodLaunchCamera, 3);
    c.camera.launchSourceClass = f.classUsing("PROFILE_FOR_SETTING");
    c.camera.captureChoiceClass =
        f.clsIn(
            "com.linecorp.line.media.picker",
            ClassMatcher.create()
                .addMethod(
                    MethodMatcher.create()
                        .name("<init>")
                        .paramTypes(
                            "android.app.Activity",
                            c.camera.launchCallbackClass,
                            c.camera.launchSourceClass)));

    c.camera.captureChooserClass =
        Finder.declaring(
            f.methodDataIn(
                Finder.packageOf(f.classUsing("orientationBaseDirectionTransformer")),
                MethodMatcher.create()
                    .modifiers(Modifier.STATIC)
                    .paramTypes("android.content.Context", null)
                    .usingNumbers(
                        f.id("string", "chathistory_attach_dialog_label_camera"),
                        f.id("string", "chathistory_attach_dialog_label_movie_camera"))));
    c.camera.methodShowCaptureChooser =
        f.staticMethodOf(c.camera.captureChooserClass, "void", "android.content.Context", null);
  }

  private static void font(Finder f, LineVersion.Config c) {
    String blocking = "Fallbacks with blocking fetches are not supported for performance reasons";
    c.font.fontConfigClass = f.classUsing(blocking);
    c.font.methodGetFontConfig =
        f.method(c.font.fontConfigClass, MethodMatcher.create().usingEqStrings(blocking));
    c.font.fontCallbackClass = f.paramType(c.font.fontConfigClass, c.font.methodGetFontConfig, 6);
    c.font.methodOnFontChanged =
        f.methodOf(c.font.fontCallbackClass, "void", "android.graphics.Typeface");

    c.font.fontManagerClass = f.classUsing("TypefaceCompat.createFromFontInfoWithFallback");
    c.font.methodGetFontSettings =
        f.method(
            c.font.fontManagerClass,
            MethodMatcher.create().addInvoke("Landroid/os/Handler;-><init>()V"));
    c.font.fontRequestExecutorClass =
        f.paramType(c.font.fontManagerClass, c.font.methodGetFontSettings, 3);
    c.font.fontCallbackWithHandlerClass =
        f.paramType(c.font.fontManagerClass, c.font.methodGetFontSettings, 4);
  }

  private static void imageQuality(Finder f, LineVersion.Config c) {
    c.imageQuality.imageUtilClass = f.classUsing("AutoPinChatAnalyzer");
    c.imageQuality.qualityProfileHighClass = f.classUsing("High(maxDimension=");
    c.imageQuality.qualityProfileMediumClass = f.classUsing("Medium(maxDimension=");
    String high = c.imageQuality.qualityProfileHighClass;
    c.imageQuality.methodGetMaxDimension = f.getter(high, f.propertyField(high, "maxDimension"));
    c.imageQuality.methodGetQuality = f.getter(high, f.propertyField(high, "quality"));
  }

  private static void media(Finder f, LineVersion.Config c) {
    c.media.droppedMediaPreprocessorClass =
        f.classAnnotatedIn(
            Finder.packageOf(f.classUsing("INVALID_VIDEO")),
            "com.linecorp.line.chat.draganddrop.ChatHistoryDroppedMediaContentPreprocessor$createDroppedMediaSendMessageRequest$2");
    c.media.galleryViewClass = f.classUsing("GalleryViewController.MuteMessageAgreement");
    c.media.fieldGalleryDurationLimit = f.fieldOf(c.media.galleryViewClass, "long");
    c.media.mediaPickerParamsClass = f.classUsing(", isPopupErrorToastOriginalOption=");
    c.media.fieldMediaPickerMaxVideoDuration =
        f.ctorField(c.media.mediaPickerParamsClass, 73, "long");

    c.media.selectionValidatorClass =
        f.classUsing("\n     applyTrimmingData:\n     startPointInUs : ");
    c.media.selectionValidatorMethod =
        f.method(
            c.media.selectionValidatorClass,
            MethodMatcher.create().addUsingField(FieldMatcher.create().name("SECONDS")));
    c.media.selectionValidatorParamClass =
        f.paramType(c.media.selectionValidatorClass, c.media.selectionValidatorMethod, 0);

    // onCreate reads the length limit from the intent right before the mute flag
    String trimmer = c.media.videoProfileTrimmerActivityClass;
    List<String> written =
        Finder.writes(
            f.methodData(
                trimmer,
                MethodMatcher.create()
                    .name("onCreate")
                    .usingEqStrings("extra_video_profile_max_length_sec")),
            trimmer);
    for (int i = 1; i < written.size(); i++) {
      if (!f.fieldType(trimmer, written.get(i)).equals("boolean")) continue;
      if (f.fieldType(trimmer, written.get(i - 1)).equals("int")) {
        c.media.fieldVideoProfileTrimmerLimit = written.get(i - 1);
      }
      break;
    }
  }

  private static void videoDuration(Finder f, LineVersion.Config c) {
    // The chat's dependency module provides it
    c.media.videoDurationCheckClass =
        f.returnTypeWhere(
            f.classUsing("maybeRequestPayAndBankRegistrationCheck"),
            ClassMatcher.create()
                .addMethod(
                    MethodMatcher.create().name("<init>").paramTypes("android.content.Context"))
                .addMethod(
                    MethodMatcher.create()
                        .addUsingField(
                            FieldMatcher.create().name("EXCEED_DURATION"), UsingType.Read)
                        .addUsingField(
                            FieldMatcher.create().name("EXCEED_FILE_SIZE"), UsingType.Read)));
    String check = c.media.videoDurationCheckClass;
    c.media.videoDurationCheckMethod =
        f.method(check, MethodMatcher.create().usingEqStrings("fileSize"));
    // The (uri) overloads pass checkFileSize to c(uri, map, boolean) as a constant
    c.media.videoDurationCheckWithSizeMethod =
        f.method(check, sig(null, "android.net.Uri").usingNumbers(1));
    c.media.videoDurationCheckWithoutSizeMethod =
        f.method(check, sig(null, "android.net.Uri").usingNumbers(0));

    c.media.videoDurationSuccessClass =
        f.clsIn(
            Finder.packageOf(f.classUsing("EXCEED_FILE_SIZE")),
            ClassMatcher.create()
                .superClass(f.returnType(check, c.media.videoDurationCheckMethod))
                .fieldCount(1)
                .addMethod(MethodMatcher.create().name("<clinit>")));
    c.media.fieldVideoDurationSuccess =
        f.fieldOf(c.media.videoDurationSuccessClass, c.media.videoDurationSuccessClass);
  }

  private static void nightMode(Finder f, LineVersion.Config c) {
    c.nightMode.darkThemeManagerClass = f.classUsing("Error while loading ");
    String manager = c.nightMode.darkThemeManagerClass;
    c.nightMode.methodIsDarkTheme =
        f.method(
            manager, sig("boolean").addInvoke("Ljava/lang/Object;->getClass()Ljava/lang/Class;"));
    // Compares with the default theme's id, without the dark theme check
    c.nightMode.methodIsDefaultTheme =
        f.method(
            manager,
            sig("boolean")
                .usingEqStrings("3e261192-3a69-4849-b35d-35aeddd5a368")
                .addNoneOf(
                    MethodMatcher.create().addUsingField(FieldMatcher.create().name("DARK"))));
    c.nightMode.methodThemeMode =
        f.method(
            manager,
            MethodMatcher.create()
                .paramCount(0)
                .returnType(
                    ClassMatcher.create()
                        .addField(FieldMatcher.create().name("LIGHT"))
                        .addField(FieldMatcher.create().name("DARK"))));

    // LINE sets AppCompat's default night mode from its ConfigureNightModeTask
    c.nightMode.nightModeConfiguratorClass =
        f.cls(
            ClassMatcher.create()
                .fieldCount(1)
                .addMethod(
                    sig("void", "boolean")
                        .modifiers(Modifier.STATIC)
                        .addCaller(
                            MethodMatcher.create()
                                .declaredClass(
                                    ClassMatcher.create()
                                        .usingEqStrings("ConfigureNightModeTask")))));
    c.nightMode.fieldSystemDarkMode = f.fieldOf(c.nightMode.nightModeConfiguratorClass, "boolean");
    c.nightMode.methodApplyNightMode =
        f.staticMethodOf(c.nightMode.nightModeConfiguratorClass, "void", "boolean");
  }

  private static void profile(Finder f, LineVersion.Config c) {
    c.profile.h13baClass =
        f.clsIn(
            Finder.packageOf(f.classUsing("InconsistencyProfileSyncResult(mismatchFieldIds=")),
            ClassMatcher.create()
                .modifiers(Modifier.INTERFACE)
                .addMethod(
                    MethodMatcher.create()
                        .name("getProfile")
                        .returnType(f.classUsing("Profile(authTokenV1=")))
                .addField(FieldMatcher.create().modifiers(Modifier.STATIC)));
    c.profile.fieldH3 =
        f.field(c.profile.h13baClass, FieldMatcher.create().modifiers(Modifier.STATIC));
    c.profile.fieldMid =
        f.propertyField(f.returnType(c.profile.h13baClass, c.profile.methodGetProfile), "mid");
  }
}
