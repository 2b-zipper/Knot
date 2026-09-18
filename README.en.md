# Knot - Xposed module for LINE

[日本語](README.md) | **English** | [繁體中文](README.zh-Hant.md)

<p>
  <a href="https://github.com/2b-zipper/Knot/releases/latest"><img src="https://img.shields.io/github/v/release/2b-zipper/Knot?sort=semver&style=flat&label=Release&color=2ea44f" alt="Release"></a>
  <a href="https://github.com/2b-zipper/Knot/releases"><img src="https://img.shields.io/github/downloads/2b-zipper/Knot/total?style=flat&label=Downloads&color=2ea44f" alt="Downloads"></a>
  <a href="https://github.com/2b-zipper/Knot/stargazers"><img src="https://img.shields.io/github/stars/2b-zipper/Knot?style=flat&label=Stars&color=2ea44f" alt="Stars"></a>
  <a href="https://github.com/2b-zipper/Knot/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-GPLv3-2ea44f?style=flat" alt="License"></a>
  <a href="https://crowdin.com/project/knot"><img src="https://img.shields.io/badge/Crowdin-Translate-2ea44f?style=flat" alt="Crowdin"></a>
</p>

![Banner](images/banner.png)

Knot is an Xposed module, currently in development, designed to improve the experience of using LINE on Android.

> ⚠️ This module is developed by individuals for educational purposes and is not affiliated with LY Corporation in any way. Using this module may violate LINE's Terms of Use, and the developers accept no responsibility for any disadvantage or damage resulting from its use, including account restrictions, suspension, or data loss. Use it at your own risk.

**Supported LINE versions**: 26.10.0, 26.10.1, 26.11.0, 26.13.0, 26.13.1, 26.14.0

**Supported languages**: 日本語, English, 繁體中文

## Screenshots

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

## Features

### Privacy & Messages
- **Read avoidance / Read history**: Read messages without marking them as read, and mark them as read only when you reply. Knot can also record who read each message and when.
- **Keep unsent messages / Longer unsend limit**: Keeps messages on your device even after the sender unsends them, and extends the time you have to unsend your own messages to 24 hours.
- **Open links in the default browser**: Opens URLs in your system's default browser instead of the in-app browser.
- **Lift photo and video sending limits**: Sends at the highest quality by skipping the automatic compression and resizing applied on send, and lets you send videos longer than 5 minutes.
- **Better in-chat search**: Filter search results by member, and search chats with just one character.
- **Chat screen tweaks**: Hide the AI icon and add seconds to message timestamps.

### Appearance & UI
- **Hide ads and recommendations**: Hides the ads in the chat list and on the Home screen, as well as recommended content and the service list.
- **Tab customization**: Hide tabs you don't need (VOOM, News/Calls, Apps, and more) and the labels under the tab icons, and extend the tab tap area.
- **Remove unneeded buttons**: Removes the "AI Friends" and "OpenChat" buttons at the top right of the Chats tab, as well as the "Agent i" UI next to the search bar and elsewhere.
- **Custom font**: Apply a TTF/OTF font file of your choice across the whole app.

### Notifications
- **Reaction notifications**: Get notified when someone reacts to your messages.
- **Hide the "Mute" notification button**: Removes the "Mute" button shown on LINE notifications.
- **Fix delayed or missing notifications (FCM Fix)**: To keep notifications from going missing on non-rooted devices, Knot hands FCM message handling directly to the service and can keep LINE running as a foreground service at all times.

## Installation

<p>
  <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22app.zipper.knot%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2F2b-zipper%2Fknot%22%2C%22author%22%3A%222b-zipper%22%2C%22name%22%3A%22Knot%22%2C%22overrideSource%22%3A%22GitHub%22%7D"><img src="images/badge_obtainium.png" height="75" alt="Get it on Obtainium"></a>
  &nbsp;&nbsp;
  <a href="https://github.com/2b-zipper/Knot/releases/latest"><img src="images/badge_github.png" height="75" alt="Get it on GitHub"></a>
</p>

### Rooted devices

1. Install an Xposed framework compatible with API 102, such as [Vector](https://github.com/JingMatrix/Vector/releases).
2. Install [Knot](https://github.com/2b-zipper/Knot/releases/latest) and enable the module in your manager.
3. Select LINE as the target app (module scope).
4. Restart LINE, then enable the features you want in Knot's module settings.
   * To open the module settings, long-press the settings button at the top right of the Home tab, or use the entry Knot adds to LINE's settings.

### Non-rooted devices
**!!! Be sure to back up your chat history before you start !!!**

#### 1. Preparation
1. Install the [forked version of NPatch](https://github.com/Nich87/NPatch).
2. Install [MicroG-RE](https://github.com/MorpheApp/MicroG-RE).
3. Open MicroG-RE and tap `Ignore optimizations` to turn off battery optimization. Then open `Self-Check` and grant all the permissions listed there.
4. Open `Accounts` in MicroG-RE and sign in with the Google account you used to back up your chat history.

#### 2. Patching and installing
5. Open NPatch, then grant it storage access and set the directory it will use.
6. Download and install the latest Knot from the release list on NPatch's home screen.
7. Tap the `Start Patch` button and choose a patching method.
   > `Download and Patch from Cloud Proxy` is recommended. If you download LINE yourself from APKMirror or a similar site, make sure you download and patch a version that Knot supports.
8. Select a LINE version or APK, then tap the `Start Patch` button at the bottom right to start patching.
9. When patching is done, tap the `Install` button at the bottom right to install it.
   > If Shizuku is installed, it is installed automatically.

#### 3. Initial setup and restoring your chat history
10. Launch LINE and log in.
    > Skip the screen that asks you to choose a Google account for restoring your chat history.
11. Tap the banner shown on LINE's Home screen to set the directory Knot will use, then long-press the settings button at the top right of Home to open Knot's module settings.
12. Enable `FCM Fix` under Notifications, then restart LINE.
13. In LINE's settings, open `Back up and restore chat history`, tap `Restore`, and choose your Google account to restore your chat history.
14. Enable the features you want in Knot's module settings.
    * To open the module settings, long-press the settings button at the top right of the Home tab, or use the entry Knot adds to LINE's settings.

## Release types

<table>
  <tr>
    <td><b>Release (stable)</b></td>
    <td>Thoroughly tested and confirmed to work reliably.<br><b>Use this version in most cases.</b></td>
  </tr>
  <tr>
    <td><b>RC (release candidate)</b></td>
    <td>A version for final checks before the official release.<br>More stable than Beta, but bugs may remain.</td>
  </tr>
  <tr>
    <td><b>Beta</b></td>
    <td>A version for testing new features and bug fixes.<br>It may contain unexpected bugs.</td>
  </tr>
</table>

## Developers

- [2b-zipper](https://github.com/2b-zipper)
- [Nich87](https://github.com/Nich87)

## License

This project is released under the [GNU GPLv3](LICENSE).

Under the GPLv3, you are free to copy, modify, and redistribute the code in this project. However, if you distribute code that uses or modifies it, you must **make the source code available and release it under the same GPLv3 license**. Distributing this code without making the source available, such as by reusing or reposting it without permission, is prohibited.
