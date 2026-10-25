package app.zipper.knot.mapping;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.BatchFindClassUsingStrings;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.enums.StringMatchType;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.MethodData;

final class MappingPackageAnchors {

  private MappingPackageAnchors() {}

  static void run(DexKitBridge bridge, String packagesFile, Writer out) throws Exception {
    List<String> packages = new ArrayList<>();
    try (BufferedReader in = new BufferedReader(new FileReader(packagesFile))) {
      for (String line; (line = in.readLine()) != null; ) if (!line.isEmpty()) packages.add(line);
    }
    Map<String, Set<String>> owners = new HashMap<>();
    Map<String, List<String>> candidates = new LinkedHashMap<>();
    for (String pkg : packages) {
      List<String> strings = new ArrayList<>();
      for (ClassData c : bridge.findClass(FindClass.create().searchPackages(pkg))) {
        if (!Finder.packageOf(c.getName().split("\\$")[0]).equals(pkg)) continue;
        Set<String> used = new LinkedHashSet<>();
        for (MethodData m : c.getMethods()) used.addAll(m.getUsingStrings());
        for (String s : used) {
          if (s.length() < 4 || s.length() > 200) continue;
          owners.computeIfAbsent(s, k -> new LinkedHashSet<>()).add(c.getName());
          strings.add(s);
        }
      }
      candidates.put(pkg, strings);
    }
    List<String> all = new ArrayList<>(owners.keySet());
    BatchFindClassUsingStrings batch = BatchFindClassUsingStrings.create();
    for (int i = 0; i < all.size(); i++) {
      List<String> one = new ArrayList<>();
      one.add(all.get(i));
      batch.addSearchGroup(String.valueOf(i), one, StringMatchType.Equals);
    }
    Map<String, ? extends List<ClassData>> found = bridge.batchFindClassUsingStrings(batch);
    Map<String, String> unique = new HashMap<>();
    for (int i = 0; i < all.size(); i++) {
      List<ClassData> users = found.get(String.valueOf(i));
      if (users != null && users.size() == 1) unique.put(all.get(i), users.get(0).getName());
    }
    JSONObject result = new JSONObject();
    for (Map.Entry<String, List<String>> e : candidates.entrySet()) {
      JSONObject anchors = new JSONObject();
      for (String s : e.getValue()) {
        String user = unique.get(s);
        if (user != null) anchors.put(s, user);
      }
      result.put(e.getKey(), anchors);
    }
    out.write(result.toString());
  }
}
