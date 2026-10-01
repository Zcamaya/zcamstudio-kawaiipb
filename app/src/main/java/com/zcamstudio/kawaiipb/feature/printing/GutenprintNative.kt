package com.zcamstudio.kawaiipb.feature.printing

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint

internal object GutenprintNative {
    init {
        System.loadLibrary("kawaii_gutenprint_jni")
    }

    external fun printL120(
        connection: UsbDeviceConnection,
        outputEndpoint: UsbEndpoint,
        dataDirectory: String,
        pageWidthMils: Int,
        pageHeightMils: Int,
        width: Int,
        height: Int,
        rgb: ByteArray,
        diagnostics: LongArray
    ): Int
}
