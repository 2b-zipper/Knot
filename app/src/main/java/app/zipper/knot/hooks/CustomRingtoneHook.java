package app.zipper.knot.hooks;

import android.content.Context;
import android.net.Uri;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;
import app.zipper.knot.Reflect;
import java.io.File;

public class CustomRingtoneHook implements BaseHook {

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    if (!config.useCustomRingtone.enabled) return;
    LineVersion.Config version = LineVersion.get();
    if (version == null || version.callTone.ringtoneWrapperClass.isEmpty()) return;
    LineVersion.Config.CallTone tone = version.callTone;
    ClassLoader cl = lpparam.classLoader;
    Class<?> uriToneSource = Reflect.findClass(tone.uriToneSourceClass, cl);

    // Leave the audio attributes to LINE: with a headset it rings in communication mode, where
    // audio marked as a ringtone is muted.
    Knot.module
        .hook(
            Reflect.findConstructorExact(
                tone.ringtoneWrapperClass, cl, Context.class, tone.toneSourceClass))
        .intercept(
            chain -> {
              File file = ringtoneFile();
              if (file == null) return chain.proceed();
              Object source = Reflect.newInstance(uriToneSource, Uri.fromFile(file));
              return chain.proceed(new Object[] {chain.getArg(0), source});
            });
  }

  private static File ringtoneFile() {
    if (!Main.options.useCustomRingtone.enabled) return null;
    String path = Main.options.customRingtonePath.value;
    File file = new File(path);
    return !path.isEmpty() && file.isFile() ? file : null;
  }
}
