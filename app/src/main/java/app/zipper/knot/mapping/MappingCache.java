package app.zipper.knot.mapping;

import android.content.Context;
import android.os.Process;
import app.zipper.knot.Knot;
import app.zipper.knot.LineVersion;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

final class MappingCache {

  // Saved in place of the mappings after a failed resolve, so only the next launch retries
  private static final String FAILED = "\tfailed";

  private final File file;
  private final String stamp;

  MappingCache(Context host, String stamp) {
    this.file = file(host);
    this.stamp = stamp;
  }

  static void delete(Context host) {
    File file = file(host);
    File[] files = file.getParentFile().listFiles();
    if (files == null) return;
    for (File f : files) if (f.getName().startsWith(file.getName())) f.delete();
  }

  private static File file(Context host) {
    return new File(new File(host.getCacheDir(), "knot"), "mappings.tsv");
  }

  LineVersion.Config read() {
    String[] lines = lines();
    if (lines == null || !stamp.equals(lines[0])) return null;
    try {
      Map<String, Object> values = new HashMap<>();
      for (int i = 1; i < lines.length; i++) {
        int tab = lines[i].indexOf('\t');
        if (tab > 0) values.put(lines[i].substring(0, tab), lines[i].substring(tab + 1));
      }
      LineVersion.Config config = new LineVersion.Config();
      ConfigKeys.write(config, values);
      return config;
    } catch (Throwable t) {
      Knot.log("Knot: mapping cache unreadable: " + t);
      return null;
    }
  }

  boolean failedBefore() {
    String[] lines = lines();
    return lines != null && (stamp + FAILED).equals(lines[0]);
  }

  void markFailed() {
    save(stamp + FAILED);
  }

  void write(LineVersion.Config config) {
    StringBuilder text = new StringBuilder(stamp);
    for (Map.Entry<String, Object> e : ConfigKeys.read(config).entrySet()) {
      if (e.getValue() == null) continue;
      String value = e.getValue().toString();
      if (value.indexOf('\t') >= 0 || value.indexOf('\n') >= 0) {
        Knot.log("Knot: mapping cache not saved: separator in " + e.getKey());
        return;
      }
      text.append('\n').append(e.getKey()).append('\t').append(value);
    }
    save(text.toString());
  }

  private String[] lines() {
    if (!file.isFile()) return null;
    try {
      return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).split("\n");
    } catch (Throwable t) {
      Knot.log("Knot: mapping cache unreadable: " + t);
      return null;
    }
  }

  // LINE's other processes can resolve at the same moment, so each writes its own temporary file
  private void save(String text) {
    try {
      file.getParentFile().mkdirs();
      File tmp = new File(file.getPath() + "." + Process.myPid() + ".tmp");
      try (FileOutputStream out = new FileOutputStream(tmp)) {
        out.write(text.getBytes(StandardCharsets.UTF_8));
      }
      if (!tmp.renameTo(file)) tmp.delete();
    } catch (Throwable t) {
      Knot.log("Knot: mapping cache not saved: " + t);
    }
  }
}
