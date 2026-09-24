package app.zipper.knot.utils;

import android.content.Context;
import android.net.Uri;
import java.io.IOException;
import java.util.Arrays;

public final class AudioAnalysis {

  private static final long WINDOW_US = 100_000;
  private static final double SILENCE_POWER = 1e-7;

  public final long durationUs;
  private final float[] power;
  private final float[] peak;
  private final float loudestRms;

  private AudioAnalysis(float[] power, float[] peak, long durationUs) {
    this.power = power;
    this.peak = peak;
    this.durationUs = durationUs;
    float loudest = 0;
    for (float p : power) loudest = Math.max(loudest, p);
    loudestRms = (float) Math.sqrt(loudest);
  }

  public static AudioAnalysis analyze(Context ctx, Uri uri) throws IOException {
    Collector collector = new Collector();
    PcmDecoder.decode(ctx, uri, 0, Long.MAX_VALUE, collector);
    return collector.build();
  }

  public float displayLevel(long fromUs, long toUs) {
    int from = window(fromUs);
    int to = Math.max(from + 1, window(toUs));
    float max = 0;
    for (int i = from; i < to; i++) max = Math.max(max, power[i]);
    return loudestRms == 0 ? 0 : (float) Math.sqrt(max) / loudestRms;
  }

  public double levelingGain(long startUs, long endUs, double targetDb) {
    int from = window(startUs);
    int to = Math.max(from + 1, window(endUs));
    double powerSum = 0;
    int counted = 0;
    float loudestPeak = 1e-6f;
    for (int i = from; i < to; i++) {
      if (power[i] > SILENCE_POWER) {
        powerSum += power[i];
        counted++;
      }
      loudestPeak = Math.max(loudestPeak, peak[i]);
    }
    if (counted == 0) return 1;
    double gainDb =
        Math.min(targetDb - 10 * Math.log10(powerSum / counted), -20 * Math.log10(loudestPeak));
    return Math.pow(10, gainDb / 20);
  }

  private int window(long us) {
    return (int) Math.max(0, Math.min(power.length - 1, us / WINDOW_US));
  }

  private static final class Collector implements PcmDecoder.Sink {
    float[] power = new float[512];
    float[] peak = new float[512];
    int windows;
    int sampleRate;
    long frames;
    double windowPower;
    int windowPeak;
    int windowFill;

    @Override
    public void write(short[] samples, int count, int sampleRate, int channels) {
      this.sampleRate = sampleRate;
      int windowFrames = (int) Math.max(1, sampleRate * WINDOW_US / 1_000_000);
      // Measures only the channels AacWriter keeps, so the gain matches what gets saved.
      int measured = Math.min(channels, AacWriter.MAX_CHANNELS);
      for (int i = 0; i + channels <= count; i += channels) {
        for (int c = 0; c < measured; c++) {
          int sample = samples[i + c];
          windowPeak = Math.max(windowPeak, Math.abs(sample));
          windowPower += (double) sample * sample / measured;
        }
        frames++;
        if (++windowFill >= windowFrames) closeWindow();
      }
    }

    AudioAnalysis build() throws IOException {
      if (windowFill > 0) closeWindow();
      if (windows == 0) throw new IOException("no audio decoded");
      return new AudioAnalysis(
          Arrays.copyOf(power, windows),
          Arrays.copyOf(peak, windows),
          frames * 1_000_000L / sampleRate);
    }

    private void closeWindow() {
      if (windows == power.length) {
        power = Arrays.copyOf(power, windows * 2);
        peak = Arrays.copyOf(peak, windows * 2);
      }
      power[windows] = (float) (windowPower / windowFill / (32768.0 * 32768.0));
      peak[windows] = windowPeak / 32768f;
      windows++;
      windowPower = 0;
      windowPeak = 0;
      windowFill = 0;
    }
  }
}
