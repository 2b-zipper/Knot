package app.zipper.knot;

import android.app.Application;
import android.util.Log;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedInterface.Hooker;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class Knot {

  public static final String TAG = "Knot";

  private static final int LOG_CAPACITY = 500;
  private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
  private static final ArrayDeque<String> logBuffer = new ArrayDeque<>();
  private static long logCount;

  public static volatile XposedInterface module;
  public static volatile String processName;

  private Knot() {}

  public static void log(String msg) {
    remember("I", msg, null);
    XposedInterface m = module;
    if (m != null) {
      m.log(Log.INFO, TAG, msg);
    } else {
      Log.i(TAG, msg);
    }
  }

  public static void log(String msg, Throwable t) {
    remember("E", msg, t);
    XposedInterface m = module;
    if (m != null) {
      m.log(Log.ERROR, TAG, msg, t);
    } else {
      Log.e(TAG, msg, t);
    }
  }

  public static List<String> recentLogs() {
    synchronized (logBuffer) {
      return new ArrayList<>(logBuffer);
    }
  }

  public static long logMark() {
    synchronized (logBuffer) {
      return logCount;
    }
  }

  public static List<String> logsSince(long mark) {
    synchronized (logBuffer) {
      int count = (int) Math.min(logCount - mark, logBuffer.size());
      List<String> entries = new ArrayList<>(count);
      Iterator<String> newestFirst = logBuffer.descendingIterator();
      for (int i = 0; i < count; i++) entries.add(newestFirst.next());
      Collections.reverse(entries);
      return entries;
    }
  }

  public static void clearLogs() {
    synchronized (logBuffer) {
      logBuffer.clear();
    }
  }

  private static void remember(String level, String msg, Throwable t) {
    String entry = LocalTime.now().format(LOG_TIME) + " " + level + " " + msg;
    if (t != null) entry += "\n" + Log.getStackTraceString(t);
    synchronized (logBuffer) {
      if (logBuffer.size() >= LOG_CAPACITY) logBuffer.removeFirst();
      logBuffer.addLast(entry);
      logCount++;
    }
  }

  public static void hookAll(Class<?> clazz, String name, Hooker hooker) {
    for (Method m : clazz.getDeclaredMethods()) {
      if (m.getName().equals(name)) {
        m.setAccessible(true);
        module.hook(m).intercept(hooker);
      }
    }
  }

  public static void hookAllCtors(Class<?> clazz, Hooker hooker) {
    for (Constructor<?> c : clazz.getDeclaredConstructors()) {
      c.setAccessible(true);
      module.hook(c).intercept(hooker);
    }
  }

  public static Application currentApplication() {
    try {
      Class<?> at = Class.forName("android.app.ActivityThread");
      return (Application) at.getMethod("currentApplication").invoke(null);
    } catch (Throwable t) {
      return null;
    }
  }
}
