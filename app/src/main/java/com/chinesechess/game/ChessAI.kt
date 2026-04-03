package com.chinesechess.game

import com.chinesechess.model.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * 象棋AI引擎
 * 实现基于Minimax算法和Alpha-Beta剪枝的AI对手
 */
class ChessAI(private val engine: ChessEngine, private val aiColor: PieceColor) {

    // 棋子基础分值
    private val pieceValues = mapOf(
        PieceType.KING to 10000,
        PieceType.CHARIOT to 900,
        PieceType.HORSE to 400,
        PieceType.CANNON to 450,
        PieceType.ELEPHANT to 200,
        PieceType.ADVISOR to 200,
        PieceType.SOLDIER to 100
    )

    // 位置加成表（简化版）
    private val positionBonus = mutableMapOf<PieceType, Array<IntArray>>()

    // 搜索深度
    private var searchDepth = 3

    // 随机因子（增加变化性）
    private val randomFactor = 10

    init {
        initPositionBonus()
    }

    /**
     * 初始化位置加成表
     */
    private fun initPositionBonus() {
        // 兵/卒的位置加成（鼓励过河和前进）
        positionBonus[PieceType.SOLDIER] = Array(10) { y ->
            IntArray(9) { x ->
                when {
                    aiColor == PieceColor.RED -> {
                        // 红方从下往上攻
                        when (y) {
                            in 0..2 -> 80  // 对方底线附近
                            in 3..4 -> 60  // 对方半场
                            5 -> 40        // 河界
                            else -> 20     // 己方半场
                        }
                    }
                    else -> {
                        // 黑方从上往下攻
                        when (y) {
                            in 7..9 -> 80  // 对方底线附近
                            in 5..6 -> 60  // 对方半场
                            4 -> 40        // 河界
                            else -> 20     // 己方半场
                        }
                    }
                }
            }
        }

        // 车的位置加成（鼓励控制中路和灵活移动）
        positionBonus[PieceType.CHARIOT] = Array(10) { y ->
            IntArray(9) { x ->
                when {
                    x == 4 -> 30  // 中路
                    y in 2..7 -> 20  // 灵活区域
                    else -> 10
                }
            }
        }

        // 马的位置加成（鼓励活跃位置）
        positionBonus[PieceType.HORSE] = Array(10) { y ->
            IntArray(9) { x ->
                when {
                    x in 2..6 && y in 2..7 -> 25  // 中心区域
                    else -> 15
                }
            }
        }

        // 炮的位置加成
        positionBonus[PieceType.CANNON] = Array(10) { y ->
            IntArray(9) { x ->
                when {
                    x == 4 -> 25  // 中路
                    y in 2..7 -> 20
                    else -> 10
                }
            }
        }
    }

    /**
     * 设置搜索深度
     */
    fun setSearchDepth(depth: Int) {
        searchDepth = depth.coerceIn(2, 5)
    }

    /**
     * 获取AI的最佳移动
     */
    fun getBestMove(): Pair<Piece, Position>? {
        val pieces = engine.getPieces().filter { it.color == aiColor && it.isAlive }
        if (pieces.isEmpty()) return null

        var bestMove: Pair<Piece, Position>? = null
        var bestScore = Int.MIN_VALUE

        // 获取所有可能的移动
        val allMoves = mutableListOf<Triple<Piece, Int, Int>>()
        for (piece in pieces) {
            val moves = engine.getValidMoves(piece)
            for (move in moves) {
                allMoves.add(Triple(piece, move.x, move.y))
            }
        }

        // 如果没有合法移动，返回null
        if (allMoves.isEmpty()) return null

        // 对移动进行排序（优化Alpha-Beta剪枝）
        // 优先尝试吃子移动
        val sortedMoves = allMoves.sortedByDescending { (_, toX, toY) ->
            val capturedPiece = engine.getPieceAt(toX, toY)
            if (capturedPiece != null) pieceValues[capturedPiece.type] ?: 0 else 0
        }

        // 使用Alpha-Beta剪枝搜索
        for ((piece, toX, toY) in sortedMoves) {
            // 模拟移动
            val capturedPiece = engine.getPieceAt(toX, toY)
            val originalX = piece.x
            val originalY = piece.y

            // 执行移动
            piece.x = toX
            piece.y = toY
            capturedPiece?.isAlive = false

            // 评估这个移动
            val score = alphaBeta(
                depth = searchDepth - 1,
                alpha = Int.MIN_VALUE,
                beta = Int.MAX_VALUE,
                isMaximizing = false
            )

            // 恢复棋盘
            piece.x = originalX
            piece.y = originalY
            capturedPiece?.isAlive = true

            // 添加随机因子增加变化性
            val finalScore = score + Random.nextInt(-randomFactor, randomFactor + 1)

            if (finalScore > bestScore) {
                bestScore = finalScore
                bestMove = Pair(piece, Position(toX, toY))
            }
        }

        return bestMove
    }

