#
# Device configuration for Samsung Galaxy S6 Flat (zeroflte)
#

$(call inherit-product-if-exists, vendor/samsung/zerofltexx/zerofltexx-vendor.mk)

LOCAL_PATH := device/samsung/zerofltexx

## device overlays
DEVICE_PACKAGE_OVERLAYS += $(LOCAL_PATH)/overlay

# Flat display density (560dpi for 5.1 inch QHD flat display)
PRODUCT_AAPT_CONFIG := normal
PRODUCT_AAPT_PREF_CONFIG := xxhdpi

# Carrier init
PRODUCT_PACKAGES += \
    init.carrier.rc

# cpboot daemon
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/ril/sbin/cbd:root/sbin/cbd

# Inherit from universal7420-common
$(call inherit-product, device/samsung/universal7420-common/device-common.mk)
