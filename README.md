# WakeTracker

WakeTracker is a strict, physically engaging Android alarm clock designed to make sure you actually get out of bed. Instead of letting you blindly tap a "snooze" button from under the covers, WakeTracker requires you to physically get up and scan a designated NFC tag to silence the alarm.

## Hardware Requirement: NFC Tag
To use this app as intended, **you must have a physical NFC tag**. 

For the app to recognize the tag and dismiss the alarm, the tag must be programmed with the specific URI configured for this app (e.g., `waketracker://dismiss`). 

*Tip: Stick this NFC tag somewhere far from your bed—like the bathroom mirror or near the coffee maker—so you are forced to start your day to turn the alarm off.*

## Installation

WakeTracker is distributed directly via GitHub. The easiest way to install it and automatically receive future updates is by using an app manager like [Obtainium](https://github.com/ImranR98/Obtainium).

### Option 1: Install via Obtainium (Recommended)
1. Download and install **Obtainium** on your Android device.
2. Open Obtainium and tap **Add App**.
3. Paste the URL of this repository: `https://github.com/InvisibleOS/WakeTracker`
4. Tap **Add** and then **Install**. 
5. Obtainium will automatically download the latest secure release and notify you whenever a new version is available.

### Option 2: Direct APK Download
1. Navigate to the **[Releases](../../releases)** section on the right side of this GitHub page.
2. Click on the latest release tag (e.g., `v1.2`).
3. Scroll down to the **Assets** dropdown.
4. Download the `app-release.apk` file directly to your phone.
5. Tap the downloaded file to install it. *(Note: You may need to grant your browser permission to "Install unknown apps" in your Android settings).*

## Setup & Usage
1. **Program your Tag:** Use a free NFC writing app (like *NFC Tools*) to write the required WakeTracker URI to your physical NFC tag.
2. **Set the Alarm:** Open WakeTracker, grant any necessary notification permissions, and set your wake-up time.
3. **Wake Up:** When the alarm triggers, the standard dismissal methods are disabled.
4. **Scan to Stop:** Get out of bed, walk to your NFC tag, and tap the back of your phone against it to successfully dismiss the alarm.

## Tech Stack & Architecture

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3), Material Icons Extended
- **Android Architecture Components:**
  - Lifecycle & ViewModel Compose
  - Room (using KSP for annotation processing)
  - WorkManager
- **Build System:** Gradle (Kotlin DSL Version Catalogs)

## Development & Requirements

- **Minimum SDK:** API 34
- **Target SDK:** API 35
- **Java / JVM Toolchain:** Version 17

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
