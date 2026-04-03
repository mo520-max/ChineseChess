package com.chinesechess.utils

import android.content.Context
import android.content.SharedPreferences
import com.chinesechess.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * 对战记录管理器
 * 负责保存、加载和复盘对战记录
 */
class GameRecordManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "chess_game_records"
        private const val KEY_RECORDS = "game_records"
        private const val MAX_RECORDS = 50 // 最多保存50条记录
        
        // 记录字段
        private const val FIELD_ID = "id"
        private const val FIELD_DATE = "date"
        private const val FIELD_GAME_MODE = "game_mode"
        private const val FIELD_RESULT = "result"
        private const val FIELD_PLAYER_COLOR = "player_color"
        private const val FIELD_OPPONENT = "opponent"
        private const val FIELD_MOVES = "moves"
        private const val FIELD_DURATION = "duration"
        private const val FIELD_NOTES = "notes"
        
        // 移动字段
        private const val FIELD_MOVE_PIECE_TYPE = "piece_type"
        private const val FIELD_MOVE_PIECE_COLOR = "piece_color"
        private const val FIELD_MOVE_FROM_X = "from_x"
        private const val FIELD_MOVE_FROM_Y = "from_y"
        private const val FIELD_MOVE_TO_X = "to_x"
        private const val FIELD_MOVE_TO_Y = "to_y"
        private const val FIELD_MOVE_CAPTURED = "captured"
        private const val FIELD_MOVE_CHECK = "check"
        private const val FIELD_MOVE_TIMESTAMP = "timestamp"
    }
    
    /**
     * 游戏记录数据类
     */
    data class GameRecord(
        val id: String = UUID.randomUUID().toString(),
        val date: Long = System.currentTimeMillis(),
        val gameMode: GameMode,
        val result: GameResult,
        val playerColor: PieceColor? = null,
        val opponent: String = "",
        val moves: List<MoveData> = emptyList(),
        val duration: Long = 0, // 游戏时长（毫秒）
        val notes: String = ""
    ) {
        fun getFormattedDate(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            return sdf.format(Date(date))
        }
        
        fun getDurationString(): String {
            val minutes = duration / 60000
            val seconds = (duration % 60000) / 1000
            return String.format("%02d:%02d", minutes, seconds)
        }
    }
    
    /**
     * 游戏结果枚举
     */
    enum class GameResult(val displayName: String) {
        WIN("胜利"),
        LOSE("失败"),
        DRAW("和棋"),
        UNKNOWN("未知");
        
        companion object {
            fun fromString(value: String): GameResult {
                return values().find { it.name == value } ?: UNKNOWN
            }
        }
    }
    
    /**
     * 移动数据类（用于保存和复盘）
     */
    data class MoveData(
        val pieceType: PieceType,
        val pieceColor: PieceColor,
        val fromX: Int,
        val fromY: Int,
        val toX: Int,
        val toY: Int,
        val captured: Boolean = false,
        val isCheck: Boolean = false,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        companion object {
            fun fromMove(move: Move): MoveData {
                return MoveData(
                    pieceType = move.piece.type,
                    pieceColor = move.piece.color,
                    fromX = move.fromX,
                    fromY = move.fromY,
                    toX = move.toX,
                    toY = move.toY,
                    captured = move.capturedPiece != null,
                    isCheck = move.isCheck,
                    timestamp = move.timestamp
                )
            }
        }
    }
    
    /**
     * 保存游戏记录
     */
    fun saveGameRecord(record: GameRecord) {
        val records = getAllRecords().toMutableList()
        
        // 添加新记录到开头
        records.add(0, record)
        
        // 限制记录数量
        if (records.size > MAX_RECORDS) {
            records.removeAt(records.size - 1)
        }
        
        // 保存到SharedPreferences
        val jsonArray = JSONArray()
        records.forEach { jsonArray.put(recordToJson(it)) }
        
        prefs.edit().putString(KEY_RECORDS, jsonArray.toString()).apply()
    }
    
    /**
     * 获取所有记录
     */
    fun getAllRecords(): List<GameRecord> {
        val jsonString = prefs.getString(KEY_RECORDS, "[]") ?: "[]"
        val jsonArray = JSONArray(jsonString)
        val records = mutableListOf<GameRecord>()
        
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            records.add(jsonToRecord(jsonObject))
        }
        
        return records
    }
    
    /**
     * 根据ID获取记录
     */
    fun getRecordById(id: String): GameRecord? {
        return getAllRecords().find { it.id == id }
    }
    
    /**
     * 删除记录
     */
    fun deleteRecord(id: String) {
        val records = getAllRecords().toMutableList()
        records.removeAll { it.id == id }
        
        val jsonArray = JSONArray()
        records.forEach { jsonArray.put(recordToJson(it)) }
        
        prefs.edit().putString(KEY_RECORDS, jsonArray.toString()).apply()
    }
    
    /**
     * 清空所有记录
     */
    fun clearAllRecords() {
        prefs.edit().remove(KEY_RECORDS).apply()
    }
    
    /**
     * 更新记录备注
     */
    fun updateRecordNotes(id: String, notes: String) {
        val records = getAllRecords().toMutableList()
        val index = records.indexOfFirst { it.id == id }
        
        if (index != -1) {
            val oldRecord = records[index]
            records[index] = oldRecord.copy(notes = notes)
            
            val jsonArray = JSONArray()
            records.forEach { jsonArray.put(recordToJson(it)) }
            
            prefs.edit().putString(KEY_RECORDS, jsonArray.toString()).apply()
        }
    }
    
    /**
     * 从当前游戏状态创建记录
     */
    fun createRecordFromGame(
        gameMode: GameMode,
        gameState: GameState,
        playerColor: PieceColor?,
        moveHistory: List<Move>,
        startTime: Long,
        opponent: String = ""
    ): GameRecord {
        val result = when (gameState) {
            GameState.RED_WIN -> {
                if (playerColor == PieceColor.RED) GameResult.WIN else GameResult.LOSE
            }
            GameState.BLACK_WIN -> {
                if (playerColor == PieceColor.BLACK) GameResult.WIN else GameResult.LOSE
            }
            GameState.DRAW -> GameResult.DRAW
            else -> GameResult.UNKNOWN
        }
        
        val moveDataList = moveHistory.map { MoveData.fromMove(it) }
        val duration = System.currentTimeMillis() - startTime
        
        return GameRecord(
            gameMode = gameMode,
            result = result,
            playerColor = playerColor,
            opponent = opponent,
            moves = moveDataList,
            duration = duration
        )
    }
    
    /**
     * 将记录转换为JSON
     */
    private fun recordToJson(record: GameRecord): JSONObject {
        val jsonObject = JSONObject().apply {
            put(FIELD_ID, record.id)
            put(FIELD_DATE, record.date)
            put(FIELD_GAME_MODE, record.gameMode.name)
            put(FIELD_RESULT, record.result.name)
            put(FIELD_PLAYER_COLOR, record.playerColor?.name)
            put(FIELD_OPPONENT, record.opponent)
            put(FIELD_DURATION, record.duration)
            put(FIELD_NOTES, record.notes)
            
            // 保存移动记录
            val movesArray = JSONArray()
            record.moves.forEach { move ->
                movesArray.put(moveToJson(move))
            }
            put(FIELD_MOVES, movesArray)
        }
        
        return jsonObject
    }
    
    /**
     * 将移动转换为JSON
     */
    private fun moveToJson(move: MoveData): JSONObject {
        return JSONObject().apply {
            put(FIELD_MOVE_PIECE_TYPE, move.pieceType.name)
            put(FIELD_MOVE_PIECE_COLOR, move.pieceColor.name)
            put(FIELD_MOVE_FROM_X, move.fromX)
            put(FIELD_MOVE_FROM_Y, move.fromY)
            put(FIELD_MOVE_TO_X, move.toX)
            put(FIELD_MOVE_TO_Y, move.toY)
            put(FIELD_MOVE_CAPTURED, move.captured)
            put(FIELD_MOVE_CHECK, move.isCheck)
            put(FIELD_MOVE_TIMESTAMP, move.timestamp)
        }
    }
    
    /**
     * 将JSON转换为记录
     */
    private fun jsonToRecord(jsonObject: JSONObject): GameRecord {
        // 解析移动记录
        val movesArray = jsonObject.getJSONArray(FIELD_MOVES)
        val moves = mutableListOf<MoveData>()
        
        for (i in 0 until movesArray.length()) {
            val moveJson = movesArray.getJSONObject(i)
            moves.add(jsonToMove(moveJson))
        }
        
        return GameRecord(
            id = jsonObject.getString(FIELD_ID),
            date = jsonObject.getLong(FIELD_DATE),
            gameMode = GameMode.valueOf(jsonObject.getString(FIELD_GAME_MODE)),
            result = GameResult.fromString(jsonObject.getString(FIELD_RESULT)),
            playerColor = jsonObject.optString(FIELD_PLAYER_COLOR)?.let { 
                if (it.isNotEmpty()) PieceColor.valueOf(it) else null 
            },
            opponent = jsonObject.optString(FIELD_OPPONENT, ""),
            moves = moves,
            duration = jsonObject.getLong(FIELD_DURATION),
            notes = jsonObject.optString(FIELD_NOTES, "")
        )
    }
    
    /**
     * 将JSON转换为移动
     */
    private fun jsonToMove(jsonObject: JSONObject): MoveData {
        return MoveData(
            pieceType = PieceType.valueOf(jsonObject.getString(FIELD_MOVE_PIECE_TYPE)),
            pieceColor = PieceColor.valueOf(jsonObject.getString(FIELD_MOVE_PIECE_COLOR)),
            fromX = jsonObject.getInt(FIELD_MOVE_FROM_X),
            fromY = jsonObject.getInt(FIELD_MOVE_FROM_Y),
            toX = jsonObject.getInt(FIELD_MOVE_TO_X),
            toY = jsonObject.getInt(FIELD_MOVE_TO_Y),
            captured = jsonObject.getBoolean(FIELD_MOVE_CAPTURED),
            isCheck = jsonObject.getBoolean(FIELD_MOVE_CHECK),
            timestamp = jsonObject.getLong(FIELD_MOVE_TIMESTAMP)
        )
    }
    
    /**
     * 导出记录为PGN格式（简化版）
     */
    fun exportToPGN(record: GameRecord): String {
        val sb = StringBuilder()
        
        // 头部信息
        sb.append("[Event \"Chinese Chess Game\"]\n")
        sb.append("[Date \"${record.getFormattedDate()}\"]\n")
        sb.append("[Mode \"${record.gameMode.name}\"]\n")
        sb.append("[Result \"${record.result.displayName}\"]\n")
        sb.append("\n")
        
        // 移动记录
        record.moves.forEachIndexed { index, move ->
            if (index % 2 == 0) {
                sb.append("${index / 2 + 1}. ")
            }
            sb.append("${move.pieceColor.name} ${move.pieceType.name} (${move.fromX},${move.fromY})->(${move.toX},${move.toY})")
            if (move.isCheck) sb.append("+")
            sb.append(" ")
            
            if ((index + 1) % 2 == 0) {
                sb.append("\n")
            }
        }
        
        sb.append("\n${record.result.displayName}")
        
        return sb.toString()
    }
    
    /**
     * 获取统计信息
     */
    fun getStatistics(): GameStatistics {
        val records = getAllRecords()
        
        val totalGames = records.size
        val wins = records.count { it.result == GameResult.WIN }
        val losses = records.count { it.result == GameResult.LOSE }
        val draws = records.count { it.result == GameResult.DRAW }
        
        val aiGames = records.count { it.gameMode == GameMode.LOCAL }
        val bluetoothGames = records.count { it.gameMode == GameMode.BLUETOOTH }
        val wifiGames = records.count { it.gameMode == GameMode.WIFI }
        
        return GameStatistics(
            totalGames = totalGames,
            wins = wins,
            losses = losses,
            draws = draws,
            aiGames = aiGames,
            bluetoothGames = bluetoothGames,
            wifiGames = wifiGames
        )
    }
    
    /**
     * 统计信息数据类
     */
    data class GameStatistics(
        val totalGames: Int,
        val wins: Int,
        val losses: Int,
        val draws: Int,
        val aiGames: Int,
        val bluetoothGames: Int,
        val wifiGames: Int
    ) {
        fun getWinRate(): Float {
            return if (totalGames > 0) wins.toFloat() / totalGames else 0f
        }
    }
}

