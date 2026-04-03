package com.chinesechess.game

import com.chinesechess.model.*

/**
 * 象棋规则引擎
 * 负责验证移动合法性、判断游戏状态等
 */
class ChessEngine {
    
    private val board = Array(9) { arrayOfNulls<Piece>(10) }
    private val pieces = mutableListOf<Piece>()
    private val moveHistory = mutableListOf<Move>()
    private var currentTurn = PieceColor.RED
    private var gameState = GameState.IDLE
    
    /**
     * 初始化棋盘
     */
    fun initBoard() {
        pieces.clear()
        pieces.addAll(Piece.createInitialPieces())
        updateBoard()
        currentTurn = PieceColor.RED
        gameState = GameState.PLAYING
        moveHistory.clear()
    }
    
    /**
     * 更新棋盘数组
     */
    private fun updateBoard() {
        // 清空棋盘
        for (x in 0..8) {
            for (y in 0..9) {
                board[x][y] = null
            }
        }
        // 放置棋子
        pieces.filter { it.isAlive }.forEach { piece ->
            board[piece.x][piece.y] = piece
        }
    }
    
    /**
     * 获取指定位置的棋子
     */
    fun getPieceAt(x: Int, y: Int): Piece? {
        if (!Position(x, y).isValid()) return null
        return board[x][y]
    }
    
    /**
     * 获取所有棋子
     */
    fun getPieces(): List<Piece> = pieces.toList()
    
    /**
     * 获取当前回合
     */
    fun getCurrentTurn(): PieceColor = currentTurn
    
    /**
     * 获取游戏状态
     */
    fun getGameState(): GameState = gameState
    
    /**
     * 获取移动历史
     */
    fun getMoveHistory(): List<Move> = moveHistory.toList()
    
    /**
     * 验证移动是否合法
     */
    fun isValidMove(piece: Piece, toX: Int, toY: Int): Boolean {
        // 检查目标位置是否有效
        if (!Position(toX, toY).isValid()) return false
        
        // 检查是否是当前回合的棋子
        if (piece.color != currentTurn) return false
        
        // 检查目标位置是否有己方棋子
        val targetPiece = getPieceAt(toX, toY)
        if (targetPiece != null && targetPiece.color == piece.color) return false
        
        // 根据棋子类型验证移动规则
        return when (piece.type) {
            PieceType.KING -> isValidKingMove(piece, toX, toY)
            PieceType.ADVISOR -> isValidAdvisorMove(piece, toX, toY)
            PieceType.ELEPHANT -> isValidElephantMove(piece, toX, toY)
            PieceType.HORSE -> isValidHorseMove(piece, toX, toY)
            PieceType.CHARIOT -> isValidChariotMove(piece, toX, toY)
            PieceType.CANNON -> isValidCannonMove(piece, toX, toY)
            PieceType.SOLDIER -> isValidSoldierMove(piece, toX, toY)
        }
    }
    
    /**
     * 将/帅的移动规则
     */
    private fun isValidKingMove(piece: Piece, toX: Int, toY: Int): Boolean {
        // 必须在九宫内
        if (toX < 3 || toX > 5) return false
        if (piece.color == PieceColor.RED) {
            if (toY < 7 || toY > 9) return false
        } else {
            if (toY < 0 || toY > 2) return false
        }
        
        // 只能移动一格
        val dx = kotlin.math.abs(toX - piece.x)
        val dy = kotlin.math.abs(toY - piece.y)
        
        // 特殊情况：将帅对面
        if (dx == 0) {
            // 检查是否可以直接吃掉对方的将/帅
            val targetPiece = getPieceAt(toX, toY)
            if (targetPiece?.type == PieceType.KING && targetPiece.color != piece.color) {
                // 检查中间是否有棋子
                val minY = kotlin.math.min(piece.y, toY)
                val maxY = kotlin.math.max(piece.y, toY)
                for (y in minY + 1 until maxY) {
                    if (getPieceAt(toX, y) != null) return false
                }
                return true
            }
        }
        
        return (dx == 1 && dy == 0) || (dx == 0 && dy == 1)
    }
    
    /**
     * 士/仕的移动规则
     */
    private fun isValidAdvisorMove(piece: Piece, toX: Int, toY: Int): Boolean {
        // 必须在九宫内
        if (toX < 3 || toX > 5) return false
        if (piece.color == PieceColor.RED) {
            if (toY < 7 || toY > 9) return false
        } else {
            if (toY < 0 || toY > 2) return false
        }
        
        // 必须沿对角线移动一格
        val dx = kotlin.math.abs(toX - piece.x)
        val dy = kotlin.math.abs(toY - piece.y)
        return dx == 1 && dy == 1
    }
    
