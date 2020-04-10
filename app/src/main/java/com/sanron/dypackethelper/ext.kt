package com.sanron.dypackethelper

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

/**
 *Author:sanron
 *Time:2020/4/9
 *Description:
 */
fun Any.debug(msg: String) {
    Log.d(
        this::class.java.simpleName, "\n--------------------------------------\n"
                + msg
                + "\n--------------------------------------"
    )
}

val MAIN_HANDLER = Handler(Looper.getMainLooper())

fun Context.dp(dp: Number): Int {
    return (resources.displayMetrics.density * dp.toDouble() + 0.5).toInt()
}

fun showToast(msg: CharSequence, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(MyApp.instance, msg, length).show()
}