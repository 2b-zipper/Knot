# Knot - Xposed module for LINE
<p>
  <a href="https://github.com/2b-zipper/Knot/releases/latest"><img src="https://img.shields.io/github/v/release/2b-zipper/Knot?sort=semver&style=flat&label=Release&color=2ea44f" alt="Release"></a>
  <a href="https://github.com/2b-zipper/Knot/releases"><img src="https://img.shields.io/github/downloads/2b-zipper/Knot/total?style=flat&label=Downloads&color=2ea44f" alt="Downloads"></a>
  <a href="https://github.com/2b-zipper/Knot/stargazers"><img src="https://img.shields.io/github/stars/2b-zipper/Knot?style=flat&label=Stars&color=2ea44f" alt="Stars"></a>
  <a href="https://github.com/2b-zipper/Knot/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-GPLv3-2ea44f?style=flat" alt="License"></a>
</p>

![Banner](images/banner.png)

Knotは、Android版LINEのユーザー体験を向上させるために設計された開発中のXposedモジュールです。


> ⚠️このモジュールは個人が学習目的で開発したものであり、LINEヤフー株式会社とは一切関係ありません。本モジュールの使用はLINEの利用規約に抵触する可能性があり、使用に起因するアカウントの制限、凍結、データ消失などのいかなる不利益や損害についても、開発者は一切の責任を負いません。自己責任でご使用ください。

**対応LINEバージョン**: 26.8.0, 26.9.0, 26.10.0, 26.10.1, 26.11.0

## スクリーンショット

<p float="left">
  <img src="images/sc_settings.png" width="200" />
  <img src="images/sc_plusmenu.png" width="200" />
  <img src="images/sc_talk.png" width="200" />
</p>
<p float="left">
  <img src="images/sc_readhistory.png" width="200" />
  <img src="images/sc_unsend.png" width="200" />
  <img src="images/sc_customfont.png" width="200" />
</p>

## 主な機能

### プライバシー & メッセージ
- **既読回避 / 既読履歴の記録**: メッセージを既読にせず読み、返信時のみ既読にできます。また、誰がいつメッセージを読んだかの履歴も記録します。
- **送信取り消し無効化 / 制限延長**: 相手が取り消したメッセージを端末に残し、自身の送信取り消し可能時間も24時間に延長します。
- **URLをデフォルトブラウザで開く**: アプリ内ブラウザではなく、システムのデフォルトブラウザでURLを開きます。
- **写真・動画の送信制限解除**: 送信時の自動圧縮・リサイズを回避して最高品質で送信し、5分以上の動画送信も可能にします。
- **トーク内検索の強化**: メンバーを指定した絞り込み検索や、1文字からのトーク内検索を可能にします。
- **チャット画面のUI改善**: AIアイコンの非表示や、時刻表示への秒の追加を行います。

### 画面表示 & UI
- **広告・おすすめ非表示**: トークリストやホーム画面の各種広告、おすすめコンテンツ、サービス一覧を非表示にします。
- **タブのカスタマイズ**: VOOM/ニュース/MINI等の不要なタブやアイコン下ラベルの非表示、タップ範囲の拡張を行えます。
- **不要なボタンの削除**: トーク右上の「AI Friends」「オープンチャット」や、検索バー横などの「Agent i」関連UIを削除します。
- **カスタムフォント**: お好みのTTF/OTFフォントファイルをアプリ全体に適用できます。

### 通知
- **リアクション通知**: メッセージについたリアクションを通知として受け取れます。
- **消音ボタンを非表示**: 通知に表示される「通知をオフ」ボタンを削除します。
- **通知の遅延・不達対策 (FCM Fix)**: 非root環境での通知不達を回避するため、FCM受信処理の直接引き渡しや、LINEの常時フォアグラウンドサービス化を行います。

## インストール方法

<p>
  <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22app.zipper.knot%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2F2b-zipper%2Fknot%22%2C%22author%22%3A%222b-zipper%22%2C%22name%22%3A%22Knot%22%2C%22overrideSource%22%3A%22GitHub%22%7D"><img src="images/badge_obtainium.png" height="75" alt="Get it on Obtainium"></a>
  &nbsp;&nbsp;
  <a href="https://github.com/2b-zipper/Knot/releases/latest"><img src="images/badge_github.png" height="75" alt="Get it on GitHub"></a>
</p>

### Root環境

1. [Vector](https://github.com/JingMatrix/Vector/actions)などの、API 101と互換性のあるXposed環境をインストールします。
   > 現時点でAPI 101対応版は正式リリースされていないため、GitHub ActionsからCanaryビルドをダウンロードしてください。
2. [Knot](https://github.com/2b-zipper/Knot/releases/latest)をインストールし、マネージャー上でモジュールを有効化します。
3. 対象アプリとしてLINEを選択します。
4. LINEを再起動し、Knotのモジュール設定から各機能を有効化してください。
   * モジュール設定へのアクセス方法: ホームタブ右上の設定ボタンを長押しするか、LINE設定内の追加項目から開けます。

### 非Root環境

1. [NPatch](https://github.com/7723mod/NPatch)をインストールします。
   > v1.0.5以上を使用してください。
2. [Knot](https://github.com/2b-zipper/Knot/releases/latest)をダウンロードし、インストールします。
3. Knotが対応しているバージョンのLINE apkをダウンロードします。
   > 分割apkの場合は、[Apktool M](https://maximoff.su/apktool/)や[AntiSplit-M](https://github.com/AbdurazaaqMohammed/AntiSplit-M)などを使用して単一のapkに結合してください。
4. NPatchアプリを開き、ダウンロードしたLINE apkを選択して```ローカル```でパッチし、インストールします。
5. NPatchの「管理」タブにあるLINEをタップし、```モジュールのスコープ```を選択します。
6. 表示されるモジュール一覧からKnotを選択します。
7. LINEを起動し、Knotのモジュール設定から各機能を有効化してください。
   * モジュール設定へのアクセス方法: ホームタブ右上の設定ボタンを長押しするか、LINE設定内の追加項目から開けます。

## リリースの種類について

<table>
  <tr>
    <td><b>Release (安定版)</b></td>
    <td>十分にテストされ、安定して動作することが確認されたバージョンです。<br><b>※通常はこちらを使用してください。</b></td>
  </tr>
  <tr>
    <td><b>RC (リリース候補版)</b></td>
    <td>正式リリースに向けた最終確認のためのバージョンです。<br>Beta版より安定していますが、不具合が残っている可能性があります。</td>
  </tr>
  <tr>
    <td><b>Beta (ベータ版)</b></td>
    <td>新機能の追加やバグ修正のテストを目的としたバージョンです。<br>予期せぬ不具合が含まれる可能性があります。</td>
  </tr>
</table>

## 開発者

- [2b-zipper](https://github.com/2b-zipper)
- [Nich87](https://github.com/Nich87)

## ライセンス

このプロジェクトは[GNU GPLv3](LICENSE)の下で公開されています。

GPLv3ライセンスに基づき、このプロジェクトのコードのコピー、改変、および再配布は自由ですが、これらを利用・改変したコードを配布する場合は、**ソースコードを公開し、同じGPLv3ライセンスの下で公開する**必要があります。ソースコードを公開せずにコードを無断流用・転載して配布することは禁止されています。
