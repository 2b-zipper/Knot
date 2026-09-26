package app.zipper.knot.ui.settings;

import static app.zipper.knot.ui.settings.SettingsViews.dp;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import app.zipper.knot.utils.AudioAnalysis;
import app.zipper.knot.utils.LineTheme;

final class RingtoneRangeView extends View {

  interface Listener {
    void onRangeChanged(long startUs, long endUs);
  }

  private static final long INITIAL_SPAN_US = 30_000_000;
  private static final long MIN_SPAN_US = 1_000_000;
  private static final long MIN_VISIBLE_US = 40_000_000;
  private static final double FIT_RATIO = 0.75;
  private static final long FIT_ANIM_MS = 250;
  private static final long EDGE_SCROLL_US_PER_FRAME = 150_000;

  private enum Drag {
    NONE,
    START,
    END,
    MOVE,
    SCROLL
  }

  private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint selectedBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint shadePaint = new Paint();
  private final Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint knobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint knobRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint baselinePaint = new Paint();
  private final float barWidth;
  private final float barGap;
  private final float knobRadius;
  private final float touchRadius;
  private final float edgeZone;

  private AudioAnalysis audio;
  private long visibleSpanUs;
  private long scrollUs;
  private long startUs;
  private long endUs;
  private long playheadUs = -1;
  private Drag drag = Drag.NONE;
  private float lastX;
  private long grabOffsetUs;
  private ValueAnimator fitAnimator;
  private Listener listener;

  private final Runnable edgeScroll =
      new Runnable() {
        @Override
        public void run() {
          if (!draggingSelection()) return;
          float push = 0;
          if (lastX < edgeZone) push = lastX - edgeZone;
          if (lastX > getWidth() - edgeZone) push = lastX - (getWidth() - edgeZone);
          if (push != 0) {
            float strength = Math.max(-1, Math.min(1, push / edgeZone));
            scrollTo(scrollUs + (long) (strength * EDGE_SCROLL_US_PER_FRAME));
            dragAt(lastX);
          }
          postOnAnimation(this);
        }
      };

  RingtoneRangeView(Context ctx) {
    super(ctx);
    int text = LineTheme.primaryTextColor(ctx);
    int accent = LineTheme.accentGreen(ctx);
    barPaint.setColor(withAlpha(text, 0x55));
    selectedBarPaint.setColor(withAlpha(text, 0xCC));
    shadePaint.setColor(withAlpha(text, 0x14));
    baselinePaint.setColor(text);
    baselinePaint.setStrokeWidth(dp(ctx, 1.5f));
    accentPaint.setColor(accent);
    accentPaint.setStrokeWidth(dp(ctx, 2));
    knobPaint.setColor(LineTheme.backgroundColor(ctx));
    knobRingPaint.setColor(accent);
    knobRingPaint.setStyle(Paint.Style.STROKE);
    knobRingPaint.setStrokeWidth(dp(ctx, 3));
    barWidth = dp(ctx, 2);
    barGap = dp(ctx, 2.5f);
    knobRadius = dp(ctx, 9);
    touchRadius = dp(ctx, 24);
    edgeZone = dp(ctx, 48);
  }

  void setListener(Listener listener) {
    this.listener = listener;
  }

  void setAudio(AudioAnalysis audio) {
    this.audio = audio;
    scrollUs = 0;
    startUs = 0;
    endUs = Math.min(INITIAL_SPAN_US, audio.durationUs);
    visibleSpanUs = fittedSpan();
    changed();
  }

  void setPlayhead(long us) {
    playheadUs = us;
    invalidate();
  }

  long startUs() {
    return startUs;
  }

