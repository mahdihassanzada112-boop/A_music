package com.amusic.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object AudioPermission {
    val name: String =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    fun isGranted(context: Context): Boolean =
        context.checkSelfPermission(name) == PackageManager.PERMISSION_GRANTED
}
