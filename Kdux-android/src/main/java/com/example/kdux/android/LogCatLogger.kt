package com.example.kdux.android

import android.util.Log
import kdux.log.KduxLogger

class LogCatLogger: KduxLogger {
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