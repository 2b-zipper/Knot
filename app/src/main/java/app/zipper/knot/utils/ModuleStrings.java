package app.zipper.knot.utils;

public class ModuleStrings {

  public static final String SETTINGS_TITLE = "モジュール設定";
  public static final String SETTINGS_RESET = "設定のリセット";
  public static final String SETTINGS_RESET_CONFIRM = "すべての設定をデフォルトに戻しますか？";
  public static final String SETTINGS_RESET_OK = "リセット";
  public static final String SETTINGS_YES = "はい";
  public static final String SETTINGS_CANCEL = "キャンセル";
  public static final String COMMON_CLOSE = "閉じる";
  public static final String SETTINGS_PATH_PICKER_HINT = "保存先を選択してください";
  public static final String SETTINGS_SEARCH_HINT = "設定を検索...";

  public static final String CAT_PRIVACY = "プライバシー";
  public static final String CAT_CHAT = "チャット";
  public static final String CAT_DISPLAY = "画面表示";
  public static final String CAT_NOTIFICATION = "通知";
  public static final String CAT_STORAGE = "保存先";
  public static final String CAT_BACKUP = "バックアップ";
  public static final String CAT_SYSTEM = "システム";
  public static final String CAT_OTHER = "その他";

  public static final String SEC_PRIVACY_READ = "既読";
  public static final String SEC_PRIVACY_UNSEND = "送信取り消し";
  public static final String SEC_PRIVACY_PROFILE = "プロフィール";

  public static final String SEC_CHAT_MEDIA = "メディア";
  public static final String SEC_CHAT_SEARCH = "検索";
  public static final String SEC_CHAT_DISPLAY = "表示";

  public static final String SEC_ADS = "広告・おすすめ";
  public static final String SEC_TABS = "タブ";
  public static final String SEC_HEADER_BTN = "ヘッダボタン";

  public static final String SEC_FONT = "フォント";
  public static final String SEC_THEME = "テーマ";

  public static final String MANAGER_RESTART_REQUIRED = "LINEの再起動が必要です";

  public static final String RESTART_TITLE = "再起動の確認";
  public static final String RESTART_MESSAGE = "設定を反映させるにはLINEの再起動が必要です。今すぐ再起動しますか？";
  public static final String RESTART_OK = "再起動";
  public static final String RESTART_LATER = "後で";

  public static final String WARN_STORAGE_UNSET = "⚠ Knot: 保存先が未設定です。タップして設定してください。";
  public static final String UNSUPPORTED_VERSION_TITLE = "Knot: Unsupported Version";
  public static final String UNSUPPORTED_VERSION_MSG = "このバージョンのLINEはサポートされていません。";

  public static final String BACKUP_SUCCESS = "バックアップが完了しました。";
  public static final String BACKUP_ERROR = "バックアップに失敗しました。";
  public static final String RESTORE_SUCCESS = "復元が完了しました。";
  public static final String RESTORE_ERROR = "復元の実行中にエラーが発生しました。";
  public static final String RESTORE_CONFIRM_TITLE = "復元の確認";
  public static final String RESTORE_CONFIRM_MSG =
      "バックアップからトーク履歴を復元しますか？現在のデータはすべて上書きされ、完了後にアプリが再起動されます。";
  public static final String RESTORE_PROCESSING = "復元処理中...";
  public static final String RESTORE_PREPARING = "準備中...";

  public static final String LABEL_PREVENT_READ = "既読回避";
  public static final String LABEL_SEND_MARK_READ = "送信後既読";
  public static final String READ_RECEIPT_VIEWER = "既読者確認";
  public static final String UNSET_TIME_PREFIX = "取消日時: ";

  public static final String READ_HISTORY_TITLE = "既読履歴";
  public static final String READ_HISTORY_EMPTY = "履歴はありません";
  public static final String READ_HISTORY_DELETE = "履歴を削除";
  public static final String READ_HISTORY_DELETE_CONFIRM_TITLE = "履歴の削除";
  public static final String READ_HISTORY_DELETE_CONFIRM_MSG = "このチャットの既読履歴をすべて削除しますか？";
  public static final String READ_HISTORY_UNKNOWN_MSG = "(不明なメッセージ)";
  public static final String READ_HISTORY_SHOW_ALL = "残り%d人を表示 ▼";
  public static final String READ_HISTORY_COLLAPSE = "折りたたむ ▲";

