#
# BoardConfig for Samsung Galaxy S6 (zeroflte)
#

# Inherit from universal7420-common
include device/samsung/universal7420-common/BoardConfigCommon.mk

# Assert
TARGET_OTA_ASSERT_DEVICE := zerofltexx,zeroflte,SM-G920F,G920F

# Kernel Defconfig
TARGET_KERNEL_CONFIG := lineage_zerofltexx_defconfig

# Partitions
BOARD_SYSTEMIMAGE_PARTITION_SIZE := 3879731200

# Radio
BOARD_MODEM_TYPE := ss333
