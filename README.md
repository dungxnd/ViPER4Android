# ViPER4Android

> ### 💡 Why another fork?
> While the tech community is rightfully cautious about "AI slop," we must also recognize an equally dangerous pitfall: **"Human slop."** 
> 
> Human slop is the stagnation that comes from conservative maintenance, stubbornness, and refusing to adapt. Ignoring AI and modern development workflows; it degrades the user's experience over time. 
> 
> This fork mission is to embrace progression: utilizing LLM, modernizing the UX and compability for our next-level [DSP backend](https://github.com/dungxnd/ViPERDSP). We build for the future, not the past.

## What this fork  (app, driver, DSP) has over the old stubborn repo

**Progressing in the era of AI. No refusal to the future of development and user experience.**
- Modern implementation of Tube Simulation with more tube types, more dynamic behavior. *Detail at [DSP#1](https://github.com/dungxnd/ViPERDSP/pull/1), [DSP#2](https://github.com/dungxnd/ViPERDSP/pull/2)*
- Fixed faulty bass effects, also made it better and more accurate. *Detail at [DSP#6](https://github.com/dungxnd/ViPERDSP/pull/6)*
- Rewrote FET Compressor internals for cleaner, punchier audio with no buzzing, pumping, or zipper noise. *Detail at [DSP#4](https://github.com/dungxnd/ViPERDSP/pull/4)*
- Fixed wrongly active output speaker detected. *Detail at [App#7](https://github.com/dungxnd/ViPER4Android-Frontier/pull/7)*
- Fixed/Improved CPU hungry Dynamic EQ & LUFS Targeting. *Detail at [DSP#11](https://github.com/dungxnd/ViPERDSP/pull/11)*
- Fixed/Improved faulty CPU consumption of Convolver/Headphone Surround+ effect. *Detail at [DSP#12](https://github.com/dungxnd/ViPERDSP/pull/12)*
- Better app experience (UI/UX). 
- Rewrote DSP driver (with modern C++23/C++26) to prevent memory leaks. *Detail at [DSP#5](https://github.com/dungxnd/ViPERDSP/pull/5)*
- Driver module auto detect and install correct device's HAL (AIDL / HIDL).
- Added Android Auto wired mode. [App#e1541a2](https://github.com/dungxnd/ViPER4Android-Frontier/commit/e1541a25708fc7d898164a8570ee16e8d84f0dc2)
- Many more fixes/improvements.

(You must [new driver](https://github.com/dungxnd/ViPERFX_RE/releases) for this app.)

---

Material Design 3 UI for ViPER4Android FX. Full feature set of the ViPER4Android DSP engine with a modern interface.

## Features

### Audio Effects

- Output Volume / Channel Pan / Limiter
- Playback Gain Control (AGC)
- LUFS Targeting
- Multiband Compressor (5-band)
- FET Compressor

- ViPER Bass (Natural / Pure Bass+ / Subwoofer)
- ViPER Bass Mono (original v0.5.0 algorithm)
- Psychoacoustic Bass Enhancement
- ViPER Clarity (Natural / OZone+ / XHiFi)
- Tube Simulator (WDF trinode mode, 5 tube types)
- AnalogX

- ViPER-DDC (Digital Device Correction)
- Spectrum Extension (VSE)
- IIR Equalizer (10 / 15 / 25 / 31 bands)
- Dynamic EQ (up to 10 bands, per-band threshold/attack/release)

- Convolver (IRS impulse response loading)
- Field Surround (Colorful Music)
- Differential Surround
- Stereo Imager (3-band width control)
- Headphone Surround+ (VHE)
- Reverberation
- Dynamic System (headphone virtualization)

- Auditory System Protection (Cure crossfeed)
- Speaker Optimization (speaker-only)

---

### App Features

- Material Design 3 with dynamic theming
- AIDL and legacy (non-AIDL) HAL support
- Per-device profile management and auto switching
- Preset import / export
- Global mode and per-app mode
- In-app log viewer (tap `Driver Version` 7 times in Settings)

## Installation

1. Download the latest APK from the [Releases](https://github.com/dungxnd/ViPER4Android/releases)
2. Install the APK
3. Flash the Magisk module from [ViPERFX_RE](https://github.com/dungxnd/ViPERFX_RE) (or the AIDL variant)
4. Reboot
5. Open the app and tune the settings.
6. Enjoy

## Q&A

Read [Q&A Wiki](https://github.com/dungxnd/ViPER4Android-Frontier/wiki/Q&A)

## Per-Device Profiles

Each audio output device keeps its **own** full effect profile. When you switch outputs, the app
automatically loads the incoming device's profile, so your speaker, wired earphones, and each pair
of Bluetooth buds all remember their own tuning.

### How a device is identified

The active output is detected from the Android audio routing API, and each device gets a stable `deviceId`:

- **Speaker** → `speaker`
- **Wired headphone / headset** → `wired_headphone`
- **Bluetooth** (A2DP / BLE / hearing aid) → the device's **MAC address** (falls back to `bt_<id>` if unavailable)
- **USB DAC / headset** → the USB device **address** (falls back to `usb_<id>`)

Because Bluetooth and USB devices are keyed by their hardware address, two different pairs of buds
are stored separately, and reconnecting the same device restores exactly its profile. The display
name comes from the device's reported product name (e.g. "Galaxy Buds Pro"), with a type-based fallback ("Bluetooth A2DP", "USB Audio", ...).

### When profiles are saved and loaded

- **On output switch**: the app detects the new active device, **loads** its profile.
- **On app background** (home button, app switch / `ON_STOP`) and **on shutdown**: the current settings are saved to the active device's profile.
- **First time a device is seen**: a profile is created automatically from the current settings.

### Managing profiles

Open the **Devices dialog** to manage saved profiles. There you can **save** the current settings to
a device, **load** a device's profile into the current settings, **rename** a device, or **delete** a
profile. Deleting a profile removes its saved settings, the device gets a fresh profile the next time it connects.

## Root Access

This app may require root access for:

- **AIDL mode**: Creating shared memory files for the AIDL driver (if not set up during module installation)
- **Per-App Mode**: Retrieving real audio session IDs via `dumpsys` (not needed when installed as a privileged system app; see Per-App Mode in the Q&A)
- **Convolver**: Copying IRS/WAV files to `/data/local/tmp/v4a/` for the driver to read (AIDL only)
- **Debug log viewer**: Reading `logcat` for driver diagnostics

Please make sure the source of any modified APK is trustworthy to avoid any security risks.

## Contributing

Contributions are welcome. Open an issue or submit a pull request.

### Localization

To help with translations, follow the [template guide](app/res-template/values-template/strings.xml).
