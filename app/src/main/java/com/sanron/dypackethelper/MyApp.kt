package com.sanron.dypackethelper

import android.app.Application

/**
 *Author:sanron
 *Time:2020/4/9
 *Description:
 */
class MyApp :Application(){

    companion object{
        lateinit var instance:MyApp
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}