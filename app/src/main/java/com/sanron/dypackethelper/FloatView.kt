package com.sanron.dypackethelper

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.yhao.floatwindow.FloatWindow
import com.yhao.floatwindow.MoveType

/**
 *Author:sanron
 *Time:2020/4/9
 *Description:
 */
class FloatView(val context: Context) {
    val content = LayoutInflater.from(context).inflate(R.layout.float_view, null)
    val start: Button = content.findViewById(R.id.btn_start)
    val setting: View = content.findViewById(R.id.btn_setting)

    val floatWindow by lazy {
        start.tag = "start"
        content.findViewById<View>(R.id.btn_start).setOnClickListener {
            if (HelperService.sInstance == null) {
                showToast("请先到辅助服务中打开助手开关")
                return@setOnClickListener
            }

            if (start.tag == "start") {
                start()
            } else {
                pause()
            }
        }
        FloatWindow
            .with(context.applicationContext)
            .setView(content)
            .setWidth(ViewGroup.LayoutParams.WRAP_CONTENT)                               //设置控件宽高
            .setHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            .setX(100)                                   //设置控件初始位置
            .setY(100)
            .setDesktopShow(false)                        //桌面显示
            .setMoveType(MoveType.slide)
            .build()
        return@lazy FloatWindow.get()
    }

    fun start() {
        HelperService.sInstance?.start()
        start.text = "停止"
        setting.visibility = View.GONE
        start.tag = "pause"
    }

    fun pause() {
        HelperService.sInstance?.pause()
        start.text = "开始"
        setting.visibility = View.VISIBLE
        start.tag = "start"
    }

    fun show() {
        floatWindow.show()
    }

    fun hide() {
        floatWindow.hide()
    }

}