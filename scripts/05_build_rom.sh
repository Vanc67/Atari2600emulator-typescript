#!/usr/bin/env bash
set -e

echo "=== Stage 5: Building ROM Image for Samsung Galaxy S6 (zerofltexx) ==="

# Check disk space limit before initiating build
AVAILABLE_KB=$(df . | awk 'NR==2 {print $4}')
REQUIRED_KB=150000000 # ~150 GB minimum for build artifacts

echo "Available Disk Space: $((AVAILABLE_KB / 1024 / 1024)) GB"

if [ "$AVAILABLE_KB" -lt "$REQUIRED_KB" ]; then
    echo "ERROR: Insufficient disk space to execute AOSP build!"
    echo "Minimum required: ~150 GB free disk space."
    echo "Current environment has only $((AVAILABLE_KB / 1024 / 1024)) GB free."
    echo "Aborting actual compilation to prevent filesystem corruption."
    exit 1
fi

# Set up environment variables
export USE_CCACHE=1
export CCACHE_EXEC=/usr/bin/ccache
export CCACHE_DIR=~/.ccache
ccache -M 50G

# Source environment and select lunch target
source build/envsetup.sh
lunch lineage_zerofltexx-userdebug

echo "Starting build process..."
mka bacon -j"$(nproc)"