    /**
     * Alpha-Beta剪枝算法
     */
    private fun alphaBeta(depth: Int, alpha: Int, beta: Int, isMaximizing: Boolean): Int {
        // 到达搜索深度或游戏结束
        if (depth == 0) {
            return evaluateBoard()
        }

        // 检查是否被将死
        val currentColor = if (isMaximizing) aiColor else aiColor.opposite()
        if (engine.isInCheck(currentColor)) {
            // 检查是否有合法移动
            val pieces = engine.getPieces().filter { it.color == currentColor && it.isAlive }
            var hasValidMove = false
            for (piece in pieces) {
                val moves = engine.getValidMoves(piece)
                if (moves.isNotEmpty()) {
                    hasValidMove = true
                    break
                }
            }
            if (!hasValidMove) {
                // 将死
                return if (isMaximizing) Int.MIN_VALUE + 1000 else Int.MAX_VALUE - 1000
            }
        }

        var alphaVar = alpha
        var betaVar = beta

        val pieces = engine.getPieces().filter {
            it.color == (if (isMaximizing) aiColor else aiColor.opposite()) && it.isAlive
        }

        // 获取所有可能的移动
        val allMoves = mutableListOf<Triple<Piece, Int, Int>>()
        for (piece in pieces) {
            val moves = engine.getValidMoves(piece)
            for (move in moves) {
                allMoves.add(Triple(piece, move.x, move.y))
            }
        }

        if (allMoves.isEmpty()) {
            // 无子可动，可能是和棋或被将死
            return if (engine.isInCheck(currentColor)) {
                if (isMaximizing) Int.MIN_VALUE + 1000 else Int.MAX_VALUE - 1000
            } else {
                0 // 和棋
            }
        }

        // 优先尝试吃子移动（优化剪枝）
        val sortedMoves = allMoves.sortedByDescending { (_, toX, toY) ->
            val capturedPiece = engine.getPieceAt(toX, toY)
            if (capturedPiece != null) pieceValues[capturedPiece.type] ?: 0 else 0
        }

        if (isMaximizing) {
            var maxScore = Int.MIN_VALUE
            for ((piece, toX, toY) in sortedMoves) {
                val capturedPiece = engine.getPieceAt(toX, toY)
                val originalX = piece.x
                val originalY = piece.y

                // 执行移动
                piece.x = toX
                piece.y = toY
                capturedPiece?.isAlive = false

                val score = alphaBeta(depth - 1, alphaVar, betaVar, false)

                // 恢复
                piece.x = originalX
                piece.y = originalY
                capturedPiece?.isAlive = true

                maxScore = max(maxScore, score)
                alphaVar = max(alphaVar, score)

                if (betaVar <= alphaVar) break // Beta剪枝
            }
            return maxScore
        } else {
            var minScore = Int.MAX_VALUE
            for ((piece, toX, toY) in sortedMoves) {
                val capturedPiece = engine.getPieceAt(toX, toY)
                val originalX = piece.x
                val originalY = piece.y

                // 执行移动
                piece.x = toX
                piece.y = toY
                capturedPiece?.isAlive = false

                val score = alphaBeta(depth - 1, alphaVar, betaVar, true)

                // 恢复
                piece.x = originalX
                piece.y = originalY
                capturedPiece?.isAlive = true

                minScore = min(minScore, score)
                betaVar = min(betaVar, score)

                if (betaVar <= alphaVar) break // Alpha剪枝
            }
            return minScore
        }
    }

    /**
     * 评估棋盘局面
     */
    private fun evaluateBoard(): Int {
        var score = 0
        val pieces = engine.getPieces().filter { it.isAlive }

        for (piece in pieces) {
            val pieceValue = pieceValues[piece.type] ?: 0
            val positionValue = positionBonus[piece.type]?.get(piece.y)?.get(piece.x) ?: 0

            val totalValue = pieceValue + positionValue

            if (piece.color == aiColor) {
                score += totalValue
            } else {
                score -= totalValue
            }
        }

        // 将军加分
        if (engine.isInCheck(aiColor.opposite())) {
            score += 100
        }
        if (engine.isInCheck(aiColor)) {
            score -= 100
        }

        // 灵活性评估（可移动棋子数）
        val aiPieces = pieces.filter { it.color == aiColor }
        val enemyPieces = pieces.filter { it.color == aiColor.opposite() }

        var aiMobility = 0
        var enemyMobility = 0

        for (piece in aiPieces) {
            aiMobility += engine.getValidMoves(piece).size
        }
        for (piece in enemyPieces) {
            enemyMobility += engine.getValidMoves(piece).size
        }

        score += (aiMobility - enemyMobility) * 5

        return score
    }

    /**
     * 获取AI难度级别
     */
    fun getDifficultyLevel(): Int = searchDepth

    /**
     * 设置AI难度级别
     */
    fun setDifficultyLevel(level: Int) {
        searchDepth = when (level) {
            1 -> 2  // 简单
            2 -> 3  // 中等
            3 -> 4  // 困难
            4 -> 5  // 专家
            else -> 3
        }
    }
}

/**
 * AI难度级别枚举
 */
enum class AIDifficulty(val level: Int, val displayName: String) {
    EASY(1, "简单"),
    MEDIUM(2, "中等"),
    HARD(3, "困难"),
    EXPERT(4, "专家");

    companion object {
        fun fromLevel(level: Int): AIDifficulty {
            return values().find { it.level == level } ?: MEDIUM
        }
    }
}
