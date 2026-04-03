package com.chinesechess.model

/**
 * 移动记录数据类
 */
data class Move(
    val piece: Piece,           // 移动的棋子
    val fromX: Int,             // 起始位置X
    val fromY: Int,             // 起始位置Y
    val toX: Int,               // 目标位置X
    val toY: Int,               // 目标位置Y
    val capturedPiece: Piece? = null,  // 被吃的棋子
    val isCheck: Boolean = false,      // 是否将军
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * 获取移动描述
     */
    fun getDescription(): String {
        val pieceName = piece.getName()
        return "$pieceName (${fromX},${fromY}) -> (${toX},${toY})"
    }
    
    /**
     * 转换为网络传输格式
     * 格式: pieceType,pieceColor,fromX,fromY,toX,toY
     */
    fun toNetworkString(): String {
        return "${piece.type},${piece.color},$fromX,$fromY,$toX,$toY"
    }
    
    companion object {
        /**
         * 从网络字符串解析移动
         */
        fun fromNetworkString(str: String, pieces: List<Piece>): Move? {
            try {
                val parts = str.split(",")
                if (parts.size != 6) return null
                
                val pieceType = PieceType.valueOf(parts[0])
                val pieceColor = PieceColor.valueOf(parts[1])
                val fromX = parts[2].toInt()
                val fromY = parts[3].toInt()
                val toX = parts[4].toInt()
                val toY = parts[5].toInt()
                
                // 查找对应的棋子
                val piece = pieces.find { 
                    it.type == pieceType && it.color == pieceColor && 
                    it.x == fromX && it.y == fromY 
                } ?: return null
                
                return Move(piece, fromX, fromY, toX, toY)
            } catch (e: Exception) {
                return null
            }
        }
    }
}

/**
 * 位置数据类
 */
data class Position(val x: Int, val y: Int) {
    fun isValid(): Boolean {
        return x in 0..8 && y in 0..9
    }
}
