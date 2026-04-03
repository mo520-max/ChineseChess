package com.chinesechess.game

import com.chinesechess.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 游戏管理器
 * 管理游戏状态、时间控制、网络通信等
 */
class GameManager {
    
    private val chessEngine = ChessEngine()
    
    // 游戏状态流
    private val _gameState = MutableStateFlow(GameState.IDLE)
    val gameState: StateFlow<GameState> = _gameState
    
    // 当前回合流
    private val _currentTurn = MutableStateFlow(PieceColor.RED)
    val currentTurn: StateFlow<PieceColor> = _currentTurn
    
    // 选中棋子流
    private val _selectedPiece = MutableStateFlow<Piece?>(null)
    val selectedPiece: StateFlow<Piece?> = _selectedPiece
    
    // 合法移动位置流
    private val _validMoves = MutableStateFlow<List<Position>>(emptyList())
    val validMoves: StateFlow<List<Position>> = _validMoves
    
    // 游戏模式
    private var gameMode = GameMode.LOCAL
    
    // 玩家颜色（网络对战时使用）
    private var playerColor: PieceColor? = null
    
    // AI相关
    private var chessAI: ChessAI? = null
    private var isAIEnabled = false
    private var aiColor: PieceColor = PieceColor.BLACK
    
    // 时间控制
    private var timeMode = TimeMode.UNLIMITED
    private var totalTime = 10 * 60 * 1000L // 10分钟
    private var stepTime = 30 * 1000L // 30秒
    
    // 剩余时间
    private var redTimeRemaining = 0L
    private var blackTimeRemaining = 0L
    private var currentStepTimeRemaining = 0L
    
    // 游戏监听器
    private var gameListener: GameListener? = null
    
    // 最后一步移动
    private var lastMove: Move? = null
    
    // AI移动回调
    private var aiMoveCallback: ((Piece, Position) -> Unit)? = null
    
    interface GameListener {
        fun onGameStateChanged(state: GameState)
        fun onTurnChanged(color: PieceColor)
        fun onPieceMoved(move: Move)
        fun onTimeUpdated(redTime: Long, blackTime: Long, stepTime: Long)
        fun onTimeOut(color: PieceColor)
        fun onCheck(color: PieceColor)
        fun onCheckmate(color: PieceColor)
        fun onDraw()
        fun onInvalidMove()
        fun onConnectionLost()
    }
    
    /**
     * 设置游戏监听器
     */
    fun setGameListener(listener: GameListener) {
        gameListener = listener
    }
    
    /**
     * 初始化新游戏
     */
    fun initGame(mode: GameMode = GameMode.LOCAL, playerSide: PieceColor? = null, enableAI: Boolean = false, aiDifficulty: Int = 2) {
        gameMode = mode
        playerColor = playerSide
        isAIEnabled = enableAI && mode == GameMode.LOCAL
        
        chessEngine.initBoard()
        
        // 初始化AI
        if (isAIEnabled) {
            aiColor = if (playerSide == PieceColor.RED) PieceColor.BLACK else PieceColor.RED
            chessAI = ChessAI(chessEngine, aiColor)
            chessAI?.setDifficultyLevel(aiDifficulty)
        } else {
            chessAI = null
        }
        
        _gameState.value = GameState.PLAYING
        _currentTurn.value = PieceColor.RED
        _selectedPiece.value = null
        _validMoves.value = emptyList()
        
        // 初始化时间
        if (timeMode == TimeMode.TOTAL) {
            redTimeRemaining = totalTime
            blackTimeRemaining = totalTime
        } else if (timeMode == TimeMode.STEP) {
            currentStepTimeRemaining = stepTime
        }
        
        lastMove = null
        
        gameListener?.onGameStateChanged(GameState.PLAYING)
        gameListener?.onTurnChanged(PieceColor.RED)
        
        // 如果AI先行（黑方），执行AI移动
        if (isAIEnabled && aiColor == PieceColor.RED) {
            makeAIMove()
        }
    }
    
    /**
     * 设置AI移动回调
     */
    fun setAIMoveCallback(callback: (Piece, Position) -> Unit) {
        aiMoveCallback = callback
    }
    
