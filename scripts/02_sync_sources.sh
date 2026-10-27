#!/usr/bin/env bash
set -e

echo "=== Stage 2: Synchronizing Sources for zerofltexx (LineageOS 20.0 / Android 13) ==="

# Directory check
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$REPO_ROOT"
echo "Target Directory: $REPO_ROOT"

# Ensure local manifest directory exists and copy tracked manifest
mkdir -p .repo/local_manifests

if [ -f manifests/local_manifest.xml ]; then
    cp manifests/local_manifest.xml .repo/local_manifests/local_manifest.xml
    echo "Manifest successfully copied from manifests/local_manifest.xml to .repo/local_manifests/local_manifest.xml"
elif [ -f .repo/local_manifests/local_manifest.xml ]; then
    echo "Local manifest found at .repo/local_manifests/local_manifest.xml"
else
    echo "Error: local_manifest.xml is missing!"
    exit 1
fi

# Initialize LineageOS 20.0 repo manifest if not initialized
if [ ! -d ".repo/manifests" ]; then
    echo "Initializing LineageOS 20.0 manifest..."
    repo init -u https://github.com/LineageOS/android.git -b lineage-20.0 --git-lfs
fi

echo "Synchronizing repositories (this requires ~150-250 GB storage)..."
# repo sync with retry and depth options for bandwidth/space optimization
repo sync -c -j"$(nproc)" --force-sync --no-clone-bundle --no-tags || {
    echo "Sync warning/failure. Retrying with reduced job count..."
    repo sync -c -j2 --force-sync --no-clone-bundle --no-tags
}

echo "=== Source synchronization script complete ==="
