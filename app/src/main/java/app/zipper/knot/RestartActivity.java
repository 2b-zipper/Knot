package app.zipper.knot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import app.zipper.knot.utils.ModuleResources;

/**
 * Relaunches LINE on behalf of the module. LINE loses the right to start activities the moment it
 * exits, so it hands the relaunch here while still in the foreground, then kills itself.
 */
public final class RestartActivity extends Activity {

  private static final String LINE_PACKAGE = "jp.naver.line.android";
  private static final String EXTRA_LIFELINE = "knot.restart.lifeline";

  // Relaunching a live process only brings it forward, so LINE's death is what is waited on.
  private static final long SETTLE_MS = 150;
  private static final long MAX_WAIT_MS = 500;

  // Static so the token cannot be collected before LINE's process dies.
  private static IBinder lifeline;

  private final Handler handler = new Handler(Looper.getMainLooper());
  private final IBinder.DeathRecipient lineGone =
      () -> handler.postDelayed(this::relaunchLine, SETTLE_MS);

  private boolean relaunched;

  public static void requestRestart(Context ctx) {
    try {
      lifeline = new Binder();
      Bundle extras = new Bundle();
      extras.putBinder(EXTRA_LIFELINE, lifeline);

      Intent relay = new Intent();
      relay.setClassName(ModuleResources.MODULE_PACKAGE, RestartActivity.class.getName());
      relay.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      relay.putExtras(extras);
      ctx.startActivity(relay);
    } catch (Throwable t) {
      Knot.log("Knot: restart relay unavailable", t);
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    watchLine();
    handler.postDelayed(this::relaunchLine, MAX_WAIT_MS);
  }

  private void watchLine() {
    Bundle extras = getIntent().getExtras();
    IBinder token = extras == null ? null : extras.getBinder(EXTRA_LIFELINE);
    if (token == null) return;
    try {
      token.linkToDeath(lineGone, 0);
    } catch (RemoteException alreadyGone) {
      lineGone.binderDied();
    }
  }

  // Touching the task races the system's own restore and closes the one that just came back.
  private void relaunchLine() {
    if (relaunched) return;
    relaunched = true;
    try {
      Intent launch = getPackageManager().getLaunchIntentForPackage(LINE_PACKAGE);
      if (launch != null) {
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(launch);
      }
    } catch (Throwable t) {
      Knot.log("Knot: could not relaunch LINE", t);
    }
    finish();
  }
}
