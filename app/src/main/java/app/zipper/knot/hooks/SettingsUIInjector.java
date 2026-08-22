package app.zipper.knot.hooks;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Reflect;
import app.zipper.knot.ui.settings.KnotSettingsDialog;
import app.zipper.knot.ui.settings.SettingsFilePickers;
import app.zipper.knot.ui.settings.SettingsViews;
import app.zipper.knot.utils.ModuleStrings;
import io.github.libxposed.api.XposedInterface;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SettingsUIInjector implements BaseHook {

  private static final String BRAND_TAG = ModuleStrings.BRAND_NAME;

  private static volatile WeakReference<Activity> foregroundActivity = null;

  private volatile Object targetAdapter = null;

  public static void openSettings(Activity activity) {
    KnotSettingsDialog.show(activity);
  }

  public static Activity getForegroundActivity() {
    WeakReference<Activity> ref = foregroundActivity;
    Activity activity = ref == null ? null : ref.get();
    return activity == null || activity.isFinishing() ? null : activity;
  }

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    LineVersion.Config cfg = LineVersion.get();

    hookSettingsFragment(cfg, lpparam);
    hookSettingsItemInjection(cfg, lpparam);
    hookViewHolderBinding(cfg, lpparam);
    hookActivityResult();
    hookHostLifecycle();
  }

  private void hookSettingsFragment(LineVersion.Config cfg, LoadParam lpparam) {
    Class<?> fragmentClass =
        Reflect.findClass(cfg.settings.mainSettingsFragmentClass, lpparam.classLoader);
    Knot.module
        .hook(Reflect.findMethodExact(fragmentClass, "onViewCreated", View.class, Bundle.class))
        .intercept(this::onSettingsFragmentViewCreated);
  }

  private void hookSettingsItemInjection(LineVersion.Config cfg, LoadParam lpparam) {
    final Class<?> proxyInterface =
        Reflect.findClass(cfg.settings.settingsItemClass, lpparam.classLoader);
    final Class<?> searchHelperCls =
        Reflect.findClass(cfg.settings.settingsSearchHelperClass, lpparam.classLoader);
    Knot.module
        .hook(
            Reflect.findMethodExact(
                cfg.settings.settingsAdapterClass,
                lpparam.classLoader,
                cfg.settings.methodSetItems,
                Collection.class))
        .intercept(chain -> injectKnotItems(chain, proxyInterface, searchHelperCls, lpparam));
  }

  private void hookViewHolderBinding(LineVersion.Config cfg, LoadParam lpparam) {
    final Class<?> searchHelperCls =
        Reflect.findClass(cfg.settings.settingsSearchHelperClass, lpparam.classLoader);
    Class<?> itemBindingClass =
        Reflect.findClass(cfg.settings.settingsBaseAdapterClass, lpparam.classLoader);
    Knot.module
        .hook(
            Reflect.findMethodExact(
                cfg.settings.settingsSearchHelperClass,
                lpparam.classLoader,
                cfg.settings.methodBindViewHolder,
                itemBindingClass,
                int.class))
        .intercept(chain -> bindKnotViewHolder(chain, searchHelperCls));
  }

  private void hookActivityResult() {
    Knot.module
        .hook(
            Reflect.findMethodExact(
                Activity.class, "onActivityResult", int.class, int.class, Intent.class))
        .intercept(this::handleActivityResult);
  }

  private void hookHostLifecycle() {
    Knot.module
        .hook(Reflect.findMethodExact(Activity.class, "onDestroy"))
        .intercept(this::onHostDestroy);
    Knot.module
        .hook(Reflect.findMethodExact(Activity.class, "onResume"))
        .intercept(this::onHostResume);
  }

  private Object onSettingsFragmentViewCreated(XposedInterface.Chain chain) throws Throwable {
    Object result = chain.proceed();
    try {
      View listView = ((View) chain.getArg(0)).findViewById(LineVersion.get().res.idSettingList);
      if (listView != null) targetAdapter = Reflect.callMethod(listView, "getAdapter");
    } catch (Throwable ignored) {
    }
    return result;
  }

  private Object handleActivityResult(XposedInterface.Chain chain) throws Throwable {
    boolean consumed =
        SettingsFilePickers.consumeResult(
            (Activity) chain.getThisObject(),
            (int) chain.getArg(0),
            (int) chain.getArg(1),
            (Intent) chain.getArg(2));
    return consumed ? null : chain.proceed();
  }

  private Object onHostDestroy(XposedInterface.Chain chain) throws Throwable {
    KnotSettingsDialog.onActivityDestroyed((Activity) chain.getThisObject());
    return chain.proceed();
  }

  private Object onHostResume(XposedInterface.Chain chain) throws Throwable {
    Activity activity = (Activity) chain.getThisObject();
    foregroundActivity = new WeakReference<>(activity);
    KnotSettingsDialog.onActivityResumed(activity);
    return chain.proceed();
  }

  private Object injectKnotItems(
      XposedInterface.Chain chain,
      Class<?> proxyInterface,
      Class<?> searchHelperCls,
      LoadParam lpparam)
      throws Throwable {
    if (chain.getThisObject() != targetAdapter
        && !searchHelperCls.isInstance(chain.getThisObject())) {
      return chain.proceed();
    }
    LineVersion.Config c = LineVersion.get();
    Collection<?> sourceItems = (Collection<?>) chain.getArg(0);
    if (containsKnotItem(sourceItems, c)) return chain.proceed();

    List<Object> items = new ArrayList<>(sourceItems);
    insertKnotEntries(
        items, personalInfoPosition(items, c), c, lpparam.classLoader, proxyInterface);
    return chain.proceed(new Object[] {items});
  }

  private static int personalInfoPosition(List<Object> items, LineVersion.Config c) {
    for (int i = 0; i < items.size(); i++) {
      try {
        Object model = Reflect.getObjectField(items.get(i), c.settings.fieldItemModel);
        if (model == null) continue;
        for (Field f : model.getClass().getDeclaredFields()) {
          if (f.getType() != int.class) continue;
          f.setAccessible(true);
          if (f.getInt(model) == c.res.idPersonalInfo) return i;
        }
      } catch (Throwable ignored) {
      }
    }
    return items.size();
  }

  // Newer adapter models cannot be proxied, so allocate uninitialised instances via Unsafe
  private static void insertKnotEntries(
      List<Object> items, int at, LineVersion.Config c, ClassLoader cl, Class<?> proxyInterface) {
    Object header = createAdapterItemProxy(proxyInterface, cl, c.res.typeSection);
    Object row = createAdapterItemProxy(proxyInterface, cl, c.res.typeRow);

    if (c.settings.settingsAdapterWrapperClass != null
        && !c.settings.settingsAdapterWrapperClass.isEmpty()) {
      try {
        Class<?> wrapperCls = Reflect.findClass(c.settings.settingsAdapterWrapperClass, cl);
        Class<?> headerCls = Reflect.findClass(c.settings.settingsHeaderItemClass, cl);
        Class<?> itemCls = Reflect.findClass(c.settings.settingsRowItemClass, cl);

        Class<?> unsafeCls = Reflect.findClass("sun.misc.Unsafe", (ClassLoader) null);
        Object unsafe = Reflect.getStaticObjectField(unsafeCls, "theUnsafe");

        Object dummyHeader = Reflect.callMethod(unsafe, "allocateInstance", headerCls);
        Object dummyRow = Reflect.callMethod(unsafe, "allocateInstance", itemCls);

        Reflect.setIntField(dummyHeader, c.settings.fieldLayoutId, c.res.typeSection);
        Reflect.setIntField(dummyRow, c.settings.fieldLayoutId, c.res.typeRow);

        header = Reflect.newInstance(wrapperCls, dummyHeader);
        row = Reflect.newInstance(wrapperCls, dummyRow);

        Reflect.setObjectField(dummyHeader, c.settings.fieldModelTag, BRAND_TAG);
        Reflect.setObjectField(dummyRow, c.settings.fieldModelTag, BRAND_TAG);
        Reflect.setBooleanField(dummyHeader, c.settings.fieldIsVisible, true);

        Class<?> bc = Reflect.findClass(c.settings.settingsHandlerBaseClass, cl);
        Object dummyHandler = Reflect.getStaticObjectField(bc, c.settings.fieldDefaultHandler);
        String[] handlerFields = {
          c.settings.fieldActionHandler,
          c.settings.fieldIconProvider,
          c.settings.fieldDescriptionProvider,
          c.settings.fieldSubActionHandler,
          c.settings.fieldVisibilityFilter
        };
        for (String f : handlerFields) {
          try {
            Reflect.setObjectField(dummyRow, f, dummyHandler);
            Reflect.setObjectField(dummyHeader, f, dummyHandler);
          } catch (Throwable ignored) {
          }
        }

        Object commonHandler = Reflect.getStaticObjectField(bc, c.settings.fieldCommonHandler);
        Reflect.setObjectField(dummyRow, c.settings.fieldVisibilityFilter, commonHandler);
        Reflect.setObjectField(dummyHeader, c.settings.fieldVisibilityFilter, commonHandler);
      } catch (Throwable e) {
        Knot.log("Knot: Adapter wrapper failed: " + e);
      }
    }

    items.add(at, header);
    items.add(at + 1, row);
  }

  private static Object createAdapterItemProxy(Class<?> itf, ClassLoader cl, int type) {
    LineVersion.Config c = LineVersion.get();
    return Proxy.newProxyInstance(
        cl,
        new Class[] {itf},
        (proxy, method, args) ->
            c.settings.methodProxyGetItemType.equals(method.getName()) ? type : null);
  }

  private static boolean containsKnotItem(Collection<?> items, LineVersion.Config c) {
    if (items == null) return false;
    for (Object item : items) {
      try {
        Object model = Reflect.getObjectField(item, c.settings.fieldItemModel);
        if (model == null) continue;
        if (BRAND_TAG.equals(Reflect.getObjectField(model, c.settings.fieldModelTag))) return true;
      } catch (Throwable ignored) {
      }
    }
    return false;
  }

  private Object bindKnotViewHolder(XposedInterface.Chain chain, Class<?> searchHelperCls)
      throws Throwable {
    if (chain.getThisObject() != targetAdapter
        && !searchHelperCls.isInstance(chain.getThisObject())) {
      return chain.proceed();
    }
    LineVersion.Config c = LineVersion.get();
    boolean ours = false;
    try {
      Object currentItem =
          Reflect.callMethod(
              chain.getThisObject(), c.settings.methodGetItem, (int) chain.getArg(1));
      if (currentItem == null) return chain.proceed();
      if (currentItem.getClass().getName().equals(c.settings.settingsAdapterWrapperClass)) {
        currentItem = Reflect.getObjectField(currentItem, c.settings.fieldItemModel);
      }
      if (currentItem == null) return chain.proceed();

      String sourceTag = (String) Reflect.getObjectField(currentItem, c.settings.fieldModelTag);
      if (!BRAND_TAG.equals(sourceTag)) return chain.proceed();

      ours = true;

      int entryType = Reflect.getIntField(currentItem, c.settings.fieldLayoutId);
      View itemView =
          (View) Reflect.getObjectField(chain.getArg(0), c.settings.fieldViewHolderView);
      if (entryType == c.res.typeSection) {
        if (itemView instanceof TextView) ((TextView) itemView).setText(BRAND_TAG);
      } else if (entryType == c.res.typeRow) {
        bindKnotSettingsRow(itemView, c);
      }
    } catch (Throwable ignored) {
    }
    return ours ? null : chain.proceed();
  }

  private void bindKnotSettingsRow(View itemView, LineVersion.Config c) {
    SettingsViews.applyVisibility(itemView, c.res.idIcon, View.VISIBLE);
    SettingsViews.applyVisibility(itemView, c.res.idDesc, View.GONE);
    SettingsViews.applyVisibility(itemView, c.res.idMark, View.GONE);
    SettingsViews.applyVisibility(itemView, c.res.idSeparator, View.GONE);
    SettingsViews.applyVisibility(itemView, c.res.idNewMark, View.GONE);
    SettingsViews.applyVisibility(itemView, c.res.idNoticeDot, View.GONE);
    SettingsViews.applyVisibility(itemView, c.res.idArrow, View.VISIBLE);

    ImageView iconView = itemView.findViewById(c.res.idIcon);
    if (iconView != null) applyKnotIcon(itemView, iconView);

    TextView title = itemView.findViewById(c.res.idTitle);
    if (title != null) title.setText(ModuleStrings.SETTINGS_TITLE);
    itemView.setOnClickListener(v -> KnotSettingsDialog.show(v.getContext()));
  }

  private void applyKnotIcon(View itemView, ImageView iconView) {
    try {
      Drawable icon = SettingsViews.moduleIcon(itemView.getContext());
      if (icon == null) return;

      iconView.setImageTintList(null);
      iconView.setImageDrawable(icon);
      iconView.clearColorFilter();
      iconView.setVisibility(View.VISIBLE);

      int size = SettingsViews.dp(itemView.getContext(), 24);
      ViewGroup.LayoutParams lp = iconView.getLayoutParams();
      if (lp != null) {
        lp.width = size;
        lp.height = size;
        iconView.setLayoutParams(lp);
      }
      iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
    } catch (Throwable ignored) {
    }
  }
}
