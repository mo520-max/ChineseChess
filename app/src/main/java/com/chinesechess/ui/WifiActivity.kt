package com.chinesechess.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chinesechess.databinding.ActivityWifiBinding
import com.chinesechess.model.GameMode
import com.chinesechess.model.PieceColor
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.*

/**
 * 局域网对战Activity
 */
class WifiActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityWifiBinding
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var isHost = false
    private val discoveredHosts = mutableListOf<HostInfo>()
    private lateinit var hostListAdapter: ArrayAdapter<String>
    
    private val SERVER_PORT = 8888
    private val DISCOVERY_PORT = 8889
    
    data class HostInfo(val ip: String, val name: String)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWifiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initViews()
    }
    
    private fun initViews() {
        hostListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        binding.lvHosts.adapter = hostListAdapter
        
        binding.btnHost.setOnClickListener {
            startAsHost()
        }
        
        binding.btnJoin.setOnClickListener {
            startAsClient()
        }
        
        binding.btnScan.setOnClickListener {
            scanHosts()
        }
        
        binding.btnConnect.setOnClickListener {
            val ip = binding.etIpAddress.text.toString().trim()
            if (ip.isNotEmpty()) {
                connectToHost(ip)
            } else {
                Toast.makeText(this, "请输入IP地址", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.lvHosts.setOnItemClickListener { _, _, position, _ ->
            if (position < discoveredHosts.size) {
                connectToHost(discoveredHosts[position].ip)
            }
        }
    }
    
    private fun startAsHost() {
        isHost = true
        binding.tvStatus.text = "创建房间中..."
        
        Thread {
            try {
                // 启动UDP广播服务，让客户端可以发现
                startDiscoveryService()
                
                serverSocket = ServerSocket(SERVER_PORT)
                runOnUiThread {
                    binding.tvStatus.text = "等待玩家加入...\nIP: ${getLocalIpAddress()}"
                }
                
                clientSocket = serverSocket?.accept()
                
                runOnUiThread {
                    onConnected()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    binding.tvStatus.text = "创建房间失败: ${e.message}"
                }
            }
        }.start()
    }
    
    private fun startAsClient() {
        isHost = false
        binding.tvStatus.text = "请选择房间或直接输入IP"
        scanHosts()
    }
    
    private fun scanHosts() {
        discoveredHosts.clear()
        updateHostList()
        binding.tvStatus.text = "正在搜索房间..."
        
        Thread {
            try {
                val localIp = getLocalIpAddress()
                val subnet = localIp.substring(0, localIp.lastIndexOf("."))
                
                // 发送UDP广播
                val socket = DatagramSocket()
                socket.broadcast = true
                
                val message = "CHESS_DISCOVER".toByteArray()
                val packet = DatagramPacket(
                    message, message.size,
                    InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT
                )
                socket.send(packet)
                
                // 接收响应
                socket.soTimeout = 3000
                val receiveBuffer = ByteArray(1024)
                val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                
                try {
                    while (true) {
                        socket.receive(receivePacket)
                        val response = String(receivePacket.data, 0, receivePacket.length)
                        if (response.startsWith("CHESS_HOST:")) {
                            val hostName = response.substring(11)
                            val hostIp = receivePacket.address.hostAddress
                            
                            if (hostIp != localIp && !discoveredHosts.any { it.ip == hostIp }) {
                                discoveredHosts.add(HostInfo(hostIp, hostName))
                                runOnUiThread {
                                    updateHostList()
                                }
                            }
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    // 超时结束
                }
                
                socket.close()
                
                runOnUiThread {
                    binding.tvStatus.text = "搜索完成，找到 ${discoveredHosts.size} 个房间"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    binding.tvStatus.text = "搜索失败: ${e.message}"
                }
            }
        }.start()
    }
    
    private fun startDiscoveryService() {
        Thread {
            try {
                val socket = DatagramSocket(DISCOVERY_PORT)
                val buffer = ByteArray(1024)
                
                while (!isDestroyed && serverSocket != null) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    
                    val message = String(packet.data, 0, packet.length)
                    if (message == "CHESS_DISCOVER") {
                        val response = "CHESS_HOST:Player".toByteArray()
                        val responsePacket = DatagramPacket(
                            response, response.size,
                            packet.address, packet.port
                        )
                        socket.send(responsePacket)
                    }
                }
                
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
    
    private fun connectToHost(ip: String) {
        binding.tvStatus.text = "正在连接 $ip..."
        
        Thread {
            try {
                clientSocket = Socket(ip, SERVER_PORT)
                
                runOnUiThread {
                    onConnected()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    binding.tvStatus.text = "连接失败: ${e.message}"
                }
            }
        }.start()
    }
    
    private fun onConnected() {
        binding.tvStatus.text = "已连接"
        Toast.makeText(this, "连接成功", Toast.LENGTH_SHORT).show()
        
        // 主机为红方，客户端为黑方
        val playerColor = if (isHost) PieceColor.RED else PieceColor.BLACK
        
        val intent = android.content.Intent(this, GameActivity::class.java).apply {
            putExtra(GameActivity.EXTRA_GAME_MODE, GameMode.WIFI.name)
            putExtra(GameActivity.EXTRA_PLAYER_COLOR, playerColor.name)
        }
        startActivity(intent)
        finish()
    }
    
    private fun updateHostList() {
        hostListAdapter.clear()
        discoveredHosts.forEach { host ->
            hostListAdapter.add("${host.name}\n${host.ip}")
        }
        hostListAdapter.notifyDataSetChanged()
    }
    
    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "127.0.0.1"
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            clientSocket?.close()
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
