package app.zipper.knot.ui;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import java.util.Arrays;

final class ShakeDetector implements SensorEventListener {

  private static final float REQUIRED_FORCE = SensorManager.GRAVITY_EARTH * 1.33f;
  private static final int REQUIRED_SHAKES = 16;
  private static final long MIN_SAMPLE_INTERVAL_NS = 20_000_000L;
  private static final long SHAKE_GAP_NS = 3_000_000_000L;

  private final Runnable onShake;
  private final float[] lastForce = new float[3];
  private long lastSampleAt;
  private long lastShakeAt;
  private int shakes;

  ShakeDetector(Runnable onShake) {
    this.onShake = onShake;
  }

  void reset() {
    shakes = 0;
    Arrays.fill(lastForce, 0);
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    long now = event.timestamp;
    if (now - lastSampleAt < MIN_SAMPLE_INTERVAL_NS) return;
    lastSampleAt = now;

    if (!flipped(event.values)) {
      if (now - lastShakeAt > SHAKE_GAP_NS) reset();
      return;
    }
    lastShakeAt = now;
    if (++shakes < REQUIRED_SHAKES) return;
    reset();
    onShake.run();
  }

  private boolean flipped(float[] values) {
    for (int axis = 0; axis < 3; axis++) {
      float force = values[axis] - (axis == 2 ? SensorManager.GRAVITY_EARTH : 0);
      if (Math.abs(force) > REQUIRED_FORCE && lastForce[axis] * force <= 0) {
        lastForce[axis] = force;
        return true;
      }
    }
    return false;
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
