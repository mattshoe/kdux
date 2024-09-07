package com.example.kdux.android

import android.content.Context
import kdux.KduxMenu

fun KduxMenu.android(context: Context) {

    cacheDir(context.cacheDir)

    globalLogger(
        LogCatLogger()
    )
}

