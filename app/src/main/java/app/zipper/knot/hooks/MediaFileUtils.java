package app.zipper.knot.hooks;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

final class MediaFileUtils {
  private MediaFileUtils() {}

  static String sniffMime(File file) {
    if (file == null) return null;
    try (InputStream in = new FileInputStream(file)) {
      String mimeType = sniffMime(in);
      if (mimeType != null) return mimeType;
    } catch (Throwable ignored) {
    }

    String lower = file.getName().toLowerCase(Locale.ROOT);
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
    if (lower.endsWith(".png")) return "image/png";
    if (lower.endsWith(".webp")) return "image/webp";
    if (lower.endsWith(".gif")) return "image/gif";
    if (lower.endsWith(".avif")) return "image/avif";
    if (lower.endsWith(".heic") || lower.endsWith(".heif")) return "image/heif";
    if (lower.endsWith(".mp4") || lower.endsWith(".m4v") || lower.endsWith(".mov")) {
      return "video/mp4";
    }
    return null;
  }

  static String sniffMime(InputStream in) {
    if (in == null) return null;
    try {
      byte[] header = new byte[16];
      int length = 0;
      while (length < header.length) {
        int read = in.read(header, length, header.length - length);
        if (read < 0) break;
        length += read;
      }
      if (length >= 3
          && (header[0] & 0xff) == 0xff
          && (header[1] & 0xff) == 0xd8
          && (header[2] & 0xff) == 0xff) return "image/jpeg";
      if (length >= 8
          && (header[0] & 0xff) == 0x89
          && header[1] == 'P'
          && header[2] == 'N'
          && header[3] == 'G') return "image/png";
      if (length >= 6 && header[0] == 'G' && header[1] == 'I' && header[2] == 'F') {
        return "image/gif";
      }
      if (length >= 12
          && header[0] == 'R'
          && header[1] == 'I'
          && header[2] == 'F'
          && header[3] == 'F'
          && header[8] == 'W'
          && header[9] == 'E'
          && header[10] == 'B'
          && header[11] == 'P') return "image/webp";
      if (length >= 12
          && header[4] == 'f'
          && header[5] == 't'
          && header[6] == 'y'
          && header[7] == 'p') {
        String brand = new String(header, 8, Math.min(4, length - 8), StandardCharsets.US_ASCII);
        if (brand.startsWith("avif") || brand.startsWith("avis")) return "image/avif";
        if (brand.startsWith("heic") || brand.startsWith("heix") || brand.startsWith("mif1")) {
          return "image/heif";
        }
        if (brand.startsWith("isom")
            || brand.startsWith("iso2")
            || brand.startsWith("mp41")
            || brand.startsWith("mp42")
            || brand.startsWith("M4V ")
            || brand.startsWith("qt  ")) {
          return "video/mp4";
        }
      }
    } catch (Throwable ignored) {
    }
    return null;
  }

  static boolean isImage(String mimeType) {
    return mimeType != null && mimeType.startsWith("image/");
  }
}
