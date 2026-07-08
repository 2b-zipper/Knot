package app.zipper.knot.hooks;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Reflect;
import app.zipper.knot.SettingsStore;
import app.zipper.knot.utils.ModuleStrings;
import java.lang.reflect.Proxy;

public class HeaderButtonInjector implements BaseHook {

  private static final ThreadLocal<Boolean> sCreating = new ThreadLocal<>();
  private Class<?> headerInterfaceAClass;
  private ClassLoader classLoader;

  @Override
  public void hook(KnotConfig options, LoadParam lpparam) throws Throwable {
    LineVersion.Config config = LineVersion.get();
    if (config == null || config.chat.headerController.isEmpty()) return;

    try {
      Class<?> headerControllerClass =
          Reflect.findClass(config.chat.headerController, lpparam.classLoader);
      Class<?> headerHelperClass = Reflect.findClass(config.chat.headerHelper, lpparam.classLoader);
      Class<?> headerButtonTypeEnum =
          Reflect.findClass(config.main.headerButtonTypeClass, lpparam.classLoader);
      headerInterfaceAClass = Reflect.findClass(config.main.headerInterfaceA, lpparam.classLoader);
      classLoader = lpparam.classLoader;
      final Object slotFarLeft =
          Reflect.getStaticObjectField(headerButtonTypeEnum, config.main.slotFarLeft);

      java.util.List<Object> ctorParams = new java.util.ArrayList<>();
      ctorParams.add(config.chatHeader.chatHistoryActivity);
      ctorParams.add(config.chatHeader.chatHistoryActivity);
      ctorParams.add(Window.class);
      ctorParams.add(View.class);
      ctorParams.add(config.chatHeader.fieldChatConfigChatId);
      ctorParams.add(config.chatHeader.fieldChatConfigIsMuted);
      ctorParams.add(config.chatHeader.fieldChatConfigType);
      ctorParams.add(headerHelperClass);
      ctorParams.add(config.chatHeader.fieldAppInfoVersion);
      ctorParams.add(config.chatHeader.fieldAppInfoPkg);
      ctorParams.add(config.chatHeader.fieldAppInfoId);

      Knot.module
          .hook(Reflect.findConstructorExact(headerControllerClass, ctorParams.toArray()))
          .intercept(
              chain -> {
                Object result = chain.proceed();
                if (SettingsStore.get("record_read_history", false)) {
                  Object controller = chain.getThisObject();
                  try {
                    View headerView = (View) chain.getArgs().get(3);
                    if (headerView != null) {
                      headerView.addOnLayoutChangeListener(
                          (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                            if (v.getVisibility() == View.VISIBLE) {
                              scheduleInjectionWithRetry(v, controller, slotFarLeft, config, 0);
                            }
                          });
                    }
                  } catch (Throwable ignored) {
                  }
                  injectButton(controller, slotFarLeft, config);
                }
                return result;
              });

      Knot.module
          .hook(
              Reflect.findMethodExact(
                  headerControllerClass,
                  config.main.methodSetHeaderButton,
                  headerButtonTypeEnum,
                  config.main.headerInterfaceA))
          .intercept(
              chain -> {
                Object result = chain.proceed();
                if (SettingsStore.get("record_read_history", false)
                    && !Boolean.TRUE.equals(sCreating.get())) {
                  injectButton(chain.getThisObject(), slotFarLeft, config);
                }
                return result;
              });

      Knot.module
          .hook(
              Reflect.findMethodExact(
                  headerControllerClass,
                  config.main.methodSetHeaderButtonVisibility,
                  headerButtonTypeEnum,
                  int.class))
          .intercept(
              chain -> {
                Object result = chain.proceed();
                if (SettingsStore.get("record_read_history", false)
                    && !Boolean.TRUE.equals(sCreating.get())) {
                  injectButton(chain.getThisObject(), slotFarLeft, config);
                }
                return result;
              });

    } catch (Throwable t) {
      Knot.log("Knot: init error: " + t.getMessage());
    }
  }

  private void scheduleInjectionWithRetry(
      View headerView, Object controller, Object slot, LineVersion.Config config, int attempt) {
    if (attempt >= 5) return;
    headerView.postDelayed(
        () -> {
          if (isInEditMode(controller, config)) {
            scheduleInjectionWithRetry(headerView, controller, slot, config, attempt + 1);
          } else {
            injectButton(controller, slot, config);
          }
        },
        200);
  }

  private static boolean isInEditMode(Object controller, LineVersion.Config config) {
    try {
      Activity activity =
          (Activity) Reflect.getObjectField(controller, config.main.fieldChatActivity);
      if (activity == null) return false;
      int execBtnId =
          activity
              .getResources()
              .getIdentifier(
                  "chat_ui_edit_mode_bottom_execution_button", "id", activity.getPackageName());
      if (execBtnId != 0) {
        View execBtn = activity.findViewById(execBtnId);
        if (execBtn != null && execBtn.isShown()) return true;
      }
      int toggleBtnId =
          activity
              .getResources()
              .getIdentifier(
                  "chat_ui_edit_mode_bottom_option_toggle_button", "id", activity.getPackageName());
      if (toggleBtnId != 0) {
        View toggleBtn = activity.findViewById(toggleBtnId);
        if (toggleBtn != null && toggleBtn.isShown()) return true;
      }
      return false;
    } catch (Throwable t) {
      return false;
    }
  }

