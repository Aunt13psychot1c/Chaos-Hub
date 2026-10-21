# 👑 Chaos Hub

**All your chats in one sparkly Android app.** Discord, Telegram, Messenger, Instagram, WhatsApp, TikTok, X and IRC, plus any website you add, each in its own tab that stays signed in. Like Franz, but for your phone.

Made by **Aunt13psychot1c**, the Queen of Chaos. Sister app to [Chaos Bell](https://github.com/Aunt13psychot1c/Chaos-Bell) 🔔

## ✨ What it does

- 📲 **One bar of chats.** Tap an icon to switch, and every chat stays logged in.
- 🔔 **Real notifications.** New messages pop up on your phone even when the hub is in the background.
- 🔴 **Unread badges** on every icon.
- 🌐 **Add any website** — Slack, Gmail, Google Chat, another IRC client, whatever you like.
- 📎 **Send pictures and files**, and save downloads to your phone.
- 🔐 **Private by design.** You log in on each site's own page. Chaos Hub never sees your passwords, has no servers, and keeps no logs. Everything stays on your phone.

| Tab | Opens |
|---|---|
| 🎮 Discord | discord.com/app (computer version) |
| ✈️ Telegram | web.telegram.org |
| 💬 Messenger | facebook.com/messages |
| 📸 Instagram | instagram.com/direct |
| 📞 WhatsApp | web.whatsapp.com (computer version, scan the QR code with WhatsApp on another phone) |
| 🎵 TikTok | tiktok.com/messages (computer version) |
| ✖️ X | x.com/messages |
| 📡 IRC | web.libera.chat |

## 📥 Install

1. Open the [latest release](https://github.com/Aunt13psychot1c/Chaos-Hub/releases/latest) on your phone and tap **Chaos-Hub-x.x.x.apk**.
2. Open the download. If your phone asks, let your browser **install unknown apps**.
3. If Play Protect warns you, tap **More details**, then **Install anyway**. Chaos Hub isn't on the Play Store yet, so Google just doesn't know it.
4. Open Chaos Hub, allow notifications, and log in to each chat.

### 🔋 Keep the alerts coming

Android loves putting apps to sleep. For notifications that keep working:

- Leave **Keep chats awake in the background** on in ⚙️ (you'll see a small, quiet notification while it's working).
- Tap **🔋 Let Chaos Hub ignore battery saving**, or go to **Settings > Apps > Chaos Hub > Battery** and pick **Unrestricted**.

## 💡 Tips

- **Tap the tab you're already on** to reload it if a page gets stuck.
- **Back** goes back inside the chat; at the start, the hub steps aside but keeps running.
- Switch chats you don't use **off** in ⚙️. Every open chat uses memory and battery.

## 🧪 Known limits (v1)

- Voice and video calls aren't supported yet.
- Some sites change often. If one stops loading, a site update is usually the reason.
- One account per site for now.

## 🛠️ For builders

Kotlin + Jetpack Compose, one Android WebView per tab. Push any branch and **Build My APK** makes a test copy that installs next to the real app. Releases are built and signed by the **Release Chaos Hub** workflow, with the version taken from `VERSION`.

MIT licensed. See [LICENSE](LICENSE).
