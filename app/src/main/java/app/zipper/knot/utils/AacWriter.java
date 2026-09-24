package app.zipper.knot.utils;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class AacWriter implements PcmDecoder.Sink, AutoCloseable {

  private static final int BIT_RATE = 192_000;
  private static final int MAX_SAMPLE_RATE = 48_000;
  private static final int[] AAC_SAMPLE_RATES = {
    8_000, 11_025, 12_000, 16_000, 22_050, 24_000, 32_000, 44_100, MAX_SAMPLE_RATE
  };
  static final int MAX_CHANNELS = 2;
  private static final long TIMEOUT_US = 10_000;
  private static final int MAX_IDLE_POLLS = 200;
  private static final int END_OF_STREAM = -1;

  private final File out;
  private final double gain;
  private final MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
  private final short[] pending = new short[16384];
  private MediaCodec codec;
  private MediaMuxer muxer;
  private int track = -1;
  private int pendingCount;
  private int decimation;
  private int outChannels;
  private int outRate;
  private long[] sums;
  private int summed;
  private double[] frame;
  private double[] previous;
  private boolean primed;
  private double step;
  private double phase;
  private long frames;

  public static void write(Context ctx, Uri source, long startUs, long endUs, double gain, File out)
      throws IOException {
    try (AacWriter writer = new AacWriter(out, gain)) {
      PcmDecoder.decode(ctx, source, startUs, endUs, writer);
      writer.finish();
    }
  }

  private AacWriter(File out, double gain) {
    this.out = out;
    this.gain = gain;
  }

  // Android's AAC encoder takes at most 48 kHz, so higher rates are averaged down by whole frames.
  // A rate it still rejects is then interpolated up to the next one it accepts.
  @Override
  public void write(short[] samples, int count, int sampleRate, int channels) throws IOException {
    if (codec == null) start(sampleRate, channels);
    for (int i = 0; i + channels <= count; i += channels) {
      for (int c = 0; c < outChannels; c++) sums[c] += samples[i + c];
      if (++summed < decimation) continue;
      for (int c = 0; c < outChannels; c++) {
        frame[c] = sums[c] * gain / decimation;
        sums[c] = 0;
      }
      summed = 0;
      resample();
    }
  }

  private void resample() throws IOException {
    if (step == 1) {
      emit(frame, frame, 0);
      return;
    }
    if (primed) {
      for (; phase < 1; phase += step) emit(previous, frame, phase);
      phase -= 1;
    }
    System.arraycopy(frame, 0, previous, 0, outChannels);
    primed = true;
  }

  private void emit(double[] from, double[] to, double t) throws IOException {
    for (int c = 0; c < outChannels; c++) {
      pending[pendingCount++] = clamp(from[c] + (to[c] - from[c]) * t);
    }
    if (pendingCount + outChannels > pending.length) flush();
  }

  private void finish() throws IOException {
    if (codec == null) throw new IOException("no audio decoded");
    flush();
    codec.queueInputBuffer(
        awaitInput(), 0, 0, presentationUs(), MediaCodec.BUFFER_FLAG_END_OF_STREAM);
    int idlePolls = 0;
    while (true) {
      int drained = drain(TIMEOUT_US);
      if (drained == END_OF_STREAM) break;
      idlePolls = drained == 0 ? idlePolls + 1 : 0;
      if (idlePolls >= MAX_IDLE_POLLS) throw new IOException("encoder stalled");
    }
    muxer.stop();
  }

  @Override
  public void close() {
    if (codec != null) codec.release();
    if (muxer != null) muxer.release();
  }

  private void start(int sampleRate, int channels) throws IOException {
    decimation = (sampleRate + MAX_SAMPLE_RATE - 1) / MAX_SAMPLE_RATE;
    double decimatedRate = (double) sampleRate / decimation;
    outRate = MAX_SAMPLE_RATE;
    for (int rate : AAC_SAMPLE_RATES) {
      if (rate >= decimatedRate) {
        outRate = rate;
        break;
      }
    }
    step = decimatedRate / outRate;
    outChannels = Math.min(channels, MAX_CHANNELS);
    sums = new long[outChannels];
    frame = new double[outChannels];
    previous = new double[outChannels];
    MediaFormat format =
        MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, outRate, outChannels);
    format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
    format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
    codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
    codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    codec.start();
    muxer = new MediaMuxer(out.getPath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
  }

  private void flush() throws IOException {
    int offset = 0;
    while (offset < pendingCount) {
      int in = awaitInput();
      ByteBuffer buffer = codec.getInputBuffer(in).order(ByteOrder.nativeOrder());
      buffer.clear();
      int n = Math.min(pendingCount - offset, buffer.remaining() / 2);
      n -= n % outChannels;
      buffer.asShortBuffer().put(pending, offset, n);
      codec.queueInputBuffer(in, 0, n * 2, presentationUs(), 0);
      frames += n / outChannels;
      offset += n;
    }
    pendingCount = 0;
    drain(0);
  }

  private int awaitInput() throws IOException {
    int idlePolls = 0;
    while (idlePolls < MAX_IDLE_POLLS) {
      int in = codec.dequeueInputBuffer(0);
      if (in >= 0) return in;
      idlePolls = drain(TIMEOUT_US) == 0 ? idlePolls + 1 : 0;
    }
    throw new IOException("encoder stalled");
  }

  private int drain(long timeoutUs) throws IOException {
    int drained = 0;
    while (true) {
      int index = codec.dequeueOutputBuffer(info, drained == 0 ? timeoutUs : 0);
      if (index == MediaCodec.INFO_TRY_AGAIN_LATER) return drained;
      if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
        track = muxer.addTrack(codec.getOutputFormat());
        muxer.start();
      } else if (index >= 0) {
        boolean config = (info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0;
        if (!config && info.size > 0 && track >= 0) {
          muxer.writeSampleData(track, codec.getOutputBuffer(index), info);
        }
        codec.releaseOutputBuffer(index, false);
        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) return END_OF_STREAM;
      }
      drained++;
    }
  }

  private long presentationUs() {
    return frames * 1_000_000L / outRate;
  }

  private static short clamp(double sample) {
    return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(sample)));
  }
}
