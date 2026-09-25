package app.zipper.knot.ui.settings;

import static app.zipper.knot.ui.settings.SettingsViews.dp;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import app.zipper.knot.Knot;
import app.zipper.knot.R;
import app.zipper.knot.utils.AacWriter;
import app.zipper.knot.utils.AudioAnalysis;
import app.zipper.knot.utils.CompressedAudio;
import app.zipper.knot.utils.LineTheme;
import app.zipper.knot.utils.ModuleResources;
import app.zipper.knot.utils.PcmPlayer;
import java.io.File;
import java.io.InterruptedIOException;
import java.util.Locale;

final class RingtoneEditor {

  interface Saver {
    boolean save(File encoded, String title);
  }

  // LINE's built-in ringtone (raw/original) measures -23.5 dBFS by AudioAnalysis.
  private static final double TARGET_LOUDNESS_DB = -23.5;
  private static final long TICK_MS = 30;
  private static final int GUTTER_DP = 24;
  private static final int ART_DP = 136;
  private static final int COMPACT_HEIGHT_DP = 560;

  private static volatile RingtoneEditor active = null;

  private final Activity host;
  private final Uri uri;
  private final Saver saver;
  private final Dialog dialog;
  private final Handler uiHandler = new Handler(Looper.getMainLooper());
  private final RingtoneRangeView range;
  private final ProgressBar progress;
  private final TextView status;
  private final TextView title;
  private final TextView artist;
  private final ImageView art;
  private final TextView artPlaceholder;
  private final TextView startLabel;
  private final TextView endLabel;
  private final PlayButton playButton;
  private final TextView saveButton;
  private View header;
  private View artFrame;
  private View waveform;
  private View labels;
  private CompressedAudio source;
  private AudioAnalysis audio;
  private PcmPlayer preview;
  private Thread saveThread;

  private final Runnable tick =
      new Runnable() {
        @Override
        public void run() {
          if (preview == null) return;
          if (preview.isFinished()) {
            stopPreview();
            return;
          }
          range.setPlayhead(preview.positionUs());
          uiHandler.postDelayed(this, TICK_MS);
        }
      };

  static void show(Activity host, Uri uri, String name, Saver saver) {
    new RingtoneEditor(host, uri, saver).open(name);
  }

  static void onActivityResumed(Activity activity) {
    RingtoneEditor current = active;
    if (current != null && current.host != activity) current.dismissNow();
  }

  static void onActivityDestroyed(Activity activity) {
    RingtoneEditor current = active;
    if (current != null && current.host == activity) current.dismissNow();
  }

  private RingtoneEditor(Activity host, Uri uri, Saver saver) {
    this.host = host;
    this.uri = uri;
    this.saver = saver;
    dialog = new Dialog(host, android.R.style.Theme_DeviceDefault_NoActionBar);
    range = new RingtoneRangeView(host);
    progress = new ProgressBar(host);
    status = text("", 14, false, LineTheme.secondaryTextColor(host));
    title = text("", 20, true, LineTheme.primaryTextColor(host));
    artist = text("", 17, false, LineTheme.secondaryTextColor(host));
    art = new ImageView(host);
    artPlaceholder = text("♪", 44, false, LineTheme.secondaryTextColor(host));
    startLabel = text("", 14, false, LineTheme.secondaryTextColor(host));
    endLabel = text("", 14, false, LineTheme.secondaryTextColor(host));
    playButton = new PlayButton(host);
    saveButton = text(ModuleResources.get(R.string.ringtone_editor_save), 16, true, Color.WHITE);
    dialog.setContentView(buildContent());
  }

  private void open(String name) {
    active = this;
    title.setText(name);
    Thread audioLoader = new Thread(this::loadAudio);
    dialog.setOnDismissListener(
        d -> {
          if (active == this) active = null;
          stopPreview();
          audioLoader.interrupt();
          if (saveThread != null) saveThread.interrupt();
        });
    SettingsViews.applyFullScreenWindow(dialog, host);
    dialog.show();
    new Thread(this::loadTrackInfo).start();
    audioLoader.start();
  }

  private void dismissNow() {
    try {
      if (dialog.isShowing()) dialog.dismiss();
    } catch (Throwable ignored) {
    }
    if (active == this) active = null;
  }

