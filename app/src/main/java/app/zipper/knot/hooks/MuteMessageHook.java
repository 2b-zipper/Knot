package app.zipper.knot.hooks;

import android.content.Context;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.Reflect;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MuteMessageHook implements BaseHook {

  private static final String LAB_MODE = "LAB";
  private static final String NOTIFICATION_DISABLED = "NOTIFICATION_DISABLED";
  private static final String SPOOF_VERSION = "26.11.0";
  private static final Set<String> SEND_METHODS =
      new HashSet<>(Arrays.asList("sendMessage", "sendMessageForLineCompactProtocol"));

  private static final ThreadLocal<Boolean> spoofingVersion = ThreadLocal.withInitial(() -> false);

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    if (!config.enableMuteMessage.enabled) return;
    LineVersion.Config version = LineVersion.get();
    if (version == null || version.muteMessage.labFeatureClass.isEmpty()) return;

    ClassLoader cl = lpparam.classLoader;
    hookLabGate(cl, version.muteMessage);
    hookSendMode(cl, version.muteMessage);
    spoofVersionOnSend(cl, version);
  }

  private static void hookLabGate(ClassLoader cl, LineVersion.Config.MuteMessage cfg) {
    try {
      Class<?> silentMessage = Reflect.findClass(cfg.silentMessageFeatureClass, cl);

      Knot.module
          .hook(
              Reflect.findMethodExact(
                  cfg.labFeatureClass, cl, cfg.methodIsFeatureEnabled, Context.class))
          .intercept(
              chain ->
                  enabled() && silentMessage.isInstance(chain.getThisObject())
                      ? Boolean.TRUE
                      : chain.proceed());
      Knot.log("Knot: mute message lab gate hooked on " + cfg.labFeatureClass);
    } catch (Throwable t) {
      Knot.log("Knot: mute message lab gate hook failed: " + t);
    }
  }

  // PREMIUM mode marks the request as a paid mute message instead of asking the server to mute it.
  private static void hookSendMode(ClassLoader cl, LineVersion.Config.MuteMessage cfg) {
    if (cfg.sendModeClass.isEmpty()) return;
    try {
      Object labMode = enumConstant(Reflect.findClass(cfg.sendModeEnumClass, cl), LAB_MODE);
      if (labMode == null) {
        Knot.log("Knot: mute message LAB mode not found on " + cfg.sendModeEnumClass);
        return;
      }

      Knot.module
          .hook(Reflect.findMethodExact(cfg.sendModeClass, cl, cfg.methodSendMode))
          .intercept(chain -> enabled() ? labMode : chain.proceed());
      Knot.log("Knot: mute message send mode forced to LAB on " + cfg.sendModeClass);
    } catch (Throwable t) {
      Knot.log("Knot: mute message send mode hook failed: " + t);
    }
  }

  private static void spoofVersionOnSend(ClassLoader cl, LineVersion.Config version) {
    if (version.thrift.protocolClass.isEmpty() || version.unsend.appInfoProviderClass.isEmpty()) {
      return;
    }
    try {
      hookSendBoundary(cl, version);
      hookUserAgent(cl, version.unsend);
      Knot.log("Knot: mute message version spoof armed (" + SPOOF_VERSION + ")");
    } catch (Throwable t) {
      Knot.log("Knot: mute message version spoof hook failed: " + t);
    }
  }

  private static void hookSendBoundary(ClassLoader cl, LineVersion.Config version) {
    LineVersion.Config.Thrift thrift = version.thrift;
    Knot.module
        .hook(
            Reflect.findMethodExact(
                thrift.protocolClass,
                cl,
                thrift.methodWriteMessageBegin,
                String.class,
                thrift.messageClass))
        .intercept(
            chain -> {
              boolean spoof =
                  enabled()
                      && SEND_METHODS.contains(chain.getArg(0))
                      && isMuteMessage(chain.getArg(1), version);
              if (spoof) {
                spoofingVersion.set(Boolean.TRUE);
                Knot.log("Knot: mute message sent as " + SPOOF_VERSION);
              }
              try {
                return chain.proceed();
              } finally {
                if (spoof) spoofingVersion.set(Boolean.FALSE);
              }
            });
  }

  private static void hookUserAgent(ClassLoader cl, LineVersion.Config.Unsend unsend) {
    Class<?> appInfo = Reflect.findClass(unsend.appInfoProviderClass, cl);
    XposedInterface.Hooker patch =
        chain -> {
          Object result = chain.proceed();
          if (!spoofingVersion.get() || !(result instanceof String)) return result;
          return ((String) result).replaceAll("(\\d+\\.\\d+\\.\\d+)", SPOOF_VERSION);
        };

    Knot.module
        .hook(Reflect.findMethodExact(appInfo, unsend.methodGetFullUserAgent))
        .intercept(patch);
    Knot.module
        .hook(Reflect.findMethodExact(appInfo, unsend.methodGetSimpleUserAgent))
        .intercept(patch);
  }

  private static boolean isMuteMessage(Object args, LineVersion.Config version) {
    if (args == null) return false;
    String metaFieldName = version != null ? version.notification.messageMetadataField : "";
    if (hasNotificationDisabled(args, metaFieldName)) return true;

    try {
      String messageClassName = version != null ? version.notification.messageClass : "";
      for (Field field : args.getClass().getDeclaredFields()) {
        Class<?> type = field.getType();
        if (type.isPrimitive() || type == String.class) continue;
        if (!messageClassName.isEmpty() && !type.getName().equals(messageClassName)) {
          if (type.getName().startsWith("java.")) continue;
        }
        field.setAccessible(true);
        Object message = field.get(args);
        if (message != null && hasNotificationDisabled(message, metaFieldName)) {
          return true;
        }
      }
    } catch (Throwable t) {
      Knot.log("Knot: mute message inspection failed: " + t);
    }
    return false;
  }

  private static boolean hasNotificationDisabled(Object target, String metaFieldName) {
    if (target == null) return false;
    try {
      if (!metaFieldName.isEmpty()) {
        Field f = target.getClass().getDeclaredField(metaFieldName);
        f.setAccessible(true);
        Object meta = f.get(target);
        if (meta instanceof Map && isNotificationDisabledMap((Map<?, ?>) meta)) {
          return true;
        }
      }
    } catch (Throwable ignored) {
    }

    try {
      for (Field f : target.getClass().getDeclaredFields()) {
        if (Map.class.isAssignableFrom(f.getType())) {
          f.setAccessible(true);
          Object meta = f.get(target);
          if (meta instanceof Map && isNotificationDisabledMap((Map<?, ?>) meta)) {
            return true;
          }
        }
      }
    } catch (Throwable ignored) {
    }
    return false;
  }

  private static boolean isNotificationDisabledMap(Map<?, ?> map) {
    Object val = map.get(NOTIFICATION_DISABLED);
    return "true".equals(val) || Boolean.TRUE.equals(val);
  }

  private static boolean enabled() {
    return Main.options.enableMuteMessage.enabled;
  }

  private static Object enumConstant(Class<?> enumClass, String name) {
    for (Object constant : enumClass.getEnumConstants()) {
      if (((Enum<?>) constant).name().equals(name)) return constant;
    }
    return null;
  }
}