  public static final String MSG_STICKER = "[スタンプ]";
  public static final String MSG_IMAGE = "[画像]";
  public static final String MSG_VIDEO = "[動画]";
  public static final String MSG_FILE = "[ファイル]";
  public static final String MSG_LOCATION = "[位置情報]";

  public static final String OPT_PREVENT_MARK_AS_READ_LABEL = "プラスメニューに「既読回避」を追加";
  public static final String OPT_PREVENT_MARK_AS_READ_DESC =
      "トークタブ右上の「+」メニューから既読回避をON/OFFできるようになります。ONにすると「送信後既読」オプションも表示されます。";
  public static final String OPT_RECORD_READ_HISTORY_LABEL = "既読履歴を記録";
  public static final String OPT_RECORD_READ_HISTORY_DESC =
      "誰がいつメッセージを既読にしたかを記録します。チャット画面上部の本アイコンから確認できます。";

  public static final String OPT_PREVENT_UNSEND_MESSAGE_LABEL = "送信取り消し無効化";
  public static final String OPT_PREVENT_UNSEND_MESSAGE_DESC =
      "相手がメッセージの送信を取り消しても、自分の端末には残るようにします。取り消されたメッセージにはアイコンが表示され、タップすることで取消日時を確認できます。";

  public static final String OPT_SHOW_PROFILE_TIMESTAMPS_LABEL = "プロフィールに詳細情報を表示";
  public static final String OPT_SHOW_PROFILE_TIMESTAMPS_DESC =
      "友だちのプロフィール画面の右上にⓘアイコンを追加します。タップすると、MID・友だち追加・お気に入り登録・プロフィール更新の日時をダイアログで表示します。記録がない項目は「—」と表示されます。";
  public static final String PROFILE_TS_FRIEND_CREATED = "友だち追加";
  public static final String PROFILE_TS_FAVORITE = "お気に入り";
  public static final String PROFILE_TS_PROFILE_UPDATED = "プロフ更新";
  public static final String PROFILE_TS_EMPTY = "—";
  public static final String PROFILE_TS_DIALOG_TITLE = "プロフィール情報";
  public static final String PROFILE_TS_MID = "MID";
  public static final String PROFILE_TS_COPY_MID = "MIDをコピー";
  public static final String PROFILE_TS_MID_COPIED = "MIDをコピーしました";

  public static final String OPT_HIGH_QUALITY_PHOTO_LABEL = "写真の送信品質を向上";
  public static final String OPT_HIGH_QUALITY_PHOTO_DESC =
      "写真送信時の自動圧縮・リサイズを回避し、最高品質で送信できるようにします。設定の「高画質」よりもさらに高品質な送信が可能になります。";
  public static final String OPT_LONG_VIDEO_LABEL = "5分以上の動画を送信可能にする";
  public static final String OPT_LONG_VIDEO_DESC = "動画送信時の5分の時間制限を解除し、長い動画を送信できるようにします。";
  public static final String OPT_USE_DEFAULT_CAMERA_LABEL = "標準カメラを使用";
  public static final String OPT_USE_DEFAULT_CAMERA_DESC = "LINE内蔵カメラの代わりに端末の標準カメラアプリを使用します。";
  public static final String OPT_MUTE_CAMERA_SHUTTER_LABEL = "LINE内蔵カメラを無音化";
  public static final String OPT_MUTE_CAMERA_SHUTTER_DESC =
      "LINE内蔵カメラのシャッター音を無音にします。標準カメラ使用時は無効です。";

  public static final String OPT_SEARCH_BY_MEMBER_LABEL = "メンバーでトーク内検索";
  public static final String OPT_SEARCH_BY_MEMBER_DESC =
      "検索ボックス内の虫眼鏡アイコンを押すことで、メンバーを選んでそのユーザーのメッセージのみを絞り込み検索できるようにします。";
  public static final String OPT_SEARCH_MIN_1_CHAR_LABEL = "1文字からトーク内検索";
  public static final String OPT_SEARCH_MIN_1_CHAR_DESC = "トーク内検索の最低文字数を2文字から1文字に緩和します。";

