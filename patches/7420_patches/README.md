# Hardware Patches for Exynos 7420 (zerofltexx)

This directory contains patches required for backporting Android 13 support to Exynos 7420 kernel 3.10 and Linaro OpenMAX/Gralloc HALs:

1. `0001-kernel-bpf-backport.patch`: Backports minimal eBPF / BPF syscall compatibility to Linux 3.10.x kernel for Android 13 system server.
2. `0002-slsi-linaro-gralloc-a13.patch`: Adjusts Gralloc buffer flags and atomic buffer operations for HWComposer compatibility.
