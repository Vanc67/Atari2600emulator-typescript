#
# BoardConfig for Samsung Galaxy S6 (zeroflte)
#

# Inherit from universal7420-common
include device/samsung/universal7420-common/BoardConfigCommon.mk

# Architecture
TARGET_ARCH := arm64
TARGET_ARCH_VARIANT := armv8-a
TARGET_CPU_ABI := arm64-v8a
TARGET_CPU_ABI2 :=
TARGET_CPU_VARIANT := generic

# Assert
TARGET_OTA_ASSERT_DEVICE := zerofltexx,zeroflte,SM-G920F,G920F

# Display / Density (Flat 1440x2560 display)
TARGET_SCREEN_DENSITY := 640

# Kernel Config
TARGET_KERNEL_CONFIG := lineage_zerofltexx_defconfig

# Partitions
BOARD_SYSTEMIMAGE_PARTITION_SIZE := 3879731200
BOARD_FLASH_BLOCK_SIZE := 4096

# Radio
BOARD_MODEM_TYPE := ss333