  private void injectButton(Object controller, Object slot, LineVersion.Config config) {
    if (isInEditMode(controller, config)) return;
    try {
      Object headerHelper = Reflect.getObjectField(controller, config.main.fieldHeaderHelper);
      if (headerHelper == null) return;

      final Context context =
          (Context) Reflect.getObjectField(controller, config.main.fieldChatActivity);
      if (context == null) return;

      Drawable icon = null;
      try {
        Context modCtx =
            context.createPackageContext("app.zipper.knot", Context.CONTEXT_IGNORE_SECURITY);
        int iconId =
            modCtx.getResources().getIdentifier("ic_book", "drawable", modCtx.getPackageName());
        if (iconId != 0) {
          icon = modCtx.getDrawable(iconId);
          if (icon != null) {

            int size = (int) (24 * context.getResources().getDisplayMetrics().density);
            android.graphics.Bitmap bitmap =
                android.graphics.Bitmap.createBitmap(
                    size, size, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
            icon.setBounds(0, 0, size, size);
            icon.draw(canvas);
            icon = new android.graphics.drawable.BitmapDrawable(context.getResources(), bitmap);
          }
        }
      } catch (Throwable t) {
        Knot.log("Knot: icon load error: " + t.getMessage());
      }

      if (icon == null) return;

      // Use stable setButtonImageViewDrawable API to avoid per-version config
      Object headerButton =
          Reflect.callMethod(headerHelper, config.main.methodGetHeaderButtonView, slot);

      if (headerButton == null) {
        // LINE cleared the button; re-create it via methodSetHeaderButton with a no-op proxy
        try {
          sCreating.set(Boolean.TRUE);
          Object noopProxy =
              Proxy.newProxyInstance(
                  classLoader,
                  new Class<?>[] {headerInterfaceAClass},
                  (p, m, args) -> {
                    Class<?> rt = m.getReturnType();
                    if (rt == boolean.class) return Boolean.FALSE;
                    if (rt == int.class) return 0;
                    if (rt == long.class) return 0L;
                    if (rt == float.class) return 0f;
                    if (rt == double.class) return 0d;
                    if (rt == short.class) return (short) 0;
                    if (rt == byte.class) return (byte) 0;
                    if (rt == char.class) return '\0';
                    return null;
                  });
          Reflect.callMethod(controller, config.main.methodSetHeaderButton, slot, noopProxy);
          headerButton =
              Reflect.callMethod(headerHelper, config.main.methodGetHeaderButtonView, slot);
        } catch (Throwable t) {
          Knot.log("Knot: button recreate error: " + t.getMessage());
        } finally {
          sCreating.remove();
        }
      }

      if (headerButton == null) return;
      Reflect.callMethod(headerButton, "setButtonImageViewDrawable", icon);

      Reflect.callMethod(
          headerHelper, config.main.methodSetHeaderLabel, slot, ModuleStrings.READ_RECEIPT_VIEWER);

      Reflect.callMethod(headerHelper, config.main.methodSetHeaderButtonVisibility, slot, 0);

      try {
        if (headerButton instanceof LinearLayout) {
          LinearLayout layout = (LinearLayout) headerButton;
          layout.setGravity(android.view.Gravity.CENTER);
          int padding = (int) (7 * context.getResources().getDisplayMetrics().density);
          layout.setPadding(padding, 0, padding, 0);

          android.view.ViewGroup.LayoutParams lp = layout.getLayoutParams();
          if (lp != null) {
            lp.width = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
            layout.setLayoutParams(lp);
          }
        }
      } catch (Throwable t) {
        Knot.log("Knot: layout error: " + t.getMessage());
      }

      Reflect.callMethod(
          headerHelper,
          config.main.methodSetHeaderOnClickListener,
          slot,
          new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              try {
                Knot.log("Knot: Clicked!");
                Activity activity =
                    (Activity) Reflect.getObjectField(controller, config.main.fieldChatActivity);
                if (activity == null) return;

                String chatId = activity.getIntent().getStringExtra("chatId");
                if (chatId == null || chatId.isEmpty()) {
                  chatId = activity.getIntent().getStringExtra("chat_id");
                }

                if (chatId == null || chatId.isEmpty()) {
                  Object request = Reflect.getObjectField(activity, config.chat.chatIdField);
                  if (request != null) {
                    chatId = (String) Reflect.callMethod(request, config.chat.methodGetChatId);
                  }
                }

                if (chatId != null && !chatId.isEmpty()) {
                  app.zipper.knot.ui.ReadHistoryViewer.show(activity, chatId);
                } else {
                  android.widget.Toast.makeText(
                          activity, "ChatId not found", android.widget.Toast.LENGTH_SHORT)
                      .show();
                }
              } catch (Throwable t) {
                Knot.log("Knot: click error: " + t.toString());
              }
            }
          });

    } catch (Throwable t) {
      Knot.log("Knot: injection error: " + t.getMessage());
    }
  }
}
