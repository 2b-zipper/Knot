package app.zipper.knot.hooks;

import app.zipper.knot.KnotConfig;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Main;

public class ForceDarkModeUiHook implements BaseHook {

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    if (!config.forceDarkModeUi.enabled) return;
    NightModePin.install(
        lpparam, () -> Main.options.forceDarkModeUi.enabled, "Knot: ForceDarkModeUi");
  }
}
