package com.jeizas.model.mahjong;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 麻将房间实体类
 *
 * @author jeizas
 * @date 2025-12-07
 */
@Data
public class MahjongRoom {

    /** 同步锁对象 */
    private final Object lock = new Object();

    /** 房间ID */
    private String roomId;

    /** 玩家列表（最多4人） */
    private List<MahjongPlayer> players;

    /** 牌堆 */
    private List<MahjongTile> deck;

    /** 当前玩家索引 */
    private int currentPlayerIndex;

    /** 庄家索引 */
    private int dealerIndex;

    /** 游戏是否已开始 */
    private boolean gameStarted;

    /** 游戏是否已结束 */
    private boolean gameOver;

    /** 总轮数 */
    private int totalRounds;

    /** 当前轮数 */
    private int currentRound;

    /** 当前轮的局数（0-3，每人都当过庄家为一轮） */
    private int currentDealerTurn;

    /** 最后打出的牌 */
    private MahjongTile lastDiscardedTile;

    /** 最后打出牌的玩家索引 */
    private int lastDiscardPlayerIndex;

    /**
     * 构造函数
     *
     * @param roomId 房间ID
     */
    public MahjongRoom(String roomId) {
        this.roomId = roomId;
        this.players = new ArrayList<>();
        this.deck = new ArrayList<>();
        this.currentPlayerIndex = 0;
        this.dealerIndex = 0;
        this.gameStarted = false;
        this.gameOver = false;
        this.totalRounds = 1;
        this.currentRound = 0;
        this.currentDealerTurn = 0;
    }

    /**
     * 添加玩家
     *
     * @param player 玩家
     * @return 是否成功添加
     */
    public boolean addPlayer(MahjongPlayer player) {
        if (players.size() >= 4) {
            return false;
        }
        player.setPosition(players.size());
        players.add(player);
        return true;
    }

    /**
     * 移除玩家
     *
     * @param playerId 玩家ID
     * @return 被移除的玩家
     */
    public MahjongPlayer removePlayer(String playerId) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getPlayerId().equals(playerId)) {
                MahjongPlayer player = players.remove(i);
                // 重新分配位置
                for (int j = i; j < players.size(); j++) {
                    players.get(j).setPosition(j);
                }
                return player;
            }
        }
        return null;
    }

    /**
     * 根据玩家ID获取玩家
     *
     * @param playerId 玩家ID
     * @return 玩家对象，未找到返回null
     */
    public MahjongPlayer getPlayer(String playerId) {
        for (MahjongPlayer player : players) {
            if (player.getPlayerId().equals(playerId)) {
                return player;
            }
        }
        return null;
    }

    /**
     * 判断房间是否已满（4人）
     *
     * @return 是否已满
     */
    public boolean isFull() {
        return players.size() == 4;
    }

    /**
     * 判断房间是否为空
     *
     * @return 是否为空
     */
    public boolean isEmpty() {
        return players.isEmpty();
    }

    /**
     * 判断所有玩家是否都准备好
     *
     * @return 是否都准备好
     */
    public boolean allPlayersReady() {
        if (players.size() != 4) {
            return false;
        }
        for (MahjongPlayer player : players) {
            if (!player.isReady()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 初始化牌堆
     */
    public void initDeck() {
        deck.clear();

        // 添加万、条、筒各36张（每种1-9各4张）
        for (MahjongTile.TileType type : new MahjongTile.TileType[]{
                MahjongTile.TileType.WAN,
                MahjongTile.TileType.TIAO,
                MahjongTile.TileType.TONG}) {
            for (int value = 1; value <= 9; value++) {
                for (int i = 0; i < 4; i++) {
                    deck.add(new MahjongTile(type, value));
                }
            }
        }

        // 添加风牌16张（东南西北各4张）
        for (int value = 1; value <= 4; value++) {
            for (int i = 0; i < 4; i++) {
                deck.add(new MahjongTile(MahjongTile.TileType.FENG, value));
            }
        }

        // 添加箭牌12张（中发白各4张）
        for (int value = 1; value <= 3; value++) {
            for (int i = 0; i < 4; i++) {
                deck.add(new MahjongTile(MahjongTile.TileType.JIAN, value));
            }
        }

        // 洗牌
        Collections.shuffle(deck);
    }

    /**
     * 发牌
     */
    public void dealTiles() {
        // 每人发13张牌
        for (MahjongPlayer player : players) {
            player.getHand().clear();
            for (int i = 0; i < 13; i++) {
                if (!deck.isEmpty()) {
                    player.addTile(deck.remove(0));
                }
            }
        }

        // 庄家多摸一张（第14张）
        if (!deck.isEmpty()) {
            players.get(dealerIndex).addTile(deck.remove(0));
        }
    }

    /**
     * 摸牌
     *
     * @return 摸到的牌，牌堆空返回null
     */
    public MahjongTile drawTile() {
        if (deck.isEmpty()) {
            return null;
        }
        return deck.remove(0);
    }

    /**
     * 获取当前玩家
     *
     * @return 当前玩家
     */
    public MahjongPlayer getCurrentPlayer() {
        if (currentPlayerIndex >= 0 && currentPlayerIndex < players.size()) {
            return players.get(currentPlayerIndex);
        }
        return null;
    }

    /**
     * 下一个玩家
     */
    public void nextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % 4;
    }

    /**
     * 开始新一局
     */
    public void startNewGame() {
        // 重置所有玩家
        for (MahjongPlayer player : players) {
            player.reset();
        }

        // 设置庄家
        players.get(dealerIndex).setDealer(true);

        // 初始化并发牌
        initDeck();
        dealTiles();

        // 庄家先出牌
        currentPlayerIndex = dealerIndex;
        gameStarted = true;
        gameOver = false;
        lastDiscardedTile = null;
        lastDiscardPlayerIndex = -1;
    }

    /**
     * 结束当前局，准备下一局
     */
    public void endCurrentGame() {
        currentDealerTurn++;

        // 如果4个人都当过庄家，进入下一轮
        if (currentDealerTurn >= 4) {
            currentDealerTurn = 0;
            currentRound++;

            // 检查是否所有轮次都完成
            if (currentRound >= totalRounds) {
                gameOver = true;
                return;
            }
        }

        // 下一个庄家
        players.get(dealerIndex).setDealer(false);
        dealerIndex = (dealerIndex + 1) % 4;
        gameStarted = false;
    }

    /**
     * 重置房间
     */
    public void reset() {
        deck.clear();
        currentPlayerIndex = 0;
        dealerIndex = 0;
        gameStarted = false;
        gameOver = false;
        currentRound = 0;
        currentDealerTurn = 0;

        for (MahjongPlayer player : players) {
            player.reset();
            player.setScore(0);
            player.setDealer(false);
        }
    }
}
