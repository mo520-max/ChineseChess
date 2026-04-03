package com.chinesechess.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.chinesechess.databinding.ActivitySettingsBinding
import com.chinesechess.model.TimeMode

/**
 * 设置Activity
 */
class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initViews()
        loadSettings()
    }
    
    private fun initViews() {
        // 时间模式选择
        val timeModes = arrayOf("无限制", "总时间", "步时")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, timeModes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTimeMode.adapter = adapter
        
        // 保存按钮
        binding.btnSave.setOnClickListener {
            saveSettings()
            finish()
        }
        
        // 返回按钮
        binding.btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun loadSettings() {
        val prefs = getSharedPreferences("chess_settings", MODE_PRIVATE)
        
        // 加载时间模式
        val timeMode = prefs.getString("time_mode", TimeMode.UNLIMITED.name)
        val modeIndex = when (TimeMode.valueOf(timeMode!!)) {
            TimeMode.UNLIMITED -> 0
            TimeMode.TOTAL -> 1
            TimeMode.STEP -> 2
        }
        binding.spinnerTimeMode.setSelection(modeIndex)
        
        // 加载总时间
        val totalTime = prefs.getInt("total_time", 10)
        binding.etTotalTime.setText(totalTime.toString())
        
        // 加载步时
        val stepTime = prefs.getInt("step_time", 30)
        binding.etStepTime.setText(stepTime.toString())
        
        // 加载音效设置
        val soundEnabled = prefs.getBoolean("sound_enabled", true)
        binding.switchSound.isChecked = soundEnabled
        
        // 加载震动设置
        val vibrationEnabled = prefs.getBoolean("vibration_enabled", true)
        binding.switchVibration.isChecked = vibrationEnabled
    }
    
    private fun saveSettings() {
        val prefs = getSharedPreferences("chess_settings", MODE_PRIVATE)
        val editor = prefs.edit()
        
        // 保存时间模式
        val timeMode = when (binding.spinnerTimeMode.selectedItemPosition) {
            0 -> TimeMode.UNLIMITED.name
            1 -> TimeMode.TOTAL.name
            2 -> TimeMode.STEP.name
            else -> TimeMode.UNLIMITED.name
        }
        editor.putString("time_mode", timeMode)
        
        // 保存总时间
        val totalTime = binding.etTotalTime.text.toString().toIntOrNull() ?: 10
        editor.putInt("total_time", totalTime)
        
        // 保存步时
        val stepTime = binding.etStepTime.text.toString().toIntOrNull() ?: 30
        editor.putInt("step_time", stepTime)
        
        // 保存音效设置
        editor.putBoolean("sound_enabled", binding.switchSound.isChecked)
        
        // 保存震动设置
        editor.putBoolean("vibration_enabled", binding.switchVibration.isChecked)
        
        editor.apply()
    }
}
