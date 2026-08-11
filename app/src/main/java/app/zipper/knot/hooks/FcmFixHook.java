package app.zipper.knot.hooks;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Handler;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Reflect;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Queue;

public class FcmFixHook implements BaseHook {

  private static final boolean VERBOSE_LOGGING = false;

  private static final String REAL_GMS_PACKAGE = "com.google.android.gms";

  private static volatile boolean forceNextTokenRefresh = false;
  private static volatile boolean refreshHookReady = false;
  private static volatile ClassLoader lastClassLoader = null;

  private static boolean isEnabled(KnotConfig config) {
    return config.experimentalFcmFix.enabled;
  }

  private static boolean isFisMode(KnotConfig config) {
    return app.zipper.knot.utils.ModuleStrings.FCM_FIX_MODE_FIS.equals(config.fcmFixMode.value);
  }

  private static byte[] hexDecode(String hex) {
    int len = hex.length();
    byte[] out = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      out[i / 2] =
          (byte)
              ((Character.digit(hex.charAt(i), 16) << 4) | Character.digit(hex.charAt(i + 1), 16));
    }
    return out;
  }

  private static void logVerbose(String message) {
    if (VERBOSE_LOGGING) {
      Knot.log("Knot: " + message);
    }
  }

  private static Object findSurroundingObject(Object innerObject, String outerClassName) {
    if (innerObject == null) {
      return null;
    }

    try {
      return Reflect.getSurroundingThis(innerObject);
    } catch (Throwable ignored) {
    }

    try {
      return Reflect.getObjectField(innerObject, "this$0");
    } catch (Throwable ignored) {
    }

    try {
      for (Field field : innerObject.getClass().getDeclaredFields()) {
        if (Modifier.isStatic(field.getModifiers())) {
          continue;
        }
        Class<?> fieldType = field.getType();
        if (fieldType == null || !outerClassName.equals(fieldType.getName())) {
          continue;
        }
        field.setAccessible(true);
        return field.get(innerObject);
      }
    } catch (Throwable ignored) {
    }

    try {
      return Reflect.getObjectField(innerObject, "a");
    } catch (Throwable ignored) {
    }

    return null;
  }

  @SuppressWarnings("deprecation")
  private static Object readBundleValue(Bundle bundle, String key) {
    return bundle.get(key);
  }

  private static String describeBundle(Bundle bundle) {
    if (bundle == null || bundle.isEmpty()) {
      return "{}";
    }

    ArrayList<String> keys = new ArrayList<>(bundle.keySet());
    Collections.sort(keys);
    StringBuilder sb = new StringBuilder("{");
    boolean first = true;
    for (String key : keys) {
      if (!first) {
        sb.append(", ");
      }
      first = false;
      sb.append(key).append('=');

      Object value = readBundleValue(bundle, key);
      if (value instanceof byte[]) {
        sb.append("byte[").append(((byte[]) value).length).append(']');
        continue;
      }
      if (value instanceof Bundle) {
        sb.append("Bundle");
        continue;
      }

      String text = String.valueOf(value);
      if (text.length() > 120) {
        text = text.substring(0, 120) + "...";
      }
      sb.append(text);
    }
    sb.append('}');
    return sb.toString();
  }

  private static String describeIntent(Intent intent) {
    if (intent == null) {
      return "intent=null";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("action=").append(intent.getAction());
    if (intent.getPackage() != null) {
      sb.append(", pkg=").append(intent.getPackage());
    }

    ComponentName component = intent.getComponent();
    if (component != null) {
      sb.append(", component=").append(component.flattenToShortString());
    }

    sb.append(", flags=0x").append(Integer.toHexString(intent.getFlags()));

    Bundle extras = intent.getExtras();
    if (extras != null && !extras.isEmpty()) {
      sb.append(", extras=").append(describeBundle(extras));
    }
    return sb.toString();
  }

  private static void logIntent(String prefix, Intent intent) {
    if (!VERBOSE_LOGGING) {
      return;
    }
    Knot.log("Knot: " + prefix + " " + describeIntent(intent));
  }

  private static Object resolveLegyStreamingManager(
      Object observer, LineVersion.Config.NotificationFix fixCfg) {
    String outerClassName = fixCfg.legyStreamingLifecycleClass.split("\\$")[0];
    return findSurroundingObject(observer, outerClassName);
  }

  private static void suppressLegyBackgroundDisconnect(
      Object streamingManager, LineVersion.Config.NotificationFix fixCfg, Object backgroundState)
      throws Throwable {
    Object stateField = Reflect.getObjectField(streamingManager, fixCfg.legyStateField);
    Reflect.callMethod(stateField, "setValue", backgroundState);
    Reflect.setLongField(streamingManager, fixCfg.legyTimeoutField, Long.MAX_VALUE);
    Reflect.setBooleanField(streamingManager, fixCfg.legyBackgroundWorkerFlagField, false);

    Handler handler = (Handler) Reflect.getObjectField(streamingManager, fixCfg.legyHandlerField);
    Runnable closeRunnable =
        (Runnable) Reflect.getObjectField(streamingManager, fixCfg.legyRunnableField);
    handler.removeCallbacks(closeRunnable);
  }

  private static boolean deliverMessagingIntentDirectly(
      Context context,
      ClassLoader cl,
      LineVersion.Config.NotificationFix fixCfg,
      Intent originalIntent) {
    if (context == null || cl == null || originalIntent == null) {
      return false;
    }

    try {
      Context appContext = context.getApplicationContext();
      Class<?> dispatcherClass = Reflect.findClass(fixCfg.firebaseDispatcherClass, cl);
      Object dispatcher =
          Reflect.getStaticObjectField(dispatcherClass, fixCfg.firebaseDispatcherSingletonField);
      if (dispatcher == null) {
        return false;
      }
      Object queueObj = Reflect.getObjectField(dispatcher, fixCfg.firebaseDispatcherQueueField);
      if (!(queueObj instanceof Queue)) {
        Knot.log("Knot: Firebase direct-delivery queue unavailable");
        return false;
      }

      @SuppressWarnings("unchecked")
      Queue<Intent> queue = (Queue<Intent>) queueObj;
      Intent queuedIntent = new Intent(originalIntent);
      if (!queue.offer(queuedIntent)) {
        Knot.log("Knot: Firebase direct-delivery queue rejected intent");
        return false;
      }

      Intent serviceIntent = new Intent("com.google.firebase.MESSAGING_EVENT");
      serviceIntent.setPackage(appContext.getPackageName());
      serviceIntent.setClassName(appContext.getPackageName(), fixCfg.lineFcmServiceClass);

      Object component = null;
      try {
        component =
            Reflect.callStaticMethod(
                Reflect.findClass(fixCfg.firebaseWakefulStartClass, cl),
                fixCfg.firebaseWakefulStartMethod,
                appContext,
                serviceIntent);
      } catch (Throwable wakefulFailure) {
        Knot.log("Knot: wakeful Firebase direct-delivery failed, falling back: " + wakefulFailure);
        component = appContext.startService(serviceIntent);
      }

      if (component == null) {
        queue.remove(queuedIntent);
        Knot.log("Knot: Firebase direct-delivery startService returned null");
        return false;
      }

      logVerbose("forced direct Firebase service delivery");
      logIntent("forced-direct-intent", originalIntent);
      return true;
    } catch (Throwable t) {
      Knot.log("Knot: Firebase direct-delivery failed: " + t);
      return false;
    }
  }

  private static void hookFisCertDigest(
      ClassLoader cl, KnotConfig config, LineVersion.Config.NotificationFix fixCfg) {
    try {
      Knot.module
          .hook(
              Reflect.findMethodExact(
                  fixCfg.fisCertDigestClass,
                  cl,
                  fixCfg.fisCertDigestMethod,
                  Context.class,
                  String.class))
          .intercept(
              chain -> {
                if (!isEnabled(config)) return chain.proceed();
                logVerbose("FIS: spoofed cert digest to official SHA-1");
                return hexDecode(fixCfg.fisCertSha1);
              });
      Knot.log("Knot: FIS cert digest hook installed on " + fixCfg.fisCertDigestClass);
    } catch (Throwable t) {
      Knot.log("Knot: FIS cert digest hook failed: " + t);
    }
  }

  /**
   * Hook to bypass LINE's GMS signature verification. With a microG-RE fork the signing certificate
   * never matches Google's, so the check always fails. Only GMS-related PackageInfo instances are
   * affected; unrelated lookups proceed normally.
   */
  private static void hookGmsSignatureCheck(
      ClassLoader cl, KnotConfig config, LineVersion.Config.NotificationFix fixCfg) {
    if (fixCfg.gmsSignatureCheckClass == null || fixCfg.gmsSignatureCheckClass.isEmpty()) {
      return;
    }
    try {
      Class<?> signatureCheckClass = Reflect.findClass(fixCfg.gmsSignatureCheckClass, cl);
      Knot.module
          .hook(
              Reflect.findMethodExact(
                  signatureCheckClass,
                  fixCfg.gmsSignatureCheckMethod,
                  PackageInfo.class,
                  boolean.class))
          .intercept(
              chain -> {
                if (!isEnabled(config)) return chain.proceed();
                Object arg = chain.getArg(0);
                if (!(arg instanceof PackageInfo)) return chain.proceed();
                PackageInfo pi = (PackageInfo) arg;
                if (pi.packageName != null && REAL_GMS_PACKAGE.equals(pi.packageName)) {
                  logVerbose("GMS signature check bypassed for " + pi.packageName);
                  return Boolean.TRUE;
                }
                return chain.proceed();
              });
      Knot.log("Knot: GMS signature check hook installed on " + fixCfg.gmsSignatureCheckClass);
    } catch (Throwable t) {
      Knot.log("Knot: GMS signature check hook failed: " + t);
    }
  }

  /**
   * Hook to always report GMS as available (success code 0). Even with the signature check
   * bypassed, the presence check can fail on devices. so force success when microG-RE is in use.
   */
  private static void hookGmsAvailability(
      ClassLoader cl, KnotConfig config, LineVersion.Config.NotificationFix fixCfg) {
    if (fixCfg.gmsAvailabilityClass == null || fixCfg.gmsAvailabilityClass.isEmpty()) {
      return;
    }
    try {
      Class<?> availabilityClass = Reflect.findClass(fixCfg.gmsAvailabilityClass, cl);
      Knot.module
          .hook(
              Reflect.findMethodExact(
                  availabilityClass, fixCfg.gmsAvailabilityMethod, Context.class, int.class))
          .intercept(
              chain -> {
                if (!isEnabled(config)) return chain.proceed();
                logVerbose("GMS availability check bypassed -> 0");
                return Integer.valueOf(0);
              });
      Knot.log("Knot: GMS availability hook installed on " + fixCfg.gmsAvailabilityClass);
    } catch (Throwable t) {
      Knot.log("Knot: GMS availability hook failed: " + t);
    }
  }

  /**
   * Hook FirebaseMessaging's token freshness check. When the UI sets forceNextTokenRefresh this
   * returns true so LINE's token fetch path performs a fresh registration instead of returning the
   * cached token.
   */
  private static void hookForceTokenRefresh(ClassLoader cl) {
    try {
      Class<?> fmClass = Reflect.findClass("com.google.firebase.messaging.FirebaseMessaging", cl);
      for (Method m : fmClass.getDeclaredMethods()) {
        if ("i".equals(m.getName())
            && m.getParameterTypes().length == 1
            && m.getReturnType() == boolean.class) {
          m.setAccessible(true);
          Knot.module
              .hook(m)
              .intercept(
                  chain -> {
                    if (forceNextTokenRefresh) {
                      forceNextTokenRefresh = false;
                      return Boolean.TRUE;
                    }
                    return chain.proceed();
                  });
          refreshHookReady = true;
          Knot.log("Knot: FirebaseMessaging token refresh hook installed");
          return;
        }
      }
      Knot.log("Knot: FirebaseMessaging freshness method not found");
    } catch (Throwable t) {
      Knot.log("Knot: failed to install FirebaseMessaging refresh hook: " + t);
    }
  }

  /**
   * Force a fresh FCM token registration from the Knot settings UI. Runs in the same LINE process;
   * the actual fetch happens on a background thread.
   */
  public static boolean requestFcmTokenRefresh(ClassLoader cl) {
    ClassLoader loader = lastClassLoader != null ? lastClassLoader : cl;
    if (loader == null) {
      Knot.log("Knot: FCM token refresh requested without a classloader");
      return false;
    }
    try {
      Class<?> fmClass =
          Reflect.findClass("com.google.firebase.messaging.FirebaseMessaging", loader);
      Class<?> firebaseAppClass = Reflect.findClass("ur.e", loader);
      Object defaultApp = Reflect.callStaticMethod(firebaseAppClass, "c");
      Object fm = Reflect.callStaticMethod(fmClass, "getInstance", defaultApp);
      forceNextTokenRefresh = true;
      final Object fmRef = fm;
      new Thread(
              () -> {
                try {
                  Reflect.callMethod(fmRef, "a");
                  Knot.log(
                      "Knot: FCM token refresh completed"
                          + (refreshHookReady ? "" : " (force hook not active)"));
                } catch (Throwable t) {
                  Knot.log("Knot: FCM token refresh failed: " + t);
                }
              },
              "knot-fcm-refresh")
          .start();
      return true;
    } catch (Throwable t) {
      Knot.log("Knot: FCM token refresh setup failed: " + t);
      forceNextTokenRefresh = false;
      return false;
    }
  }

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    final ClassLoader cl = lpparam.classLoader;
    LineVersion.Config versionConfig = LineVersion.get();
    if (versionConfig == null) {
      return;
    }
    final LineVersion.Config.NotificationFix fixCfg = versionConfig.notificationFix;

    lastClassLoader = cl;

    // GMS availability/signature hooks apply to both FIS and Legy modes.
    hookGmsSignatureCheck(cl, config, fixCfg);
    hookGmsAvailability(cl, config, fixCfg);

    // Install the FirebaseMessaging freshness hook so a UI-triggered token
    // refresh can force a new registration.
    hookForceTokenRefresh(cl);

    if (isFisMode(config)) {
      hookFisCertDigest(cl, config, fixCfg);
      return;
    }

    try {
      final Class<?> streamingStateClass = Reflect.findClass(fixCfg.legyStreamingStateClass, cl);
      final Object backgroundState =
          Reflect.getStaticObjectField(streamingStateClass, fixCfg.legyBackgroundStateField);

      Knot.module
          .hook(
              Reflect.findMethodExact(
                  fixCfg.legyStreamingLifecycleClass,
                  cl,
                  fixCfg.legyStreamingLifecycleMethod,
                  Reflect.findClass(fixCfg.legyLifecycleOwnerClass, cl),
                  Reflect.findClass(fixCfg.legyLifecycleEventClass, cl)))
          .intercept(
              chain -> {
                if (!isEnabled(config)) return chain.proceed();

                Object event = chain.getArg(1);
                if (event == null || !"ON_STOP".equals(event.toString())) {
                  return chain.proceed();
                }

                try {
                  Object observer = chain.getThisObject();
                  Object streamingManager = resolveLegyStreamingManager(observer, fixCfg);
                  if (streamingManager == null) {
                    Knot.log("Knot: failed to resolve legy streaming outer instance");
                    return chain.proceed();
                  }

                  suppressLegyBackgroundDisconnect(streamingManager, fixCfg, backgroundState);
                  logVerbose("suppressed legy streaming background disconnect timer");
                  return null;
                } catch (Throwable t) {
                  Knot.log("Knot: failed to suppress legy background disconnect: " + t);
                  return chain.proceed();
                }
              });
    } catch (Throwable ignored) {
    }

    try {
      Knot.module
          .hook(Reflect.findMethodExact(fixCfg.legyDisconnectRunnableClass, cl, "run"))
          .intercept(
              chain -> {
                if (!isEnabled(config)) return chain.proceed();
                logVerbose("blocked legy streaming disconnect runnable");
                return null;
              });
    } catch (Throwable ignored) {
    }

    try {
      Class<?> fcmServiceClass = Reflect.findClass(fixCfg.lineFcmServiceClass, cl);
      Class<?> remoteMessageClass = Reflect.findClass(fixCfg.firebaseRemoteMessageClass, cl);

      Knot.module
          .hook(
              Reflect.findMethodExact(
                  fcmServiceClass, fixCfg.lineFcmDispatchMethod, remoteMessageClass))
          .intercept(
              chain -> {
                if (isEnabled(config)) logVerbose("LINE FCM message dispatch entered");
                return chain.proceed();
              });

      for (Method method : fcmServiceClass.getDeclaredMethods()) {
        if (!fixCfg.lineFcmOwnershipMethod.equals(method.getName())
            || method.getParameterTypes().length != 2) {
          continue;
        }
        method.setAccessible(true);
        Knot.module
            .hook(method)
            .intercept(
                chain -> {
                  if (isEnabled(config)) {
                    logVerbose("forced LINE FCM ownership validation pass");
                    return Boolean.TRUE;
                  }
                  return chain.proceed();
                });
        break;
      }

      Knot.module
          .hook(Reflect.findMethodExact(fcmServiceClass, fixCfg.lineFcmTokenMethod, String.class))
          .intercept(
              chain -> {
                if (isEnabled(config))
                  logVerbose("LINE received Firebase token refresh: " + chain.getArg(0));
                return chain.proceed();
              });
    } catch (Throwable ignored) {
    }

    try {
      Class<?> receiverEnvelopeClass = Reflect.findClass(fixCfg.firebaseReceiverEnvelopeClass, cl);
      Knot.module
          .hook(
              Reflect.findMethodExact(
                  fixCfg.firebaseReceiverClass,
                  cl,
                  fixCfg.firebaseReceiverMethod,
                  Context.class,
                  receiverEnvelopeClass))
          .intercept(
              chain -> {
                if (!isEnabled(config)) return chain.proceed();

                Object envelope = chain.getArg(1);
                Intent intent =
                    envelope == null
                        ? null
                        : (Intent)
                            Reflect.getObjectField(envelope, fixCfg.firebaseReceiverIntentField);
                logIntent("FirebaseInstanceIdReceiver received", intent);
                return chain.proceed();
              });
    } catch (Throwable ignored) {
    }

    try {
      Knot.module
          .hook(
              Reflect.findMethodExact(
                  fixCfg.firebaseDispatcherClass,
                  cl,
                  fixCfg.firebaseDispatcherMethod,
                  Intent.class))
          .intercept(
              chain -> {
                if (!isEnabled(config)) return chain.proceed();

                Intent intent = (Intent) chain.getArg(0);
                logIntent("Firebase dispatcher queued", intent);

                if (intent == null
                    || !"com.google.android.c2dm.intent.RECEIVE".equals(intent.getAction())) {
                  return chain.proceed();
                }

                Context context =
                    (Context)
                        Reflect.getObjectField(
                            chain.getThisObject(), fixCfg.firebaseDispatcherContextField);
                if (!deliverMessagingIntentDirectly(context, cl, fixCfg, intent)) {
                  return chain.proceed();
                }

                return Reflect.callStaticMethod(
                    Reflect.findClass(fixCfg.firebaseCompletedTaskClass, cl),
                    fixCfg.firebaseCompletedTaskMethod,
                    Integer.valueOf(-1));
              });
    } catch (Throwable ignored) {
    }

    try {
      Knot.module
          .hook(
              Reflect.findMethodExact(
                  fixCfg.firebaseBindDeliveryClass,
                  cl,
                  fixCfg.firebaseBindDeliveryMethod,
                  Intent.class))
          .intercept(
              chain -> {
                if (!isEnabled(config)) return chain.proceed();
                logIntent("Firebase bind-delivery queued", (Intent) chain.getArg(0));
                return chain.proceed();
              });
    } catch (Throwable ignored) {
    }

    try {
      Knot.module
          .hook(
              Reflect.findMethodExact(
                  fixCfg.firebaseMessagingServiceClass,
                  cl,
                  fixCfg.firebaseMessagingHandleMethod,
                  Intent.class))
          .intercept(
              chain -> {
                if (!isEnabled(config)) return chain.proceed();
                logIntent("FirebaseMessagingService handling", (Intent) chain.getArg(0));
                return chain.proceed();
              });
    } catch (Throwable ignored) {
    }

    try {
      Knot.module
          .hook(
              Reflect.findMethodExact(
                  fixCfg.lineFcmServiceBaseClass,
                  cl,
                  "onStartCommand",
                  Intent.class,
                  int.class,
                  int.class))
          .intercept(
              chain -> {
                if (!isEnabled(config)) return chain.proceed();
                if (!(chain.getThisObject() instanceof Service)) return chain.proceed();

                Service service = (Service) chain.getThisObject();
                if (!fixCfg.lineFcmServiceClass.equals(service.getClass().getName())) {
                  return chain.proceed();
                }

                logIntent("LineFirebaseMessagingService onStartCommand", (Intent) chain.getArg(0));
                return chain.proceed();
              });
    } catch (Throwable ignored) {
    }
  }
}
