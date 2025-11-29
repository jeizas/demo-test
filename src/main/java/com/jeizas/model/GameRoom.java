package com.jeizas.model;

import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

@Data
public class GameRoom {
    private String roomId;
    private WebSocketSession blackPlayer;  // 黑棋玩家
    private WebSocketSession whitePlayer;  // 白棋玩家
    private int[][] board;                  // 棋盘 0=空, 1=黑, 2=白
    private int currentPlayer;             // 当前下棋方 1=黑, 2=白
    private boolean gameStarted;
    private boolean gameOver;
    private int winner;                     // 0=无, 1=黑赢, 2=白赢

    public GameRoom(String roomId) {
        this.roomId = roomId;
        this.board = new int[15][15];
        this.currentPlayer = 1;  // 黑棋先下
        this.gameStarted = false;
        this.gameOver = false;
        this.winner = 0;
    }

    public boolean isFull() {
        return blackPlayer != null && whitePlayer != null;
    }

    public boolean isEmpty() {
        return blackPlayer == null && whitePlayer == null;
    }

    public int getPlayerColor(WebSocketSession session) {
        if (session.equals(blackPlayer)) {
            return 1;
        } else if (session.equals(whitePlayer)) {
            return 2;
        }
        return 0;
    }

    public WebSocketSession getOpponent(WebSocketSession session) {
        if (session.equals(blackPlayer)) {
            return whitePlayer;
        } else if (session.equals(whitePlayer)) {
            return blackPlayer;
        }
        return null;
    }

    public void reset() {
        this.board = new int[15][15];
        this.currentPlayer = 1;
        this.gameStarted = false;
        this.gameOver = false;
        this.winner = 0;
    }

    // 检查是否有人获胜
    public boolean checkWin(int row, int col, int player) {
        return checkDirection(row, col, player, 1, 0) ||   // 水平
               checkDirection(row, col, player, 0, 1) ||   // 垂直
               checkDirection(row, col, player, 1, 1) ||   // 对角线
               checkDirection(row, col, player, 1, -1);    // 反对角线
    }

    private boolean checkDirection(int row, int col, int player, int dRow, int dCol) {
        int count = 1;
        // 正向检查
        for (int i = 1; i < 5; i++) {
            int r = row + i * dRow;
            int c = col + i * dCol;
            if (r >= 0 && r < 15 && c >= 0 && c < 15 && board[r][c] == player) {
                count++;
            } else {
                break;
            }
        }
        // 反向检查
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

