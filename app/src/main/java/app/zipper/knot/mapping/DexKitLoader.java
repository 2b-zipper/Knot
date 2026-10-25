package app.zipper.knot.mapping;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import app.zipper.knot.Knot;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

// The library must be loaded from a module class so JNI binds it to DexKit's class loader
final class DexKitLoader {

  private static final String LIB_FILE = "libdexkit.so";

  private static volatile boolean loaded;

  private DexKitLoader() {}

  static synchronized boolean load(Context host) {
    if (loaded) return true;
    try {
      System.loadLibrary("dexkit");
      return loaded = true;
    } catch (Throwable ignored) {
      // Not on the module class loader's library path inside LINE
    }

    ApplicationInfo module = moduleInfo();
    if (module == null) return false;
    if (module.nativeLibraryDir != null && tryLoad(new File(module.nativeLibraryDir, LIB_FILE))) {
      return true;
    }
    File staged = stage(module, host);
    return staged != null && tryLoad(staged);
  }

  private static ApplicationInfo moduleInfo() {
    try {
      return Knot.module.getModuleApplicationInfo();
    } catch (Throwable t) {
      Knot.log("Knot: module ApplicationInfo unavailable: " + t);
      return null;
    }
  }

  private static boolean tryLoad(File lib) {
    if (!lib.isFile()) return false;
    try {
      System.load(lib.getAbsolutePath());
      return loaded = true;
    } catch (Throwable t) {
      Knot.log("Knot: loading " + lib + " failed: " + t);
      return false;
    }
  }

  private static File stage(ApplicationInfo module, Context host) {
    if (module.sourceDir == null || host == null) return null;
    File dir = new File(host.getCodeCacheDir(), "knot");
    File out = new File(dir, LIB_FILE);
    try (ZipFile apk = new ZipFile(module.sourceDir)) {
      ZipEntry entry = null;
      for (String abi : Build.SUPPORTED_ABIS) {
        entry = apk.getEntry("lib/" + abi + "/" + LIB_FILE);
        if (entry != null) break;
      }
      if (entry == null) {
        Knot.log("Knot: " + LIB_FILE + " missing from the module APK");
        return null;
      }
      if (out.isFile() && out.length() == entry.getSize()) return out;

      dir.mkdirs();
      File tmp = new File(dir, LIB_FILE + ".tmp");
      try (InputStream in = apk.getInputStream(entry);
          OutputStream os = new FileOutputStream(tmp)) {
        byte[] buf = new byte[65536];
        int n;
        while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
      }
      if (!tmp.renameTo(out)) {
        tmp.delete();
        return null;
      }
      return out;
    } catch (Throwable t) {
      Knot.log("Knot: staging " + LIB_FILE + " failed: " + t);
      return null;
    }
  }
}
