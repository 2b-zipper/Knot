package app.zipper.knot.hooks;

import android.os.SystemClock;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.Reflect;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class MuteMessageHook implements BaseHook {

  private static final String LAB_MODE = "LAB";
  private static final String SILENT_STATE = "TO_BE_SENT_SILENTLY";
  private static final String SPOOF_VERSION = "26.11.0";
  private static final long MUTE_SEND_WINDOW_MS = 10_000;
  private static final Set<String> SEND_METHODS =
      new HashSet<>(Arrays.asList("sendMessage", "sendMessageForLineCompactProtocol"));

  private static volatile long muteSendAt;
  private static final ThreadLocal<Boolean> spoofingVersion = ThreadLocal.withInitial(() -> false);

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    if (!config.enableMuteMessage.enabled) return;
    LineVersion.Config version = LineVersion.get();
    if (version == null || version.muteMessage.labFeatureClass.isEmpty()) return;

    ClassLoader cl = lpparam.classLoader;
    hookLabGate(cl, version.muteMessage);
    hookSendMode(cl, version.muteMessage);
    markMuteSends(cl, version.muteMessage);
    spoofVersionOnSend(cl, version);
  }

  private static void hookLabGate(ClassLoader cl, LineVersion.Config.MuteMessage cfg) {
    try {
      Class<?> silentMessage = Reflect.findClass(cfg.silentMessageFeatureClass, cl);
      Method gate =
          findMethod(
              Reflect.findClass(cfg.labFeatureClass, cl),
              cfg.methodIsFeatureEnabled,
              method -> method.getReturnType() == boolean.class);
      if (gate == null) {
        Knot.log("Knot: mute message lab gate not found on " + cfg.labFeatureClass);
        return;
      }

      Knot.module
          .hook(gate)
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

  // Mute messages share the send RPC with normal ones, so the request is tagged while it is built.
  private static void markMuteSends(ClassLoader cl, LineVersion.Config.MuteMessage cfg) {
    if (cfg.silentFlagWriterClass.isEmpty()) return;
    try {
      Method writer =
          findMethod(
              Reflect.findClass(cfg.silentFlagWriterClass, cl),
              cfg.methodWriteSilentFlag,
              method -> method.getParameterTypes().length == 2);
      if (writer == null) {
        Knot.log("Knot: mute message silent writer not found on " + cfg.silentFlagWriterClass);
        return;
      }

      Knot.module
          .hook(writer)
          .intercept(
              chain -> {
                if (enabled() && isSilentState(chain.getArg(1))) {
                  muteSendAt = SystemClock.elapsedRealtime();
                }
                return chain.proceed();
              });
      Knot.log("Knot: mute message send marker hooked on " + cfg.silentFlagWriterClass);
    } catch (Throwable t) {
      Knot.log("Knot: mute message send marker hook failed: " + t);
    }
  }

  private static void spoofVersionOnSend(ClassLoader cl, LineVersion.Config version) {
    if (version.muteMessage.silentFlagWriterClass.isEmpty()
        || version.thrift.protocolClass.isEmpty()
        || version.unsend.appInfoProviderClass.isEmpty()) {
      return;
    }
    try {
      hookSendBoundary(cl, version.thrift);
      hookUserAgent(cl, version.unsend);
      Knot.log("Knot: mute message version spoof armed (" + SPOOF_VERSION + ")");
    } catch (Throwable t) {
      Knot.log("Knot: mute message version spoof hook failed: " + t);
    }
  }

  private static void hookSendBoundary(ClassLoader cl, LineVersion.Config.Thrift thrift) {
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
              boolean spoof = SEND_METHODS.contains(chain.getArg(0)) && muteSendPending();
              if (spoof) {
                muteSendAt = 0;
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

  private static boolean enabled() {
    return Main.options.enableMuteMessage.enabled;
  }

  private static boolean muteSendPending() {
    long markedAt = muteSendAt;
    return markedAt > 0 && SystemClock.elapsedRealtime() - markedAt < MUTE_SEND_WINDOW_MS;
  }

  private static boolean isSilentState(Object state) {
    return state instanceof Enum && SILENT_STATE.equals(((Enum<?>) state).name());
  }

  private static Object enumConstant(Class<?> enumClass, String name) {
    for (Object constant : enumClass.getEnumConstants()) {
      if (((Enum<?>) constant).name().equals(name)) return constant;
    }
    return null;
  }

  // 26.10.x declares the lab gate without arguments, 26.11.0 and later take a Context.
  private static Method findMethod(Class<?> owner, String name, Predicate<Method> filter) {
    for (Method method : owner.getDeclaredMethods()) {
      if (method.getName().equals(name) && filter.test(method)) {
        method.setAccessible(true);
        return method;
      }
    }
    return null;
  }
}
