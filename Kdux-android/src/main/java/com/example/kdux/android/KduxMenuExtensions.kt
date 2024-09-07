package com.example.kdux.android

import android.content.Context
import android.util.Log
import kdux.KduxMenu
import kdux.log.KduxLogger

fun KduxMenu.android(context: Context) {

    cacheDir(context.cacheDir)

    globalLogger(
        object : KduxLogger {
            override fun i(msg: String?) {
                Log.i("KDUX", msg.toString())
            }

            override fun d(msg: String?) {
                Log.d("KDUX", msg.toString())
            }

            override fun w(msg: String?) {
                Log.w("KDUX", msg.toString())
            }

            override fun e(msg: String?, e: Throwable) {
                Log.e("KDUX", msg.toString(), e)
            }

        }
    )
}