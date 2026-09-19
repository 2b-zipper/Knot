package app.zipper.knot.ui;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import java.util.ArrayDeque;

final class ShakeDetector implements SensorEventListener {

  // Most of the last half second has to stay above ~1.33 g, so walking or a single bump won't fire
  private static final float THRESHOLD = 13f;
  private static final long WINDOW_NS = 500_000_000L;
  private static final long MIN_SPAN_NS = 250_000_000L;
  private static final int MIN_SAMPLES = 4;
  private static final long COOLDOWN_NS = 2_000_000_000L;

  private static final class Sample {
    final long time;
    final boolean strong;

    Sample(long time, boolean strong) {
      this.time = time;
      this.strong = strong;
    }
  }

  private final Runnable onShake;
  private final ArrayDeque<Sample> window = new ArrayDeque<>();
  private int strongCount;
  private long lastShake;

  ShakeDetector(Runnable onShake) {
    this.onShake = onShake;
  }

  void reset() {
    window.clear();
    strongCount = 0;
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    float x = event.values[0];
    float y = event.values[1];
    float z = event.values[2];
    boolean strong = x * x + y * y + z * z > THRESHOLD * THRESHOLD;
    long now = event.timestamp;

    window.addLast(new Sample(now, strong));
    if (strong) strongCount++;
    while (now - window.peekFirst().time > WINDOW_NS) {
      if (window.pollFirst().strong) strongCount--;
    }

    if (!isShaking(now)) return;
    reset();
    if (now - lastShake < COOLDOWN_NS) return;
    lastShake = now;
    onShake.run();
  }

  private boolean isShaking(long now) {
    int size = window.size();
    return size >= MIN_SAMPLES
        && now - window.peekFirst().time >= MIN_SPAN_NS
        && strongCount >= size - size / 4;
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
