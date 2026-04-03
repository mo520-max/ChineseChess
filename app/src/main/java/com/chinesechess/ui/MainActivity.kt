package com.chinesechess.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.chinesechess.databinding.ActivityMainBinding
import com.chinesechess.model.GameMode

/**
 * 主界面Activity
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initViews()
    }
    
    private fun initViews() {
        // 蓝牙对战
        binding.btnBluetooth.setOnClickListener {
            val intent = Intent(this, BluetoothActivity::class.java)
            startActivity(intent)
        }
        
        // 局域网对战
        binding.btnWifi.setOnClickListener {
            val intent = Intent(this, WifiActivity::class.java)
            startActivity(intent)
        }
        
        // 单机练习
        binding.btnLocal.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java).apply {
                putExtra(GameActivity.EXTRA_GAME_MODE, GameMode.LOCAL.name)
            }
            startActivity(intent)
        }
        
        // 设置
        binding.btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
}
