package com.sanron.dypackethelper

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.*

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

fun runUnitDone(
    timeout: Long,
    interval: Long = 10,
    run: suspend () -> Boolean,
    onTimeOut: suspend () -> Unit = {}
): Job {
    return GlobalScope.launch {
        val parentJob = this.coroutineContext[Job]
        try {
            withTimeout(timeout) {
                while (isActive && parentJob?.isCancelled != true) {
                    if (run()) {
                        return@withTimeout
                    }
                    delay(interval)
                }
            }
        } catch (e: TimeoutCancellationException) {
            onTimeOut()
        }
    }
}