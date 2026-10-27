#!/usr/bin/env bash
set -e

echo "=== Stage 1: Setting up Build Environment for Samsung Galaxy S6 (zerofltexx) ==="

# Check OS release
if [ -f /etc/os-release ]; then
    . /etc/os-release
    echo "OS: $NAME $VERSION"
fi

# Required dependencies for LineageOS 20 / AOSP build
PACKAGES=(
    bc bison build-essential ccached curl flex g++-multilib gcc-multilib
    git git-lfs gnupg gperf imagemagick lib32ncurses5-dev lib32readline-dev
    lib32z1-dev libelf-dev libgl1-mesa-dev libxml2-utils xsltproc unzip fontconfig
    libncurses5 libncurses5-dev libpulse0 libsdl1.2-dev libssl-dev libxml2
    libxslt1-dev lzop pngcrush rsync schedtool squashfs-tools xsltproc zip zlib1g-dev
    python3 python3-pip python-is-python3 openjdk-11-jdk
)

echo "Installing required system packages (requires sudo access)..."
if command -v apt-get >/dev/null 2>&1; then
    sudo apt-get update || true
    sudo apt-get install -y "${PACKAGES[@]}" || true
else
    echo "Package manager not apt-get. Ensure dependencies are installed manually."
fi

# Ensure repo tool is installed in ~/.bin or /usr/local/bin
mkdir -p ~/.bin
if ! command -v repo >/dev/null 2>&1; then
    echo "Installing repo tool..."
    curl https://storage.googleapis.com/git-repo-downloads/repo > ~/.bin/repo
    chmod a+rx ~/.bin/repo
fi

export PATH=~/.bin:$PATH

echo "=== Environment setup script complete ==="
