# Samsung Galaxy S6 (SM-G920F / zerofltexx) Android 13 Build Package

## ⚠️ CRITICAL DEVICE SAFETY & COMPATIBILITY WARNING

This build package and device configuration are strictly configured for:
* **Target Device**: Samsung Galaxy S6 (Flat / International)
* **Model Number**: **SM-G920F**
* **Codename**: `zerofltexx` / `zeroflte`
* **SoC**: Samsung Exynos 7420

### 🚫 DO NOT FLASH OR USE ON:
* **Samsung Galaxy S6 Edge**: `SM-G925F` (`zeroltexx`), `SM-G925A`, `SM-G925T`, etc.
* **Samsung Galaxy S6 Edge+**: `SM-G928F` (`zenltexx`)
* **Samsung Galaxy S6 Active**: `SM-G890A` (`zeroflteatt`)
* **Samsung Galaxy S7**: `SM-G930F` (`heroltexx`)
* **Regional S6 Variants**: CDMA / Carrier-locked variants without explicitly verifying baseband compatibility.

Flashing builds intended for `zerofltexx` on other device variants may permanently brick your device or break radio/IMEI/EFS partitions. Always verify your device model under **Settings → About Phone → Model Number** or on the physical back cover before flashing any custom firmware.

---

## 📌 Project Architecture & Base Disclosure

### Pixel Experience Status
* The official **Pixel Experience** project was officially discontinued by its lead developer in February 2024.
* **No official or maintained Pixel Experience 13 (Android 13) device tree exists for Exynos 7420 (`zerofltexx`)**.

### Selected Base: LineageOS 20.0 (Android 13)
* To avoid unmaintainable framework hacks or non-booting ROMs, this reproducible build package utilizes **LineageOS 20.0 (Android 13)** as the verified open-source OS base.
* Legacy Exynos 7420 hardware HALs (`hardware/samsung_slsi/linaro_*`) and Linux 3.10 kernel backports are integrated into this build configuration.
* Pixel-inspired UI overlays and default app options can be applied cleanly on top of LineageOS 20 using the TWRP Pixel Overlay generator (`tools/make_pixel_overlay_zip.sh`). **This build is explicitly LineageOS 20-based with safe Pixel overlays and NOT official Pixel Experience.**

---

## 🎨 Safe Pixel Overlay Customization Package

To transform a clean LineageOS 20 installation into a Pixel-like UI experience without corrupting `system.img` signatures or causing bootloops, use the modular TWRP overlay generator:

```bash
# Generate flashable Pixel Overlay ZIP
bash tools/make_pixel_overlay_zip.sh
```

Output: `out/Pixel_Overlay_SM-G920F.zip`

### Flashing Order in TWRP:
1. Flash **LineageOS 20.0 ROM ZIP** (`lineage-20.0-*-zerofltexx.zip`).
2. Flash **OpenGApps / MindTheGapps 13.0 ARM64** (Google Services).
3. Flash **`Pixel_Overlay_SM-G920F.zip`** (Pixel System UI & RRO Overlays).
4. Reboot System.

---

## 💻 Build System Requirements

To compile this Android 13 (LineageOS 20 / AOSP) ROM package, your build system must meet the following minimum hardware and software specs:

| Resource | Minimum Requirement | Recommended |
|---|---|---|
| **OS** | Ubuntu 20.04 LTS / 22.04 LTS x86_64 | Ubuntu 22.04 LTS x86_64 |
| **CPU** | 8 Cores / 16 Threads | 16+ Cores |
| **RAM** | 16 GB + 32 GB Swap | 32 GB+ RAM |
| **Storage** | 250 GB Free SSD / NVMe | 300 GB+ Fast NVMe SSD |
| **Java** | OpenJDK 11 | OpenJDK 11 |
| **Python** | Python 3.8 - 3.10 | Python 3.10 |
| **Tools** | `repo`, `git`, `clang`, `ccache`, `bison`, `flex` | Latest AOSP toolchain |

---

## 🚀 Step-by-Step Build Instructions

### 1. Environment Setup
Run the setup script to install all required host build dependencies:
```bash
bash scripts/01_setup_environment.sh
```

### 2. Synchronize Source Repositories
Initialize the manifest and sync all required device trees, kernel, and hardware HALs:
```bash
bash scripts/02_sync_sources.sh
```

### 3. Proprietary Vendor Blob Extraction
Vendor binaries (Mali GPU drivers, camera ISP, RIL daemons) must be extracted legally from an official Samsung Android 7.0 Nougat firmware dump for `SM-G920F`:
```bash
bash scripts/03_extract_blobs.sh
```

### 4. Apply Hardware & Kernel Patches
Apply required Linux 3.10 backports and Exynos 7420 HAL compatibility patches:
```bash
bash scripts/04_apply_patches.sh
```

### 5. Compile the ROM Image
Build the flashable recovery ZIP package:
```bash
bash scripts/05_build_rom.sh
```

### 6. Verify Output Artifacts
Inspect the generated package integrity and SHA-256 checksum:
```bash
bash scripts/06_package_verify.sh
```

---

## 🛡️ Flashing & Safety Guide for SM-G920F

### Critical Safety Rules:
1. **Mandatory EFS / IMEI Backup**:
   - Before wiping or flashing, boot into TWRP Recovery.
   - Go to **Backup** → Select **EFS** and **NVRAM** partitions.
   - Copy the backup directory off the device onto a computer or external storage.
   - *Losing the EFS partition will destroy your cellular modem functionality and IMEI number.*

2. **Required Stock Firmware Base**:
   - Ensure your SM-G920F is updated to the latest official Samsung Android 7.0 (Nougat) bootloader/modem firmware (`G920FXXU6EVG1` or equivalent) prior to flashing.

3. **Required Recovery**:
   - Use official TWRP Recovery for Galaxy S6 Flat (`zeroflte`): `twrp-3.7.0_9-0-zeroflte.img`.

4. **Flashing Steps**:
   - Boot to TWRP (`Volume Up + Home + Power`).
   - Perform **Wipe → Advanced Wipe** → Select `System`, `Data`, `Cache`, `Dalvik/ART Cache`. *(Caution: All user data will be erased!)*
   - Transfer `lineage-20.0-*-zerofltexx.zip` to `/sdcard`.
   - Select **Install** → Choose the ZIP file → Swipe to Confirm Flash.
   - Flash MindTheGapps (ARM64, Android 13) and `Pixel_Overlay_SM-G920F.zip`.
   - Reboot to System. First boot may take 5 to 10 minutes.

5. **Stock Rollback Procedure**:
   - Download official Samsung stock Android 7.0 ROM for SM-G920F (`.tar.md5`).
   - Reboot device into Download Mode (`Volume Down + Home + Power`).
   - Open Odin on Windows PC, load BL, AP, CP, CSC files, and click **Start**.

---

## 📑 Verification & Manifest File Listing

* Manifest Location: `manifests/local_manifest.xml`
* Device Configuration: `device/samsung/zerofltexx/`
* Common Platform: `device/samsung/universal7420-common/`
* Kernel Config: `lineage_zerofltexx_defconfig` inside `kernel/samsung/universal7420`
* Overlay Generator: `tools/make_pixel_overlay_zip.sh`