    /**
     * 象/相的移动规则
     */
    private fun isValidElephantMove(piece: Piece, toX: Int, toY: Int): Boolean {
        // 不能过河
        if (piece.color == PieceColor.RED && toY < 5) return false
        if (piece.color == PieceColor.BLACK && toY > 4) return false
        
        // 必须沿对角线移动两格
        val dx = kotlin.math.abs(toX - piece.x)
        val dy = kotlin.math.abs(toY - piece.y)
        if (dx != 2 || dy != 2) return false
        
        // 检查象眼是否被堵
        val eyeX = (piece.x + toX) / 2
        val eyeY = (piece.y + toY) / 2
        return getPieceAt(eyeX, eyeY) == null
    }
    
    /**
     * 马的移动规则
     */
    private fun isValidHorseMove(piece: Piece, toX: Int, toY: Int): Boolean {
        val dx = kotlin.math.abs(toX - piece.x)
        val dy = kotlin.math.abs(toY - piece.y)
        
        // 必须是日字形
        if (!((dx == 2 && dy == 1) || (dx == 1 && dy == 2))) return false
        
        // 检查马腿
        val legX = if (dx == 2) (piece.x + toX) / 2 else piece.x
        val legY = if (dy == 2) (piece.y + toY) / 2 else piece.y
        return getPieceAt(legX, legY) == null
    }
    
    /**
     * 车的移动规则
     */
    private fun isValidChariotMove(piece: Piece, toX: Int, toY: Int): Boolean {
        val dx = toX - piece.x
        val dy = toY - piece.y
        
        // 必须直线移动
        if (dx != 0 && dy != 0) return false
        
        // 检查路径上是否有棋子
        if (dx == 0) {
            // 垂直移动
            val minY = kotlin.math.min(piece.y, toY)
            val maxY = kotlin.math.max(piece.y, toY)
            for (y in minY + 1 until maxY) {
                if (getPieceAt(piece.x, y) != null) return false
            }
        } else {
            // 水平移动
            val minX = kotlin.math.min(piece.x, toX)
            val maxX = kotlin.math.max(piece.x, toX)
            for (x in minX + 1 until maxX) {
                if (getPieceAt(x, piece.y) != null) return false
            }
        }
        
        return true
    }
    
    /**
     * 炮的移动规则
     */
    private fun isValidCannonMove(piece: Piece, toX: Int, toY: Int): Boolean {
        val dx = toX - piece.x
        val dy = toY - piece.y
        
        // 必须直线移动
        if (dx != 0 && dy != 0) return false
        
        val targetPiece = getPieceAt(toX, toY)
        var pieceCount = 0
        
        // 计算路径上的棋子数
        if (dx == 0) {
            // 垂直移动
            val minY = kotlin.math.min(piece.y, toY)
            val maxY = kotlin.math.max(piece.y, toY)
            for (y in minY + 1 until maxY) {
                if (getPieceAt(piece.x, y) != null) pieceCount++
            }
        } else {
            // 水平移动
            val minX = kotlin.math.min(piece.x, toX)
            val maxX = kotlin.math.max(piece.x, toX)
            for (x in minX + 1 until maxX) {
                if (getPieceAt(x, piece.y) != null) pieceCount++
            }
        }
        
        // 不吃子时需要0个棋子，吃子时需要1个棋子
        return if (targetPiece == null) {
            pieceCount == 0
        } else {
            pieceCount == 1
        }
    }
    
    /**
     * 兵/卒的移动规则
     */
    private fun isValidSoldierMove(piece: Piece, toX: Int, toY: Int): Boolean {
        val dx = kotlin.math.abs(toX - piece.x)
        val dy = toY - piece.y
        
        // 只能移动一格
        if (dx + kotlin.math.abs(dy) != 1) return false
        
        // 根据颜色判断方向
        return if (piece.color == PieceColor.RED) {
            // 红方向上移动（y减小）
            if (piece.y >= 5) {
                // 未过河只能前进
                dy == -1 && dx == 0
            } else {
                // 过河后可以左右移动
                dy == -1 || (dy == 0 && dx == 1)
            }
        } else {
            // 黑方向下移动（y增大）
            if (piece.y <= 4) {
                // 未过河只能前进
                dy == 1 && dx == 0
            } else {
                // 过河后可以左右移动
                dy == 1 || (dy == 0 && dx == 1)
            }
        }
    }
    
    /**
     * 执行移动
     */
    fun makeMove(piece: Piece, toX: Int, toY: Int): Boolean {
        if (!isValidMove(piece, toX, toY)) return false
        
        // 检查移动后是否会被将军
        if (wouldBeInCheckAfterMove(piece, toX, toY)) return false
        
        val capturedPiece = getPieceAt(toX, toY)
        capturedPiece?.isAlive = false
        
        val fromX = piece.x
        val fromY = piece.y
        
        // 移动棋子
        piece.x = toX
        piece.y = toY
        
        updateBoard()
        
        // 检查是否将军
        val isCheck = isInCheck(currentTurn.opposite())
        
        // 记录移动
        val move = Move(piece, fromX, fromY, toX, toY, capturedPiece, isCheck)
        moveHistory.add(move)
        
        // 切换回合
        currentTurn = currentTurn.opposite()
        
        // 检查游戏状态
        checkGameState()
        
        return true
    }
    
