package com.chinesechess.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.chinesechess.R
import com.chinesechess.game.GameManager
import com.chinesechess.model.*
import kotlin.math.min

/**
 * 象棋棋盘自定义View
 */
class ChessBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private var gameManager: GameManager? = null
    
    // 画笔
    private val boardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val piecePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lastMovePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // 棋盘参数
    private var cellSize = 0f
    private var boardPadding = 0f
    private var pieceRadius = 0f
    
    // 棋盘尺寸
    private val boardWidth = 9
    private val boardHeight = 10
    
    // 点击监听器
    private var onBoardClickListener: ((Int, Int) -> Unit)? = null
    
    // 视图方向（true为红方在下，false为黑方在下）
    private var isRedAtBottom = true
    
    init {
        initPaints()
    }
    
    private fun initPaints() {
        // 棋盘线画笔
        boardPaint.apply {
            color = context.getColor(R.color.board_line)
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }
        
        // 棋子画笔
        piecePaint.apply {
            style = Paint.Style.FILL
        }
        
        // 文字画笔
        textPaint.apply {
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        
        // 高亮画笔
        highlightPaint.apply {
            color = context.getColor(R.color.highlight_valid)
            alpha = 128
            style = Paint.Style.FILL
        }
        
        // 最后一步画笔
        lastMovePaint.apply {
            color = context.getColor(R.color.highlight_last_move)
            alpha = 150
            style = Paint.Style.FILL
        }
    }
    
    /**
     * 设置游戏管理器
     */
    fun setGameManager(manager: GameManager) {
        gameManager = manager
        invalidate()
    }
    
    /**
     * 设置点击监听器
     */
    fun setOnBoardClickListener(listener: (Int, Int) -> Unit) {
        onBoardClickListener = listener
    }
    
    /**
     * 设置视图方向
     */
    fun setOrientation(redAtBottom: Boolean) {
        isRedAtBottom = redAtBottom
        invalidate()
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // 计算棋盘大小
        val minSize = min(w, h)
        boardPadding = minSize * 0.05f
        
        val availableWidth = w - 2 * boardPadding
        val availableHeight = h - 2 * boardPadding
        
        cellSize = min(availableWidth / (boardWidth - 1), availableHeight / (boardHeight - 1))
        pieceRadius = cellSize * 0.4f
        
        textPaint.textSize = pieceRadius * 1.2f
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        drawBoard(canvas)
        drawHighlights(canvas)
        drawPieces(canvas)
    }
    
    /**
     * 绘制棋盘
     */
    private fun drawBoard(canvas: Canvas) {
        val startX = boardPadding
        val startY = boardPadding
        val endX = startX + cellSize * (boardWidth - 1)
        val endY = startY + cellSize * (boardHeight - 1)
        
        // 绘制背景
        canvas.drawColor(context.getColor(R.color.board_background))
        
        // 绘制外边框
        val borderRect = RectF(startX - 5, startY - 5, endX + 5, endY + 5)
        boardPaint.strokeWidth = 5f
        canvas.drawRect(borderRect, boardPaint)
        boardPaint.strokeWidth = 3f
        
        // 绘制横线
        for (i in 0 until boardHeight) {
            val y = startY + i * cellSize
            canvas.drawLine(startX, y, endX, y, boardPaint)
        }
        
        // 绘制竖线（中间断开）
        for (i in 0 until boardWidth) {
            val x = startX + i * cellSize
            // 上方
            canvas.drawLine(x, startY, x, startY + 4 * cellSize, boardPaint)
            // 下方
            canvas.drawLine(x, startY + 5 * cellSize, x, endY, boardPaint)
        }
        
        // 绘制九宫格斜线
        // 上方九宫格
        canvas.drawLine(
            startX + 3 * cellSize, startY,
            startX + 5 * cellSize, startY + 2 * cellSize,
            boardPaint
        )
        canvas.drawLine(
            startX + 5 * cellSize, startY,
            startX + 3 * cellSize, startY + 2 * cellSize,
            boardPaint
        )
        
        // 下方九宫格
        canvas.drawLine(
            startX + 3 * cellSize, endY - 2 * cellSize,
            startX + 5 * cellSize, endY,
            boardPaint
        )
        canvas.drawLine(
            startX + 5 * cellSize, endY - 2 * cellSize,
            startX + 3 * cellSize, endY,
            boardPaint
        )
        
        // 绘制炮位和兵位标记
        drawPositionMarks(canvas, startX, startY)
        
        // 绘制楚河汉界文字
        textPaint.textSize = cellSize * 0.6f
        textPaint.color = context.getColor(R.color.board_line)
        
        val riverY = startY + 4.5f * cellSize
        val chuText = "楚 河"
        val hanText = "汉 界"
        
        canvas.drawText(chuText, startX + 2 * cellSize, riverY + cellSize * 0.2f, textPaint)
        canvas.drawText(hanText, startX + 6 * cellSize, riverY + cellSize * 0.2f, textPaint)
        
        textPaint.textSize = pieceRadius * 1.2f
    }
    
    /**
     * 绘制位置标记
     */
    private fun drawPositionMarks(canvas: Canvas, startX: Float, startY: Float) {
        val markLength = cellSize * 0.15f
        val positions = listOf(
            // 黑方炮位
            Pair(1, 2), Pair(7, 2),
            // 黑方卒位
            Pair(0, 3), Pair(2, 3), Pair(4, 3), Pair(6, 3), Pair(8, 3),
            // 红方炮位
            Pair(1, 7), Pair(7, 7),
            // 红方兵位
            Pair(0, 6), Pair(2, 6), Pair(4, 6), Pair(6, 6), Pair(8, 6)
        )
        
        for ((x, y) in positions) {
            val px = startX + x * cellSize
            val py = startY + y * cellSize
            
            // 左侧标记
            if (x > 0) {
                canvas.drawLine(px - markLength, py - markLength, px - markLength * 0.3f, py - markLength, boardPaint)
                canvas.drawLine(px - markLength, py - markLength, px - markLength, py - markLength * 0.3f, boardPaint)
                canvas.drawLine(px - markLength, py + markLength, px - markLength * 0.3f, py + markLength, boardPaint)
                canvas.drawLine(px - markLength, py + markLength, px - markLength, py + markLength * 0.3f, boardPaint)
            }
            
            // 右侧标记
            if (x < boardWidth - 1) {
                canvas.drawLine(px + markLength, py - markLength, px + markLength * 0.3f, py - markLength, boardPaint)
                canvas.drawLine(px + markLength, py - markLength, px + markLength, py - markLength * 0.3f, boardPaint)
                canvas.drawLine(px + markLength, py + markLength, px + markLength * 0.3f, py + markLength, boardPaint)
                canvas.drawLine(px + markLength, py + markLength, px + markLength, py + markLength * 0.3f, boardPaint)
            }
        }
    }
    
    /**
     * 绘制高亮
     */
    private fun drawHighlights(canvas: Canvas) {
        val manager = gameManager ?: return
        
        // 绘制最后一步移动的高亮
        val lastMove = manager.getLastMove()
        if (lastMove != null) {
            val fromPos = boardToScreen(lastMove.fromX, lastMove.fromY)
            val toPos = boardToScreen(lastMove.toX, lastMove.toY)
            
            canvas.drawCircle(fromPos.first, fromPos.second, pieceRadius * 0.8f, lastMovePaint)
            canvas.drawCircle(toPos.first, toPos.second, pieceRadius * 0.8f, lastMovePaint)
        }
        
        // 绘制选中棋子的高亮
        val selectedPiece = manager.selectedPiece.value
        if (selectedPiece != null) {
            val pos = boardToScreen(selectedPiece.x, selectedPiece.y)
            highlightPaint.color = context.getColor(R.color.highlight_selected)
            canvas.drawCircle(pos.first, pos.second, pieceRadius * 0.9f, highlightPaint)
        }
        
        // 绘制合法移动位置
        highlightPaint.color = context.getColor(R.color.highlight_valid)
        val validMoves = manager.validMoves.value
        for (move in validMoves) {
            val pos = boardToScreen(move.x, move.y)
            canvas.drawCircle(pos.first, pos.second, pieceRadius * 0.3f, highlightPaint)
        }
        
        // 绘制将军警告
        if (manager.gameState.value == GameState.PLAYING) {
            val currentTurn = manager.currentTurn.value
            // 检查是否被将军
            // 这里简化处理，实际应该从GameManager获取将军状态
        }
    }
    
    /**
     * 绘制棋子
     */
    private fun drawPieces(canvas: Canvas) {
        val manager = gameManager ?: return
        val pieces = manager.getAllPieces()
        
        for (piece in pieces.filter { it.isAlive }) {
            val pos = boardToScreen(piece.x, piece.y)
            drawPiece(canvas, piece, pos.first, pos.second)
        }
    }
    
    /**
     * 绘制单个棋子
     */
    private fun drawPiece(canvas: Canvas, piece: Piece, x: Float, y: Float) {
        // 棋子背景色
        val bgColor = if (piece.color == PieceColor.RED) {
            context.getColor(R.color.piece_red)
        } else {
            context.getColor(R.color.piece_black)
        }
        
        // 绘制棋子外圈（边框）
        piecePaint.color = if (piece.color == PieceColor.RED) {
            context.getColor(R.color.piece_red_border)
        } else {
            context.getColor(R.color.piece_black_border)
        }
        canvas.drawCircle(x, y, pieceRadius, piecePaint)
        
        // 绘制棋子内圈
        piecePaint.color = Color.WHITE
        canvas.drawCircle(x, y, pieceRadius * 0.9f, piecePaint)
        
        // 绘制棋子颜色环
        piecePaint.color = bgColor
        canvas.drawCircle(x, y, pieceRadius * 0.75f, piecePaint)
        
        // 绘制文字
        textPaint.color = if (piece.color == PieceColor.RED) {
            context.getColor(R.color.piece_text_red)
        } else {
            context.getColor(R.color.piece_text_black)
        }
        
        val text = piece.getName()
        val textBounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val textY = y + textBounds.height() / 2f
        
        canvas.drawText(text, x, textY, textPaint)
    }
    
    /**
     * 棋盘坐标转换为屏幕坐标
     */
    private fun boardToScreen(boardX: Int, boardY: Int): Pair<Float, Float> {
        val x = if (isRedAtBottom) {
            boardPadding + boardX * cellSize
        } else {
            boardPadding + (boardWidth - 1 - boardX) * cellSize
        }
        
        val y = if (isRedAtBottom) {
            boardPadding + (boardHeight - 1 - boardY) * cellSize
        } else {
            boardPadding + boardY * cellSize
        }
        
        return Pair(x, y)
    }
    
    /**
     * 屏幕坐标转换为棋盘坐标
     */
    private fun screenToBoard(screenX: Float, screenY: Float): Pair<Int, Int>? {
        val x = if (isRedAtBottom) {
            ((screenX - boardPadding + cellSize / 2) / cellSize).toInt()
        } else {
            (boardWidth - 1 - ((screenX - boardPadding + cellSize / 2) / cellSize).toInt())
        }
        
        val y = if (isRedAtBottom) {
            (boardHeight - 1 - ((screenY - boardPadding + cellSize / 2) / cellSize).toInt())
        } else {
            ((screenY - boardPadding + cellSize / 2) / cellSize).toInt()
        }
        
        return if (x in 0 until boardWidth && y in 0 until boardHeight) {
            Pair(x, y)
        } else {
            null
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val boardPos = screenToBoard(event.x, event.y)
                if (boardPos != null) {
                    onBoardClickListener?.invoke(boardPos.first, boardPos.second)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
