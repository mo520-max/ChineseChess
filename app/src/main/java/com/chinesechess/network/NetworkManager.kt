package com.chinesechess.network

import android.os.Handler
import android.os.Looper
import com.chinesechess.model.Move
import com.chinesechess.model.Piece
import java.io.*
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 网络通信管理器（优化版）
 * 处理蓝牙和WiFi对战时的数据传输，增强稳定性
 */
class NetworkManager {
    
    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null
    
    private val isConnected = AtomicBoolean(false)
    private var messageListener: MessageListener? = null
    private var receiveThread: Thread? = null
    private var heartbeatThread: Thread? = null
    private val handler = Handler(Looper.getMainLooper())
    
    // 线程池用于异步操作
    private val executorService = Executors.newSingleThreadExecutor()
    
    // 重连机制
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 3
    private val reconnectDelay = 3000L // 3秒
    
    // 心跳机制
    private val heartbeatInterval = 5000L // 5秒
    private val heartbeatTimeout = 15000L // 15秒超时
    private var lastHeartbeatTime = System.currentTimeMillis()
    private val missedHeartbeats = AtomicBoolean(false)
    
    // 消息队列（用于断线时缓存消息）
    private val messageQueue = ConcurrentLinkedQueue<String>()
    private val maxQueueSize = 50
    
    // 连接状态回调
    var onConnectionStateChanged: ((ConnectionState) -> Unit)? = null
    
    enum class ConnectionState {
        CONNECTING,
        CONNECTED,
        DISCONNECTED,
        RECONNECTING,
        ERROR
    }
    
    interface MessageListener {
        fun onMoveReceived(moveData: String)
        fun onDrawRequest()
        fun onDrawResponse(accepted: Boolean)
        fun onSurrender()
        fun onConnectionLost()
        fun onMessageReceived(message: String)
        fun onHeartbeatTimeout()
    }
    
    /**
     * 设置消息监听器
     */
    fun setMessageListener(listener: MessageListener) {
        messageListener = listener
    }
    
    /**
     * 连接到Socket（带超时和优化）
     */
    fun connect(socket: Socket): Boolean {
        return try {
            updateConnectionState(ConnectionState.CONNECTING)
            
            // 设置Socket参数
            socket.tcpNoDelay = true // 禁用Nagle算法，减少延迟
            socket.keepAlive = true // 启用KeepAlive
            socket.soTimeout = 30000 // 读取超时30秒
            socket.receiveBufferSize = 8192
            socket.sendBufferSize = 8192
            
            this.socket = socket
            inputStream = socket.getInputStream()
            outputStream = socket.getOutputStream()
            reader = BufferedReader(InputStreamReader(inputStream), 8192)
            writer = PrintWriter(OutputStreamWriter(outputStream), true)
            
            isConnected.set(true)
            reconnectAttempts = 0
            lastHeartbeatTime = System.currentTimeMillis()
            
            updateConnectionState(ConnectionState.CONNECTED)
            
            // 启动接收线程
            startReceiving()
            
            // 启动心跳线程
            startHeartbeat()
            
            // 发送队列中的消息
            flushMessageQueue()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            updateConnectionState(ConnectionState.ERROR)
            false
        }
    }
    
    /**
     * 安全发送消息（带重试机制）
     */
    fun sendMessageSafe(message: String, maxRetries: Int = 2): Boolean {
        if (!isConnected.get()) {
            // 如果未连接，加入队列
            queueMessage(message)
            return false
        }
        
        var attempts = 0
        while (attempts <= maxRetries) {
            if (sendMessageInternal(message)) {
                return true
            }
            attempts++
            if (attempts <= maxRetries) {
                Thread.sleep(100) // 短暂延迟后重试
            }
        }
        
        // 发送失败，加入队列
        queueMessage(message)
        return false
    }
    
