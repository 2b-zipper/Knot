package app.zipper.knot.hooks;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.Reflect;
import io.github.libxposed.api.XposedInterface.Hooker;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class SignatureSpoofHook implements BaseHook {

  private static final String LINE_CERT_B64 =
      "MIICqzCCAhSgAwIBAgIETWsBKjANBgkqhkiG9w0BAQUFADCBmDELMAkGA1UEBhMCSlAxDjAMBgNVBAgTBVRva3lvMRswGQYDVQQHExJPb3Nha2kgU2luYWdhd2Eta3UxEzARBgNVBAoTCk5hdmVySmFwYW4xKTAnBgNVBAsTIFNlYXJjaCBTZXJ2aWNlIERldmVsb3BtZW50IDNUZWFtMRwwGgYDVQQDExN0c3V0b211IGhvcml5YXNoaWtpMCAXDTExMDIyODAxNTgwMloYDzIxMTEwMjA0MDE1ODAyWjCBmDELMAkGA1UEBhMCSlAxDjAMBgNVBAgTBVRva3lvMRswGQYDVQQHExJPb3Nha2kgU2luYWdhd2Eta3UxEzARBgNVBAoTCk5hdmVySmFwYW4xKTAnBgNVBAsTIFNlYXJjaCBTZXJ2aWNlIERldmVsb3BtZW50IDNUZWFtMRwwGgYDVQQDExN0c3V0b211IGhvcml5YXNoaWtpMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCNqGsbHHbOnk6ET0AvxQoo5K/FbNhJLZ7kLmfQAupjlChh+gCB4E41Pj9yiPaJd3kpavpBkrSLI42AYu3Gj+68n3gfjI0OoBhGvNlwwxWL1KZUypJwUhfR7Bxam4dtjVkmzGqRM0xejYuXysqVAW2hMDMHkr76s49CxPeESd7wZwIDAQABMA0GCSqGSIb3DQEBBQUAA4GBAC2HdP71+BV8sQm1HDuUSGDaDf51Mmbw8fpfbif+cS94Qj7Xl//zg9byq4VWWl+3rkCPrOcvq4wVdMuN5HghudgQmHiPzFt/Bsrt6863wTskAhlNBDPchtZfhq5wnnAyUSLn6zpzmAE1yNjmUJlLnDSdg4V6w7kbZfBSAA/aYffa";

  private static final String ORIGIN_PREFIX = "android:apk-key-hash-sha256:";
  private static final String OFFICIAL_HASH = computeKeyHash();

  private static boolean enabled() {
    return Main.options.fixSignatureMismatch.enabled;
  }

  private static String computeKeyHash() {
    try {
      byte[] der = Base64.decode(LINE_CERT_B64, Base64.DEFAULT);
      byte[] sha = MessageDigest.getInstance("SHA-256").digest(der);
      return Base64.encodeToString(sha, Base64.NO_PADDING | Base64.NO_WRAP);
    } catch (Throwable t) {
      return null;
    }
  }

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    if (!config.fixSignatureMismatch.enabled) return;

    hookPackageManager(lpparam);
    hookBiometricAuth();
  }

  private void hookPackageManager(LoadParam lpparam) {
    try {
      Class<?> pkgManagerClass =
          Reflect.findClass("android.app.ApplicationPackageManager", lpparam.classLoader);
      if (pkgManagerClass == null) {
        Knot.log("SignatureSpoofHook: Cannot find ApplicationPackageManager");
        return;
      }

      Knot.hookAll(
          pkgManagerClass,
          "getPackageInfo",
          chain -> {
            String packageName = (String) chain.getArg(0);

            int flags = 0;
            if (chain.getArg(1) instanceof Integer) {
              flags = (Integer) chain.getArg(1);
            }

            Object result = chain.proceed();

            if ("jp.naver.line.android".equals(packageName)
                && (flags & PackageManager.GET_SIGNATURES) != 0) {
              if (result instanceof PackageInfo) {
                PackageInfo packageInfo = (PackageInfo) result;
                byte[] der = Base64.decode(LINE_CERT_B64, Base64.DEFAULT);
                Signature fakeSignature = new Signature(der);
                packageInfo.signatures = new Signature[] {fakeSignature};
                Knot.log("SignatureSpoofHook: Applied official LINE signature.");
              }
            }
            return result;
          });
    } catch (Throwable t) {
      Knot.log("SignatureSpoofHook (PackageManager) error: " + t);
    }
  }

  private void hookBiometricAuth() {
    if (OFFICIAL_HASH == null) return;

    Hooker rewriter =
        chain -> {
          byte[] fixed = enabled() ? rewriteOrigin((byte[]) chain.getArg(0)) : null;
          if (fixed == null) return chain.proceed();
          Object[] args = chain.getArgs().toArray();
          args[0] = fixed;
          return chain.proceed(args);
        };

    try {
      hook(MessageDigest.class.getDeclaredMethod("digest", byte[].class), rewriter);
      hook(Base64.class.getDeclaredMethod("encodeToString", byte[].class, int.class), rewriter);
    } catch (Throwable t) {
      Knot.log("SignatureSpoofHook: BiometricAuth hook setup failed: " + t);
    }
  }

  private static byte[] rewriteOrigin(byte[] payload) {
    if (payload == null || payload.length < 2 || payload[0] != '{') return null;
    String text = new String(payload, StandardCharsets.UTF_8);
    int at = text.indexOf(ORIGIN_PREFIX);
    if (at < 0) return null;
    int from = at + ORIGIN_PREFIX.length();
    int to = text.indexOf('"', from);
    if (to < 0 || text.regionMatches(from, OFFICIAL_HASH, 0, to - from)) return null;
    String result = text.substring(0, from) + OFFICIAL_HASH + text.substring(to);
    return result.getBytes(StandardCharsets.UTF_8);
  }

  private void hook(Method method, Hooker hooker) {
    try {
      method.setAccessible(true);
      Knot.module.hook(method).intercept(hooker);
    } catch (Throwable t) {
      Knot.log("Knot: SignatureSpoofHook " + method.getName() + " failed: " + t);
    }
  }
}