    /**
     * AI执行移动
     */
    fun makeAIMove() {
        if (!isAIEnabled || _gameState.value != GameState.PLAYING) return
        if (_currentTurn.value != aiColor) return
        
        val ai = chessAI ?: return
        
        // 延迟执行AI移动，让UI有时间更新
        Thread {
            Thread.sleep(500) // 500ms延迟，让移动更自然
            
            val move = ai.getBestMove()
            if (move != null) {
                val (piece, position) = move
                
                // 在主线程执行移动
                aiMoveCallback?.invoke(piece, position)
            }
        }.start()
    }
    
    /**
     * 检查是否是AI的回合
     */
    fun isAITurn(): Boolean {
        return isAIEnabled && _currentTurn.value == aiColor
    }
    
    /**
     * 选择棋子
     */
    fun selectPiece(piece: Piece?): Boolean {
        if (_gameState.value != GameState.PLAYING) return false
        
        // 网络对战时只能操作自己的棋子
        if (gameMode != GameMode.LOCAL && piece?.color != playerColor) return false
        
        // 只能选当前回合的棋子
        if (piece != null && piece.color != _currentTurn.value) return false
        
        _selectedPiece.value = piece
        
        // 计算合法移动
        if (piece != null) {
            _validMoves.value = chessEngine.getValidMoves(piece)
        } else {
            _validMoves.value = emptyList()
        }
        
        return true
    }
    
    /**
     * 移动棋子
     */
    fun movePiece(toX: Int, toY: Int): Boolean {
        if (_gameState.value != GameState.PLAYING) return false
        
        val piece = _selectedPiece.value ?: return false
        
        // 执行移动
        if (!chessEngine.makeMove(piece, toX, toY)) {
            gameListener?.onInvalidMove()
            return false
        }
        
        // 获取最后一步
        val moveHistory = chessEngine.getMoveHistory()
        lastMove = moveHistory.lastOrNull()
        
        // 更新状态
        _currentTurn.value = chessEngine.getCurrentTurn()
        _selectedPiece.value = null
        _validMoves.value = emptyList()
        _gameState.value = chessEngine.getGameState()
        
        // 通知监听器
        lastMove?.let { gameListener?.onPieceMoved(it) }
        gameListener?.onTurnChanged(_currentTurn.value)
        
        // 检查将军
        if (lastMove?.isCheck == true) {
            gameListener?.onCheck(_currentTurn.value)
        }
        
        // 检查游戏结束
        when (_gameState.value) {
            GameState.RED_WIN -> gameListener?.onCheckmate(PieceColor.BLACK)
            GameState.BLACK_WIN -> gameListener?.onCheckmate(PieceColor.RED)
            GameState.DRAW -> gameListener?.onDraw()
            else -> {}
        }
        
        return true
    }
    
    /**
     * 从网络接收移动
     */
    fun receiveNetworkMove(move: Move) {
        if (gameMode == GameMode.LOCAL) return
        
        // 执行对方的移动
        val piece = chessEngine.getPieceAt(move.fromX, move.fromY)
        if (piece != null && chessEngine.makeMove(piece, move.toX, move.toY)) {
            _currentTurn.value = chessEngine.getCurrentTurn()
            _gameState.value = chessEngine.getGameState()
            
            lastMove = move
            gameListener?.onPieceMoved(move)
            gameListener?.onTurnChanged(_currentTurn.value)
            
            if (move.isCheck) {
                gameListener?.onCheck(_currentTurn.value)
            }
        }
    }
    
    /**
     * 悔棋
     */
    fun undoMove(): Boolean {
        if (_gameState.value != GameState.PLAYING && _gameState.value != GameState.PAUSED) return false
        
        // 网络对战不支持悔棋，或者需要对方同意
        if (gameMode != GameMode.LOCAL) return false
        
        if (chessEngine.undoMove()) {
            _currentTurn.value = chessEngine.getCurrentTurn()
            _selectedPiece.value = null
            _validMoves.value = emptyList()
            
            // 更新最后一步
            val moveHistory = chessEngine.getMoveHistory()
            lastMove = moveHistory.lastOrNull()
            
            gameListener?.onTurnChanged(_currentTurn.value)
            return true
        }
        
        return false
    }
    
    /**
     * 暂停游戏
     */
    fun pauseGame() {
        chessEngine.pauseGame()
        _gameState.value = chessEngine.getGameState()
        gameListener?.onGameStateChanged(_gameState.value)
    }
    
    /**
     * 恢复游戏
     */
    fun resumeGame() {
        chessEngine.resumeGame()
        _gameState.value = chessEngine.getGameState()
        gameListener?.onGameStateChanged(_gameState.value)
    }
    
