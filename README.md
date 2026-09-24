# VoiceAlert

**Your phone reads your notifications out loud — so you never have to look at the screen.**

VoiceAlert listens for notifications on your phone and speaks them aloud through
whatever Bluetooth device you're connected to, while automatically protecting
private information (OTPs, bank details, message contents) and adjusting its
behavior based on where you are and what you're doing.

---

## The problem this solves

You're riding a bike/scooter, driving, cooking, working out, or just don't want
to keep unlocking your phone every time it buzzes. Normally you either:

- Ignore notifications and miss something important, or
- Keep checking your phone, which is distracting (and dangerous while driving/riding).

VoiceAlert reads the notification to you instead, through your earphones, car
speaker, or any connected Bluetooth audio device — hands-free, eyes-free.

---

## Real-life use cases

### 🏍️ Riding or driving
Connect your helmet/car Bluetooth. Set the device profile to **Car Audio System**.
WhatsApp, SMS, and call notifications get read aloud automatically, but private
message content is summarized ("New message from Rahul") instead of read in
full — so a stranger's voice in your car doesn't broadcast your private texts.

### 🏃 Working out / commuting with earbuds
Connect your earbuds, leave the default **Personal Headphones** profile active.
Notifications are spoken in full through your earbuds. If a song or YouTube
video is playing, VoiceAlert automatically lowers that volume, speaks the
alert, then restores your media volume exactly where it was.

### 🏠 At home with a Bluetooth speaker
Set the profile to **Home / Public Speaker**. Alerts still play, but quietly
and with sender-only detail ("New message from Priya") — so your family in
the next room doesn't hear your OTPs or bank balance read out loud.

### 🌙 At night / in a meeting
Turn on **Quiet Hours** (e.g. 10 PM – 7 AM) in Settings, or just enable your
phone's system **Do Not Disturb** — VoiceAlert respects both automatically
and goes completely silent, except for anything you've explicitly marked
**Speak Over** (e.g. your spouse, your boss, an on-call alert).

### 🔒 Privacy by default
- **OTP codes are never read aloud** — masked automatically.
- **Bank/UPI transaction messages** are cleaned up so account numbers aren't spoken.
- **Privacy Mode** reads only the sender's name, not the message content.
- A phone call ringing/being answered instantly interrupts and stops any
  speech in progress. Dismissing or opening a notification you're currently
  hearing also stops it early — no talking over you.

### 🎯 Fine-grained control per app
On the **Apps** tab, every app installed on your phone can be set to:
- **Speak Over** — interrupts whatever's playing and always gets through,
  even during Quiet Hours or system DND (for apps/contacts you never want to miss).
- **Speak** — reads normally, ducking any background music/video first.
- **Disabled** — never speaks for that app.

Search, filter by mode, and bulk-apply a mode to everything currently shown
(e.g. search "games" → Disable Shown) instead of tapping through apps one by one.

### 🔋 Know your earbuds' status at a glance
The Home and Devices screens show exactly which Bluetooth device is
connected (its real name, not a placeholder) and its battery percentage,
when the device reports one.

### ⚡ Quick toggle without opening the app
Add the **VoiceAlert** tile to your Quick Settings (swipe down from the
notification shade) to flip Travel Mode on/off in one tap.

---

## How it works, briefly

1. **VoiceNotificationListenerService** listens to every notification posted
   on the device (requires one-time "Notification access" permission).
2. Each notification is checked against: is Travel Mode on, is this app
   enabled, is it Quiet Hours / system DND, what's the connected device's
   profile — and skipped if any of those say "stay quiet."
3. What's left goes through **PrivacyFilter** (OTP masking, bank message
   cleanup, sender-only mode) to build a safe-to-speak sentence.
4. **TTSManager** ducks any playing media, speaks the sentence at a volume
   matched to your device profile, then restores media volume.

---

## Getting started

1. Install the APK (see [Building](#building) below, or grab the latest
   build from the repo's GitHub Actions artifacts).
2. On first launch, grant the requested permissions (Bluetooth, phone state,
   notifications).
3. **Settings → Notification access → enable VoiceAlert** (Android requires
   this to be granted manually — the app can't request it directly).
4. Connect a Bluetooth device, go to the **Devices** tab, and confirm/adjust
   its profile.
5. Toggle **Travel Mode** on from the Home screen. You're set.

## Building

```bash
./gradlew assembleDebug
# APK output: app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17 and the Android SDK (compileSdk 34). A GitHub Actions
workflow (`.github/workflows/build-apk.yml`) builds a debug APK automatically
on every push to `main`.

## Tech stack

- Kotlin + Jetpack Compose (Material 3)
- `NotificationListenerService` for capturing notifications
- Android `TextToSpeech` engine for speech output
- DataStore Preferences for settings persistence
- Bluetooth A2DP/HFP profile APIs for device detection
