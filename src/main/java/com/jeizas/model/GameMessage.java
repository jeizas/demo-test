package com.jeizas.model;

import lombok.Data;

@Data
public class GameMessage {
    private String type;      // JOIN, MOVE, GAME_START, GAME_OVER, OPPONENT_LEFT, ERROR, WAITING, RESET
    private int row;          // 落子行
    private int col;          // 落子列
    private int player;       // 玩家颜色 1=黑, 2=白
    private int winner;       // 获胜者
    private String message;   // 消息内容
    private int[][] board;    // 棋盘状态
    private int currentPlayer; // 当前回合

    public GameMessage() {}

    public GameMessage(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public static GameMessage waiting() {
        GameMessage msg = new GameMessage();
        msg.setType("WAITING");
        msg.setMessage("等待对手加入...");
        return msg;
    }

    public static GameMessage gameStart(int playerColor) {
        GameMessage msg = new GameMessage();
        msg.setType("GAME_START");
        msg.setPlayer(playerColor);
        msg.setCurrentPlayer(1);  // 黑棋先行
        msg.setMessage(playerColor == 1 ? "你是黑棋，你先走" : "你是白棋，对方先走");
        return msg;
    }

    public static GameMessage move(int row, int col, int player, int currentPlayer, int[][] board) {
        GameMessage msg = new GameMessage();
        msg.setType("MOVE");
        msg.setRow(row);
        msg.setCol(col);
        msg.setPlayer(player);
        msg.setCurrentPlayer(currentPlayer);
        msg.setBoard(board);
        return msg;
    }

    public static GameMessage gameOver(int winner) {
        GameMessage msg = new GameMessage();
        msg.setType("GAME_OVER");
        msg.setWinner(winner);
        msg.setMessage(winner == 1 ? "黑棋获胜！" : "白棋获胜！");
        return msg;
    }

    public static GameMessage opponentLeft() {
        GameMessage msg = new GameMessage();
        msg.setType("OPPONENT_LEFT");
        msg.setMessage("对手已离开游戏");
        return msg;
    }

    public static GameMessage error(String message) {
        GameMessage msg = new GameMessage();
        msg.setType("ERROR");
        msg.setMessage(message);
        return msg;
    }

    public static GameMessage reset() {
        GameMessage msg = new GameMessage();
        msg.setType("RESET");
        msg.setMessage("游戏已重置，黑棋先行");
        msg.setCurrentPlayer(1);
        return msg;
    }
}

