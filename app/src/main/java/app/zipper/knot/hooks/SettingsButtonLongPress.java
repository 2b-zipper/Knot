package app.zipper.knot.hooks;

import android.app.Activity;
import android.view.View;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Reflect;

public class SettingsButtonLongPress implements BaseHook {

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    LineVersion.Config cfg = LineVersion.get();

    try {
      Knot.module
          .hook(
              Reflect.findMethodExact(
                  cfg.main.headerButton,
                  lpparam.classLoader,
                  "setButtonOnClickListener",
                  View.OnClickListener.class))
          .intercept(
              chain -> {
                Object result = chain.proceed();
                attachInteractionHandler((View) chain.getThisObject());
                return result;
              });
    } catch (Throwable ignored) {
    }

    try {
      Knot.module
          .hook(
              Reflect.findMethodExact(View.class, "setOnClickListener", View.OnClickListener.class))
          .intercept(
              chain -> {
                Object result = chain.proceed();
                View target = (View) chain.getThisObject();
                if (target == null) return result;
                int id = target.getId();
                if (id != View.NO_ID) {
                  LineVersion.Config c = LineVersion.get();
                  String entry = "";
                  try {
                    entry = target.getResources().getResourceEntryName(id);
                  } catch (Throwable ignored) {
                  }
                  if (c.res.resSettingsHeaderBtn.equals(entry)
                      || c.res.resSettingsBtn.equals(entry)) {
                    attachInteractionHandler(target);
                  }
                }
                return result;
              });
    } catch (Throwable ignored) {
    }
  }

  private void attachInteractionHandler(View root) {
    if (root == null) return;
    root.setOnLongClickListener(interactionListener);
  }

  private final View.OnLongClickListener interactionListener =
      v -> {
        try {
          Activity host = findHostActivity(v.getContext());
          if (host != null) {
            HomeSettingsTooltip.markShown();
            SettingsUIInjector.openSettings(host);
            return true;
          }
        } catch (Throwable t) {
          Knot.log("Knot: Interaction error: " + t);
        }
        return false;
      };

  private Activity findHostActivity(android.content.Context ctx) {
    while (ctx instanceof android.content.ContextWrapper) {
      if (ctx instanceof Activity) return (Activity) ctx;
      ctx = ((android.content.ContextWrapper) ctx).getBaseContext();
    }
    return null;
  }
}
