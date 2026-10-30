#!/usr/bin/env python3
"""Сборка offline WebView APK напрямую через AAPT2, ECJ, D8 и APK Signer."""
from __future__ import annotations

import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parent
TOOLS = Path(os.environ.get("ANDROID_BUILD_TOOLS", ROOT / ".tools")).resolve()
BUILD = ROOT / "build"
GEN = BUILD / "generated"
CLASSES = BUILD / "classes"
DEX = BUILD / "dex"
UNSIGNED = BUILD / "ServerTycoon-unsigned.apk"
OUTPUT = ROOT / "ServerTycoon.apk"
JAVA = TOOLS / "jdk" / "bin" / "java"
AAPT2 = TOOLS / "aapt2"
ANDROID_JAR = TOOLS / "android.jar"
ECJ = TOOLS / "ecj-3.45.0.jar"
D8 = TOOLS / "d8.jar"
SIGNER = TOOLS / "apksigner.jar"
KEYSTORE = ROOT / "android" / "debug.keystore"


def run(command: list[str], label: str) -> None:
    print(f"\n==> {label}", flush=True)
    subprocess.run([str(part) for part in command], cwd=ROOT, check=True)


def require_tools() -> None:
    required = [JAVA, AAPT2, ANDROID_JAR, ECJ, D8, SIGNER, KEYSTORE]
    missing = [str(path) for path in required if not path.exists()]
    if missing:
        raise SystemExit("Не найдены компоненты toolchain:\n" + "\n".join(missing))


def main() -> None:
    require_tools()
    if BUILD.exists():
        shutil.rmtree(BUILD)
    for directory in (GEN, CLASSES, DEX):
        directory.mkdir(parents=True, exist_ok=True)

    compiled = BUILD / "resources.flat.zip"
    run([AAPT2, "compile", "--dir", ROOT / "android" / "res", "-o", compiled], "AAPT2: компиляция ресурсов")
    run([
        AAPT2, "link", "-o", UNSIGNED,
        "-I", ANDROID_JAR,
        "--manifest", ROOT / "android" / "AndroidManifest.xml",
        "--java", GEN,
        "--min-sdk-version", "23",
        "--target-sdk-version", "35",
        "--auto-add-overlay",
        compiled,
    ], "AAPT2: связывание манифеста и ресурсов")

    sources = sorted((ROOT / "android" / "src").rglob("*.java")) + sorted(GEN.rglob("*.java"))
    run([
        JAVA, "-jar", ECJ, "-1.8", "-proc:none", "-nowarn",
        "-classpath", ANDROID_JAR, "-d", CLASSES,
        *sources,
    ], "ECJ: компиляция нативной WebView-активности")

    classes_jar = BUILD / "classes.jar"
    with zipfile.ZipFile(classes_jar, "w", zipfile.ZIP_DEFLATED) as archive:
        for source in sorted(CLASSES.rglob("*.class")):
            archive.write(source, source.relative_to(CLASSES).as_posix())

    run([
        JAVA, "-cp", D8, "com.android.tools.r8.D8",
        "--release", "--min-api", "23", "--lib", ANDROID_JAR,
        "--output", DEX, classes_jar,
    ], "D8: генерация classes.dex")

    with zipfile.ZipFile(UNSIGNED, "a", zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        archive.write(DEX / "classes.dex", "classes.dex")
        for source in sorted((ROOT / "www").rglob("*")):
            if source.is_file():
                archive.write(source, "assets/www/" + source.relative_to(ROOT / "www").as_posix())

    if OUTPUT.exists():
        OUTPUT.unlink()
    idsig = Path(str(OUTPUT) + ".idsig")
    if idsig.exists():
        idsig.unlink()
    run([
        JAVA, "-cp", SIGNER, "com.android.apksigner.ApkSignerTool", "sign",
        "--ks", KEYSTORE,
        "--ks-key-alias", "androiddebugkey",
        "--ks-pass", "pass:android",
        "--key-pass", "pass:android",
        "--v1-signing-enabled", "true",
        "--v2-signing-enabled", "true",
        "--v4-signing-enabled", "false",
        "--out", OUTPUT,
        UNSIGNED,
    ], "APK Signer: подпись установочного пакета")
    run([
        JAVA, "-cp", SIGNER, "com.android.apksigner.ApkSignerTool", "verify",
        "--verbose", "--print-certs", OUTPUT,
    ], "Проверка подписи APK")
    run([AAPT2, "dump", "badging", OUTPUT], "Проверка манифеста APK")

    digest = hashlib.sha256(OUTPUT.read_bytes()).hexdigest()
    print(f"\nГотово: {OUTPUT}")
    print(f"Размер: {OUTPUT.stat().st_size / 1024 / 1024:.2f} МБ")
    print(f"SHA-256: {digest}")


if __name__ == "__main__":
    try:
        main()
    except subprocess.CalledProcessError as error:
        print(f"Сборка остановлена с кодом {error.returncode}", file=sys.stderr)
        raise
