package app.zipper.knot.hooks;

import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Reflect;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Map;
import org.json.JSONObject;

public class NotificationMediaSourceHook implements BaseHook {
  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    LineVersion.Config version = LineVersion.get();
    if (version == null) return;
    LineVersion.Config.Notification mediaConfig = version.notification;
    if (!hasText(mediaConfig.decryptedResultClass) || !hasText(mediaConfig.messageClass)) return;

    Class<?> messageClass = Reflect.findClass(mediaConfig.messageClass, lpparam.classLoader);
    Constructor<?> decryptedConstructor =
        Reflect.findConstructorExact(
            Reflect.findClass(mediaConfig.decryptedResultClass, lpparam.classLoader), messageClass);

    Knot.module
        .hook(decryptedConstructor)
        .intercept(
            chain -> {
              try {
                Object message = chain.getArg(0);
                if (message != null && messageClass.isInstance(message)) {
                  captureMessage(message, mediaConfig);
                }
              } catch (Throwable t) {
                Knot.log(
                    "Knot: notification media capture failed: " + t.getClass().getSimpleName());
              }
              return chain.proceed();
            });

    try {
      hookThumbnailSource(mediaConfig, lpparam, messageClass);
    } catch (Throwable t) {
      Knot.log("Knot: notification thumbnail source hook setup failed", t);
    }

    try {
      hookStickerCacheCompletion(mediaConfig, lpparam);
    } catch (Throwable t) {
      Knot.log("Knot: sticker cache completion hook setup failed", t);
    }

    try {
      hookCombinationMetadataCompletion(mediaConfig, lpparam);
    } catch (Throwable t) {
      Knot.log("Knot: combination sticker metadata cache hook setup failed", t);
    }

