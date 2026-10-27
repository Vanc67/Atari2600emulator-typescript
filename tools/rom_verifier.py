#!/usr/bin/env python3
import os
import hashlib
import sys

def get_sha256(filepath):
    h = hashlib.sha256()
    with open(filepath, 'rb') as f:
        while chunk := f.read(8192):
            h.update(chunk)
    return h.hexdigest()

def inspect_device_tree(device_dir):
    print(f"=== Inspecting Device Tree: {device_dir} ===")
    if not os.path.exists(device_dir):
        print(f"Error: Directory {device_dir} does not exist.")
        return False

    files = os.listdir(device_dir)
    print("Files found:", files)

    board_config = os.path.join(device_dir, "BoardConfig.mk")
    if os.path.exists(board_config):
        with open(board_config, 'r') as f:
            content = f.read()
            print("\n--- BoardConfig.mk Highlights ---")
            for line in content.splitlines():
                if "TARGET_OTA_ASSERT_DEVICE" in line or "BOARD_SYSTEMIMAGE_PARTITION_SIZE" in line or "TARGET_KERNEL_CONFIG" in line:
                    print("  ", line.strip())

    return True

if __name__ == "__main__":
    if len(sys.argv) > 1:
        cmd = sys.argv[1]
        if cmd == "hash" and len(sys.argv) > 2:
            print(f"SHA256 ({sys.argv[2]}): {get_sha256(sys.argv[2])}")
        elif cmd == "inspect" and len(sys.argv) > 2:
            inspect_device_tree(sys.argv[2])
        else:
            print("Usage: rom_verifier.py [hash <file> | inspect <device_dir>]")
    else:
        inspect_device_tree("device/samsung/zerofltexx")