    /**
     * 检查移动后是否会被将军
     */
    private fun wouldBeInCheckAfterMove(piece: Piece, toX: Int, toY: Int): Boolean {
        // 保存原状态
        val originalX = piece.x
        val originalY = piece.y
        val capturedPiece = getPieceAt(toX, toY)
        val wasAlive = capturedPiece?.isAlive ?: true
        
        // 模拟移动
        piece.x = toX
        piece.y = toY
        capturedPiece?.isAlive = false
        updateBoard()
        
        // 检查是否被将军
        val inCheck = isInCheck(currentTurn)
        
        // 恢复状态
        piece.x = originalX
        piece.y = originalY
        capturedPiece?.isAlive = wasAlive
        updateBoard()
        
        return inCheck
    }
    
    /**
     * 检查指定颜色是否被将军
     */
    fun isInCheck(color: PieceColor): Boolean {
        val king = pieces.find { it.type == PieceType.KING && it.color == color && it.isAlive }
            ?: return false
        
        // 检查对方所有棋子是否能吃掉王
        return pieces.filter { it.color != color && it.isAlive }.any { enemyPiece ->
            canCaptureWithoutCheckRule(enemyPiece, king.x, king.y)
        }
    }
    
    /**
     * 检查棋子是否能移动到目标位置（不检查将军规则）
     */
    private fun canCaptureWithoutCheckRule(piece: Piece, toX: Int, toY: Int): Boolean {
        return when (piece.type) {
            PieceType.KING -> isValidKingMove(piece, toX, toY)
            PieceType.ADVISOR -> isValidAdvisorMove(piece, toX, toY)
            PieceType.ELEPHANT -> isValidElephantMove(piece, toX, toY)
            PieceType.HORSE -> isValidHorseMove(piece, toX, toY)
            PieceType.CHARIOT -> isValidChariotMove(piece, toX, toY)
            PieceType.CANNON -> isValidCannonMove(piece, toX, toY)
            PieceType.SOLDIER -> isValidSoldierMove(piece, toX, toY)
        }
    }
    
    /**
     * 检查游戏状态
     */
    private fun checkGameState() {
        // 检查是否将死
        if (isCheckmate(currentTurn)) {
            gameState = if (currentTurn == PieceColor.RED) GameState.BLACK_WIN else GameState.RED_WIN
            return
        }
        
        // 检查是否被将军
        if (isInCheck(currentTurn)) {
            // 将军状态，但游戏继续
        }
    }
    
    /**
     * 检查是否将死
     */
    private fun isCheckmate(color: PieceColor): Boolean {
        if (!isInCheck(color)) return false
        
        // 检查是否有任何合法移动可以解除将军
        val colorPieces = pieces.filter { it.color == color && it.isAlive }
        
        for (piece in colorPieces) {
            for (x in 0..8) {
                for (y in 0..9) {
                    if (isValidMove(piece, x, y)) {
                        return false
                    }
                }
            }
        }
        
        return true
    }
    
    /**
     * 悔棋
     */
    fun undoMove(): Boolean {
        if (moveHistory.isEmpty()) return false
        
        val lastMove = moveHistory.removeAt(moveHistory.size - 1)
        
        // 恢复棋子位置
        lastMove.piece.x = lastMove.fromX
        lastMove.piece.y = lastMove.fromY
        
        // 恢复被吃的棋子
        lastMove.capturedPiece?.isAlive = true
        
        updateBoard()
        
        // 切换回合
        currentTurn = currentTurn.opposite()
        
        // 恢复游戏状态
        gameState = GameState.PLAYING
        
        return true
    }
    
    /**
     * 获取所有合法移动位置
     */
    fun getValidMoves(piece: Piece): List<Position> {
        val moves = mutableListOf<Position>()
        
        for (x in 0..8) {
            for (y in 0..9) {
                if (isValidMove(piece, x, y)) {
                    moves.add(Position(x, y))
                }
            }
        }
        
        return moves
    }
    
    /**
     * 设置游戏状态
     */
    fun setGameState(state: GameState) {
        gameState = state
    }
    
    /**
     * 暂停游戏
     */
    fun pauseGame() {
        if (gameState == GameState.PLAYING) {
            gameState = GameState.PAUSED
        }
    }
    
    /**
     * 恢复游戏
     */
    fun resumeGame() {
        if (gameState == GameState.PAUSED) {
            gameState = GameState.PLAYING
        }
    }
    
    /**
     * 认输
     */
    fun surrender(color: PieceColor) {
        gameState = if (color == PieceColor.RED) GameState.BLACK_WIN else GameState.RED_WIN
    }
    
    /**
     * 和棋
     */
    fun draw() {
        gameState = GameState.DRAW
    }
    
    /**
     * 复制当前棋盘状态
     */
    fun copyState(): ChessEngine {
        val newEngine = ChessEngine()
        newEngine.pieces.clear()
        newEngine.pieces.addAll(pieces.map { it.copy() })
        newEngine.updateBoard()
        newEngine.currentTurn = currentTurn
        newEngine.gameState = gameState
        return newEngine
    }
}
