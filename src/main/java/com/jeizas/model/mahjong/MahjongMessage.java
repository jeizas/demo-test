package com.jeizas.model.mahjong;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 麻将游戏消息类
 *
 * @author jeizas
 * @date 2025-12-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MahjongMessage {

    /** 消息类型 */
    private String type;

    /** 消息内容 */
    private String message;

    /** 玩家ID */
    private String playerId;

    /** 牌信息 */
    private MahjongTile tile;

    /** 手牌列表 */
    private List<MahjongTile> hand;

    /** 玩家列表信息 */
    private List<Map<String, Object>> players;

    /** 当前玩家索引 */
    private Integer currentPlayerIndex;

    /** 庄家索引 */
    private Integer dealerIndex;

    /** 当前轮数 */
    private Integer currentRound;

    /** 总轮数 */
    private Integer totalRounds;

    /** 最后打出的牌 */
    private MahjongTile lastDiscardedTile;

    /** 最后打出牌的玩家索引 */
    private Integer lastDiscardPlayerIndex;

    /** 剩余牌数 */
    private Integer remainingTiles;

    /** 胡牌信息 */
    private Map<String, Object> winInfo;

    /** 分数信息 */
    private List<Map<String, Object>> scores;

    /** 是否可以胡牌 */
    private Boolean canWin;

    /** 是否可以碰牌 */
    private Boolean canPeng;

    /** 是否可以明杠 */
    private Boolean canGang;

    /** 碰/杠的牌组 */
    private MahjongMeld meld;

    /**
     * 创建加入房间消息
     */
    public static MahjongMessage joinRoom(String playerId, String playerName) {
        MahjongMessage msg = new MahjongMessage();
        msg.setType("JOIN_ROOM");
        msg.setPlayerId(playerId);
        msg.setMessage("玩家 " + playerName + " 加入房间");
        return msg;
    }

    /**
     * 创建等待开始消息
     */
    public static MahjongMessage waiting(String message) {
        MahjongMessage msg = new MahjongMessage();
        msg.setType("WAITING");
        msg.setMessage(message);
        return msg;
    }

    /**
     * 创建游戏开始消息
     */
    public static MahjongMessage gameStart(List<MahjongTile> hand, List<Map<String, Object>> players,
                                           int dealerIndex, int totalRounds) {
        MahjongMessage msg = new MahjongMessage();
        msg.setType("GAME_START");
        msg.setMessage("游戏开始");
        msg.setHand(hand);
        msg.setPlayers(players);
        msg.setDealerIndex(dealerIndex);
        msg.setTotalRounds(totalRounds);
        msg.setCurrentRound(1);
        return msg;
    }

    /**
     * 创建摸牌消息
     */
    public static MahjongMessage drawTile(MahjongTile tile, boolean canWin) {
        MahjongMessage msg = new MahjongMessage();
        msg.setType("DRAW_TILE");
        msg.setMessage("摸牌");
        msg.setTile(tile);
        msg.setCanWin(canWin);
        return msg;
    }

    /**
     * 创建打牌消息
     */
    public static MahjongMessage discardTile(String playerId, MahjongTile tile,
                                             int nextPlayerIndex, int remainingTiles) {
        MahjongMessage msg = new MahjongMessage();
        msg.setType("DISCARD_TILE");
        msg.setMessage("打牌");
        msg.setPlayerId(playerId);
        msg.setTile(tile);
        msg.setCurrentPlayerIndex(nextPlayerIndex);
        msg.setRemainingTiles(remainingTiles);
        return msg;
    }

    /**
     * 创建胡牌消息
     */
    public static MahjongMessage win(String playerId, Map<String, Object> winInfo) {
        MahjongMessage msg = new MahjongMessage();
        msg.setType("WIN");
        msg.setMessage("胡牌");
        msg.setPlayerId(playerId);
        msg.setWinInfo(winInfo);
        return msg;
    }

    /**
     * 创建游戏结束消息
     */
    public static MahjongMessage gameOver(List<Map<String, Object>> scores) {
        MahjongMessage msg = new MahjongMessage();
        msg.setType("GAME_OVER");
        msg.setMessage("游戏结束");
        msg.setScores(scores);
        return msg;
    }

    /**
     * 创建下一局消息
     */
    public static MahjongMessage nextRound(int currentRound, int totalRounds) {
        MahjongMessage msg = new MahjongMessage();
        msg.setType("NEXT_ROUND");
        msg.setMessage("准备下一局");
        msg.setCurrentRound(currentRound);
        msg.setTotalRounds(totalRounds);
        return msg;
    }

    /**
     * 创建玩家离开消息
     */
    public static MahjongMessage playerLeft(String message) {
        MahjongMessage msg = new MahjongMessage();
        msg.setType("PLAYER_LEFT");
        msg.setMessage(message);
        return msg;
    }

    /**
     * 创建错误消息
     */
    public static MahjongMessage error(String message) {
        MahjongMessage msg = new MahjongMessage();
        msg.setType("ERROR");
        msg.setMessage(message);
        return msg;
    }

    /**
     * 创建房间状态更新消息
     */
    public static MahjongMessage roomUpdate(List<Map<String, Object>> players) {
        MahjongMessage msg = new MahjongMessage();
        msg.setType("ROOM_UPDATE");
        msg.setMessage("房间状态更新");
        msg.setPlayers(players);
        return msg;
    }

    /**
     * 创建可以碰/杠的通知消息
     */
    public static MahjongMessage canMeld(MahjongTile tile, boolean canPeng, boolean canGang) {
        MahjongMessage msg = new MahjongMessage();
        msg.setType("CAN_MELD");
        msg.setMessage("可以碰/杠");
        msg.setTile(tile);
        msg.setCanPeng(canPeng);
        msg.setCanGang(canGang);
        return msg;
    }

    /**
     * 创建碰牌消息
     */
    public static MahjongMessage peng(String playerId, MahjongMeld meld, int currentPlayerIndex) {
        MahjongMessage msg = new MahjongMessage();
        msg.setType("PENG");
        msg.setMessage("碰牌");
        msg.setPlayerId(playerId);
        msg.setMeld(meld);
        msg.setCurrentPlayerIndex(currentPlayerIndex);
        return msg;
    }

    /**
     * 创建杠牌消息
     */
    public static MahjongMessage gang(String playerId, MahjongMeld meld, int currentPlayerIndex) {
        MahjongMessage msg = new MahjongMessage();
        msg.setType("GANG");
        msg.setMessage("杠牌");
        msg.setPlayerId(playerId);
        msg.setMeld(meld);
        msg.setCurrentPlayerIndex(currentPlayerIndex);
        return msg;
    }
}
