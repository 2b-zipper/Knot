package app.zipper.knot.utils;

import app.zipper.knot.Knot;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class AudioAnalysis {

  private static final long WINDOW_US = 100_000;
  private static final double SILENCE_POWER = 1e-7;
  private static final int MAX_WORKERS = 4;
  private static final long MIN_CHUNK_US = 10_000_000;

  public final long durationUs;
  private final float[] power;
  private final float[] peak;
  private final float[] level;

  private AudioAnalysis(float[] power, float[] peak, long durationUs) {
    this.power = power;
    this.peak = peak;
    this.durationUs = durationUs;
    float loudest = 0;
    for (float p : power) loudest = Math.max(loudest, p);
    level = new float[power.length];
    if (loudest > 0) {
      for (int i = 0; i < power.length; i++) level[i] = (float) Math.sqrt(power[i] / loudest);
    }
  }

  // Software decoders are single-threaded, so separate chunks decode side by side.
  public static AudioAnalysis analyze(CompressedAudio source) throws IOException {
    int workers = Math.min(MAX_WORKERS, Runtime.getRuntime().availableProcessors());
    int chunks = (int) Math.max(1, Math.min(workers, source.lastTimeUs() / MIN_CHUNK_US));
    try {
      return analyze(source, chunks);
    } catch (InterruptedIOException e) {
      throw e;
    } catch (IOException e) {
      if (chunks == 1) throw e;
      // Some devices cannot run that many decoders at once.
      Knot.log("Knot: Parallel audio analysis failed, retrying sequentially: " + e);
      return analyze(source, 1);
    }
  }

  private static AudioAnalysis analyze(CompressedAudio source, int chunks) throws IOException {
    long chunkUs = (source.lastTimeUs() / chunks / WINDOW_US + 1) * WINDOW_US;
    List<Callable<Collector>> tasks = new ArrayList<>();
    for (int i = 0; i < chunks; i++) {
      long startUs = i * chunkUs;
      long endUs = i == chunks - 1 ? Long.MAX_VALUE : startUs + chunkUs;
      tasks.add(
          () -> {
            Collector collector = new Collector(startUs);
            PcmDecoder.decode(source, startUs, endUs, collector);
            return collector;
          });
    }
    return merge(runAll(tasks));
  }

  public float displayLevel(long fromUs, long toUs) {
    int from = window(fromUs);
    int to = Math.max(from + 1, window(toUs));
    float max = 0;
    for (int i = from; i < to; i++) max = Math.max(max, level[i]);
    return max;
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

  private static List<Collector> runAll(List<Callable<Collector>> tasks) throws IOException {
    ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
    try {
      List<Collector> parts = new ArrayList<>();
      for (Future<Collector> future : pool.invokeAll(tasks)) parts.add(future.get());
      return parts;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new InterruptedIOException();
    } catch (ExecutionException e) {
      if (e.getCause() instanceof IOException) throw (IOException) e.getCause();
      throw new IOException(e.getCause());
    } finally {
      pool.shutdownNow();
    }
  }

  private static AudioAnalysis merge(List<Collector> parts) throws IOException {
    int windows = 0;
    long frames = 0;
    int sampleRate = 0;
    for (Collector part : parts) {
      part.finish();
      windows = Math.max(windows, part.firstWindow + part.windows);
      frames += part.frames;
      if (part.frames > 0) sampleRate = part.sampleRate;
    }
    if (frames == 0) throw new IOException("no audio decoded");
    float[] power = new float[windows];
    float[] peak = new float[windows];
    for (Collector part : parts) {
      System.arraycopy(part.power, 0, power, part.firstWindow, part.windows);
      System.arraycopy(part.peak, 0, peak, part.firstWindow, part.windows);
    }
    return new AudioAnalysis(power, peak, frames * 1_000_000L / sampleRate);
  }

  private static final class Collector implements PcmDecoder.Sink {
    final int firstWindow;
    float[] power = new float[512];
    float[] peak = new float[512];
    int windows;
    int sampleRate;
    int measured = 1;
    long frames;
    long windowSquares;
    int windowPeak;
    int windowFill;

    Collector(long startUs) {
      firstWindow = (int) (startUs / WINDOW_US);
    }

    @Override
    public void write(short[] samples, int count, int sampleRate, int channels) {
      this.sampleRate = sampleRate;
      int windowFrames = (int) Math.max(1, sampleRate * WINDOW_US / 1_000_000);
      // Measures only the channels AacWriter keeps, so the gain matches what gets saved.
      measured = Math.min(channels, AacWriter.MAX_CHANNELS);
      for (int i = 0; i + channels <= count; i += channels) {
        for (int c = 0; c < measured; c++) {
          int sample = samples[i + c];
          windowPeak = Math.max(windowPeak, Math.abs(sample));
          windowSquares += sample * sample;
        }
        frames++;
        if (++windowFill >= windowFrames) closeWindow();
      }
    }

    void finish() {
      if (windowFill > 0) closeWindow();
    }

    private void closeWindow() {
      if (windows == power.length) {
        power = Arrays.copyOf(power, windows * 2);
        peak = Arrays.copyOf(peak, windows * 2);
      }
      power[windows] =
          (float) ((double) windowSquares / ((long) windowFill * measured) / (32768.0 * 32768.0));
      peak[windows] = windowPeak / 32768f;
      windows++;
      windowSquares = 0;
      windowPeak = 0;
      windowFill = 0;
    }
  }
}
