#
# Device configuration for Samsung Galaxy S6 Flat (zeroflte)
#

$(call inherit-product-if-exists, vendor/samsung/zerofltexx/zerofltexx-vendor.mk)

LOCAL_PATH := device/samsung/zerofltexx

## device overlays
DEVICE_PACKAGE_OVERLAYS += $(LOCAL_PATH)/overlay

# Flat display configuration - explicitly exclude Edge screen software / People Edge / Edge Feeds
PRODUCT_PROPERTY_OVERRIDES += \
    ro.config.edge_screen=false \
    ro.factory.tool=flat

# Carrier init
PRODUCT_PACKAGES += \
    init.carrier.rc

# cpboot daemon
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/ril/sbin/cbd:root/sbin/cbd

# Inherit from universal7420-common
$(call inherit-product, device/samsung/universal7420-common/device-common.mk)
