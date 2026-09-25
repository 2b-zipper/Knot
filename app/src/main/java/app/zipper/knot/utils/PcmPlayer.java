package app.zipper.knot.utils;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import app.zipper.knot.Knot;
import java.io.IOException;
import java.io.InterruptedIOException;

// Shares AacWriter's decode path, channels and gain, so what plays is exactly what gets saved.
public final class PcmPlayer implements PcmDecoder.Sink {

  private final CompressedAudio source;
  private final long startUs;
  private final long endUs;
  private final double gain;
  private final Thread thread;
  private short[] out = new short[0];
  private AudioTrack track;
  private int sampleRate;
  private long written;
  private boolean done;
  private boolean stopped;

  public PcmPlayer(CompressedAudio source, long startUs, long endUs, double gain) {
    this.source = source;
    this.startUs = startUs;
    this.endUs = endUs;
    this.gain = gain;
    thread = new Thread(this::run);
    thread.start();
  }

  public synchronized long positionUs() {
    if (track == null) return startUs;
    return startUs + track.getPlaybackHeadPosition() * 1_000_000L / sampleRate;
  }

  public synchronized boolean isFinished() {
    return done && (track == null || track.getPlaybackHeadPosition() >= written);
  }

  public void stop() {
    synchronized (this) {
      stopped = true;
      if (track != null) {
        track.pause();
        track.flush();
      }
      notifyAll();
    }
    thread.interrupt();
  }

  @Override
  public void write(short[] samples, int count, int sampleRate, int channels) throws IOException {
    int kept = Math.min(channels, AacWriter.MAX_CHANNELS);
    AudioTrack output = open(sampleRate, kept);
    int frames = count / channels;
    int length = frames * kept;
    if (out.length < length) out = new short[length];
    for (int f = 0; f < frames; f++) {
      for (int c = 0; c < kept; c++) out[f * kept + c] = clamp(samples[f * channels + c] * gain);
    }
    for (int offset = 0; offset < length; ) {
      if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
      int n = output.write(out, offset, length - offset);
      if (n < 0) throw new IOException("AudioTrack write failed: " + n);
      offset += n;
    }
    synchronized (this) {
      written += frames;
    }
  }

  private void run() {
    try {
      PcmDecoder.decode(source, startUs, endUs, this);
    } catch (InterruptedIOException ignored) {
    } catch (Throwable t) {
      Knot.log("Knot: Ringtone preview failed: " + t);
    }
    // Released here rather than in stop(), which can run while this thread is inside write().
    synchronized (this) {
      done = true;
      while (!stopped) {
        try {
          wait();
        } catch (InterruptedException ignored) {
        }
      }
      if (track != null) {
        track.release();
        track = null;
      }
    }
  }

  private synchronized AudioTrack open(int sampleRate, int channels) throws IOException {
    if (stopped) throw new InterruptedIOException();
    if (track != null) return track;
    int mask = channels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
    track =
        new AudioTrack.Builder()
            .setAudioAttributes(
                new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
            .setAudioFormat(
                new AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(mask)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build())
            .setBufferSizeInBytes(
                AudioTrack.getMinBufferSize(sampleRate, mask, AudioFormat.ENCODING_PCM_16BIT))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build();
    this.sampleRate = sampleRate;
    track.play();
    return track;
  }

  private static short clamp(double sample) {
    return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(sample)));
  }
}