    /**
     * 认输
     */
    fun surrender(color: PieceColor) {
        chessEngine.surrender(color)
        _gameState.value = chessEngine.getGameState()
        
        if (color == PieceColor.RED) {
            gameListener?.onCheckmate(PieceColor.RED)
        } else {
            gameListener?.onCheckmate(PieceColor.BLACK)
        }
    }
    
    /**
     * 和棋
     */
    fun draw() {
        chessEngine.draw()
        _gameState.value = chessEngine.getGameState()
        gameListener?.onDraw()
    }
    
    /**
     * 更新时间
     */
    fun updateTime(deltaTime: Long) {
        if (_gameState.value != GameState.PLAYING) return
        
        when (timeMode) {
            TimeMode.TOTAL -> {
                if (_currentTurn.value == PieceColor.RED) {
                    redTimeRemaining -= deltaTime
                    if (redTimeRemaining <= 0) {
                        redTimeRemaining = 0
                        chessEngine.surrender(PieceColor.RED)
                        _gameState.value = GameState.BLACK_WIN
                        gameListener?.onTimeOut(PieceColor.RED)
                    }
                } else {
                    blackTimeRemaining -= deltaTime
                    if (blackTimeRemaining <= 0) {
                        blackTimeRemaining = 0
                        chessEngine.surrender(PieceColor.BLACK)
                        _gameState.value = GameState.RED_WIN
                        gameListener?.onTimeOut(PieceColor.BLACK)
                    }
                }
                gameListener?.onTimeUpdated(redTimeRemaining, blackTimeRemaining, 0)
            }
            TimeMode.STEP -> {
                currentStepTimeRemaining -= deltaTime
                if (currentStepTimeRemaining <= 0) {
                    currentStepTimeRemaining = 0
                    chessEngine.surrender(_currentTurn.value)
                    _gameState.value = if (_currentTurn.value == PieceColor.RED) 
                        GameState.BLACK_WIN else GameState.RED_WIN
                    gameListener?.onTimeOut(_currentTurn.value)
                }
                gameListener?.onTimeUpdated(0, 0, currentStepTimeRemaining)
            }
            TimeMode.UNLIMITED -> {
                // 无限制模式不更新时间
            }
        }
    }
    
    /**
     * 重置步时
     */
    fun resetStepTime() {
        if (timeMode == TimeMode.STEP) {
            currentStepTimeRemaining = stepTime
        }
    }
    
    /**
     * 设置时间模式
     */
    fun setTimeMode(mode: TimeMode, total: Long = 10 * 60 * 1000L, step: Long = 30 * 1000L) {
        timeMode = mode
        totalTime = total
        stepTime = step
    }
    
    /**
     * 获取棋盘上的棋子
     */
    fun getPieceAt(x: Int, y: Int): Piece? {
        return chessEngine.getPieceAt(x, y)
    }
    
    /**
     * 获取所有棋子
     */
    fun getAllPieces(): List<Piece> {
        return chessEngine.getPieces()
    }
    
    /**
     * 获取移动历史
     */
    fun getMoveHistory(): List<Move> {
        return chessEngine.getMoveHistory()
    }
    
    /**
     * 获取最后一步
     */
    fun getLastMove(): Move? = lastMove
    
    /**
     * 获取游戏模式
     */
    fun getGameMode(): GameMode = gameMode
    
    /**
     * 获取玩家颜色
     */
    fun getPlayerColor(): PieceColor? = playerColor
    
    /**
     * 检查是否是玩家的回合
     */
    fun isPlayerTurn(): Boolean {
        return gameMode == GameMode.LOCAL || playerColor == _currentTurn.value
    }
    
    /**
     * 检查位置是否是合法移动目标
     */
    fun isValidMovePosition(x: Int, y: Int): Boolean {
        return _validMoves.value.any { it.x == x && it.y == y }
    }
    
    /**
     * 获取当前时间设置
     */
    fun getTimeSettings(): Triple<TimeMode, Long, Long> {
        return Triple(timeMode, totalTime, stepTime)
    }
    
    /**
     * 获取剩余时间
     */
    fun getRemainingTime(): Triple<Long, Long, Long> {
        return Triple(redTimeRemaining, blackTimeRemaining, currentStepTimeRemaining)
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        gameListener = null
    }
}