    try {
      hookSticonCacheCompletion(mediaConfig, lpparam);
    } catch (Throwable t) {
      Knot.log("Knot: sticon cache completion hook setup failed", t);
    }
  }

  private static void hookCombinationMetadataCompletion(
      LineVersion.Config.Notification config, LoadParam lpparam) throws Throwable {
    if (!hasText(config.combinationStickerMetadataFileManagerClass)
        || !hasText(config.combinationStickerMetadataFileMethod)
        || !hasText(config.combinationStickerMetadataWriteMethod)
        || !hasText(config.combinationStickerMetadataResponseClass)) {
      return;
    }

    ClassLoader loader = lpparam.classLoader;
    Class<?> managerClass =
        Reflect.findClass(config.combinationStickerMetadataFileManagerClass, loader);

    Knot.module
        .hook(
            Reflect.findMethodExact(
                managerClass, config.combinationStickerMetadataFileMethod, String.class))
        .intercept(
            chain -> {
              Object result = chain.proceed();
              if (result instanceof File) {
                File file = (File) result;
                LineCombinationStickerMediaResolver.onLineMetadataCached(file);
              }
              return result;
            });

    Knot.module
        .hook(
            Reflect.findMethodExact(
                managerClass,
                config.combinationStickerMetadataWriteMethod,
                File.class,
                Reflect.findClass(config.combinationStickerMetadataResponseClass, loader)))
        .intercept(
            chain -> {
              Object result = chain.proceed();
              try {
                Object file = chain.getArg(0);
                if (file instanceof File) {
                  File metadataFile = (File) file;
                  if (Boolean.TRUE.equals(result)) {
                    LineCombinationStickerMediaResolver.onLineMetadataCached(metadataFile);
                  }
                }
              } catch (Throwable t) {
                Knot.log("Knot: combination sticker metadata cache handling failed", t);
              }
              return result;
            });
  }

  private static void hookSticonCacheCompletion(
      LineVersion.Config.Notification config, LoadParam lpparam) throws Throwable {
    if (!hasText(config.sticonImageCacheImplementationClass)
        || !hasText(config.sticonImageCachePutMethod)
        || !hasText(config.sticonImageKeyClass)) {
      return;
    }

    ClassLoader loader = lpparam.classLoader;
    Class<?> cacheClass = Reflect.findClass(config.sticonImageCacheImplementationClass, loader);
    Class<?> keyClass = Reflect.findClass(config.sticonImageKeyClass, loader);
    Knot.module
        .hook(
            Reflect.findMethodExact(
                cacheClass,
                config.sticonImageCachePutMethod,
                keyClass,
                android.graphics.drawable.Drawable.class))
        .intercept(
            chain -> {
              Object result = chain.proceed();
              try {
                Object key = chain.getArg(0);
                Object drawable = chain.getArg(1);

                LineSticonMediaResolver.onLineCacheAvailable(
                    key,
                    drawable instanceof android.graphics.drawable.Drawable
                        ? (android.graphics.drawable.Drawable) drawable
                        : null);
              } catch (Throwable t) {
                Knot.log("Knot: sticon cache completion handling failed", t);
              }
              return result;
            });
  }

  private static void hookStickerCacheCompletion(
      LineVersion.Config.Notification config, LoadParam lpparam) throws Throwable {
    ClassLoader loader = lpparam.classLoader;

    if (hasText(config.stickerFileManagerClass) && hasText(config.stickerMainCacheFileMethod)) {
      Class<?> managerClass = Reflect.findClass(config.stickerFileManagerClass, loader);
      Knot.module
          .hook(
              Reflect.findMethodExact(
                  managerClass, config.stickerMainCacheFileMethod, long.class, long.class))
          .intercept(
              chain -> {
                Object result = chain.proceed();
                if (result instanceof File) {
                  File file = (File) result;
                  LineStickerMediaResolver.onLineStickerFetchCompleted(file);
                }
                return result;
              });
    }

    if (!hasText(config.stickerRemoteFetcherClass)
        || !hasText(config.stickerRemoteCompleteMethod)
        || !hasText(config.stickerRemoteCallClass)
        || !hasText(config.stickerRemoteResponseClass)
        || !hasText(config.stickerRemoteTargetFileField)) {
      return;
    }

    Class<?> fetcherClass = Reflect.findClass(config.stickerRemoteFetcherClass, loader);
    Knot.module
        .hook(
            Reflect.findMethodExact(
                fetcherClass,
                config.stickerRemoteCompleteMethod,
                Reflect.findClass(config.stickerRemoteCallClass, loader),
                Reflect.findClass(config.stickerRemoteResponseClass, loader)))
        .intercept(
            chain -> {
              Object result = chain.proceed();
              try {
                Object receiver = chain.getThisObject();
                Object value =
                    Reflect.getObjectField(receiver, config.stickerRemoteTargetFileField);
                if (value instanceof File) {
                  File file = (File) value;
                  LineStickerMediaResolver.onLineStickerFetchCompleted(file);
                }
              } catch (Throwable t) {
                Knot.log("Knot: sticker cache completion handling failed", t);
              }
              return result;
            });
  }

  private static void hookThumbnailSource(
      LineVersion.Config.Notification config, LoadParam lpparam, Class<?> messageClass)
      throws Throwable {
    if (!hasText(config.messageProcessorClass)
        || !hasText(config.messageProcessorMethod)
        || !hasText(config.messageProcessorContentTypeClass)
        || !hasText(config.messageProcessorMetadataClass)
        || !hasText(config.messageProcessorAuxClass)
        || !hasText(config.messageProcessorContinuationClass)) {
      return;
    }

    ClassLoader loader = lpparam.classLoader;
    Class<?> metadataClass = Reflect.findClass(config.messageProcessorMetadataClass, loader);
    Knot.module
        .hook(
            Reflect.findMethodExact(
                Reflect.findClass(config.messageProcessorClass, loader),
                config.messageProcessorMethod,
                messageClass,
                Reflect.findClass(config.messageProcessorContentTypeClass, loader),
                metadataClass,
                String.class,
                Reflect.findClass(config.messageProcessorAuxClass, loader),
                Long.class,
                Reflect.findClass(config.messageProcessorContinuationClass, loader)))
        .intercept(
            chain -> {
              try {
                Object message = chain.getArg(0);
                Object contentType =
                    message == null ? null : objectField(message, config.messageContentTypeField);
                String type = contentType == null ? null : contentType.toString();
                if ("IMAGE".equals(type) || "VIDEO".equals(type)) {
                  String messageId = stringField(message, config.messageServerIdField);
                  Object metadata = chain.getArg(2);
                  Object chatId = chain.getArg(3);
                  Object localId = chain.getArg(5);
                  if (hasText(messageId)
                      && metadata != null
                      && chatId instanceof String
                      && localId instanceof Long) {
                    String chat = (String) chatId;
                    NotificationMediaThumbnailKeyStore.captureKey(
                        messageId,
                        chat,
                        (Long) localId,
                        metadataString(metadata, loader, config),
                        encryptionData(metadata, metadataClass, loader, config),
                        isSquareChat(chat, loader, config));
                  }
                }
              } catch (Throwable t) {
                Knot.log("Knot: notification thumbnail source capture failed", t);
              }
              return chain.proceed();
            });
  }

  private static String metadataString(
      Object metadata, ClassLoader loader, LineVersion.Config.Notification config) {
    if (!hasText(config.messageMetadataKeyClass)
        || !hasText(config.messageMetadataStringMethod)
        || !hasText(config.messageObsPopField)) {
      return null;
    }
    try {
      Class<?> keyClass = Reflect.findClass(config.messageMetadataKeyClass, loader);
      Object key = Reflect.getStaticObjectField(keyClass, config.messageObsPopField);
      Object value =
          Reflect.findMethodExact(metadata.getClass(), config.messageMetadataStringMethod, keyClass)
              .invoke(metadata, key);
      return value == null ? null : String.valueOf(value);
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static Object encryptionData(
      Object metadata,
      Class<?> metadataClass,
      ClassLoader loader,
      LineVersion.Config.Notification config) {
    if (!hasText(config.messageVisualBuilderClass)
        || !hasText(config.messageVisualEncryptionMethod)) {
      return null;
    }
    try {
      return Reflect.findMethodExact(
              Reflect.findClass(config.messageVisualBuilderClass, loader),
              config.messageVisualEncryptionMethod,
              metadataClass)
          .invoke(null, metadata);
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static boolean isSquareChat(
      String chatId, ClassLoader loader, LineVersion.Config.Notification config) {
    if (!hasText(config.messageSquareUtilsClass) || !hasText(config.messageSquareCheckMethod)) {
      return false;
    }
    try {
      Object value =
          Reflect.findMethodExact(
                  Reflect.findClass(config.messageSquareUtilsClass, loader),
                  config.messageSquareCheckMethod,
                  String.class)
              .invoke(null, chatId);
      return value instanceof Boolean && (Boolean) value;
    } catch (Throwable ignored) {
      return false;
    }
  }

  private static void captureMessage(Object message, LineVersion.Config.Notification config) {
    String messageId = stringField(message, config.messageServerIdField);
    if (!hasText(messageId)) return;

    Object contentType = objectField(message, config.messageContentTypeField);
    String contentTypeName = contentType == null ? null : contentType.toString();

    int type;
    String text = null;
    String parameter;
    if ("IMAGE".equals(contentTypeName)) {
      type = NotificationMediaCaptureStore.TYPE_IMAGE;
      parameter = null;
    } else if ("VIDEO".equals(contentTypeName)) {
      type = NotificationMediaCaptureStore.TYPE_VIDEO;
      parameter = null;
    } else if ("STICKER".equals(contentTypeName)) {
      type = NotificationMediaCaptureStore.TYPE_STICKER;
      parameter =
          metadataJson(
              objectField(message, config.messageMetadataField), "CSSTKID", "STKID", "STKPKGID");
    } else if ("NONE".equals(contentTypeName)) {
      parameter = metadataJson(objectField(message, config.messageMetadataField), "REPLACE");
      if (!hasText(parameter)) return;
      text = stringField(message, config.messageTextField);
      if (!hasText(text)) return;
      type = NotificationMediaCaptureStore.TYPE_STICON;
    } else {
      return;
    }

    NotificationMediaCaptureStore.capture(messageId, type, text, parameter);
  }

  private static String metadataJson(Object value, String... keys) {
    if (!(value instanceof Map) || keys == null || keys.length == 0) return null;
    Map<?, ?> metadata = (Map<?, ?>) value;
    try {
      JSONObject json = new JSONObject();
      for (String key : keys) {
        Object metadataValue = metadata.get(key);
        if (metadataValue != null) {
          String text = String.valueOf(metadataValue);
          if (hasText(text)) json.put(key, text);
        }
      }
      return json.length() == 0 ? null : json.toString();
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static Object objectField(Object object, String name) {
    if (!hasText(name)) return null;
    try {
      return Reflect.getObjectField(object, name);
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static String stringField(Object object, String name) {
    Object value = objectField(object, name);
    return value == null ? null : String.valueOf(value);
  }

  private static boolean hasText(String value) {
    return value != null && !value.isEmpty();
  }
}
