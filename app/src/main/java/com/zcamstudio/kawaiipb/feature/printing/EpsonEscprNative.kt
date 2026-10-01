package com.zcamstudio.kawaiipb.feature.printing

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint

internal object EpsonEscprNative {
    init {
        System.loadLibrary("kawaii_escpr_jni")
    }

    external fun printRgb(
        connection: UsbDeviceConnection,
        interfaceId: Int,
        vendorId: Int,
        productId: Int,
        inputEndpoint: UsbEndpoint,
        outputEndpoint: UsbEndpoint,
        mediaSizeId: Int,
        width: Int,
        height: Int,
        rgb: ByteArray,
        diagnostics: LongArray,
        deviceIdOutput: ByteArray
    ): Int
}