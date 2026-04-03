package com.chinesechess.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.chinesechess.databinding.ActivityBluetoothBinding
import com.chinesechess.model.GameMode
import com.chinesechess.model.PieceColor
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

/**
 * 蓝牙对战Activity
 */
class BluetoothActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityBluetoothBinding
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var connectedSocket: BluetoothSocket? = null
    private var serverSocket: BluetoothServerSocket? = null
    private var isHost = false
    
    private val discoveredDevices = mutableListOf<BluetoothDevice>()
    private lateinit var deviceListAdapter: ArrayAdapter<String>
    
    private val APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_PERMISSIONS = 2
    
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (!discoveredDevices.contains(it)) {
                            discoveredDevices.add(it)
                            updateDeviceList()
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    binding.tvStatus.text = "搜索完成，找到 ${discoveredDevices.size} 个设备"
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBluetoothBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        initViews()
        checkPermissions()
    }
    
    private fun initViews() {
        deviceListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        binding.lvDevices.adapter = deviceListAdapter
        
        binding.btnHost.setOnClickListener {
            startAsHost()
        }
        
        binding.btnClient.setOnClickListener {
            startAsClient()
        }
        
        binding.btnScan.setOnClickListener {
            startDiscovery()
        }
        
        binding.lvDevices.setOnItemClickListener { _, _, position, _ ->
            if (position < discoveredDevices.size) {
                connectToDevice(discoveredDevices[position])
            }
        }
    }
    
    private fun checkPermissions() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), REQUEST_PERMISSIONS)
        } else {
            checkBluetoothEnabled()
        }
    }
    
    private fun checkBluetoothEnabled() {
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }
    
    private fun startAsHost() {
        isHost = true
        binding.tvStatus.text = "等待客户端连接..."
        
        Thread {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                    != PackageManager.PERMISSION_GRANTED) {
                    return@Thread
                }
                
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord("ChineseChess", APP_UUID)
                connectedSocket = serverSocket?.accept()
                
                runOnUiThread {
                    onConnected()
                }
            } catch (e: IOException) {
                runOnUiThread {
                    binding.tvStatus.text = "连接失败: ${e.message}"
                }
            }
        }.start()
    }
    
    private fun startAsClient() {
        isHost = false
        binding.tvStatus.text = "请选择要连接的设备"
        startDiscovery()
    }
    
    private fun startDiscovery() {
        discoveredDevices.clear()
        updateDeviceList()
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) 
            != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter?.cancelDiscovery()
        }
        
        bluetoothAdapter?.startDiscovery()
        binding.tvStatus.text = "正在搜索设备..."
        
        // 注册广播接收器
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        registerReceiver(receiver, filter)
    }
    
    private fun connectToDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) 
            != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        bluetoothAdapter?.cancelDiscovery()
        binding.tvStatus.text = "正在连接 ${device.name}..."
        
        Thread {
            try {
                connectedSocket = device.createRfcommSocketToServiceRecord(APP_UUID)
                connectedSocket?.connect()
                
                runOnUiThread {
                    onConnected()
                }
            } catch (e: IOException) {
                runOnUiThread {
                    binding.tvStatus.text = "连接失败: ${e.message}"
                }
            }
        }.start()
    }
    
    private fun onConnected() {
        binding.tvStatus.text = "已连接"
        Toast.makeText(this, "蓝牙连接成功", Toast.LENGTH_SHORT).show()
        
        // 主机为红方，从机为黑方
        val playerColor = if (isHost) PieceColor.RED else PieceColor.BLACK
        
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra(GameActivity.EXTRA_GAME_MODE, GameMode.BLUETOOTH.name)
            putExtra(GameActivity.EXTRA_PLAYER_COLOR, playerColor.name)
        }
        startActivity(intent)
        finish()
    }
    
    private fun updateDeviceList() {
        deviceListAdapter.clear()
        discoveredDevices.forEach { device ->
            val name = if (ActivityCompat.checkSelfPermission(this, 
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                device.name ?: "未知设备"
            } else {
                "未知设备"
            }
            deviceListAdapter.add("$name\n${device.address}")
        }
        deviceListAdapter.notifyDataSetChanged()
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                checkBluetoothEnabled()
            } else {
                Toast.makeText(this, "需要蓝牙权限才能使用此功能", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, "需要开启蓝牙才能使用此功能", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
        
        try {
            connectedSocket?.close()
            serverSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
