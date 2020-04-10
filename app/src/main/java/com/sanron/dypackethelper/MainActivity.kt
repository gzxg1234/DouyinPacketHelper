package com.sanron.dypackethelper

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (AssistUtil.isAccessibilitySettingsOn(this, HelperService::class.java)) {
            btn_to_open.text = "服务已开启"
            btn_to_open.isEnabled = false
        } else {
            btn_to_open.isEnabled = true
            btn_to_open.text = "辅助服务未开启"
        }
        btn_to_open.setOnClickListener {
            AssistUtil.goSetService(this)
        }
    }
}
