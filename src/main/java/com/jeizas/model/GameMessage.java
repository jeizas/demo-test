package com.jeizas.model;

import lombok.Data;

/**
 * 游戏消息实体类，用于WebSocket通信的消息传递
 *
 * @author jeizas
 * @date 2025-11-29
 */
@Data
public class GameMessage {
    /** 消息类型: JOIN, MOVE, GAME_START, GAME_OVER, OPPONENT_LEFT, ERROR, WAITING, RESET */
    private String type;

    /** 落子行坐标 */
    private int row;

    /** 落子列坐标 */
    private int col;

    /** 玩家颜色，1=黑棋，2=白棋 */
    private int player;

    /** 获胜者，1=黑棋，2=白棋，0=未分出胜负 */
    private int winner;

    /** 消息内容 */
    private String message;

    /** 棋盘状态，15x15的二维数组 */
    private int[][] board;

    /** 当前回合玩家，1=黑棋，2=白棋 */
    private int currentPlayer;

    /**
     * 默认构造函数
     */
    public GameMessage() {}

    /**
     * 带参数的构造函数
     *
     * @param type 消息类型
     * @param message 消息内容
     */
    public GameMessage(String type, String message) {
        this.type = type;
        this.message = message;
    }

    /**
     * 创建等待消息
     *
     * @return 等待对手加入的消息对象
     */
    public static GameMessage waiting() {
        GameMessage msg = new GameMessage();
        msg.setType("WAITING");
        msg.setMessage("等待对手加入...");
        return msg;
    }

    /**
     * 创建游戏开始消息
     *
     * @param playerColor 玩家颜色，1=黑棋，2=白棋
     * @return 游戏开始消息对象
     */
    public static GameMessage gameStart(int playerColor) {
        GameMessage msg = new GameMessage();
        msg.setType("GAME_START");
        msg.setPlayer(playerColor);
        msg.setCurrentPlayer(1);
        msg.setMessage(playerColor == 1 ? "你是黑棋，你先走" : "你是白棋，对方先走");
        return msg;
    }

    /**
     * 创建落子消息
     *
     * @param row 落子行坐标
     * @param col 落子列坐标
     * @param player 玩家颜色
     * @param currentPlayer 当前回合玩家
     * @param board 当前棋盘状态
     * @return 落子消息对象
     */
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

    /**
     * 创建游戏结束消息
     *
     * @param winner 获胜者，1=黑棋，2=白棋
     * @return 游戏结束消息对象
     */
    public static GameMessage gameOver(int winner) {
        GameMessage msg = new GameMessage();
        msg.setType("GAME_OVER");
        msg.setWinner(winner);
        msg.setMessage(winner == 1 ? "黑棋获胜！" : "白棋获胜！");
        return msg;
    }

    /**
     * 创建对手离开消息
     *
     * @return 对手离开消息对象
     */
    public static GameMessage opponentLeft() {
        GameMessage msg = new GameMessage();
        msg.setType("OPPONENT_LEFT");
        msg.setMessage("对手已离开游戏");
        return msg;
    }

    /**
     * 创建错误消息
     *
     * @param message 错误消息内容
     * @return 错误消息对象
     */
    public static GameMessage error(String message) {
        GameMessage msg = new GameMessage();
        msg.setType("ERROR");
        msg.setMessage(message);
        return msg;
    }

    /**
     * 创建重置游戏消息
     *
     * @return 重置游戏消息对象
     */
    public static GameMessage reset() {
        GameMessage msg = new GameMessage();
        msg.setType("RESET");
        msg.setMessage("游戏已重置，黑棋先行");
        msg.setCurrentPlayer(1);
        return msg;
    }
}

