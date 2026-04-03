package com.chinesechess.model

/**
 * 棋子类型枚举
 */
enum class PieceType {
    KING,       // 将/帅
    ADVISOR,    // 士/仕
    ELEPHANT,   // 象/相
    HORSE,      // 马
    CHARIOT,    // 车
    CANNON,     // 炮
    SOLDIER     // 兵/卒
}

/**
 * 棋子颜色枚举
 */
enum class PieceColor {
    RED, BLACK;
    
    fun opposite(): PieceColor = if (this == RED) BLACK else RED
}

/**
 * 游戏状态枚举
 */
enum class GameState {
    IDLE,       // 空闲
    PLAYING,    // 进行中
    PAUSED,     // 暂停
    RED_WIN,    // 红方胜
    BLACK_WIN,  // 黑方胜
    DRAW        // 和棋
}

/**
 * 游戏模式枚举
 */
enum class GameMode {
    LOCAL,      // 本地对战
    BLUETOOTH,  // 蓝牙对战
    WIFI        // 局域网对战
}

/**
 * 时间模式枚举
 */
enum class TimeMode {
    UNLIMITED,  // 无限制
    TOTAL,      // 总时间模式
    STEP        // 步时模式
}
