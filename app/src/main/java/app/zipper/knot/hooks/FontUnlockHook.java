package app.zipper.knot.hooks;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.widget.EditText;
import android.widget.TextView;
import app.zipper.knot.Knot;
import app.zipper.knot.KnotConfig;
import app.zipper.knot.LineVersion;
import app.zipper.knot.LoadParam;
import app.zipper.knot.Reflect;
import app.zipper.knot.SettingsStore;
import io.github.libxposed.api.XposedInterface;
import java.io.File;
import java.util.List;

public class FontUnlockHook implements BaseHook {
  private static Typeface customTypeface = null;
  private static boolean overrideActive = false;

  @Override
  public void hook(KnotConfig config, LoadParam lpparam) throws Throwable {
    final LineVersion.Config cfg = LineVersion.get();
    if (cfg == null || cfg.font.fontConfigClass.isEmpty()) return;

    initTypeface();
    if (!overrideActive || customTypeface == null) return;

    Knot.log("Knot: Initializing Font hooks");

    try {
      Knot.module
          .hook(Reflect.findMethodExact(TextView.class, "setIncludeFontPadding", boolean.class))
          .intercept(chain -> chain.proceed(new Object[] {false}));
    } catch (Throwable ignored) {
    }

    XposedInterface.Hooker globalHook =
        chain -> {
          chain.proceed();
          return customTypeface;
        };
    Knot.module
        .hook(Reflect.findMethodExact(Typeface.class, "create", String.class, int.class))
        .intercept(globalHook);
    Knot.module
        .hook(Reflect.findMethodExact(Typeface.class, "create", Typeface.class, int.class))
        .intercept(globalHook);
    Knot.module
        .hook(Reflect.findMethodExact(Typeface.class, "defaultFromStyle", int.class))
        .intercept(globalHook);

    XposedInterface.Hooker textViewHook =
        chain -> {
          if (!overrideActive || customTypeface == null) return chain.proceed();
          Object[] args = chain.getArgs().toArray();
          boolean changed = false;
          if (args.length > 0 && args[0] instanceof Typeface) {
            args[0] = customTypeface;
            changed = true;
          }
          if (chain.getThisObject() instanceof TextView) {
            TextView tv = (TextView) chain.getThisObject();
            tv.setIncludeFontPadding(false);
            if (tv instanceof EditText
                || (cfg.res.idChatMessageText != 0 && tv.getId() == cfg.res.idChatMessageText)) {
              tv.setPadding(tv.getPaddingLeft(), 0, tv.getPaddingRight(), 0);
            }
          }
          return changed ? chain.proceed(args) : chain.proceed();
        };

    try {
      Knot.module
          .hook(Reflect.findMethodExact(TextView.class, "setTypeface", Typeface.class, int.class))
          .intercept(textViewHook);
      Knot.module
          .hook(Reflect.findMethodExact(TextView.class, "setTypeface", Typeface.class))
          .intercept(textViewHook);
      XposedInterface.Hooker constructHook =
          chain -> {
            Object result = chain.proceed();
            if (overrideActive
                && customTypeface != null
                && chain.getThisObject() instanceof TextView) {
              ((TextView) chain.getThisObject()).setIncludeFontPadding(false);
            }
            return result;
          };
      Knot.module
          .hook(
              Reflect.findConstructorExact(
                  TextView.class, android.content.Context.class, android.util.AttributeSet.class))
          .intercept(constructHook);
      Knot.module
          .hook(
              Reflect.findConstructorExact(
                  TextView.class,
                  android.content.Context.class,
                  android.util.AttributeSet.class,
                  int.class))
          .intercept(constructHook);
    } catch (Throwable ignored) {
    }

    XposedInterface.Hooker metricsHook =
        chain -> {
          Object result = chain.proceed();
          if (!overrideActive || customTypeface == null) return result;
          Paint paint = (Paint) chain.getThisObject();
          float textSize = paint.getTextSize();
          if (textSize <= 0) return result;
          List<Object> args = chain.getArgs();
          Object last = args.isEmpty() ? null : args.get(args.size() - 1);
          Object m =
              (last instanceof Paint.FontMetricsInt || last instanceof Paint.FontMetrics)
                  ? last
                  : result;
          if (m instanceof Paint.FontMetricsInt) {
            Paint.FontMetricsInt fmi = (Paint.FontMetricsInt) m;
            fmi.ascent = Math.round(-textSize * 0.95f);
            fmi.descent = Math.round(textSize * 0.20f);
            fmi.top = fmi.ascent;
            fmi.bottom = fmi.descent;
            fmi.leading = 0;
          } else if (m instanceof Paint.FontMetrics) {
            Paint.FontMetrics fm = (Paint.FontMetrics) m;
            fm.ascent = -textSize * 0.95f;
            fm.descent = textSize * 0.20f;
            fm.top = fm.ascent;
            fm.bottom = fm.descent;
            fm.leading = 0;
          } else if ("getFontSpacing".equals(chain.getExecutable().getName())) {
            return textSize * 1.15f;
          }
          return result;
        };

    try {
      Knot.module
          .hook(
              Reflect.findMethodExact(Paint.class, "getFontMetricsInt", Paint.FontMetricsInt.class))
          .intercept(metricsHook);
      Knot.module
          .hook(Reflect.findMethodExact(Paint.class, "getFontMetricsInt"))
          .intercept(metricsHook);
      Knot.module
          .hook(Reflect.findMethodExact(Paint.class, "getFontMetrics", Paint.FontMetrics.class))
          .intercept(metricsHook);
      Knot.module
          .hook(Reflect.findMethodExact(Paint.class, "getFontMetrics"))
          .intercept(metricsHook);
      Knot.module
          .hook(Reflect.findMethodExact(Paint.class, "getFontSpacing"))
          .intercept(metricsHook);
      try {
        Knot.module
            .hook(
                Reflect.findMethodExact(
                    Paint.class,
                    "getFontMetricsInt",
                    CharSequence.class,
                    int.class,
                    int.class,
                    int.class,
                    int.class,
                    boolean.class,
                    Paint.FontMetricsInt.class))
            .intercept(metricsHook);
      } catch (Throwable ignored) {
      }
    } catch (Throwable ignored) {
    }

    try {
      Knot.module
          .hook(Reflect.findMethodExact(TextView.class, "onMeasure", int.class, int.class))
          .intercept(
              chain -> {
                if (overrideActive && customTypeface != null) {
                  TextView tv = (TextView) chain.getThisObject();
                  tv.setIncludeFontPadding(false);
                  if (tv instanceof EditText)
                    tv.setPadding(tv.getPaddingLeft(), 0, tv.getPaddingRight(), 0);
                  try {
                    Reflect.callMethod(tv, "setFallbackLineSpacing", false);
                  } catch (Throwable ignored) {
                  }
                }
                return chain.proceed();
              });
    } catch (Throwable ignored) {
    }

    try {
      Knot.module
          .hook(Reflect.findMethodExact(Paint.class, "setElegantTextHeight", boolean.class))
          .intercept(
              chain -> {
                if (overrideActive && customTypeface != null) {
                  return chain.proceed(new Object[] {false});
                }
                return chain.proceed();
              });
    } catch (Throwable ignored) {
    }

    Knot.module
        .hook(
            Reflect.findMethodExact(
                cfg.font.fontConfigClass,
                lpparam.classLoader,
                cfg.font.methodGetFontConfig,
                android.content.Context.class,
                java.util.List.class,
                int.class,
                boolean.class,
                int.class,
                android.os.Handler.class,
                cfg.font.fontCallbackClass))
        .intercept(
            chain -> {
              Object callback = chain.getArg(6);
              if (callback != null)
                try {
                  Reflect.callMethod(callback, cfg.font.methodOnFontChanged, customTypeface);
                } catch (Throwable ignored) {
                }
              return customTypeface;
            });

    try {
      Knot.module
          .hook(
              Reflect.findMethodExact(
                  cfg.font.fontCallbackClass,
                  lpparam.classLoader,
                  cfg.font.methodOnFontChanged,
                  Typeface.class))
          .intercept(chain -> chain.proceed(new Object[] {customTypeface}));
    } catch (Throwable ignored) {
    }

    Knot.module
        .hook(
            Reflect.findMethodExact(
                cfg.font.fontManagerClass,
                lpparam.classLoader,
                "c",
                android.content.Context.class,
                java.util.List.class,
                int.class,
                cfg.font.fontRequestExecutorClass,
                cfg.font.fontCallbackWithHandlerClass))
        .intercept(chain -> customTypeface);

    Knot.module
        .hook(
            Reflect.findMethodExact(
                cfg.font.fontManagerClass,
                lpparam.classLoader,
                cfg.font.methodInitializeFont,
                String.class,
                android.content.Context.class,
                java.util.List.class,
                int.class))
        .intercept(
            chain -> {
              Object res = chain.proceed();
              if (res != null) Reflect.setObjectField(res, cfg.font.fieldTypeface, customTypeface);
              return res;
            });
  }

  private void initTypeface() {
    if (!SettingsStore.get("use_custom_font", false)) {
      overrideActive = false;
      return;
    }
    String path = SettingsStore.getString("custom_font_path", "");
    if (!path.isEmpty()) {
      File f = new File(path);
      if (f.exists()) {
        try {
          customTypeface = Typeface.createFromFile(f);
          if (customTypeface != null) {
            overrideActive = true;
            Knot.log("Knot: Custom font loaded: " + path);
            return;
          }
        } catch (Throwable t) {
          Knot.log("Knot: Failed to load font: " + t.getMessage());
        }
      }
    }
    overrideActive = false;
  }
}
