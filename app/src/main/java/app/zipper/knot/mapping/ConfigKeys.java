package app.zipper.knot.mapping;

import app.zipper.knot.LineVersion;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ConfigKeys {

  private ConfigKeys() {}

  static Map<String, Object> read(LineVersion.Config config) {
    Map<String, Object> values = new LinkedHashMap<>();
    try {
      for (Field section : sections()) {
        Object holder = section.get(config);
        for (Field value : values(section)) {
          values.put(section.getName() + "." + value.getName(), value.get(holder));
        }
      }
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
    return values;
  }

  static Map<String, String> since() {
    Map<String, String> versions = new HashMap<>();
    for (Field section : sections()) {
      for (Field value : values(section)) {
        LineVersion.Since since = value.getAnnotation(LineVersion.Since.class);
        if (since != null) versions.put(section.getName() + "." + value.getName(), since.value());
      }
    }
    return versions;
  }

  static void write(LineVersion.Config config, Map<String, Object> values) {
    try {
      for (Field section : sections()) {
        Object holder = section.get(config);
        for (Field value : values(section)) {
          Object v = values.get(section.getName() + "." + value.getName());
          if (v == null) continue;
          if (value.getType() == int.class) {
            value.setInt(
                holder,
                v instanceof Number ? ((Number) v).intValue() : Integer.parseInt(v.toString()));
          } else {
            value.set(holder, v.toString());
          }
        }
      }
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  private static Iterable<Field> sections() {
    List<Field> sections = new ArrayList<>();
    for (Field f : LineVersion.Config.class.getDeclaredFields()) {
      if (Modifier.isStatic(f.getModifiers())) continue;
      if (f.getType().getDeclaringClass() == LineVersion.Config.class) sections.add(f);
    }
    return sections;
  }

  private static Iterable<Field> values(Field section) {
    List<Field> values = new ArrayList<>();
    for (Field f : section.getType().getDeclaredFields()) {
      if (Modifier.isStatic(f.getModifiers())) continue;
      if (f.getType() == String.class || f.getType() == int.class) values.add(f);
    }
    return values;
  }
}
