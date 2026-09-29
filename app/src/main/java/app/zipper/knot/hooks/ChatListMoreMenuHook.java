package app.zipper.knot.hooks;

import android.util.Pair;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Reflect;
import java.util.List;

public class ChatListMoreMenuHook implements BaseHook {

  private static final int TAG_CLICK_WRAPPED = 0x64010006;

  @Override
  public void hook(KnotConfig options, LoadParam lpparam) throws Throwable {
    LineVersion.Config cfg = LineVersion.get();
    if (cfg.chatListMoreMenu.popupListViewClass.isEmpty()
        || cfg.chatListMoreMenu.fieldListView.isEmpty()
        || cfg.chatListMoreMenu.popupListAdapterClass.isEmpty()
        || cfg.chatListMoreMenu.fieldPopupItems.isEmpty()
        || cfg.chatListMoreMenu.clickListenerClass.isEmpty()
        || cfg.chatListMoreMenu.methodAddItem.isEmpty()) {
      return;
    }
    Class<?> popupCls =
        Reflect.findClass(cfg.chatListMoreMenu.popupListViewClass, lpparam.classLoader);

    Knot.module
        .hook(
            Reflect.findMethodExact(
                popupCls, "setOnItemClickListener", AdapterView.OnItemClickListener.class))
        .intercept(
            chain -> {
              Object result = chain.proceed();
              Object listener = chain.getArg(0);
              if (isTargetOriginalListener(listener)) {
                installClickWrapperIfNeeded(
                    chain.getThisObject(), (AdapterView.OnItemClickListener) listener);
              }
              return result;
            });

    Knot.module
        .hook(
            Reflect.findMethodExact(
                popupCls,
                cfg.chatListMoreMenu.methodAddItem,
                String.class,
                int.class,
                boolean.class))
        .intercept(
            chain -> {
              Object result = chain.proceed();
              injectPairItems(chain.getThisObject(), (boolean) chain.getArg(2));
              return result;
            });
  }

  private static void installClickWrapperIfNeeded(
      Object popupListView, AdapterView.OnItemClickListener original) {
    ListView listView = getListView(popupListView);
    if (listView == null) return;
    listView.setOnItemClickListener(new KnotMoreMenuClickListener(original));
    listView.setTag(TAG_CLICK_WRAPPED, Boolean.TRUE);
    injectPairItems(popupListView, true);
  }

  @SuppressWarnings("unchecked")
  private static boolean injectPairItems(Object popupListView, boolean notify) {
    try {
      LineVersion.Config cfg = LineVersion.get();
      ListView listView = getListView(popupListView);
      if (listView == null) return false;
      if (!Boolean.TRUE.equals(listView.getTag(TAG_CLICK_WRAPPED))) return false;
      ListAdapter adapter = listView.getAdapter();
      if (!isTargetAdapter(adapter)) return false;

      Object itemsObj = Reflect.getObjectField(adapter, cfg.chatListMoreMenu.fieldPopupItems);
      if (!(itemsObj instanceof List)) return false;

      List<Object> items = (List<Object>) itemsObj;
      removeKnotPairs(items);
      if (!hasOriginalPair(items)) return false;

      for (ReadToggle toggle : ReadToggle.values()) {
        if (toggle.isAvailable()) items.add(new Pair<>(-1, toggle.label(toggle.isOn())));
      }

      if (notify && adapter instanceof BaseAdapter) {
        ((BaseAdapter) adapter).notifyDataSetChanged();
      }
      return true;
    } catch (Throwable t) {
      Knot.log("Knot: ChatListMoreMenu model inject error: " + t);
      return false;
    }
  }

  private static ListView getListView(Object popupListView) {
    try {
      LineVersion.Config cfg = LineVersion.get();
      Object listView = Reflect.getObjectField(popupListView, cfg.chatListMoreMenu.fieldListView);
      return listView instanceof ListView ? (ListView) listView : null;
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static boolean isTargetAdapter(Object adapter) {
    try {
      LineVersion.Config cfg = LineVersion.get();
      return cfg != null
          && !cfg.chatListMoreMenu.popupListAdapterClass.isEmpty()
          && adapter != null
          && adapter.getClass().getName().equals(cfg.chatListMoreMenu.popupListAdapterClass);
    } catch (Throwable ignored) {
      return false;
    }
  }

  private static boolean isTargetOriginalListener(Object listener) {
    try {
      LineVersion.Config cfg = LineVersion.get();
      return cfg != null
          && !cfg.chatListMoreMenu.clickListenerClass.isEmpty()
          && listener != null
          && listener.getClass().getName().equals(cfg.chatListMoreMenu.clickListenerClass);
    } catch (Throwable ignored) {
      return false;
    }
  }

  private static boolean hasOriginalPair(List<Object> items) {
    for (Object item : items) {
      if (item instanceof Pair && !isKnotPair(item)) return true;
    }
    return false;
  }

  private static void removeKnotPairs(List<Object> items) {
    for (int i = items.size() - 1; i >= 0; i--) {
      if (isKnotPair(items.get(i))) items.remove(i);
    }
  }

  private static boolean isKnotPair(Object item) {
    return toggleOf(item) != null;
  }

  private static ReadToggle toggleOf(Object item) {
    if (!(item instanceof Pair)) return null;
    Object label = ((Pair<?, ?>) item).second;
    return label instanceof String ? ReadToggle.fromLabel((String) label) : null;
  }

  private static class KnotMoreMenuClickListener implements AdapterView.OnItemClickListener {
    private final AdapterView.OnItemClickListener original;

    KnotMoreMenuClickListener(AdapterView.OnItemClickListener original) {
      this.original = original;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      try {
        Adapter adapter = parent.getAdapter();
        ReadToggle toggle = toggleOf(adapter == null ? null : adapter.getItem(position));
        if (toggle != null) {
          if (toggle.isAvailable()) toggle.toggle();
          injectPairItems(parent.getParent(), true);
          return;
        }
      } catch (Throwable t) {
        Knot.log("Knot: ChatListMoreMenu click error: " + t);
      }
      if (original != null) original.onItemClick(parent, view, position, id);
    }
  }
}