  public static final String OPT_SHOW_SECONDS_IN_CHAT_TIME_LABEL = "チャットの時刻に秒を表示";
  public static final String OPT_SHOW_SECONDS_IN_CHAT_TIME_DESC =
      "各メッセージ横の時刻表示に秒を追加します。 (例: 12:34 → 12:34:56)";
  public static final String OPT_SELECT_ALL_IN_EDIT_MODE_LABEL = "メッセージ削除画面に全選択ボタンを追加";
  public static final String OPT_SELECT_ALL_IN_EDIT_MODE_DESC =
      "メッセージ削除画面の下部に、すべてのメッセージを選択・選択解除できるボタンを追加します。";
  public static final String OPT_FIX_ANNOUNCEMENT_NAME_LABEL = "アナウンス者名の表示を修正";
  public static final String OPT_FIX_ANNOUNCEMENT_NAME_DESC =
      "パッチ済みのLINEで、アナウンス登録時のシステムメッセージが「がアナウンスしました」と名前が空になる不具合を修正し、正しく表示されるようにします。";

  public static final String OPT_HIDE_AI_ICON_PERMANENTLY_LABEL = "AIアイコンを永久に非表示";
  public static final String OPT_HIDE_AI_ICON_PERMANENTLY_DESC =
      "チャット画面のテキストボックス内にあるAIアイコンを常に非表示にします。通常は30日間のみ非表示にできますが、このオプションを有効にすると設定に関わらず永続的に非表示になります。";

  public static final String OPT_OPEN_URL_IN_DEFAULT_BROWSER_LABEL = "URLをデフォルトブラウザで開く";
  public static final String OPT_OPEN_URL_IN_DEFAULT_BROWSER_DESC =
      "URLをアプリ内ブラウザではなく、システムのデフォルトブラウザで開くようにします。";

  public static final String OPT_REMOVE_ADS_LABEL = "広告を非表示";
  public static final String OPT_REMOVE_ADS_DESC = "トークリスト上部やホーム画面などに表示される広告を非表示にします。";
  public static final String OPT_REMOVE_HOME_RECOMMENDATIONS_LABEL = "ホームのおすすめを非表示";
  public static final String OPT_REMOVE_HOME_RECOMMENDATIONS_DESC = "ホーム画面に表示されるおすすめコンテンツを非表示にします。";
  public static final String OPT_REMOVE_HOME_SERVICES_LABEL = "ホームのサービスを非表示";
  public static final String OPT_REMOVE_HOME_SERVICES_DESC = "ホーム画面に表示されるサービス一覧を非表示にします。";
  public static final String OPT_REMOVE_HOME_ACCORDION_LABEL = "ホーム上部のアコーディオンを非表示";
  public static final String OPT_REMOVE_HOME_ACCORDION_DESC =
      "ホームタブ上部（検索バーと友達リストの間）に表示されるLYP特典などのアコーディオン枠を非表示にします。";
  public static final String OPT_REMOVE_TAB_VOOM_LABEL = "VOOMタブを非表示";
  public static final String OPT_REMOVE_TAB_VOOM_DESC = "下部のVOOMタブを隠します。";
  public static final String OPT_REMOVE_TAB_NEWS_LABEL = "ニュース/通話タブを非表示";
  public static final String OPT_REMOVE_TAB_NEWS_DESC = "下部のニュース/通話タブを隠します。";
  public static final String OPT_REMOVE_TAB_MINI_LABEL = "アプリタブを非表示";
  public static final String OPT_REMOVE_TAB_MINI_DESC = "下部のアプリタブを隠します。";
  public static final String OPT_REMOVE_TAB_COMMERCE_LABEL = "ショッピングタブを非表示";
  public static final String OPT_REMOVE_TAB_COMMERCE_DESC = "下部のショッピングタブを隠します。";
  public static final String OPT_REMOVE_TAB_WALLET_LABEL = "ウォレットタブを非表示";
  public static final String OPT_REMOVE_TAB_WALLET_DESC = "下部のウォレットタブを隠します。";
  public static final String OPT_EXTEND_TAB_CLICK_AREA_LABEL = "タブのタップ範囲を拡張";
  public static final String OPT_EXTEND_TAB_CLICK_AREA_DESC = "下部タブの反応範囲を広げ、押しやすくします。";
  public static final String OPT_HIDE_TAB_TEXT_LABEL = "タブラベルを非表示";
  public static final String OPT_HIDE_TAB_TEXT_DESC = "下部タブのアイコン下のテキストを非表示にします。";
  public static final String OPT_HOME_TAB_TYPE_LABEL = "ホームの種類を変更";
  public static final String OPT_HOME_TAB_TYPE_DESC =
      "ホームタブの種類を変更します。従来のホームに戻す場合は\"HOME\"を選択してください。";
  public static final String HOME_TYPE_DEFAULT = "デフォルト";

