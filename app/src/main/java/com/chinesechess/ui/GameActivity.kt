package com.chinesechess.ui

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.chinesechess.R
import com.chinesechess.databinding.ActivityGameBinding
import com.chinesechess.game.GameManager
import com.chinesechess.model.*
import kotlinx.coroutines.launch

/**
 * 游戏界面Activity
 */
class GameActivity : AppCompatActivity(), GameManager.GameListener {
    
    companion object {
        const val EXTRA_GAME_MODE = "game_mode"
        const val EXTRA_PLAYER_COLOR = "player_color"
    }
    
    private lateinit var binding: ActivityGameBinding
    private lateinit var gameManager: GameManager
    private val handler = Handler(Looper.getMainLooper())
    private var isPaused = false
    
    // 时间更新任务
    private val timeUpdateRunnable = object : Runnable {
        override fun run() {
            if (!isPaused) {
                gameManager.updateTime(1000) // 更新1秒
            }
            handler.postDelayed(this, 1000)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 获取游戏模式
        val gameModeStr = intent.getStringExtra(EXTRA_GAME_MODE) ?: GameMode.LOCAL.name
        val gameMode = GameMode.valueOf(gameModeStr)
        
        val playerColorStr = intent.getStringExtra(EXTRA_PLAYER_COLOR)
        val playerColor = playerColorStr?.let { PieceColor.valueOf(it) }
        
        // 初始化游戏管理器
        gameManager = GameManager()
        gameManager.setGameListener(this)
        
        // 设置棋盘
        binding.chessBoard.setGameManager(gameManager)
        binding.chessBoard.setOnBoardClickListener { x, y ->
            onBoardClicked(x, y)
        }
        
        // 设置玩家视角
        if (playerColor != null) {
            binding.chessBoard.setOrientation(playerColor == PieceColor.RED)
        }
        
        // 初始化游戏
        gameManager.initGame(gameMode, playerColor)
        
        // 初始化按钮
        initButtons()
        
        // 开始时间更新
        handler.post(timeUpdateRunnable)
        
        // 观察游戏状态变化
        observeGameState()
    }
    
    private fun initButtons() {
        // 悔棋
        binding.btnUndo.setOnClickListener {
            if (gameManager.undoMove()) {
                binding.chessBoard.invalidate()
                Toast.makeText(this, "已悔棋", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "无法悔棋", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 暂停/继续
        binding.btnPause.setOnClickListener {
            if (isPaused) {
                gameManager.resumeGame()
                binding.btnPause.text = getString(R.string.btn_pause)
                isPaused = false
            } else {
                gameManager.pauseGame()
                binding.btnPause.text = getString(R.string.btn_resume)
                isPaused = true
            }
        }
        
        // 和棋
        binding.btnDraw.setOnClickListener {
            showDrawDialog()
        }
        
        // 认输
        binding.btnSurrender.setOnClickListener {
            showSurrenderDialog()
        }
    }
    
    private fun observeGameState() {
        lifecycleScope.launch {
            gameManager.currentTurn.collect { color ->
                updateTurnDisplay(color)
            }
        }
        
        lifecycleScope.launch {
            gameManager.gameState.collect { state ->
                if (state == GameState.PAUSED) {
                    // 游戏暂停
                }
            }
        }
    }
    
    private fun onBoardClicked(x: Int, y: Int) {
        val piece = gameManager.getPieceAt(x, y)
        
        if (gameManager.selectedPiece.value != null) {
            // 已有选中棋子，尝试移动
            if (gameManager.isValidMovePosition(x, y)) {
                if (gameManager.movePiece(x, y)) {
                    binding.chessBoard.invalidate()
                    
                    // 网络对战时发送移动数据
                    if (gameManager.getGameMode() != GameMode.LOCAL) {
                        // TODO: 发送网络数据
                    }
                }
            } else {
                // 选择其他棋子或取消选择
                if (piece != null && piece.color == gameManager.currentTurn.value) {
                    gameManager.selectPiece(piece)
                } else {
                    gameManager.selectPiece(null)
                }
                binding.chessBoard.invalidate()
            }
        } else {
            // 选择棋子
            if (piece != null && piece.color == gameManager.currentTurn.value) {
                if (gameManager.selectPiece(piece)) {
                    binding.chessBoard.invalidate()
                }
            }
        }
    }
    
    private fun updateTurnDisplay(color: PieceColor) {
        val turnText = if (color == PieceColor.RED) {
            getString(R.string.game_red_turn)
        } else {
            getString(R.string.game_black_turn)
        }
        binding.tvTurn.text = turnText
    }
    
    private fun showDrawDialog() {
        AlertDialog.Builder(this)
            .setTitle("请求和棋")
            .setMessage("确定要请求和棋吗？")
            .setPositiveButton(R.string.dialog_yes) { _, _ ->
                if (gameManager.getGameMode() == GameMode.LOCAL) {
                    gameManager.draw()
                    showGameOverDialog("和棋")
                } else {
                    // TODO: 网络对战需要对方同意
                    Toast.makeText(this, "已向对方发送和棋请求", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.dialog_no, null)
            .show()
    }
    
    private fun showSurrenderDialog() {
        AlertDialog.Builder(this)
            .setTitle("认输")
            .setMessage("确定要认输吗？")
            .setPositiveButton(R.string.dialog_yes) { _, _ ->
                val playerColor = gameManager.getPlayerColor() ?: PieceColor.RED
                gameManager.surrender(playerColor)
            }
            .setNegativeButton(R.string.dialog_no, null)
            .show()
    }
    
    private fun showGameOverDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("游戏结束")
            .setMessage(message)
            .setPositiveButton("再来一局") { _, _ ->
                gameManager.initGame(gameManager.getGameMode(), gameManager.getPlayerColor())
                binding.chessBoard.invalidate()
            }
            .setNegativeButton("返回主菜单") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    // GameListener 回调
    override fun onGameStateChanged(state: GameState) {
        // 游戏状态变化
    }
    
    override fun onTurnChanged(color: PieceColor) {
        runOnUiThread {
            updateTurnDisplay(color)
        }
    }
    
    override fun onPieceMoved(move: Move) {
        runOnUiThread {
            binding.chessBoard.invalidate()
        }
    }
    
    override fun onTimeUpdated(redTime: Long, blackTime: Long, stepTime: Long) {
        runOnUiThread {
            val (timeMode, _, _) = gameManager.getTimeSettings()
            when (timeMode) {
                TimeMode.TOTAL -> {
                    binding.tvRedTime.text = formatTime(redTime)
                    binding.tvBlackTime.text = formatTime(blackTime)
                }
                TimeMode.STEP -> {
                    val currentTurn = gameManager.currentTurn.value
                    if (currentTurn == PieceColor.RED) {
                        binding.tvRedTime.text = formatTime(stepTime)
                    } else {
                        binding.tvBlackTime.text = formatTime(stepTime)
                    }
                }
                TimeMode.UNLIMITED -> {
                    binding.tvRedTime.text = "--:--"
                    binding.tvBlackTime.text = "--:--"
                }
            }
        }
    }
    
    override fun onTimeOut(color: PieceColor) {
        runOnUiThread {
            val winner = if (color == PieceColor.RED) "黑方胜" else "红方胜"
            showGameOverDialog("$winner (超时)")
        }
    }
    
    override fun onCheck(color: PieceColor) {
        runOnUiThread {
            Toast.makeText(this, "将军！", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCheckmate(color: PieceColor) {
        runOnUiThread {
            val winner = if (color == PieceColor.RED) "黑方胜" else "红方胜"
            showGameOverDialog(winner)
        }
    }
    
    override fun onDraw() {
        runOnUiThread {
            showGameOverDialog("和棋")
        }
    }
    
    override fun onInvalidMove() {
        runOnUiThread {
            Toast.makeText(this, "非法移动", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onConnectionLost() {
        runOnUiThread {
            Toast.makeText(this, "连接断开", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun formatTime(timeMs: Long): String {
        val minutes = timeMs / 60000
        val seconds = (timeMs % 60000) / 1000
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timeUpdateRunnable)
        gameManager.cleanup()
    }
}
