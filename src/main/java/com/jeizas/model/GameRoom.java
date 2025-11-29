package com.jeizas.model;

import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * 游戏房间实体类，管理五子棋游戏的状态和玩家
 *
 * @author jeizas
 * @date 2025-11-29
 */
@Data
public class GameRoom {

    /** 同步锁对象 */
    private final Object lock = new Object();

    /** 房间ID */
    private String roomId;

    /** 黑棋玩家会话 */
    private WebSocketSession blackPlayer;

    /** 白棋玩家会话 */
    private WebSocketSession whitePlayer;

    /** 棋盘状态，15x15的二维数组，0=空，1=黑棋，2=白棋 */
    private int[][] board;

    /** 当前下棋方，1=黑棋，2=白棋 */
    private int currentPlayer;

    /** 游戏是否已开始 */
    private boolean gameStarted;

    /** 游戏是否已结束 */
    private boolean gameOver;

    /** 获胜者，0=无，1=黑棋获胜，2=白棋获胜 */
    private int winner;

    /**
     * 构造函数
     *
     * @param roomId 房间ID
     */
    public GameRoom(String roomId) {
        this.roomId = roomId;
        this.board = new int[15][15];
        this.currentPlayer = 1;
        this.gameStarted = false;
        this.gameOver = false;
        this.winner = 0;
    }

    /**
     * 判断房间是否已满
     *
     * @return 如果黑白双方都已加入则返回true，否则返回false
     */
    public boolean isFull() {
        return blackPlayer != null && whitePlayer != null;
    }

    /**
     * 判断房间是否为空
     *
     * @return 如果房间中没有玩家则返回true，否则返回false
     */
    public boolean isEmpty() {
        return blackPlayer == null && whitePlayer == null;
    }

    /**
     * 获取指定会话对应的玩家颜色
     *
     * @param session WebSocket会话
     * @return 1=黑棋，2=白棋，0=不在房间中
     */
    public int getPlayerColor(WebSocketSession session) {
        if (session.equals(blackPlayer)) {
            return 1;
        } else if (session.equals(whitePlayer)) {
            return 2;
        }
        return 0;
    }

    /**
     * 获取指定会话的对手会话
     *
     * @param session WebSocket会话
     * @return 对手的WebSocket会话，如果没有对手则返回null
     */
    public WebSocketSession getOpponent(WebSocketSession session) {
        if (session.equals(blackPlayer)) {
            return whitePlayer;
        } else if (session.equals(whitePlayer)) {
            return blackPlayer;
        }
        return null;
    }

    /**
     * 重置游戏状态
     */
    public void reset() {
        this.board = new int[15][15];
        this.currentPlayer = 1;
        this.gameStarted = false;
        this.gameOver = false;
        this.winner = 0;
    }

    /**
     * 检查指定位置落子后是否有玩家获胜
     *
     * @param row    落子行坐标
     * @param col    落子列坐标
     * @param player 玩家颜色
     * @return 如果形成五子连珠则返回true，否则返回false
     */
    public boolean checkWin(int row, int col, int player) {
        return checkDirection(row, col, player, 1, 0) ||
                checkDirection(row, col, player, 0, 1) ||
                checkDirection(row, col, player, 1, 1) ||
                checkDirection(row, col, player, 1, -1);
    }

    /**
     * 检查指定方向上是否形成五子连珠
     *
     * @param row    落子行坐标
     * @param col    落子列坐标
     * @param player 玩家颜色
     * @param dRow   行方向增量
     * @param dCol   列方向增量
     * @return 如果该方向上连续棋子数大于等于5则返回true，否则返回false
     */
    private boolean checkDirection(int row, int col, int player, int dRow, int dCol) {
        int count = 1;

        for (int i = 1; i < 5; i++) {
            int r = row + i * dRow;
            int c = col + i * dCol;
            if (r >= 0 && r < 15 && c >= 0 && c < 15 && board[r][c] == player) {
                count++;
            } else {
                break;
            }
        }

        for (int i = 1; i < 5; i++) {
            int r = row - i * dRow;
            int c = col - i * dCol;
            if (r >= 0 && r < 15 && c >= 0 && c < 15 && board[r][c] == player) {
                count++;
            } else {
                break;
            }
        }
        return count >= 5;
    }
}
