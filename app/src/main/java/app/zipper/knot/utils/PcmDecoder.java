package app.zipper.knot.utils;

import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class PcmDecoder {

  public interface Sink {
    void write(short[] samples, int count, int sampleRate, int channels) throws IOException;
  }

  private static final long TIMEOUT_US = 10_000;
  private static final int MAX_IDLE_POLLS = 200;

  private PcmDecoder() {}

  public static void decode(Context ctx, Uri uri, long startUs, long endUs, Sink sink)
      throws IOException {
    MediaExtractor extractor = new MediaExtractor();
    MediaCodec codec = null;
    try {
      extractor.setDataSource(ctx, uri, null);
      MediaFormat format = selectAudioTrack(extractor);
      if (startUs > 0) extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
      codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
      codec.configure(format, null, null, 0);
      codec.start();

      Pcm pcm = new Pcm(format);
      MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
      boolean inputDone = false;
      int idlePolls = 0;
      while (idlePolls < MAX_IDLE_POLLS) {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
        // Software codecs run out of process, so keep every input slot filled rather than
        // waiting on one frame at a time.
        boolean fed = false;
        while (!inputDone) {
          int in = codec.dequeueInputBuffer(0);
          if (in < 0) break;
          inputDone = queueSample(extractor, codec, in, endUs);
          fed = true;
        }
        int out = codec.dequeueOutputBuffer(info, fed ? 0 : TIMEOUT_US);
        if (out == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
          pcm = new Pcm(codec.getOutputFormat());
        } else if (out >= 0) {
          idlePolls = 0;
          ByteBuffer data = codec.getOutputBuffer(out);
          boolean reachedEnd = false;
          if (data != null && info.size > 0) {
            data.position(info.offset).limit(info.offset + info.size);
            reachedEnd =
                pcm.write(
                    data.order(ByteOrder.nativeOrder()),
                    info.presentationTimeUs,
                    startUs,
                    endUs,
                    sink);
          }
          codec.releaseOutputBuffer(out, false);
          if (reachedEnd || (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) return;
        } else if (!fed) {
          idlePolls++;
        }
      }
      throw new IOException("decoder stalled");
    } finally {
      if (codec != null) codec.release();
      extractor.release();
    }
  }

  private static MediaFormat selectAudioTrack(MediaExtractor extractor) throws IOException {
    for (int i = 0; i < extractor.getTrackCount(); i++) {
      MediaFormat format = extractor.getTrackFormat(i);
      String mime = format.getString(MediaFormat.KEY_MIME);
      if (mime != null && mime.startsWith("audio/")) {
        extractor.selectTrack(i);
        return format;
      }
    }
    throw new IOException("no audio track");
  }

  private static boolean queueSample(
      MediaExtractor extractor, MediaCodec codec, int in, long endUs) {
    int size = extractor.readSampleData(codec.getInputBuffer(in), 0);
    long time = extractor.getSampleTime();
    if (size < 0 || time > endUs) {
      codec.queueInputBuffer(in, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
      return true;
    }
    codec.queueInputBuffer(in, 0, size, time, 0);
    extractor.advance();
    return false;
  }

  private static final class Pcm {
    final int sampleRate;
    final int channels;
    final int encoding;
    final int bytesPerSample;
    short[] samples = new short[0];

    Pcm(MediaFormat format) throws IOException {
      sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
      channels = Math.max(1, format.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
      encoding =
          format.containsKey(MediaFormat.KEY_PCM_ENCODING)
              ? format.getInteger(MediaFormat.KEY_PCM_ENCODING)
              : AudioFormat.ENCODING_PCM_16BIT;
      bytesPerSample = bytesPerSample(encoding);
    }

    boolean write(ByteBuffer data, long timeUs, long startUs, long endUs, Sink sink)
        throws IOException {
      int frames = data.remaining() / (bytesPerSample * channels);
      int first = (int) Math.max(0, Math.min(frames, framesUntil(startUs - timeUs)));
      int last = (int) Math.max(first, Math.min(frames, framesUntil(endUs - Math.max(timeUs, 0))));
      int count = (last - first) * channels;
      if (count > 0) {
        if (samples.length < count) samples = new short[count];
        data.position(data.position() + first * channels * bytesPerSample);
        read(data, count);
        sink.write(samples, count, sampleRate, channels);
      }
      return last < frames;
    }

    // 24-bit packed samples are little-endian: low, mid, high byte.
    private void read(ByteBuffer data, int count) {
      int base = data.position();
      switch (encoding) {
        case AudioFormat.ENCODING_PCM_FLOAT:
          FloatBuffer floats = data.asFloatBuffer();
          for (int i = 0; i < count; i++) samples[i] = toShort(floats.get(i));
          break;
        case AudioFormat.ENCODING_PCM_32BIT:
          IntBuffer ints = data.asIntBuffer();
          for (int i = 0; i < count; i++) samples[i] = (short) (ints.get(i) >> 16);
          break;
        case AudioFormat.ENCODING_PCM_24BIT_PACKED:
          for (int i = 0; i < count; i++) {
            int p = base + i * 3;
            samples[i] = (short) ((data.get(p + 2) << 8) | (data.get(p + 1) & 0xFF));
          }
          break;
        case AudioFormat.ENCODING_PCM_8BIT:
          for (int i = 0; i < count; i++) {
            samples[i] = (short) (((data.get(base + i) & 0xFF) - 128) << 8);
          }
          break;
        default:
          data.asShortBuffer().get(samples, 0, count);
      }
    }

    private static int bytesPerSample(int encoding) throws IOException {
      switch (encoding) {
        case AudioFormat.ENCODING_DEFAULT:
        case AudioFormat.ENCODING_PCM_16BIT:
          return 2;
        case AudioFormat.ENCODING_PCM_8BIT:
          return 1;
        case AudioFormat.ENCODING_PCM_24BIT_PACKED:
          return 3;
        case AudioFormat.ENCODING_PCM_32BIT:
        case AudioFormat.ENCODING_PCM_FLOAT:
          return 4;
        default:
          throw new IOException("unsupported PCM encoding " + encoding);
      }
    }

    private long framesUntil(long us) {
      if (us <= 0) return 0;
      if (us >= Long.MAX_VALUE / sampleRate) return Long.MAX_VALUE;
      return (us * sampleRate + 999_999) / 1_000_000;
    }

    private static short toShort(float sample) {
      return (short)
          Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(sample * 32768)));
    }
  }
}
