package app.zipper.knot.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

// Sequential reads give exact timestamps even where seeking is approximate, such as VBR MP3.
public final class CompressedAudio {

  private static final int DEFAULT_SIZE = 1 << 20;

  final MediaFormat format;
  private byte[] data;
  private int[] offsets = new int[4096];
  private long[] timesUs = new long[4096];
  private int count;

  // The samples are a subset of the file, so its size holds them all without regrowing.
  private CompressedAudio(MediaFormat format, long fileSize) {
    this.format = format;
    data = new byte[fileSize > 0 && fileSize <= Integer.MAX_VALUE ? (int) fileSize : DEFAULT_SIZE];
  }

  public static CompressedAudio read(Context ctx, Uri uri) throws IOException {
    MediaExtractor extractor = new MediaExtractor();
    try (AssetFileDescriptor file = ctx.getContentResolver().openAssetFileDescriptor(uri, "r")) {
      if (file == null) throw new IOException("cannot open " + uri);
      extractor.setDataSource(file);
      CompressedAudio audio = new CompressedAudio(selectAudioTrack(extractor), file.getLength());
      ByteBuffer buffer = ByteBuffer.allocateDirect(maxSampleSize(audio.format));
      int size;
      while ((size = extractor.readSampleData(buffer, 0)) >= 0) {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
        audio.append(buffer, size, extractor.getSampleTime());
        extractor.advance();
      }
      if (audio.count == 0) throw new IOException("no audio samples");
      return audio;
    } finally {
      extractor.release();
    }
  }

  String mime() {
    return format.getString(MediaFormat.KEY_MIME);
  }

  int count() {
    return count;
  }

  long timeUs(int index) {
    return timesUs[index];
  }

  long lastTimeUs() {
    return timesUs[count - 1];
  }

  int copySample(int index, ByteBuffer dst) {
    int size = offsets[index + 1] - offsets[index];
    dst.put(data, offsets[index], size);
    return size;
  }

  int indexAtOrBefore(long timeUs) {
    int found = Arrays.binarySearch(timesUs, 0, count, timeUs);
    return found >= 0 ? found : Math.max(0, -found - 2);
  }

  private void append(ByteBuffer buffer, int size, long timeUs) {
    int end = offsets[count] + size;
    if (end > data.length) data = Arrays.copyOf(data, Math.max(end, data.length * 2));
    if (count + 2 > offsets.length) {
      offsets = Arrays.copyOf(offsets, offsets.length * 2);
      timesUs = Arrays.copyOf(timesUs, timesUs.length * 2);
    }
    buffer.get(data, offsets[count], size);
    timesUs[count] = timeUs;
    offsets[++count] = end;
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

  private static int maxSampleSize(MediaFormat format) {
    return format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)
        ? format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        : DEFAULT_SIZE;
  }
}
