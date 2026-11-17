package app.zipper.knot.hooks;

import android.content.Context;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Reflect;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class GoogleSignInRedirectHook implements BaseHook {

  private static final String GOOGLE_ID_TOKEN =
      "com.google.android.libraries.identity.googleid.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL";

  private final ThreadLocal<Boolean> signingIn = new ThreadLocal<>();
  private Class<?> requestClass;
  private Field optionsField;
  private Class<?> optionClass;
  private Field typeField;

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    LineVersion.Config version = LineVersion.get();
    if (version == null || version.credentials.methodBestProvider.isEmpty()) return;
    LineVersion.Config.Credentials cfg = version.credentials;
    ClassLoader cl = lpparam.classLoader;

    requestClass = Reflect.findClass(cfg.getRequestClass, cl);
    optionsField = requestClass.getDeclaredField(cfg.getRequestOptionsField);
    optionsField.setAccessible(true);
    optionClass = Reflect.findClass(cfg.optionClass, cl);
    typeField = optionClass.getDeclaredField(cfg.optionTypeField);
    typeField.setAccessible(true);

    Class<?> factoryClass = Reflect.findClass(cfg.providerFactoryClass, cl);
    Method playServicesProvider =
        Reflect.findMethodExact(factoryClass, cfg.methodPlayServicesProvider);

    // Android 14+ sends Google sign-in to the real GMS through the system, past the redirect
    for (Method m :
        Reflect.findMethodsByName(cfg.providerFactoryClass, cl, cfg.methodBestProvider)) {
      int factory = Reflect.paramIndex(m, factoryClass);
      Knot.module
          .hook(m)
          .intercept(
              chain -> {
                // R8 types the request parameter as Object
                List<Object> args = chain.getArgs();
                if (args.isEmpty() || !reroutes(args.get(args.size() - 1))) return chain.proceed();
                Object provider =
                    playServicesProvider.invoke(
                        factory < 0 ? chain.getThisObject() : args.get(factory));
                return provider != null ? provider : chain.proceed();
              });
    }

    // microG's Identity Credentials replies are Bundles androidx can't read on Android 14+
    Class<?> impl = Reflect.findClass(cfg.playServicesProviderClass, cl);
    for (Method m :
        Reflect.findMethodsByName(cfg.playServicesProviderClass, cl, cfg.methodGetCredential)) {
      int request = Reflect.paramIndex(m, requestClass);
      if (request < 0) continue;
      Knot.module
          .hook(m)
          .intercept(
              chain -> {
                if (!reroutes(chain.getArg(request))) return chain.proceed();
                signingIn.set(Boolean.TRUE);
                try {
                  return chain.proceed();
                } finally {
                  signingIn.remove();
                }
              });
    }
    Knot.module
        .hook(Reflect.findMethodExact(impl, cfg.methodIsAvailableOnDevice, int.class))
        .intercept(chain -> signingIn.get() != null ? Boolean.FALSE : chain.proceed());
  }

  private boolean reroutes(Object request) {
    if (!requestClass.isInstance(request)) return false;
    try {
      List<?> options = (List<?>) optionsField.get(request);
      if (options == null || options.isEmpty()) return false;
      for (Object option : options) {
        if (!optionClass.isInstance(option) || !GOOGLE_ID_TOKEN.equals(typeField.get(option))) {
          return false;
        }
      }
    } catch (Throwable t) {
      Knot.log("Knot: failed to read the credential request", t);
      return false;
    }
    Context context = Knot.currentApplication();
    return context != null && GmsAvailabilityHook.isRedirected(context);
  }
}
