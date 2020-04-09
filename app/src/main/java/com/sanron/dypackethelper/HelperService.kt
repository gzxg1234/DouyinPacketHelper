package com.sanron.dypackethelper

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo


/**
 *Author:sanron
 *Time:2020/4/9
 *Description:
 */
class HelperService : AccessibilityService() {

    enum class State {
        //等待寻找红包
        FIND_PACKET,

        //等待红包弹窗弹出
        WAIT_PACKET_WINDOW_SHOW,

        //等待红包可以抢
        WAIT_PACKET_CAN_ROB,

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
            debug(
                "抢红包状态改变==>${
                when (value) {
                    State.FIND_PACKET -> "寻找红包挂件"
                    State.WAIT_PACKET_CAN_ROB -> "等待红包可点击"
                    State.WAIT_RESULT -> "等待红包结果"
                    else -> "位置状态"
                }
                }"
            )
        }

    val flowView by lazy {
        FloatView(this)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        debug("onServiceConnected")
        sInstance = this
    }

    override fun onInterrupt() {
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
                } else if (event.isFullScreen) {
                    flowView.pause()
                    flowView.hide()
                }
                debug("TYPE_WINDOW_STATE_CHANGED->${event}")
                debug("SOURCE->${event.source}")
            }
        }

        if (!running || event.packageName != DOUYIN_PKG) {
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                when (state) {
                    State.FIND_PACKET -> {
                        //寻找红包时，弹出其他弹窗，需要关闭
                        if (event.className != CLASS_WINDOW_LIVE) {
                            performGlobalAction(GLOBAL_ACTION_BACK)
                        }
                    }
                    State.WAIT_PACKET_WINDOW_SHOW -> {
                        if (event.className == CLASS_WINDOW_ROB) {
                            //红包弹窗弹出了，改变状态
                            state = State.WAIT_PACKET_CAN_ROB
                        } else if (event.className != CLASS_WINDOW_LIVE) {
                            performGlobalAction(GLOBAL_ACTION_BACK)
                        }
                    }

                    State.WAIT_PACKET_CAN_ROB -> {
                        if (event.className == CLASS_WINDOW_LIVE) {
                            //等待红包弹窗可抢过程中，关闭了红包弹窗，需要重新打开
                            state = State.FIND_PACKET
                        } else {
                            //等待红包弹窗可抢过程中，弹出其他弹窗，需要关闭
                            performGlobalAction(GLOBAL_ACTION_BACK)
                        }
                    }

                    State.WAIT_RESULT -> {
                        if (event.className != CLASS_WINDOW_LIVE) {
                            //等待红包结果中，弹出其他弹窗，需要关闭
                            performGlobalAction(GLOBAL_ACTION_BACK)
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
                    state = State.WAIT_PACKET_WINDOW_SHOW
                    return
                }
            }
            State.WAIT_PACKET_CAN_ROB -> {
                //寻找“抢”图标，找到了说明红包已可抢
                val packetNode = findRobNode()
                if (packetNode != null) {
                    packetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    state = State.WAIT_RESULT
                    return
                }
            }
            State.WAIT_RESULT -> {
                //“抢”图标消失，说明抢好了，不管结果
                val packetNode = findRobNode()
                if (packetNode == null) {
                    MAIN_HANDLER.postDelayed({
                        findClosePopNode()?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        state = State.FIND_PACKET
                    }, SHOW_RESULT_TIME)
                    return
                }
            }
        }

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