  long endUs() {
    return endUs;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    if (audio == null) return;
    float baseline = getHeight() - knobRadius - knobRingPaint.getStrokeWidth();
    float left = xOf(startUs);
    float right = xOf(endUs);

    canvas.drawRect(left, 0, right, baseline, shadePaint);
    drawBars(canvas, baseline, left, right);
    canvas.drawLine(0, baseline, getWidth(), baseline, baselinePaint);
    if (playheadUs >= 0) {
      canvas.drawLine(xOf(playheadUs), 0, xOf(playheadUs), baseline, accentPaint);
    }
    drawHandle(canvas, left, baseline);
    drawHandle(canvas, right, baseline);
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    if (enabled) return;
    drag = Drag.NONE;
    removeCallbacks(edgeScroll);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (audio == null || !isEnabled()) return false;
    float x = event.getX();
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        if (fitAnimator != null && fitAnimator.isRunning()) fitAnimator.end();
        drag = pick(x);
        lastX = x;
        grabOffsetUs = timeAt(x) - startUs;
        if (draggingSelection()) postOnAnimation(edgeScroll);
        getParent().requestDisallowInterceptTouchEvent(true);
        return true;
      case MotionEvent.ACTION_MOVE:
        if (drag == Drag.SCROLL) {
          scrollTo(scrollUs - (long) ((x - lastX) * usPerPx()));
        } else {
          dragAt(x);
        }
        lastX = x;
        return true;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        if (draggingSelection()) fitSelection();
        drag = Drag.NONE;
        removeCallbacks(edgeScroll);
        return true;
      default:
        return false;
    }
  }

  private Drag pick(float x) {
    float startDistance = Math.abs(x - xOf(startUs));
    float endDistance = Math.abs(x - xOf(endUs));
    if (Math.min(startDistance, endDistance) <= touchRadius) {
      return startDistance < endDistance ? Drag.START : Drag.END;
    }
    return x > xOf(startUs) && x < xOf(endUs) ? Drag.MOVE : Drag.SCROLL;
  }

  private boolean draggingSelection() {
    return drag == Drag.START || drag == Drag.END || drag == Drag.MOVE;
  }

  private void dragAt(float x) {
    long us = timeAt(x);
    long minSpan = Math.min(MIN_SPAN_US, audio.durationUs);
    switch (drag) {
      case START:
        startUs = clamp(us, 0, endUs - minSpan);
        break;
      case END:
        endUs = clamp(us, startUs + minSpan, audio.durationUs);
        break;
      case MOVE:
        long span = endUs - startUs;
        startUs = clamp(us - grabOffsetUs, 0, audio.durationUs - span);
        endUs = startUs + span;
        break;
      default:
        return;
    }
    changed();
  }

  private long fittedSpan() {
    long wanted = Math.max(MIN_VISIBLE_US, (long) ((endUs - startUs) / FIT_RATIO));
    return Math.max(1, Math.min(wanted, audio.durationUs));
  }

  // Runs after every edit so the whole selection stays on screen with room around it to grab.
  private void fitSelection() {
    long fromSpan = visibleSpanUs;
    long fromScroll = scrollUs;
    long toSpan = fittedSpan();
    boolean fits = startUs >= fromScroll && endUs <= fromScroll + toSpan;
    long centered = startUs - (toSpan - (endUs - startUs)) / 2;
    long toScroll = clamp(fits ? fromScroll : centered, 0, audio.durationUs - toSpan);
    if (toSpan == fromSpan && toScroll == fromScroll) return;

    fitAnimator = ValueAnimator.ofFloat(0, 1);
    fitAnimator.setDuration(FIT_ANIM_MS);
    fitAnimator.setInterpolator(new DecelerateInterpolator());
    fitAnimator.addUpdateListener(
        animation -> {
          float t = animation.getAnimatedFraction();
          visibleSpanUs = fromSpan + (long) ((toSpan - fromSpan) * t);
          scrollUs = fromScroll + (long) ((toScroll - fromScroll) * t);
          invalidate();
        });
    fitAnimator.start();
  }

  private long timeAt(float x) {
    return scrollUs + (long) ((x - knobRadius) * usPerPx());
  }

  private void scrollTo(long us) {
    scrollUs = clamp(us, 0, maxScroll());
    invalidate();
  }

  private long maxScroll() {
    return Math.max(0, audio.durationUs - visibleSpanUs);
  }

  private void changed() {
    invalidate();
    if (listener != null) listener.onRangeChanged(startUs, endUs);
  }

  private void drawBars(Canvas canvas, float baseline, float left, float right) {
    float waveHeight = baseline - knobRadius;
    double barUs = (barWidth + barGap) * usPerPx();
    long first = (long) Math.floor(Math.max(0, scrollUs - knobRadius * usPerPx()) / barUs);
    for (long bar = first; ; bar++) {
      long fromUs = (long) (bar * barUs);
      float x = xOf(fromUs);
      if (fromUs >= audio.durationUs || x > getWidth()) break;
      long toUs = (long) ((bar + 1) * barUs);
      float top = baseline - Math.max(barWidth, audio.displayLevel(fromUs, toUs) * waveHeight);
      float end = x + barWidth;
      float selectedFrom = Math.max(x, left);
      float selectedTo = Math.min(end, right);
      if (selectedFrom >= selectedTo) {
        canvas.drawRect(x, top, end, baseline, barPaint);
        continue;
      }
      if (x < selectedFrom) canvas.drawRect(x, top, selectedFrom, baseline, barPaint);
      canvas.drawRect(selectedFrom, top, selectedTo, baseline, selectedBarPaint);
      if (selectedTo < end) canvas.drawRect(selectedTo, top, end, baseline, barPaint);
    }
  }

  private void drawHandle(Canvas canvas, float x, float baseline) {
    canvas.drawLine(x, 0, x, baseline, accentPaint);
    canvas.drawCircle(x, baseline, knobRadius, knobPaint);
    canvas.drawCircle(x, baseline, knobRadius, knobRingPaint);
  }

  private double usPerPx() {
    return visibleSpanUs / (double) Math.max(1, getWidth() - 2 * knobRadius);
  }

  private float xOf(long us) {
    return (float) (knobRadius + (us - scrollUs) / usPerPx());
  }

  private static long clamp(long value, long min, long max) {
    return Math.max(min, Math.min(max, value));
  }

  private static int withAlpha(int color, int alpha) {
    return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
  }
}