/**
 * 复盘控制器
 */
class ReplayController(private val record: GameRecordManager.GameRecord) {
    
    private var currentMoveIndex = -1
    private var isPlaying = false
    private var replaySpeed = 1000L // 默认1秒一步
    
    var onMoveChanged: ((Int, GameRecordManager.MoveData) -> Unit)? = null
    var onReplayFinished: (() -> Unit)? = null
    
    /**
     * 获取总步数
     */
    fun getTotalMoves(): Int = record.moves.size
    
    /**
     * 获取当前步数
     */
    fun getCurrentMoveIndex(): Int = currentMoveIndex
    
    /**
     * 下一步
     */
    fun nextMove(): Boolean {
        if (currentMoveIndex < record.moves.size - 1) {
            currentMoveIndex++
            onMoveChanged?.invoke(currentMoveIndex, record.moves[currentMoveIndex])
            return true
        }
        return false
    }
    
    /**
     * 上一步
     */
    fun previousMove(): Boolean {
        if (currentMoveIndex > 0) {
            currentMoveIndex--
            onMoveChanged?.invoke(currentMoveIndex, record.moves[currentMoveIndex])
            return true
        }
        return false
    }
    
    /**
     * 跳转到指定步
     */
    fun goToMove(index: Int): Boolean {
        if (index in -1 until record.moves.size) {
            currentMoveIndex = index
            if (index >= 0) {
                onMoveChanged?.invoke(currentMoveIndex, record.moves[currentMoveIndex])
            }
            return true
        }
        return false
    }
    
    /**
     * 回到开始
     */
    fun goToStart() {
        currentMoveIndex = -1
    }
    
    /**
     * 跳转到结束
     */
    fun goToEnd() {
        currentMoveIndex = record.moves.size - 1
        if (currentMoveIndex >= 0) {
            onMoveChanged?.invoke(currentMoveIndex, record.moves[currentMoveIndex])
        }
    }
    
    /**
     * 设置播放速度
     */
    fun setSpeed(speedMs: Long) {
        replaySpeed = speedMs.coerceIn(200, 5000)
    }
    
    /**
     * 获取当前移动
     */
    fun getCurrentMove(): GameRecordManager.MoveData? {
        return if (currentMoveIndex in 0 until record.moves.size) {
            record.moves[currentMoveIndex]
        } else null
    }
    
    /**
     * 获取所有移动
     */
    fun getAllMoves(): List<GameRecordManager.MoveData> = record.moves
}