  private View buildContent() {
    LinearLayout root =
        new LinearLayout(host) {
          @Override
          protected void onConfigurationChanged(Configuration config) {
            super.onConfigurationChanged(config);
            applyLayout(config);
          }
        };
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(LineTheme.backgroundColor(host));
    SettingsViews.padForSystemBars(root);

    header = buildHeader();
    root.addView(header);
    root.addView(new View(host), flexible());

    title.setMaxLines(2);
    title.setEllipsize(TextUtils.TruncateAt.END);
    root.addView(title, block(0));
    artist.setSingleLine();
    artist.setEllipsize(TextUtils.TruncateAt.END);
    artist.setVisibility(View.GONE);
    root.addView(artist, block(dp(host, 6)));
    artFrame = buildArt();
    LinearLayout.LayoutParams artParams =
        new LinearLayout.LayoutParams(dp(host, ART_DP), dp(host, ART_DP));
    artParams.gravity = Gravity.CENTER_HORIZONTAL;
    artParams.topMargin = dp(host, 28);
    root.addView(artFrame, artParams);

    waveform = buildWaveform();
    root.addView(waveform);

    LinearLayout labelRow = new LinearLayout(host);
    startLabel.setGravity(Gravity.START);
    endLabel.setGravity(Gravity.END);
    labelRow.addView(
        startLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    labelRow.addView(
        endLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    labels = labelRow;
    root.addView(labels, block(0));

    playButton.setEnabled(false);
    playButton.setOnClickListener(v -> togglePreview());
    LinearLayout.LayoutParams playParams = new LinearLayout.LayoutParams(0, 0);
    playParams.gravity = Gravity.CENTER_HORIZONTAL;
    root.addView(playButton, playParams);

    root.addView(new View(host), flexible());

    GradientDrawable saveBackground = new GradientDrawable();
    saveBackground.setColor(LineTheme.accentGreen(host));
    saveBackground.setCornerRadius(dp(host, 4));
    saveButton.setBackground(saveBackground);
    saveButton.setOnClickListener(v -> save());
    setSaveEnabled(false);
    root.addView(saveButton, block(0));

    applyLayout(host.getResources().getConfiguration());
    return root;
  }

  // Follows LINE's own editor, which drops the artwork on short screens such as landscape.
  private void applyLayout(Configuration config) {
    boolean compact = config.screenHeightDp < COMPACT_HEIGHT_DP;
    artFrame.setVisibility(compact ? View.GONE : View.VISIBLE);
    resize(header, ViewGroup.LayoutParams.MATCH_PARENT, dp(host, compact ? 44 : 56), 0, 0);
    resize(
        waveform,
        ViewGroup.LayoutParams.MATCH_PARENT,
        dp(host, compact ? 80 : 140),
        dp(host, compact ? 8 : 48),
        0);
    resize(
        labels,
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        dp(host, compact ? 4 : 8),
        0);
    int play = dp(host, compact ? 40 : 72);
    resize(playButton, play, play, dp(host, compact ? 0 : 24), 0);
    resize(
        saveButton,
        ViewGroup.LayoutParams.MATCH_PARENT,
        dp(host, compact ? 44 : 56),
        0,
        dp(host, compact ? 24 : 32));
  }

  private static void resize(View view, int width, int height, int topMargin, int bottomMargin) {
    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
    params.width = width;
    params.height = height;
    params.topMargin = topMargin;
    params.bottomMargin = bottomMargin;
    view.setLayoutParams(params);
  }

  private View buildHeader() {
    FrameLayout header = new FrameLayout(host);
    TextView heading =
        text(
            ModuleResources.get(R.string.ringtone_editor_title),
            17,
            true,
            LineTheme.primaryTextColor(host));
    header.addView(heading, frame(Gravity.CENTER));
    CloseButton close = new CloseButton(host);
    close.setOnClickListener(v -> dialog.dismiss());
    header.addView(
        close,
        new FrameLayout.LayoutParams(
            dp(host, 56), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END));
    return header;
  }

  private View buildArt() {
    FrameLayout frame = new FrameLayout(host);
    GradientDrawable tile = new GradientDrawable();
    tile.setColor(LineTheme.cardColor(host));
    tile.setCornerRadius(dp(host, 8));
    frame.setBackground(tile);
    frame.setClipToOutline(true);
    frame.addView(artPlaceholder, frame(Gravity.CENTER));
    art.setScaleType(ImageView.ScaleType.CENTER_CROP);
    frame.addView(
        art,
        new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    return frame;
  }

  private View buildWaveform() {
    FrameLayout frame = new FrameLayout(host);
    frame.addView(
        range,
        new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    progress.setIndeterminateTintList(ColorStateList.valueOf(LineTheme.accentGreen(host)));
    frame.addView(progress, frame(Gravity.CENTER));
    status.setVisibility(View.GONE);
    status.setPadding(dp(host, GUTTER_DP), 0, dp(host, GUTTER_DP), 0);
    frame.addView(status, frame(Gravity.CENTER));
    range.setListener(
        (startUs, endUs) -> {
          stopPreview();
          startLabel.setText(time(startUs));
          endLabel.setText(time(endUs));
        });
    return frame;
  }

  private void loadTrackInfo() {
    TrackInfo info = TrackInfo.read(host, uri, dp(host, ART_DP));
    uiHandler.post(() -> showTrackInfo(info));
  }

  private void loadAudio() {
    try {
      CompressedAudio compressed = CompressedAudio.read(host, uri);
      AudioAnalysis analysis = AudioAnalysis.analyze(compressed);
      uiHandler.post(() -> onAudioLoaded(compressed, analysis));
    } catch (Throwable t) {
      if (!Thread.currentThread().isInterrupted()) {
        Knot.log("Knot: Failed to analyze ringtone audio: " + t);
      }
      uiHandler.post(() -> onAudioLoaded(null, null));
    }
  }

  private void showTrackInfo(TrackInfo info) {
    if (!TextUtils.isEmpty(info.title)) title.setText(info.title);
    if (!TextUtils.isEmpty(info.artist)) {
      artist.setText(info.artist);
      artist.setVisibility(View.VISIBLE);
    }
    if (info.art != null) {
      art.setImageBitmap(info.art);
      artPlaceholder.setVisibility(View.GONE);
    }
  }

  private void onAudioLoaded(CompressedAudio compressed, AudioAnalysis analysis) {
    if (!dialog.isShowing()) return;
    progress.setVisibility(View.GONE);
    if (analysis == null) {
      status.setText(ModuleResources.get(R.string.ringtone_editor_unsupported));
      status.setVisibility(View.VISIBLE);
      return;
    }
    source = compressed;
    audio = analysis;
    range.setAudio(analysis);
    playButton.setEnabled(true);
    setSaveEnabled(true);
  }

  private double levelingGain() {
    return audio.levelingGain(range.startUs(), range.endUs(), TARGET_LOUDNESS_DB);
  }

  private void togglePreview() {
    if (preview != null) {
      stopPreview();
      return;
    }
    preview = new PcmPlayer(source, range.startUs(), range.endUs(), levelingGain());
    playButton.setPlaying(true);
    uiHandler.post(tick);
  }

  private void stopPreview() {
    uiHandler.removeCallbacks(tick);
    if (preview != null) {
      preview.stop();
      preview = null;
    }
    range.setPlayhead(-1);
    playButton.setPlaying(false);
  }

  private void save() {
    stopPreview();
    setSaveEnabled(false);
    range.setEnabled(false);
    saveButton.setText(ModuleResources.get(R.string.ringtone_editor_saving));
    long startUs = range.startUs();
    long endUs = range.endUs();
    double gain = levelingGain();
    String name = title.getText().toString();
    saveThread =
        new Thread(
            () -> {
              boolean saved = encodeAndSave(startUs, endUs, gain, name);
              uiHandler.post(() -> onSaved(saved));
            });
    saveThread.start();
  }

  private boolean encodeAndSave(long startUs, long endUs, double gain, String name) {
    File encoded = null;
    try {
      encoded = File.createTempFile("knot_ringtone_", ".m4a", host.getCacheDir());
      AacWriter.write(source, startUs, endUs, gain, encoded);
      return saver.save(encoded, name);
    } catch (InterruptedIOException e) {
      return false;
    } catch (Throwable t) {
      Knot.log("Knot: Failed to encode ringtone: " + t);
      return false;
    } finally {
      if (encoded != null) encoded.delete();
    }
  }

  private void onSaved(boolean saved) {
    if (!dialog.isShowing()) return;
    if (saved) {
      dialog.dismiss();
      return;
    }
    saveButton.setText(ModuleResources.get(R.string.ringtone_editor_save));
    setSaveEnabled(true);
    range.setEnabled(true);
    Toast.makeText(
            host, ModuleResources.get(R.string.ringtone_editor_unsupported), Toast.LENGTH_SHORT)
        .show();
  }

  private void setSaveEnabled(boolean enabled) {
    saveButton.setEnabled(enabled);
    saveButton.setAlpha(enabled ? 1f : 0.4f);
  }

  private TextView text(String value, int sizeSp, boolean bold, int color) {
    TextView view = new TextView(host);
    view.setText(value);
    view.setTextSize(sizeSp);
    view.setTextColor(color);
    view.setGravity(Gravity.CENTER);
    if (bold) view.setTypeface(Typeface.DEFAULT_BOLD);
    return view;
  }

  private LinearLayout.LayoutParams block(int topMargin) {
    LinearLayout.LayoutParams params =
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.leftMargin = dp(host, GUTTER_DP);
    params.rightMargin = dp(host, GUTTER_DP);
    params.topMargin = topMargin;
    return params;
  }

  private static LinearLayout.LayoutParams flexible() {
    return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
  }

  private static FrameLayout.LayoutParams frame(int gravity) {
    return new FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, gravity);
  }

  private static String time(long us) {
    long seconds = us / 1_000_000;
    return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
  }

  private static final class TrackInfo {
    final String title;
    final String artist;
    final Bitmap art;

    private TrackInfo(String title, String artist, Bitmap art) {
      this.title = title;
      this.artist = artist;
      this.art = art;
    }

    static TrackInfo read(Context ctx, Uri uri, int artSize) {
      MediaMetadataRetriever retriever = new MediaMetadataRetriever();
      try {
        retriever.setDataSource(ctx, uri);
        byte[] picture = retriever.getEmbeddedPicture();
        return new TrackInfo(
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
            picture == null ? null : decodeArt(picture, artSize));
      } catch (Throwable t) {
        Knot.log("Knot: Failed to read ringtone tags: " + t);
        return new TrackInfo(null, null, null);
      } finally {
        try {
          retriever.release();
        } catch (Throwable ignored) {
        }
      }
    }

    private static Bitmap decodeArt(byte[] picture, int size) {
      BitmapFactory.Options bounds = new BitmapFactory.Options();
      bounds.inJustDecodeBounds = true;
      BitmapFactory.decodeByteArray(picture, 0, picture.length, bounds);
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inSampleSize = 1;
      int longest = Math.max(bounds.outWidth, bounds.outHeight);
      while (longest / (options.inSampleSize * 2) >= size) options.inSampleSize *= 2;
      return BitmapFactory.decodeByteArray(picture, 0, picture.length, options);
    }
  }

  private static final class CloseButton extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float arm;

    CloseButton(Context ctx) {
      super(ctx);
      paint.setColor(LineTheme.primaryTextColor(ctx));
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeCap(Paint.Cap.ROUND);
      paint.setStrokeWidth(dp(ctx, 2));
      arm = dp(ctx, 7);
      setContentDescription(ModuleResources.get(R.string.common_close));
    }

    @Override
    protected void onDraw(Canvas canvas) {
      float cx = getWidth() / 2f;
      float cy = getHeight() / 2f;
      canvas.drawLine(cx - arm, cy - arm, cx + arm, cy + arm, paint);
      canvas.drawLine(cx - arm, cy + arm, cx + arm, cy - arm, paint);
    }
  }

  private static final class PlayButton extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path triangle = new Path();
    private boolean playing;

    PlayButton(Context ctx) {
      super(ctx);
      paint.setColor(LineTheme.primaryTextColor(ctx));
    }

    void setPlaying(boolean playing) {
      this.playing = playing;
      invalidate();
    }

    @Override
    public void setEnabled(boolean enabled) {
      super.setEnabled(enabled);
      setAlpha(enabled ? 1f : 0.4f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
      float size = Math.min(getWidth(), getHeight()) * 0.45f;
      float cx = getWidth() / 2f;
      float cy = getHeight() / 2f;
      if (playing) {
        float half = size * 0.4f;
        canvas.drawRect(cx - half, cy - half, cx + half, cy + half, paint);
        return;
      }
      triangle.reset();
      triangle.moveTo(cx - size * 0.35f, cy - size / 2);
      triangle.lineTo(cx + size * 0.55f, cy);
      triangle.lineTo(cx - size * 0.35f, cy + size / 2);
      triangle.close();
      canvas.drawPath(triangle, paint);
    }
  }
}
