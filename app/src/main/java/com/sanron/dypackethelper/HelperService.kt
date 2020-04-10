package com.sanron.dypackethelper

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Path
import android.os.Build
import android.text.format.DateUtils
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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
        //房间寻找红包超时时间
        val FIND_TIME_OUT = 2500L

        val DELAY_CLOSE_ROB_WINDOW = 2000L

        //自动划页最多多少页，炒过之后回到小时榜第一名
        val MAX_PAGE = 50

        //红包倒计时最多多少
        val MAX_PACKET_WAIT_TIME = 60

        var sInstance: HelperService? = null

        //展示结果的事件
        val SHOW_RESULT_TIME = 2000L

        //红包viewid
        val PACKET_VIEW_ID = "com.ss.android.ugc.aweme:id/dvy"

        //弹出的红包弹窗的“抢”图片，表示可以抢了
        val ROB_ID = "com.ss.android.ugc.aweme:id/e3u"

        //关闭弹窗的viewid，点击关闭抢红包的弹窗
        val PACK_POP_CLOSE_ID = "com.ss.android.ugc.aweme:id/gas"


        //主页面
        val CLASS_WINDOW_MAIN = "com.ss.android.ugc.aweme.main.MainActivity"

        //直播页面class
        val CLASS_WINDOW_LIVE = "com.ss.android.ugc.aweme.live.LivePlayActivity"

        //抢红包弹窗class
        val CLASS_WINDOW_ROB = "com.bytedance.android.livesdk.chatroom.ui.da"

        val CLASS_DIALOG = "android.app.Dialog"

        //抖音包名
        val DOUYIN_PKG = "com.ss.android.ugc.aweme"
    }

    var running = false

    var state = State.FIND_PACKET
        set(value) {
            field = value
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
        processLog("onServiceConnected")
        sInstance = this
    }

    override fun onInterrupt() {
        stopForeground(true)
        sInstance = null
    }

    fun start() {
        running = true
        startFindPacket()
    }

    fun pause() {
        running = false
        findPacketJob = null
        autoRobJob = null
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
        }

        if (!running) {
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (event.className == CLASS_WINDOW_ROB) {
                    startRobPacket()
                } else if (event.className == CLASS_WINDOW_LIVE) {
                    startFindPacket()
                } else if (event.className == CLASS_WINDOW_MAIN) {
                    MAIN_HANDLER.postDelayed({
                        processLog("跑到主页去了，尝试重新打开直播")
                        currentPage = MAX_PAGE - 2
                        rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.ss.android.ugc.aweme:id/b2v")
                            ?.firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }, 1000)
                } else {
                    if (isXsb(event)) {
                        //小时榜
                        processLog("小时榜被打开，准备去第一个房间")
                        autoRobJob = null
                        findPacketJob = null
                        runUnitDone(3000L, interval = 100L, run = {
                            rootInActiveWindow?.findAccessibilityNodeInfosByViewId(
                                "com.ss.android.ugc.aweme:id/f3m"
                            )
                                ?.firstOrNull()?.getChild(1)?.let { firstRoom ->
                                    processLog("点击小时榜第一个房间")
                                    firstRoom.performAction(
                                        AccessibilityNodeInfo.ACTION_CLICK
                                    )
                                    delay(1000)
                                    currentPage = 1
                                    startFindPacket()
                                    return@runUnitDone true
                                }
                            false
                        }, onTimeOut = {
                            processLog("加载小时榜超时了,关闭小时榜弹窗")
                            performGlobalAction(GLOBAL_ACTION_BACK)
                            startFindPacket()
                        })
//                        GlobalScope.launch {
//                            try {
//                                withTimeout(3000) {
//                                    while (isActive) {
//                                        rootInActiveWindow?.findAccessibilityNodeInfosByViewId(
//                                            "com.ss.android.ugc.aweme:id/f3m"
//                                        )
//                                            ?.firstOrNull()?.getChild(1)?.let { firstRoom ->
//                                                processLog("点击小时榜第一个房间")
//                                                firstRoom.performAction(
//                                                    AccessibilityNodeInfo.ACTION_CLICK
//                                                )
//                                                delay(1000)
//                                                currentPage = 1
//                                                startFindPacket()
//                                                return@withTimeout
//                                            }
//                                        delay(100)
//                                    }
//                                }
//                            } catch (e: TimeoutCancellationException) {
//                                processLog("加载小时榜超时了,关闭小时榜弹窗")
//                                performGlobalAction(GLOBAL_ACTION_BACK)
//                                startFindPacket()
//                            }
//                        }
                    } else {
                        debug("弹出弹窗${event.className},尝试关闭")
                        performGlobalAction(GLOBAL_ACTION_BACK)
                    }
                }
            }
        }
    }

    fun isXsb(event: AccessibilityEvent): Boolean {
        return event.className == CLASS_DIALOG
                && (event.source?.findAccessibilityNodeInfosByViewId("com.ss.android.ugc.aweme:id/ep6")
            ?.firstOrNull() != null)
    }

    var autoRobJob: Job? = null
        set(value) {
            field?.cancel()
            field = value
            field?.invokeOnCompletion {
                if (it is CancellationException) {
                    processLog("autoRobJob被取消")
                }
            }
        }

    var findPacketJob: Job? = null
        set(value) {
            field?.cancel()
            field = value
            field?.invokeOnCompletion {
                if (it is CancellationException) {
                    processLog("findPacketJob被取消")
                }
            }
        }

    var currentPage = 1


    fun startFindPacket() {
        autoRobJob = null
        this@HelperService.processLog("开始等待寻找红包")
        findPacketJob = runUnitDone(FIND_TIME_OUT, interval = 100, run = {
            findRedPacketNode()?.let {
                val timeNode =
                    it.findAccessibilityNodeInfosByViewId("com.ss.android.ugc.aweme:id/fk4")
                        ?.firstOrNull()
                var shouldClick = timeNode?.text?.split(":")?.let { splitTimes ->
                    splitTimes.size == 2 && (splitTimes[0].toInt() * 60 + splitTimes[1].toInt()) < MAX_PACKET_WAIT_TIME
                } ?: true

                if (shouldClick) {
                    this@HelperService.processLog("找到了红包按钮并且时间少于$MAX_PACKET_WAIT_TIME，点点点")
                    it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                } else {
                    this@HelperService.processLog("找到了红包按钮但是时间大于$MAX_PACKET_WAIT_TIME，先去下一页")
                    scrollToNextRoom()
                }
                return@runUnitDone true
            }
            false
        }, onTimeOut = {
            this@HelperService.processLog("超时没有找到红包，切换到下个房间")
            scrollToNextRoom()
        })

//        findPacketJob = GlobalScope.launch {
//            this@HelperService.processLog("开始等待寻找红包")
//            try {
//                withTimeout(FIND_TIME_OUT) {
//                    while (isActive) {
//                        findRedPacketNode()?.let {
//                            val timeNode =
//                                it.findAccessibilityNodeInfosByViewId("com.ss.android.ugc.aweme:id/fk4")
//                                    ?.firstOrNull()
//                            var shouldClick = timeNode?.text?.split(":")?.let { splitTimes ->
//                                splitTimes.size == 2 && (splitTimes[0].toInt() * 60 + splitTimes[1].toInt()) < MAX_PACKET_WAIT_TIME
//                            } ?: true
//
//                            if (shouldClick) {
//                                this@HelperService.processLog("找到了红包按钮并且时间少于$MAX_PACKET_WAIT_TIME，点点点")
//                                it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
//                            } else {
//                                this@HelperService.processLog("找到了红包按钮但是时间大于$MAX_PACKET_WAIT_TIME，先去下一页")
//                                scrollToNextRoom()
//                            }
//                            return@withTimeout
//                        }
//                        delay(100)
//                    }
//                }
//            } catch (e: TimeoutCancellationException) {
//                this@HelperService.processLog("超时没有找到红包，切换到下个房间")
//                scrollToNextRoom()
//            }
//        }
    }

    fun scrollToNextRoom() {
        if (currentPage == MAX_PAGE) {
            toFirstRoom()
            return
        }

        val path = Path()
        var y: Float = (resources.displayMetrics.heightPixels * 4 / 5f)
        var x: Float = (resources.displayMetrics.widthPixels * 4f / 5f)
        path.moveTo(x, y)
        path.lineTo(x, y - resources.displayMetrics.heightPixels * 3 / 5f)
        val gestureDescription = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()
        dispatchGesture(gestureDescription, object : GestureResultCallback() {

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                MAIN_HANDLER.postDelayed({
                    startFindPacket()
                }, 1000)
            }

            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                this@HelperService.processLog("翻页手势已完毕")
                currentPage++
                GlobalScope.launch(Dispatchers.Main) {
                    delay(1000)
                    startFindPacket()
                }
            }
        }, MAIN_HANDLER)
    }

    fun startRobPacket() {
        findPacketJob = null
        autoRobJob = runUnitDone(5 * DateUtils.MINUTE_IN_MILLIS, interval = 1L, run = {
            val flagView =
                rootInActiveWindow?.findAccessibilityNodeInfosByViewId("com.ss.android.ugc.aweme:id/fia")
                    ?.firstOrNull()
            if (flagView == null || "送你一个好运气" != flagView.text) {
                startFindPacket()
                return@runUnitDone true
            }
            val packetNode = findRobNode()
            if (packetNode != null) {
                packetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                this@HelperService.processLog("点击了抢红包按钮，延迟${DELAY_CLOSE_ROB_WINDOW}关闭弹窗")
                delay(DELAY_CLOSE_ROB_WINDOW)
                findClosePopNode()?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return@runUnitDone true
            }
            false
        })
//        autoRobJob = GlobalScope.launch(Dispatchers.IO) {
//            this@HelperService.processLog("等待红包变为可抢")
//            while (isActive) {
//                val flagView =
//                    rootInActiveWindow?.findAccessibilityNodeInfosByViewId("com.ss.android.ugc.aweme:id/fia")
//                        ?.firstOrNull()
//                if (flagView == null || "送你一个好运气" != flagView.text) {
//                    startFindPacket()
//                    return@launch
//                }
//                val packetNode = findRobNode()
//                if (packetNode != null) {
//                    packetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
//                    this@HelperService.processLog("点击了抢红包按钮，延迟${DELAY_CLOSE_ROB_WINDOW}关闭弹窗")
//                    delay(DELAY_CLOSE_ROB_WINDOW)
//                    findClosePopNode()?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
//                    return@launch
//                }
//                delay(1)
//            }
//        }
    }

    //去第一名的房间
    fun toFirstRoom() {
        processLog("去小时榜第一名的房间")
        autoRobJob = null
        findPacketJob = null
        val bandanbtn =
            rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.ss.android.ugc.aweme:id/ag8")
                ?.firstOrNull()
        bandanbtn?.parent?.let {
            it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }

    fun processLog(msg: String) {
        debug("状态改变==>$msg")
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