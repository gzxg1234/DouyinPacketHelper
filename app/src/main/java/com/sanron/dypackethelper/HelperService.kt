package com.sanron.dypackethelper

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*


/**
 *Author:sanron
 *Time:2020/4/9
 *Description:
 */
class HelperService : AccessibilityService() {

    enum class State {

        //等待翻页
        WAIT_NEXT_PAGE,

        //等待寻找红包
        FIND_PACKET,

        //等待弹窗
        WAIT_PACKET_POP,

        //等待红包可以抢
        WAIT_PACKET_CAN_ROB,

        //等待红包弹窗关闭
        WAIT_PACKET_POP_CLOSE,

        //等待抢的结果
        WAIT_RESULT
    }

    companion object {

        var sInstance: HelperService? = null

        //展示结果的事件
        val SHOW_RESULT_TIME = 2000L

        //红包viewid
        val PACKET_VIEW_ID = "com.ss.android.ugc.aweme:id/dvy"

        //弹出的红包弹窗的“抢”图片，表示可以抢了
        val ROB_ID = "com.ss.android.ugc.aweme:id/e3u"

        //关闭弹窗的viewid，点击关闭抢红包的弹窗
        val PACK_POP_CLOSE_ID = "com.ss.android.ugc.aweme:id/gas"


        //直播页面class
        val CLASS_WINDOW_LIVE = "com.ss.android.ugc.aweme.live.LivePlayActivity"

        //抢红包弹窗class
        val CLASS_WINDOW_ROB = "com.bytedance.android.livesdk.chatroom.ui.da"

        //抖音包名
        val DOUYIN_PKG = "com.ss.android.ugc.aweme"
    }

    var running = false

