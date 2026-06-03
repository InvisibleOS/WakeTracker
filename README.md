# WakeTracker

**Engineered for Consistent Waking.**

Traditional alarms rely heavily on willpower, allowing individuals to fall back into sleep cycles through continuous "snoozing." WakeTracker fundamentally disrupts this pattern. It is a strict, physically engaging Android alarm application designed for professionals and individuals seeking to establish a rigorous, highly consistent morning routine.

### How It Operates

Rather than permitting dismissal from the comfort of a bed, WakeTracker forces physical relocation. To silence the active alarm, users are required to physically leave their sleeping environment and scan a designated, pre-programmed NFC tag. 

*Recommendation: Place the NFC tag in a bathroom or kitchen to guarantee physical separation from the bed before the alarm can be disabled.*

### Essential Features

- **Zero-Tolerance Snooze Policy:** Standard software dismissal options are entirely disabled. The alarm can only be deactivated through physical hardware interaction.
- **Hardware-Enforced Waking:** Utilizes Near-Field Communication (NFC) technology to demand active, external engagement from the user.
- **Circadian Rhythm Correction:** By enforcing a strict wake-up routine, the application aids in establishing long-term consistency, which is critical for circadian rhythm stabilization.
- **Productivity & Consistency Tracking:** Monitors and visualizes your daily wake-up success rate. By showing how consistently you get out of bed on or before your scheduled time, the application encourages habit transformation, measures your progress, and ultimately boosts overall productivity.
- **Streamlined UX:** Features an efficient, distraction-free interface built upon Google's Material 3 design language.

---

### Installation & Deployment

WakeTracker is open-source, privacy-focused, and exclusively available via GitHub.

#### Option 1: Automated Updates via Obtainium (Recommended)
Deploying WakeTracker via [Obtainium](https://github.com/ImranR98/Obtainium) ensures secure, automated background updates directly from this repository.
1. Install Obtainium on your Android device.
2. Add a new application within Obtainium.
3. Input the repository URL: `https://github.com/InvisibleOS/WakeTracker`
4. Execute the installation.

#### Option 2: Direct Release Download
1. Navigate to the **[Releases](../../releases)** section of this repository.
2. Select the latest available release version.
3. Download the `app-release.apk` asset.
4. Proceed with manual installation on your Android device (ensure "Install unknown apps" permissions are granted).

---

### Configuration Guide

1. **Procure an NFC Tag:** The application requires a standard, writable NFC tag or sticker.
2. **Program the Tag:** Access the Settings page within the WakeTracker application and utilize the "Setup NFC Tag" feature to natively encode your tag with the required dismissal code.
3. **Strategic Placement:** Secure the NFC tag in a location that forces you out of the bedroom.
4. **Initialize Schedule:** Open the WakeTracker application, configure the targeted wake-time, and ensure all device permissions are granted.

---

### Current Goals

- **React Native Rewrite:** Rewrite the complete app in React Native to support iOS devices too.

---

<details>
<summary><b>License</b></summary>

This software is distributed under the [Apache License 2.0](LICENSE).
</details>
