package app.zipper.knot.hooks;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.R;
import app.zipper.knot.Reflect;
import app.zipper.knot.SettingsStore;
import app.zipper.knot.utils.LineTheme;
import app.zipper.knot.utils.ModuleResources;
import io.github.libxposed.api.XposedInterface;

public class UnsupportedPatcherWarning implements BaseHook {

  private static final String WARNED_PATCHER_KEY = "patcher_warned";
  private static final String FORK_FRAMEWORK_NAME = "NPatch for Knot";

  private static volatile boolean handled;

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    XposedInterface module = Knot.module;
    // Root frameworks always report CAP_SYSTEM; NPatch, forked or not, never does
    if ((module.getFrameworkProperties() & XposedInterface.PROP_CAP_SYSTEM) != 0) return;
    String name = module.getFrameworkName();
    if (FORK_FRAMEWORK_NAME.equals(name)) return;
    String patcher = name + " " + module.getFrameworkVersion();

    LineVersion.Config cfg = LineVersion.get();
    if (cfg == null) return;
    Class<?> activityCls = lpparam.classLoader.loadClass(cfg.main.mainActivity);

    module
        .hook(Reflect.findMethodExact(activityCls, "onResume"))
        .intercept(
            chain -> {
              Object result = chain.proceed();
              maybeShow((Activity) chain.getThisObject(), patcher);
              return result;
            });
  }

  private static void maybeShow(Activity host, String patcher) {
    if (handled || !SettingsStore.isConfigured()) return;
    handled = true;
    if (patcher.equals(SettingsStore.getString(WARNED_PATCHER_KEY, ""))) return;
    if (host.isFinishing() || host.isDestroyed()) return;
    try {
      String message =
          ModuleResources.get(
              R.string.patcher_warning_msg,
              patcher,
              ModuleResources.get(R.string.patcher_warning_readme_url));
      AlertDialog dialog =
          new AlertDialog.Builder(host, LineTheme.dialogTheme(host))
              .setTitle(ModuleResources.get(R.string.patcher_warning_title))
              .setMessage(ReleaseNotesPopup.render(host, message))
              .setPositiveButton(ModuleResources.get(R.string.common_close), null)
              .show();
      LineTheme.applyDialogColors(dialog, host);
      TextView text = dialog.findViewById(android.R.id.message);
      if (text != null) {
        text.setMovementMethod(LinkMovementMethod.getInstance());
        text.setLinkTextColor(LineTheme.linkColor(host));
      }
      SettingsStore.save(WARNED_PATCHER_KEY, patcher);
    } catch (Throwable t) {
      Knot.log("Knot: patcher warning failed: " + t);
    }
  }
}
