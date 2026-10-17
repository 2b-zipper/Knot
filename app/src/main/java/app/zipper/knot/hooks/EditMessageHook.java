package app.zipper.knot.hooks;

import android.content.Context;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.Reflect;
import io.github.libxposed.api.XposedInterface;

public class EditMessageHook implements BaseHook {

  private static final String SPOOF_VERSION = "26.15.1";
  private static final String EDIT_MESSAGE_METHOD = "editMessage";

  private static final ThreadLocal<Boolean> spoofingVersion = ThreadLocal.withInitial(() -> false);

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    if (!config.enableMessageEdit.enabled) return;
    LineVersion.Config version = LineVersion.get();
    if (version == null || version.editMessage.labFeatureClass.isEmpty()) return;

    ClassLoader cl = lpparam.classLoader;
    hookLabGate(cl, version.editMessage);
    hookChatGate(cl, version.editMessage);
    hookAgreement(cl, version.editMessage);
    spoofVersionOnSend(cl, version);
  }

  private static void hookLabGate(ClassLoader cl, LineVersion.Config.EditMessage cfg) {
    try {
      Class<?> editMessage = Reflect.findClass(cfg.editMessageFeatureClass, cl);

      Knot.module
          .hook(
              Reflect.findMethodExact(
                  cfg.labFeatureClass, cl, cfg.methodIsFeatureEnabled, Context.class))
          .intercept(
              chain ->
                  enabled() && editMessage.isInstance(chain.getThisObject())
                      ? Boolean.TRUE
                      : chain.proceed());
      Knot.log("Knot: edit message lab gate hooked on " + cfg.labFeatureClass);
    } catch (Throwable t) {
      Knot.log("Knot: edit message lab gate hook failed: " + t);
    }
  }

  private static void hookChatGate(ClassLoader cl, LineVersion.Config.EditMessage cfg) {
    if (cfg.chatGateClass.isEmpty()) return;
    try {
      Knot.module
          .hook(Reflect.findMethodExact(cfg.chatGateClass, cl, cfg.methodIsFeatureAvailable))
          .intercept(chain -> enabled() ? Boolean.TRUE : chain.proceed());

      Knot.module
          .hook(Reflect.findMethodExact(cfg.chatGateClass, cl, cfg.methodIsLabEnabled))
          .intercept(chain -> enabled() ? Boolean.TRUE : chain.proceed());

      if (!cfg.methodIsAgreed.isEmpty()) {
        Knot.module
            .hook(Reflect.findMethodExact(cfg.chatGateClass, cl, cfg.methodIsAgreed))
            .intercept(chain -> enabled() ? Boolean.TRUE : chain.proceed());
      }
      Knot.log("Knot: edit message chat gate hooked on " + cfg.chatGateClass);
    } catch (Throwable t) {
      Knot.log("Knot: edit message chat gate hook failed: " + t);
    }
  }

  private static void hookAgreement(ClassLoader cl, LineVersion.Config.EditMessage cfg) {
    if (cfg.agreementClass.isEmpty() || cfg.methodIsAgreementDone.isEmpty()) return;
    try {
      Knot.module
          .hook(Reflect.findMethodExact(cfg.agreementClass, cl, cfg.methodIsAgreementDone))
          .intercept(chain -> enabled() ? Boolean.TRUE : chain.proceed());
      Knot.log("Knot: edit message agreement hooked on " + cfg.agreementClass);
    } catch (Throwable t) {
      Knot.log("Knot: edit message agreement hook failed: " + t);
    }
  }

  private static void spoofVersionOnSend(ClassLoader cl, LineVersion.Config version) {
    if (version.editMessage.chatGateClass.isEmpty()
        || version.thrift.protocolClass.isEmpty()
        || version.unsend.appInfoProviderClass.isEmpty()) {
      return;
    }
    try {
      hookSendBoundary(cl, version.thrift);
      hookUserAgent(cl, version.unsend);
      Knot.log("Knot: edit message version spoof armed (" + SPOOF_VERSION + ")");
    } catch (Throwable t) {
      Knot.log("Knot: edit message version spoof hook failed: " + t);
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
              boolean spoof = enabled() && EDIT_MESSAGE_METHOD.equals(chain.getArg(0));
              if (spoof) {
                spoofingVersion.set(Boolean.TRUE);
                Knot.log("Knot: edit message sent as " + SPOOF_VERSION);
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
    return Main.options.enableMessageEdit.enabled;
  }
}