    /**
     * 内部发送消息
     */
    private fun sendMessageInternal(message: String): Boolean {
        return try {
            writer?.let {
                synchronized(it) {
                    it.println(message)
                    it.flush()
                }
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 将消息加入队列
     */
    private fun queueMessage(message: String) {
        if (messageQueue.size < maxQueueSize) {
            messageQueue.offer(message)
        }
    }
    
    /**
     * 刷新消息队列
     */
    private fun flushMessageQueue() {
        executorService.execute {
            while (messageQueue.isNotEmpty() && isConnected.get()) {
                val message = messageQueue.poll()
                if (message != null) {
                    sendMessageInternal(message)
                }
            }
        }
    }
    
    /**
     * 发送移动数据
     */
    fun sendMove(move: Move): Boolean {
        return sendMessageSafe("MOVE:${move.toNetworkString()}")
    }
    
    /**
     * 发送悔棋请求
     */
    fun sendUndoRequest(): Boolean {
        return sendMessageSafe("UNDO_REQUEST")
    }
    
    /**
     * 发送悔棋响应
     */
    fun sendUndoResponse(accepted: Boolean): Boolean {
        return sendMessageSafe("UNDO_RESPONSE:$accepted")
    }
    
    /**
     * 发送和棋请求
     */
    fun sendDrawRequest(): Boolean {
        return sendMessageSafe("DRAW_REQUEST")
    }
    
    /**
     * 发送和棋响应
     */
    fun sendDrawResponse(accepted: Boolean): Boolean {
        return sendMessageSafe("DRAW_RESPONSE:$accepted")
    }
    
    /**
     * 发送认输消息
     */
    fun sendSurrender(): Boolean {
        return sendMessageSafe("SURRENDER")
    }
    
    /**
     * 发送游戏开始消息
     */
    fun sendGameStart(): Boolean {
        return sendMessageSafe("GAME_START")
    }
    
    /**
     * 发送心跳包
     */
    private fun sendHeartbeat(): Boolean {
        return sendMessageInternal("HEARTBEAT")
    }
    
    /**
     * 开始接收消息
     */
    private fun startReceiving() {
        receiveThread = Thread {
            try {
                val buffer = CharArray(1024)
                while (isConnected.get() && !Thread.currentThread().isInterrupted) {
                    try {
                        val message = reader?.readLine()
                        if (message != null) {
                            handleMessage(message)
                        } else {
                            // 连接断开
                            handleDisconnection()
                            break
                        }
                    } catch (e: SocketTimeoutException) {
                        // 读取超时，继续
                        continue
                    } catch (e: SocketException) {
                        // Socket异常
                        handleDisconnection()
                        break
                    }
                }
            } catch (e: IOException) {
                if (isConnected.get()) {
                    handleDisconnection()
                }
            }
        }
        receiveThread?.start()
    }
    
    /**
     * 启动心跳检测
     */
    private fun startHeartbeat() {
        heartbeatThread = Thread {
            while (isConnected.get() && !Thread.currentThread().isInterrupted) {
                try {
                    Thread.sleep(heartbeatInterval)
                    
                    if (!isConnected.get()) break
                    
                    // 发送心跳
                    if (!sendHeartbeat()) {
                        // 发送失败
                        missedHeartbeats.set(true)
                    }
                    
                    // 检查心跳超时
                    val timeSinceLastHeartbeat = System.currentTimeMillis() - lastHeartbeatTime
                    if (timeSinceLastHeartbeat > heartbeatTimeout) {
                        // 心跳超时
                        handler.post {
                            messageListener?.onHeartbeatTimeout()
                        }
                        handleDisconnection()
                        break
                    }
                    
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
        heartbeatThread?.start()
    }
    
    /**
     * 处理接收到的消息
     */
    private fun handleMessage(message: String) {
        when {
            message.startsWith("MOVE:") -> {
                val moveData = message.substring(5)
                handler.post {
                    messageListener?.onMoveReceived(moveData)
                }
            }
            message == "UNDO_REQUEST" -> {
                handler.post {
                    messageListener?.onMessageReceived(message)
                }
            }
            message.startsWith("UNDO_RESPONSE:") -> {
                val accepted = message.substring(14).toBoolean()
                handler.post {
                    messageListener?.onMessageReceived(message)
                }
            }
            message == "DRAW_REQUEST" -> {
                handler.post {
                    messageListener?.onDrawRequest()
                }
            }
            message.startsWith("DRAW_RESPONSE:") -> {
                val accepted = message.substring(14).toBoolean()
                handler.post {
                    messageListener?.onDrawResponse(accepted)
                }
            }
            message == "SURRENDER" -> {
                handler.post {
                    messageListener?.onSurrender()
                }
            }
            message == "GAME_START" -> {
                handler.post {
                    messageListener?.onMessageReceived(message)
                }
            }
            message == "HEARTBEAT" -> {
                // 收到心跳，更新时间
                lastHeartbeatTime = System.currentTimeMillis()
                missedHeartbeats.set(false)
                // 回复心跳确认
                sendMessageInternal("HEARTBEAT_ACK")
            }
            message == "HEARTBEAT_ACK" -> {
                // 收到心跳确认
                lastHeartbeatTime = System.currentTimeMillis()
                missedHeartbeats.set(false)
            }
            else -> {
                handler.post {
                    messageListener?.onMessageReceived(message)
                }
            }
        }
    }
    
    /**
     * 处理断开连接
     */
    private fun handleDisconnection() {
        if (isConnected.compareAndSet(true, false)) {
            updateConnectionState(ConnectionState.DISCONNECTED)
            
            handler.post {
                messageListener?.onConnectionLost()
            }
            
            // 尝试重连
            // attemptReconnect()
        }
    }
    
    /**
     * 尝试重连（可选功能）
     */
    private fun attemptReconnect() {
        if (reconnectAttempts >= maxReconnectAttempts) {
            updateConnectionState(ConnectionState.ERROR)
            return
        }
        
        reconnectAttempts++
        updateConnectionState(ConnectionState.RECONNECTING)
        
        handler.postDelayed({
            // 这里可以实现重连逻辑
            // 需要保存连接信息（IP、端口等）
        }, reconnectDelay)
    }
    
    /**
     * 断开连接
     */
    fun disconnect() {
        isConnected.set(false)
        updateConnectionState(ConnectionState.DISCONNECTED)
        
        // 停止线程
        receiveThread?.interrupt()
        heartbeatThread?.interrupt()
        
        // 关闭资源
        try {
            reader?.close()
            writer?.close()
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        reader = null
        writer = null
        inputStream = null
        outputStream = null
        socket = null
        
        // 清空队列
        messageQueue.clear()
    }
    
    /**
     * 更新连接状态
     */
    private fun updateConnectionState(state: ConnectionState) {
        handler.post {
            onConnectionStateChanged?.invoke(state)
        }
    }
    
    /**
     * 检查是否已连接
     */
    fun isConnected(): Boolean {
        return isConnected.get() && socket?.isConnected == true && !socket!!.isClosed
    }
    
    /**
     * 获取连接状态
     */
    fun getConnectionState(): ConnectionState {
        return when {
            isConnected() -> ConnectionState.CONNECTED
            reconnectAttempts > 0 && reconnectAttempts < maxReconnectAttempts -> ConnectionState.RECONNECTING
            else -> ConnectionState.DISCONNECTED
        }
    }
    
    /**
     * 获取连接状态字符串
     */
    fun getConnectionStatus(): String {
        return when (getConnectionState()) {
            ConnectionState.CONNECTING -> "连接中..."
            ConnectionState.CONNECTED -> "已连接"
            ConnectionState.DISCONNECTED -> "未连接"
            ConnectionState.RECONNECTING -> "重连中..."
            ConnectionState.ERROR -> "连接错误"
        }
    }
    
    /**
     * 清理资源
     */
    fun release() {
        disconnect()
        executorService.shutdown()
        messageListener = null
    }
}

/**
 * 通信协议定义
 */
object NetworkProtocol {
    const val CMD_MOVE = "MOVE"
    const val CMD_UNDO_REQUEST = "UNDO_REQUEST"
    const val CMD_UNDO_RESPONSE = "UNDO_RESPONSE"
    const val CMD_DRAW_REQUEST = "DRAW_REQUEST"
    const val CMD_DRAW_RESPONSE = "DRAW_RESPONSE"
    const val CMD_SURRENDER = "SURRENDER"
    const val CMD_GAME_START = "GAME_START"
    const val CMD_HEARTBEAT = "HEARTBEAT"
    const val CMD_HEARTBEAT_ACK = "HEARTBEAT_ACK"
    
    /**
     * 构建移动消息
     */
    fun buildMoveMessage(move: Move): String {
        return "$CMD_MOVE:${move.toNetworkString()}"
    }
    
    /**
     * 解析移动消息
     */
    fun parseMoveMessage(message: String, pieces: List<Piece>): Move? {
        if (!message.startsWith("$CMD_MOVE:")) return null
        val moveData = message.substring(CMD_MOVE.length + 1)
        return Move.fromNetworkString(moveData, pieces)
    }
}