    var state = State.FIND_PACKET
        set(value) {
            field = value
            if (value == State.WAIT_PACKET_CAN_ROB) {
                startRob()
            } else {
                stopRob()
            }

            if (value == State.FIND_PACKET) {
                postDelayNext()
            } else {
                scollToNextJob = null
            }

            debug(
                "抢红包状态改变==>${
                when (value) {
                    State.WAIT_NEXT_PAGE -> "翻到下个主播"
                    State.FIND_PACKET -> "寻找红包挂件"
                    State.WAIT_PACKET_POP -> "等待红包弹窗"
                    State.WAIT_PACKET_CAN_ROB -> "等待红包可点击"
                    State.WAIT_RESULT -> "等待红包结果"
                    State.WAIT_PACKET_POP_CLOSE -> "等待红包弹窗关闭"
                }
                }"
            )
        }

    var scollToNextJob: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun postDelayNext() {
        scollToNextJob = GlobalScope.launch(Dispatchers.Main) {
            this@HelperService.debug("倒计时准备翻到下一页")
            delay(5000)
            if (!running) {
                return@launch
            }
            val path = Path()

            var y: Float = (resources.displayMetrics.heightPixels * 4 / 5f)
            var x: Float = (resources.displayMetrics.widthPixels * 4f / 5f)
            path.moveTo(x, y)
            path.lineTo(x, y - resources.displayMetrics.heightPixels * 3 / 5f)

            val gestureDescription = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
                .build()
            state = State.WAIT_NEXT_PAGE
            dispatchGesture(gestureDescription, object : GestureResultCallback() {
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                }

                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    this@HelperService.debug("翻页手势已完毕")
                    state = State.FIND_PACKET
                }
            }, MAIN_HANDLER)
        }
    }

    val flowView by lazy {
        FloatView(this)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val nm: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var notification: Notification? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    "ddd",
                    "前台",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
            notification = Notification.Builder(this, "ddd").build()
        } else {
            notification = Notification.Builder(this).build()
        }
        startForeground(2, notification)
        debug("onServiceConnected")
        sInstance = this
    }

    override fun onInterrupt() {
        stopForeground(true)
        sInstance = null
    }

    fun start() {
        running = true
        state = State.FIND_PACKET
    }

    fun pause() {
        running = false
        state = State.FIND_PACKET
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (event.className == CLASS_WINDOW_LIVE) {
                    debug("show floatview")
                    flowView.show()
                }
                debug("TYPE_WINDOW_STATE_CHANGED->${event}")
                debug("SOURCE->${event.source}")
                debug("SOURCE_WINDOW->${event.source?.window}")
                debug("SOURCE_WINDOW_PARENT->${event.source?.window?.parent}")
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
//                if (event.packageName != DOUYIN_PKG) {
//                    return
//                }
//                debug("TYPE_WINDOW_CONTENT_CHANGED->${event}")
            }
        }

        if (!running) {
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                when (state) {
                    State.FIND_PACKET -> {
                        //寻找红包时，弹出其他弹窗，需要关闭
                        if (event.className == CLASS_WINDOW_ROB) {
                            //红包弹窗弹出了，改变状态
                            state = State.WAIT_PACKET_CAN_ROB
                        } else if (event.className != CLASS_WINDOW_LIVE) {
                            debug("弹出${event.className},需要关闭")
                            performGlobalAction(GLOBAL_ACTION_BACK)
                        }
                    }
                    State.WAIT_PACKET_POP -> {
                        if (event.className == CLASS_WINDOW_ROB) {
                            //红包弹窗弹出了，改变状态
                            state = State.WAIT_PACKET_CAN_ROB
                        }
                    }
                    State.WAIT_PACKET_CAN_ROB -> {
                        if (event.className == CLASS_WINDOW_LIVE) {
                            //等待红包弹窗可抢过程中，关闭了红包弹窗，需要重新打开
                            state = State.FIND_PACKET
                        } else {
                            //等待红包弹窗可抢过程中，弹出其他弹窗，需要关闭
                            debug("弹出${event.className},需要关闭")
                            performGlobalAction(GLOBAL_ACTION_BACK)
                        }
                    }

                    State.WAIT_RESULT -> {
                        if (event.className != CLASS_WINDOW_LIVE) {
                            //等待红包结果中，弹出其他弹窗，需要关闭
                            debug("弹出${event.className},需要关闭")
                            performGlobalAction(GLOBAL_ACTION_BACK)
                        }
                    }
                    State.WAIT_PACKET_POP_CLOSE -> {
                        if (event.className == CLASS_WINDOW_LIVE) {
                            //弹窗已关闭
                            state = State.FIND_PACKET
                        }
                    }
                }
            }
        }

        when (state) {
            State.FIND_PACKET -> {
                //寻找红包挂件view
                val packetNode = findRedPacketNode()
                if (packetNode != null) {
                    packetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    state = State.WAIT_PACKET_POP
                    return
                }
            }
//            State.WAIT_PACKET_CAN_ROB -> {
//                //寻找“抢”图标，找到了说明红包已可抢
//                val packetNode = findRobNode()
//                if (packetNode != null) {
//                    packetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
//                    state = State.WAIT_RESULT
//                    return
//                }
//            }
            State.WAIT_RESULT -> {
                //“抢”图标消失，说明抢好了，不管结果
                val packetNode = findRobNode()
                if (packetNode == null) {
                    state = State.WAIT_PACKET_POP_CLOSE
                    MAIN_HANDLER.postDelayed({
                        findClosePopNode()?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }, SHOW_RESULT_TIME)
                    return
                }
            }
        }

    }


    var robJob: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }

    fun startRob() {
        robJob = GlobalScope.launch(Dispatchers.IO) {
            this@HelperService.debug("循环等待红包变为可抢")
            while (isActive) {
                val packetNode = findRobNode()
                if (packetNode != null) {
                    this@HelperService.debug("循环等待红包变为可抢")
                    packetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    state = State.WAIT_RESULT
                    robJob = null
                }
                delay(1)
            }
        }
    }

    fun stopRob() {
        robJob = null
    }


    fun findClosePopNode(): AccessibilityNodeInfo? {
        return rootInActiveWindow?.findAccessibilityNodeInfosByViewId(PACK_POP_CLOSE_ID)
            ?.firstOrNull()
    }

    fun findRobNode(): AccessibilityNodeInfo? {
        return rootInActiveWindow?.findAccessibilityNodeInfosByViewId(ROB_ID)
            ?.firstOrNull()
    }

    fun findRedPacketNode(): AccessibilityNodeInfo? {
        return rootInActiveWindow?.findAccessibilityNodeInfosByViewId(PACKET_VIEW_ID)
            ?.firstOrNull()
    }
}