  public static final String OPT_REMOVE_AI_FRIENDS_BUTTON_LABEL = "AI Friendsボタンを非表示";
  public static final String OPT_REMOVE_AI_FRIENDS_BUTTON_DESC = "トークタブ右上の「AI Friends」ボタンを非表示にします。";
  public static final String OPT_REMOVE_SEARCH_BAR_AGENT_I_BUTTON_LABEL = "Agent i関連のボタンを非表示";
  public static final String OPT_REMOVE_SEARCH_BAR_AGENT_I_BUTTON_DESC =
      "各タブの「Agent i」ボタンと、トークルーム内の「+」メニューにある「トークルームのAgent iを表示」トグルをまとめて非表示にします。";
  public static final String OPT_REMOVE_OPEN_CHAT_BUTTON_LABEL = "オープンチャットボタンを非表示";
  public static final String OPT_REMOVE_OPEN_CHAT_BUTTON_DESC = "トークタブ右上の「オープンチャット」ボタンを非表示にします。";
  public static final String OPT_REMOVE_CALENDAR_BUTTON_LABEL = "カレンダーボタンを非表示";
  public static final String OPT_REMOVE_CALENDAR_BUTTON_DESC = "トークタブ右上の「カレンダー」ボタンを非表示にします。";
  public static final String OPT_REMOVE_ALBUM_BUTTON_LABEL = "アルバムボタンを非表示(サブ端末用)";
  public static final String OPT_REMOVE_ALBUM_BUTTON_DESC = "サブ端末のトークタブ右上に表示される「アルバム」ボタンを非表示にします。";

  public static final String OPT_USE_CUSTOM_FONT_LABEL = "カスタムフォントを有効にする";
  public static final String OPT_USE_CUSTOM_FONT_DESC =
      "選択したフォントファイルをアプリ全体に適用します。反映にはアプリの再起動が必要です。";
  public static final String OPT_CUSTOM_FONT_PATH_LABEL = "フォントファイルを選択";
  public static final String OPT_CUSTOM_FONT_PATH_DESC = "使用するフォントファイル (.ttf / .otf) を選択します。";

  public static final String OPT_USE_AMOLED_THEME_LABEL = "AMOLEDテーマを適用する";
  public static final String OPT_USE_AMOLED_THEME_DESC =
      "AMOLEDテーマを適用します。\"ダークモードでは「ブラック」着せかえを適用\"をオフにする必要があります。";

  public static final String OPT_SHOW_THEME_ON_SUB_DEVICE_LABEL = "サブ端末で着せかえ項目を表示";
  public static final String OPT_SHOW_THEME_ON_SUB_DEVICE_DESC =
      "サブ端末でログインした際、公式設定で非表示になる「着せかえ」項目を表示します。※着せかえショップは開けません。購入はメイン端末で行ってください。";

  public static final String OPT_REACTION_NOTIFICATION_LABEL = "リアクション通知";
  public static final String OPT_REACTION_NOTIFICATION_DESC =
      "メッセージについたリアクションを通知します。※アプリを開くと送信されます。フォアグラウンドサービス化している場合、LINEを起動していなくても通知が届きます。";

  public static final String OPT_STACK_MESSAGE_NOTIFICATIONS_LABEL = "通知を上書きしない";
  public static final String OPT_STACK_MESSAGE_NOTIFICATIONS_DESC =
      "通知を上書きせず、最大7件まで一つの通知の中にまとめて表示します。";

  public static final String OPT_REMOVE_NOTIFICATION_MUTE_BUTTON_LABEL = "「通知をオフ」ボタンを非表示";
  public static final String OPT_REMOVE_NOTIFICATION_MUTE_BUTTON_DESC =
      "LINEの通知に表示される「通知をオフ」ボタンを削除します。";

