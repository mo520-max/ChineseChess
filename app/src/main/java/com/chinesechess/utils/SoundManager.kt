package com.chinesechess.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import com.chinesechess.R

/**
 * 音效和震动管理器
 */
class SoundManager(private val context: Context) {

    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<Int, Int>()
    
    // 音效开关
    var soundEnabled = true
    var vibrationEnabled = true
    
    // 震动器
    private var vibrator: Vibrator? = null
    
    // 音效ID
    companion object {
        const val SOUND_MOVE = 1
        const val SOUND_CAPTURE = 2
        const val SOUND_CHECK = 3
        const val SOUND_CHECKMATE = 4
        const val SOUND_WIN = 5
        const val SOUND_LOSE = 6
        const val SOUND_DRAW = 7
        const val SOUND_BUTTON_CLICK = 8
        const val SOUND_GAME_START = 9
        const val SOUND_UNDO = 10
        const val SOUND_INVALID_MOVE = 11
    }
    
    init {
        initSoundPool()
        initVibrator()
        loadSounds()
    }
    
    /**
     * 初始化SoundPool
     */
    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()
        
        soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
            // 音效加载完成
        }
    }
    
    /**
     * 初始化震动器
     */
    private fun initVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    /**
     * 加载音效
     */
    private fun loadSounds() {
        // 由于我们没有实际的音效文件，这里使用系统音效或留空
        // 实际项目中应该加载自定义音效文件
        
        // 示例：加载音效文件
        // soundMap[SOUND_MOVE] = soundPool?.load(context, R.raw.sound_move, 1) ?: 0
        // soundMap[SOUND_CAPTURE] = soundPool?.load(context, R.raw.sound_capture, 1) ?: 0
        // ...
    }
    
    /**
     * 播放音效
     */
    fun playSound(soundId: Int) {
        if (!soundEnabled) return
        
        val soundResId = soundMap[soundId]
        if (soundResId != null && soundResId != 0) {
            soundPool?.play(soundResId, 1.0f, 1.0f, 1, 0, 1.0f)
        }
    }
    
    /**
     * 播放移动音效
     */
    fun playMoveSound() {
        playSound(SOUND_MOVE)
        vibrate(VIBRATION_MOVE)
    }
    
    /**
     * 播放吃子音效
     */
    fun playCaptureSound() {
        playSound(SOUND_CAPTURE)
        vibrate(VIBRATION_CAPTURE)
    }
    
    /**
     * 播放将军音效
     */
    fun playCheckSound() {
        playSound(SOUND_CHECK)
        vibrate(VIBRATION_CHECK)
    }
    
    /**
     * 播放将死音效
     */
    fun playCheckmateSound() {
        playSound(SOUND_CHECKMATE)
        vibrate(VIBRATION_CHECKMATE)
    }
    
    /**
     * 播放胜利音效
     */
    fun playWinSound() {
        playSound(SOUND_WIN)
        vibrate(VIBRATION_WIN)
    }
    
    /**
     * 播放失败音效
     */
    fun playLoseSound() {
        playSound(SOUND_LOSE)
        vibrate(VIBRATION_LOSE)
    }
    
    /**
     * 播放和棋音效
     */
    fun playDrawSound() {
        playSound(SOUND_DRAW)
        vibrate(VIBRATION_DRAW)
    }
    
    /**
     * 播放按钮点击音效
     */
    fun playButtonClickSound() {
        playSound(SOUND_BUTTON_CLICK)
        vibrate(VIBRATION_BUTTON)
    }
    
    /**
     * 播放游戏开始音效
     */
    fun playGameStartSound() {
        playSound(SOUND_GAME_START)
        vibrate(VIBRATION_GAME_START)
    }
    
    /**
     * 播放悔棋音效
     */
    fun playUndoSound() {
        playSound(SOUND_UNDO)
        vibrate(VIBRATION_UNDO)
    }
    
    /**
     * 播放非法移动音效
     */
    fun playInvalidMoveSound() {
        playSound(SOUND_INVALID_MOVE)
        vibrate(VIBRATION_INVALID)
    }
    
    /**
     * 震动模式
     */
    companion object VibrationPatterns {
        // 移动震动 - 轻微短震
        val VIBRATION_MOVE = longArrayOf(0, 30)
        
        // 吃子震动 - 中等震动
        val VIBRATION_CAPTURE = longArrayOf(0, 50, 30, 50)
        
        // 将军震动 - 较强震动
        val VIBRATION_CHECK = longArrayOf(0, 100, 50, 100, 50, 100)
        
        // 将死震动 - 长震动
        val VIBRATION_CHECKMATE = longArrayOf(0, 300, 100, 300)
        
        // 胜利震动 - 欢快震动
        val VIBRATION_WIN = longArrayOf(0, 100, 50, 100, 50, 100, 50, 200)
        
        // 失败震动 - 沉重震动
        val VIBRATION_LOSE = longArrayOf(0, 200, 100, 200)
        
        // 和棋震动 - 平稳震动
        val VIBRATION_DRAW = longArrayOf(0, 100, 50, 100)
        
        // 按钮震动 - 轻微震动
        val VIBRATION_BUTTON = longArrayOf(0, 20)
        
        // 游戏开始震动
        val VIBRATION_GAME_START = longArrayOf(0, 50, 30, 50, 30, 50)
        
        // 悔棋震动
        val VIBRATION_UNDO = longArrayOf(0, 40)
        
        // 非法移动震动 - 快速短震
        val VIBRATION_INVALID = longArrayOf(0, 20, 10, 20)
    }
    
    /**
     * 执行震动
     */
    fun vibrate(pattern: LongArray) {
        if (!vibrationEnabled) return
        
        vibrator?.let { vib ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createWaveform(pattern, -1)
                vib.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(pattern, -1)
            }
        }
    }
    
    /**
     * 简单震动
     */
    fun vibrate(milliseconds: Long) {
        if (!vibrationEnabled) return
        
        vibrator?.let { vib ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE)
                vib.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(milliseconds)
            }
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        soundPool?.release()
        soundPool = null
        soundMap.clear()
    }
}
