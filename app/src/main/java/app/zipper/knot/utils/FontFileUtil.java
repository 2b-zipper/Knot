package app.zipper.knot.utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class FontFileUtil {

  private static final int NAME_ID_FAMILY = 1;
  private static final int NAME_ID_FULL = 4;

  private static final int PLATFORM_UNICODE = 0;
  private static final int PLATFORM_MAC = 1;
  private static final int PLATFORM_WINDOWS = 3;

  private static final int LANGUAGE_NEUTRAL = 0;
  private static final int LANGUAGE_EN_US = 0x0409;

  private static final int TABLE_DIRECTORY = 12;
  private static final int TABLE_ENTRY_SIZE = 16;
  private static final int NAME_RECORDS = 6;
  private static final int NAME_RECORD_SIZE = 12;

  private FontFileUtil() {}

  public static String readFontName(File file) {
    if (file == null || !file.isFile()) return null;
    try (RandomAccessFile font = new RandomAccessFile(file, "r")) {
      long nameTable = findNameTable(font);
      if (nameTable < 0) return null;

      NameRecord best = pickNameRecord(font, nameTable);
      return best == null ? null : readName(font, best);
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static long findNameTable(RandomAccessFile font) throws IOException {
    long base = "ttcf".equals(readTag(font, 0)) ? readOffset(font, TABLE_DIRECTORY) : 0;
    int tableCount = readUShort(font, base + 4);
    for (int i = 0; i < tableCount; i++) {
      long entry = base + TABLE_DIRECTORY + i * (long) TABLE_ENTRY_SIZE;
      if ("name".equals(readTag(font, entry))) return readOffset(font, entry + 8);
    }
    return -1;
  }

  private static NameRecord pickNameRecord(RandomAccessFile font, long nameTable)
      throws IOException {
    int recordCount = readUShort(font, nameTable + 2);
    long strings = nameTable + readUShort(font, nameTable + 4);

    NameRecord best = null;
    for (int i = 0; i < recordCount; i++) {
      long entry = nameTable + NAME_RECORDS + i * (long) NAME_RECORD_SIZE;
      NameRecord record = readNameRecord(font, entry, strings);
      if (record != null && (best == null || record.score() > best.score())) best = record;
    }
    return best;
  }

  private static NameRecord readNameRecord(RandomAccessFile font, long entry, long strings)
      throws IOException {
    font.seek(entry);
    int platform = font.readUnsignedShort();
    font.skipBytes(2);
    int language = font.readUnsignedShort();
    int nameId = font.readUnsignedShort();
    int length = font.readUnsignedShort();
    long offset = strings + font.readUnsignedShort();

    if (nameId != NAME_ID_FULL && nameId != NAME_ID_FAMILY) return null;
    return length > 0 ? new NameRecord(platform, language, nameId, length, offset) : null;
  }

  private static String readName(RandomAccessFile font, NameRecord record) throws IOException {
    byte[] raw = new byte[record.length];
    font.seek(record.offset);
    font.readFully(raw);

    Charset charset =
        record.platform == PLATFORM_MAC ? StandardCharsets.ISO_8859_1 : StandardCharsets.UTF_16BE;
    String name = new String(raw, charset).replace('\0', ' ').trim();
    return name.isEmpty() ? null : name;
  }

  private static String readTag(RandomAccessFile font, long position) throws IOException {
    byte[] tag = new byte[4];
    font.seek(position);
    font.readFully(tag);
    return new String(tag, StandardCharsets.US_ASCII);
  }

  private static int readUShort(RandomAccessFile font, long position) throws IOException {
    font.seek(position);
    return font.readUnsignedShort();
  }

  private static long readOffset(RandomAccessFile font, long position) throws IOException {
    font.seek(position);
    return font.readInt() & 0xFFFFFFFFL;
  }

  private static final class NameRecord {
    final int platform;
    final int language;
    final int nameId;
    final int length;
    final long offset;

    NameRecord(int platform, int language, int nameId, int length, long offset) {
      this.platform = platform;
      this.language = language;
      this.nameId = nameId;
      this.length = length;
      this.offset = offset;
    }

    int score() {
      return nameScore() + platformScore() + languageScore();
    }

    private int nameScore() {
      return nameId == NAME_ID_FULL ? 100 : 50;
    }

    private int platformScore() {
      if (platform == PLATFORM_WINDOWS) return 10;
      return platform == PLATFORM_UNICODE ? 8 : 4;
    }

    private int languageScore() {
      return language == LANGUAGE_EN_US || language == LANGUAGE_NEUTRAL ? 1 : 0;
    }
  }
}
