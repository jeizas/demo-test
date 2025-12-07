package com.jeizas.model.mahjong;

import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;

/**
 * 麻将玩家实体类
 *
 * @author jeizas
 * @date 2025-12-07
 */
@Data
public class MahjongPlayer {

    /** 玩家ID */
    private String playerId;

    /** 玩家名称 */
    private String playerName;

    /** 是否是机器人 */
    private boolean isBot;

    /** WebSocket会话（机器人为null） */
    private WebSocketSession session;

    /** 手牌 */
    private List<MahjongTile> hand;

    /** 打出的牌 */
    private List<MahjongTile> discardedTiles;

    /** 碰/杠的牌组 */
    private List<MahjongMeld> melds;

    /** 分数 */
    private int score;

    /** 是否是庄家 */
    private boolean isDealer;

    /** 座位位置：0=东, 1=南, 2=西, 3=北 */
    private int position;

    /** 是否准备 */
    private boolean ready;

    /**
     * 构造函数
     *
     * @param playerId 玩家ID
     * @param playerName 玩家名称
     * @param isBot 是否是机器人
     * @param session WebSocket会话
     */
    public MahjongPlayer(String playerId, String playerName, boolean isBot, WebSocketSession session) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.isBot = isBot;
        this.session = session;
        this.hand = new ArrayList<>();
        this.discardedTiles = new ArrayList<>();
        this.melds = new ArrayList<>();
        this.score = 0;
        this.ready = false;
    }

    /**
     * 添加牌到手牌
     *
     * @param tile 要添加的牌
     */
    public void addTile(MahjongTile tile) {
        hand.add(tile);
        sortHand();
    }

    /**
     * 从手牌中移除牌
     *
     * @param tile 要移除的牌
     * @return 是否成功移除
     */
    public boolean removeTile(MahjongTile tile) {
        // 使用equals方法进行比较（基于type和value）
        return hand.remove(tile);
    }

    /**
     * 打出一张牌
     *
     * @param tile 要打出的牌
     */
    public void discardTile(MahjongTile tile) {
        removeTile(tile);
        discardedTiles.add(tile);
    }

    /**
     * 整理手牌（按类型和值排序）
     */
    private void sortHand() {
        hand.sort((t1, t2) -> {
            if (t1.getType() != t2.getType()) {
                return t1.getType().compareTo(t2.getType());
            }
            return Integer.compare(t1.getValue(), t2.getValue());
        });
    }

    /**
     * 碰牌
     *
     * @param tile 要碰的牌
     * @return 是否成功碰牌
     */
    public boolean peng(MahjongTile tile) {
        // 检查手牌中是否有2张相同的牌
        int count = 0;
        for (MahjongTile t : hand) {
            if (t.equals(tile)) {
                count++;
            }
        }

        if (count >= 2) {
            // 从手牌中移除2张相同的牌
            for (int i = 0; i < 2; i++) {
                removeTile(tile);
            }

            // 创建碰的牌组
            List<MahjongTile> meldTiles = new ArrayList<>();
            meldTiles.add(tile);
            meldTiles.add(new MahjongTile(tile.getType(), tile.getValue()));
            meldTiles.add(new MahjongTile(tile.getType(), tile.getValue()));

            MahjongMeld meld = new MahjongMeld(MahjongMeld.MeldType.PENG, meldTiles);
            melds.add(meld);

            return true;
        }

        return false;
    }

    /**
     * 明杠（从别人打出的牌杠）
     *
     * @param tile 要杠的牌
     * @return 是否成功杠牌
     */
    public boolean mingGang(MahjongTile tile) {
        // 检查手牌中是否有3张相同的牌
        int count = 0;
        for (MahjongTile t : hand) {
            if (t.equals(tile)) {
                count++;
            }
        }

        if (count >= 3) {
            // 从手牌中移除3张相同的牌
            for (int i = 0; i < 3; i++) {
                removeTile(tile);
            }

            // 创建明杠的牌组
            List<MahjongTile> meldTiles = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                meldTiles.add(new MahjongTile(tile.getType(), tile.getValue()));
            }

            MahjongMeld meld = new MahjongMeld(MahjongMeld.MeldType.MINGGANG, meldTiles);
            melds.add(meld);

            return true;
        }

        return false;
    }

    /**
     * 暗杠（自己手里的4张牌）
     *
     * @param tile 要杠的牌
     * @return 是否成功杠牌
     */
    public boolean anGang(MahjongTile tile) {
        // 检查手牌中是否有4张相同的牌
        int count = 0;
        for (MahjongTile t : hand) {
            if (t.equals(tile)) {
                count++;
            }
        }

        if (count >= 4) {
            // 从手牌中移除4张相同的牌
            for (int i = 0; i < 4; i++) {
                removeTile(tile);
            }

            // 创建暗杠的牌组
            List<MahjongTile> meldTiles = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                meldTiles.add(new MahjongTile(tile.getType(), tile.getValue()));
            }

            MahjongMeld meld = new MahjongMeld(MahjongMeld.MeldType.ANGANG, meldTiles);
            melds.add(meld);

            return true;
        }

        return false;
    }

    /**
     * 补杠（已经碰过的牌，再摸到一张可以补杠）
     *
     * @param tile 要补杠的牌
     * @return 是否成功补杠
     */
    public boolean buGang(MahjongTile tile) {
        // 检查是否有碰过这张牌
        for (MahjongMeld meld : melds) {
            if (meld.getType() == MahjongMeld.MeldType.PENG
                    && meld.getTiles().get(0).equals(tile)) {
                // 检查手牌中是否有这张牌
                if (removeTile(tile)) {
                    // 将碰转换为杠
                    meld.setType(MahjongMeld.MeldType.GANG);
                    meld.getTiles().add(new MahjongTile(tile.getType(), tile.getValue()));
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查是否可以碰牌
     *
     * @param tile 要碰的牌
     * @return 是否可以碰牌
     */
    public boolean canPeng(MahjongTile tile) {
        int count = 0;
        for (MahjongTile t : hand) {
            if (t.equals(tile)) {
                count++;
            }
        }
        return count >= 2;
    }

    /**
     * 检查是否可以明杠
     *
     * @param tile 要杠的牌
     * @return 是否可以明杠
     */
    public boolean canMingGang(MahjongTile tile) {
        int count = 0;
        for (MahjongTile t : hand) {
            if (t.equals(tile)) {
                count++;
            }
        }
        return count >= 3;
    }

    /**
     * 重置玩家状态（新一局）
     */
    public void reset() {
        hand.clear();
        discardedTiles.clear();
        melds.clear();
        ready = false;
    }
}
