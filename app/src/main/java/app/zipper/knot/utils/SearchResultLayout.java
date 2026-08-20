package app.zipper.knot.utils;

import app.zipper.knot.LineVersion;
import app.zipper.knot.Reflect;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Reads MessageSearchInChatResult's constructor against the mapped argument order.
public final class SearchResultLayout {

  private static final Map<Class<?>, SearchResultLayout> CACHE = new ConcurrentHashMap<>();

  public final Constructor<?> constructor;
  public final int chatId;
  public final int keyword;
  public final int count;
  public final int idList;

  private SearchResultLayout(
      Constructor<?> constructor, int chatId, int keyword, int count, int idList) {
    this.constructor = constructor;
    this.chatId = chatId;
    this.keyword = keyword;
    this.count = count;
    this.idList = idList;
  }

  public static SearchResultLayout of(LineVersion.Config.Chat chat, ClassLoader cl) {
    return CACHE.computeIfAbsent(
        Reflect.findClass(chat.searchResultClass, cl),
        clazz -> resolve(clazz, chat.searchResultCtorArgs));
  }

  public Object newInstance(String chatId, String keyword, int count, List<Long> ids)
      throws ReflectiveOperationException {
    Class<?>[] params = constructor.getParameterTypes();
    Object[] args = new Object[params.length];
    for (int i = 0; i < params.length; i++) {
      args[i] = defaultValue(params[i]);
    }
    args[this.chatId] = chatId;
    args[this.keyword] = keyword;
    args[this.count] = count;
    args[this.idList] = ids;
    return constructor.newInstance(args);
  }

  private static Object defaultValue(Class<?> type) {
    // Parameters newer builds appended are Kotlin non-null and get null-checked in the constructor.
    if (type == List.class) return Collections.emptyList();
    if (type.isPrimitive()) return Array.get(Array.newInstance(type, 1), 0);
    return null;
  }

  private static SearchResultLayout resolve(Class<?> clazz, String argOrder) {
    Constructor<?>[] constructors = clazz.getDeclaredConstructors();
    if (constructors.length != 1) {
      throw new IllegalStateException(
          clazz.getName() + " has " + constructors.length + " constructors, expected 1");
    }
    Constructor<?> constructor = constructors[0];
    constructor.setAccessible(true);

    List<String> roles = Arrays.asList(argOrder.split(","));
    int chatId = roles.indexOf("chatId");
    int keyword = roles.indexOf("keyword");
    int count = roles.indexOf("count");
    int idList = roles.indexOf("idList");
    if (chatId < 0 || keyword < 0 || count < 0 || idList < 0) {
      throw new IllegalStateException("searchResultCtorArgs is incomplete: " + argOrder);
    }
    if (roles.size() > constructor.getParameterCount()) {
      throw new IllegalStateException(
          "searchResultCtorArgs does not fit " + clazz.getName() + ": " + argOrder);
    }
    return new SearchResultLayout(constructor, chatId, keyword, count, idList);
  }
}
