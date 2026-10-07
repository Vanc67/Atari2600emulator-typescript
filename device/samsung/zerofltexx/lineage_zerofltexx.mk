#
# Copyright (C) 2025 The LineageOS Project
#

PRODUCT_RUNTIMES := runtime_libart_default

# Inherit from core specs
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
$(call inherit-product, device/samsung/zerofltexx/device.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Set device variables for Samsung Galaxy S6 Flat
PRODUCT_NAME := lineage_zerofltexx
PRODUCT_DEVICE := zeroflte
PRODUCT_BRAND := Samsung
PRODUCT_MANUFACTURER := samsung
PRODUCT_MODEL := SM-G920F