  public static final String OPT_FIX_SETTINGS_TALK_CRASH_LABEL = "トーク設定のクラッシュを修正";
  public static final String OPT_FIX_SETTINGS_TALK_CRASH_DESC =
      "パッチ済みのLINEで、公式設定の「トーク」を開くとクラッシュする問題を修正します。";

  public static final String OPT_EXPERIMENTAL_FCM_FIX_LABEL = "FCM Fix";
  public static final String OPT_EXPERIMENTAL_FCM_FIX_DESC =
      "非root環境で通知が1分程度で届かなくなる問題を回避するため、LINEのFCM通知受信処理を直接サービスに引き渡す挙動を有効にします。";
  public static final String OPT_LINE_FOREGROUND_KEEP_ALIVE_LABEL = "LINEを常にフォアグラウンドサービス化する";
  public static final String OPT_LINE_FOREGROUND_KEEP_ALIVE_DESC =
      "バッテリー使用量が増加する可能性がありますが、FCMFixがタスクキル後も動作します。非root環境で通知が届かなくなる問題の回避に有効です。Knotアップデート時は強制停止か一時停止が必要になる場合があります。";

  public static final String OPT_SPOOF_VERSION_UNSEND_ONLY_LABEL = "送信取り消しの時間制限を延長";
  public static final String OPT_SPOOF_VERSION_UNSEND_ONLY_DESC =
      "送信取り消し時のみバージョンを15.12.2に偽装し、1時間の時間制限を24時間に戻します。";
  public static final String OPT_SPOOF_VERSION_LABEL = "アプリバージョンの偽装 (常時)";
  public static final String OPT_SPOOF_VERSION_DESC =
      "常にアプリバージョンを15.12.2に偽装します。なにか特別な目的がない限り使用しないでください。";

  public static final String OPT_FIX_SIGNATURE_MISMATCH_LABEL = "公式署名を偽装";
  public static final String OPT_FIX_SIGNATURE_MISMATCH_DESC =
      "パッチ済みのLINEで、フォントのダウンロードや生体認証の連携などに失敗する問題を回避します。";

  public static final String DESC_PATH_ROW = "モジュールの設定ファイルなどが保存されるディレクトリを選択します。";
  public static final String DESC_RESET_ROW = "すべてのモジュール設定をデフォルト状態に戻します。";
  public static final String OPT_BACKUP_LABEL = "トーク履歴のバックアップ";
  public static final String OPT_BACKUP_DESC = "現在のトーク履歴を保存先にバックアップします。";
  public static final String OPT_RESTORE_LABEL = "トーク履歴の復元";
  public static final String OPT_RESTORE_DESC = "バックアップファイルからトーク履歴を復元します。現在の履歴が上書きされます。";

  public static final String REACTION_NOTIF_TITLE = "%sが以下のメッセージにリアクションしました";

  public static final String TOOLTIP_SETTINGS_LONG_PRESS = "長押しでKnotの設定を開けます";
  public static final String TOOLTIP_SEARCH_BY_MEMBER = "タップしてメンバーごとに絞り込み検索";

  public static final String SEARCH_BY_MEMBER_TITLE = "メンバーで絞り込み";
  public static final String SEARCH_BY_MEMBER_EMPTY = "メンバーが見つかりません";
  public static final String SEARCH_BY_MEMBER_FILTERING = "絞り込み中: ";
  public static final String SEARCH_BY_MEMBER_FILTER_CLEARED = "絞り込みを解除しました";

  public static final String OPT_ABOUT_LABEL = "Knotについて";
  public static final String OPT_ABOUT_DESC = "バージョン情報など";
  public static final String ABOUT_TAGLINE = "A brand-new Xposed module for LINE";
  public static final String ABOUT_SEC_DEVELOPERS = "開発者";
  public static final String ABOUT_SEC_CONTRIBUTORS = "コントリビュータ";
  public static final String ABOUT_SEC_LINKS = "リンク";
  public static final String ABOUT_LINK_REPO = "GitHubリポジトリ";
  public static final String ABOUT_LINK_LICENSE = "ライセンス";
  public static final String ABOUT_LICENSE_VALUE = "GNU GPLv3";
  public static final String ABOUT_DISCLAIMER =
      "このモジュールを使用したことによる不利益について、開発者は一切の責任を負いません。自己責任で使用してください。";
}
