# bootable image with tboot
BBPATH += ""

LICENSE = "MIT"
LIC_FILES_CHKSUM = " \
    file://${COREBASE}/LICENSE;md5=3f40d7994397109285ec7b81fdeb3b58 \
    file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420 \
    "

SRC_URI = " \
    file://syslinux.cfg;md5=7ae4fc16f1e54b4dc232e76ffd47b0d5 \
    file://grub.cfg;md5=9b0245d085f18f5b93dea9ddc18c8174 \
"

# fetch/unpack tasks don't normally run for image recipes. This means our
# bootloader configs won't end up in WORKDIR unless we do some magic here.
python () {
	# Ensure we run these usually noexec tasks
	d.delVarFlag("do_fetch", "noexec")
	d.delVarFlag("do_unpack", "noexec")
}

IMAGE_INSTALL += "base-passwd packagegroup-tboot"

INITRD_IMAGE ?= "core-image-tpm-initramfs"
INITRD = "${DEPLOY_DIR_IMAGE}/${INITRD_IMAGE}-${MACHINE}.cpio.gz"

ROOTFS_IMAGE ?= "core-image-tpm"
ROOTFS = "${DEPLOY_DIR_IMAGE}/${ROOTFS_IMAGE}-${MACHINE}.ext3"

NOHDD = "1"

TBOOT_CMDLINE = "loglvl=all logging=serial,vga,memory"
KERNEL_CMDLINE = "ramdisk_size=32768 root=/dev/ram0 ro measureroot rootimg=rootfs.img rootimgpcr=9 console=tty0 console=ttyS0,115200n8"

# be sure the bootimg is built after the initrd and rootfs
do_bootimg[depends] += "${INITRD_IMAGE}:do_rootfs"
do_bootimg[depends] += "${ROOTFS_IMAGE}:do_rootfs"

inherit core-image
inherit bootimg
inherit measured-bootimg

syslinux_hddimg_populate_append() {
	install -m 0444 ${STAGING_DATADIR}/syslinux/libcom32.c32 $hdd_dir${SYSLINUXDIR}
	install -m 0444 ${STAGING_DATADIR}/syslinux/mboot.c32 $hdd_dir${SYSLINUXDIR}
}

syslinux_iso_populate_append() {
	install -m 0444 ${STAGING_DATADIR}/syslinux/libcom32.c32 $iso_dir${ISOLINUXDIR}
	install -m 0444 ${STAGING_DATADIR}/syslinux/mboot.c32 $iso_dir${ISOLINUXDIR}
}

# have bootimg populate function grab tboot and ACM
populate_append() {
	install -m 0644 ${DEPLOY_DIR_IMAGE}/tboot-${MACHINE}.gz ${DEST}/tboot.gz
	install -m 0644 ${DEPLOY_DIR_IMAGE}/acm_*.bin ${DEST}/
}

build_syslinux_cfg () {
    sed -e "s&@TBOOT_CMDLINE@&${TBOOT_CMDLINE}&" \
        -e "s&@KERNEL_CMDLINE@&${KERNEL_CMDLINE}&" \
        ${WORKDIR}/syslinux.cfg > ${SYSLINUXCFG}
}

build_efi_cfg() {
    sed -e "s&@TBOOT_CMDLINE@&${TBOOT_CMDLINE}&" \
        -e "s&@KERNEL_CMDLINE@&${KERNEL_CMDLINE}&" \
        ${WORKDIR}/grub.cfg > ${GRUBCFG}
}
