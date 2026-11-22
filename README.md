# Ikachan (J2ME)

A port of Studio Pixel's *Ikachan* built for CLDC 1.1 and MIDP 2.0 devices such as the Nokia 6300. (Japanese release Only)

## Features

- Full cavern tilemap rendering (`MapEngine`) with parallax background, scrolling water currents, foreground overlays, and smooth camera tracking.
- Swimming physics, dash charging, snack and item pickups, spike hazards, and leveling (`MyChar`).
- Pre-allocated particle effects (`EffectSystem`) for bubbles, stars, damage numbers, and level-up indicators.
- NPC entity loading and AI (`NpCharSystem`) from `NPChar.dat`, plus the Ironhead boss encounter (`BossIronhead`).
- Shift-JIS bitmap font rendering (`BitmapFont`) and event scripting (`EventVM`) with dialogue boxes, choices, screen fades, and `RecordStore` save/load support.
- Item inventory overlay (`ItemMenu`), opening/intro/credits flow (`OpeningScreen`), and `.amr` audio playback (`AudioManager`).
- Toggleable `240x320` portrait and rotated `320x240` landscape view modes.

## Prerequisites

- **JDK 8** (`javac` and `jar`) for compiling Java 1.3 / CLDC 1.1 bytecode.
- **Sun Wireless Toolkit (WTK 2.5.2)** or a compatible MIDP 2.0 SDK providing `preverify.exe` and the CLDC/MIDP/MMAPI libraries.
- **Python 3** with `Pillow` and `pefile` if you run the asset packing or icon extraction scripts (`tools/pack_data.py`, `tools/extract_icon.py`).
- Original *Ikachan* data files (`NPChar.dat`, `Event.ptx`, `Words.ptx`, `Staff.ptx`, map data, sprites, and audio assets) placed in your asset folders before building.

## Building

1. Pack the binary data into `res/data.pak` using your packing script:
   ```bat
   python tools/pack_data.py
   ```
2. Optionally extract the MIDlet icon from the original executable:
   ```bat
   python tools/extract_icon.py
   ```
3. Update `WTK_HOME` and `JDK_BIN` at the top of `build.bat` if your paths differ, then run:
   ```bat
   build.bat
   ```
4. Deploy the generated `dist\Ikachan.jar` and `dist\Ikachan.jad` to your emulator or Nokia 6300.

## Controls

- **Move / Aim:** D-pad or `2` / `4` / `6` / `8`
- **Swim / Dash / Confirm:** `5`, `9`, or D-pad center (`FIRE`)
- **Inventory:** `7` (during gameplay)
- **Toggle BGM:** `1`
- **Toggle Portrait / Landscape:** `#`
- **Exit:** `*`
