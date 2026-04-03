package com.chinesechess.model

/**
 * 棋子数据类
 */
data class Piece(
    val type: PieceType,
    val color: PieceColor,
    var x: Int,  // 0-8 (横向)
    var y: Int,  // 0-9 (纵向)
    var isAlive: Boolean = true
) {
    /**
     * 获取棋子显示名称
     */
    fun getName(): String {
        return when (color) {
            PieceColor.RED -> when (type) {
                PieceType.KING -> "帅"
                PieceType.ADVISOR -> "仕"
                PieceType.ELEPHANT -> "相"
                PieceType.HORSE -> "傌"
                PieceType.CHARIOT -> "俥"
                PieceType.CANNON -> "炮"
                PieceType.SOLDIER -> "兵"
            }
            PieceColor.BLACK -> when (type) {
                PieceType.KING -> "将"
                PieceType.ADVISOR -> "士"
                PieceType.ELEPHANT -> "象"
                PieceType.HORSE -> "马"
                PieceType.CHARIOT -> "车"
                PieceType.CANNON -> "砲"
                PieceType.SOLDIER -> "卒"
            }
        }
    }
    
    /**
     * 复制棋子
     */
    fun copy(): Piece {
        return Piece(type, color, x, y, isAlive)
    }
    
    companion object {
        /**
         * 创建初始棋子布局
         */
        fun createInitialPieces(): List<Piece> {
            val pieces = mutableListOf<Piece>()
            
            // 黑方棋子 (y = 0-4)
            // 第一行
            pieces.add(Piece(PieceType.CHARIOT, PieceColor.BLACK, 0, 0))
            pieces.add(Piece(PieceType.HORSE, PieceColor.BLACK, 1, 0))
            pieces.add(Piece(PieceType.ELEPHANT, PieceColor.BLACK, 2, 0))
            pieces.add(Piece(PieceType.ADVISOR, PieceColor.BLACK, 3, 0))
            pieces.add(Piece(PieceType.KING, PieceColor.BLACK, 4, 0))
            pieces.add(Piece(PieceType.ADVISOR, PieceColor.BLACK, 5, 0))
            pieces.add(Piece(PieceType.ELEPHANT, PieceColor.BLACK, 6, 0))
            pieces.add(Piece(PieceType.HORSE, PieceColor.BLACK, 7, 0))
            pieces.add(Piece(PieceType.CHARIOT, PieceColor.BLACK, 8, 0))
            
            // 炮
            pieces.add(Piece(PieceType.CANNON, PieceColor.BLACK, 1, 2))
            pieces.add(Piece(PieceType.CANNON, PieceColor.BLACK, 7, 2))
            
            // 卒
            pieces.add(Piece(PieceType.SOLDIER, PieceColor.BLACK, 0, 3))
            pieces.add(Piece(PieceType.SOLDIER, PieceColor.BLACK, 2, 3))
            pieces.add(Piece(PieceType.SOLDIER, PieceColor.BLACK, 4, 3))
            pieces.add(Piece(PieceType.SOLDIER, PieceColor.BLACK, 6, 3))
            pieces.add(Piece(PieceType.SOLDIER, PieceColor.BLACK, 8, 3))
            
            // 红方棋子 (y = 5-9)
            // 卒
            pieces.add(Piece(PieceType.SOLDIER, PieceColor.RED, 0, 6))
            pieces.add(Piece(PieceType.SOLDIER, PieceColor.RED, 2, 6))
            pieces.add(Piece(PieceType.SOLDIER, PieceColor.RED, 4, 6))
            pieces.add(Piece(PieceType.SOLDIER, PieceColor.RED, 6, 6))
            pieces.add(Piece(PieceType.SOLDIER, PieceColor.RED, 8, 6))
            
            // 炮
            pieces.add(Piece(PieceType.CANNON, PieceColor.RED, 1, 7))
            pieces.add(Piece(PieceType.CANNON, PieceColor.RED, 7, 7))
            
            // 第一行
            pieces.add(Piece(PieceType.CHARIOT, PieceColor.RED, 0, 9))
            pieces.add(Piece(PieceType.HORSE, PieceColor.RED, 1, 9))
            pieces.add(Piece(PieceType.ELEPHANT, PieceColor.RED, 2, 9))
            pieces.add(Piece(PieceType.ADVISOR, PieceColor.RED, 3, 9))
            pieces.add(Piece(PieceType.KING, PieceColor.RED, 4, 9))
            pieces.add(Piece(PieceType.ADVISOR, PieceColor.RED, 5, 9))
            pieces.add(Piece(PieceType.ELEPHANT, PieceColor.RED, 6, 9))
            pieces.add(Piece(PieceType.HORSE, PieceColor.RED, 7, 9))
            pieces.add(Piece(PieceType.CHARIOT, PieceColor.RED, 8, 9))
            
            return pieces
        }
    }
}
