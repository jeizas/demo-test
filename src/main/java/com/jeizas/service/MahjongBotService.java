package com.jeizas.service;

import com.jeizas.model.mahjong.MahjongPlayer;
import com.jeizas.model.mahjong.MahjongTile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 麻将机器人服务类
 *
 * @author jeizas
 * @date 2025-12-07
 */
@Slf4j
@Service
public class MahjongBotService {

    @Autowired
    private MahjongGameService gameService;

    /**
     * 机器人选择要打出的牌
     * 策略：
     * 1. 如果能胡牌，不打牌（等待胡牌）
     * 2. 优先打出孤张（不能组成顺子或刻子的牌）
     * 3. 打出最不容易组成牌型的牌
     *
     * @param player 机器人玩家
     * @param drawnTile 刚摸的牌
     * @return 要打出的牌
     */
    public MahjongTile chooseTileToDiscard(MahjongPlayer player, MahjongTile drawnTile) {
        List<MahjongTile> hand = player.getHand();

        // 检查是否能胡牌
        if (gameService.canWin(hand)) {
            log.info("机器人 {} 可以胡牌！", player.getPlayerName());
            return null; // 不打牌，准备胡牌
        }

        // 计算每张牌的价值
        Map<MahjongTile, Integer> tileValues = new HashMap<>();
        for (MahjongTile tile : hand) {
            int value = calculateTileValue(tile, hand);
            tileValues.put(tile, value);
        }

        // 找到价值最低的牌
        MahjongTile worstTile = null;
        int minValue = Integer.MAX_VALUE;

        for (Map.Entry<MahjongTile, Integer> entry : tileValues.entrySet()) {
            if (entry.getValue() < minValue) {
                minValue = entry.getValue();
                worstTile = entry.getKey();
            }
        }

        log.info("机器人 {} 选择打出: {}", player.getPlayerName(),
                worstTile != null ? worstTile.toString() : "无");

        return worstTile;
    }

    /**
     * 计算牌的价值
     * 价值越高表示越重要，越不应该打出
     *
     * @param tile 要评估的牌
     * @param hand 手牌
     * @return 牌的价值
     */
    private int calculateTileValue(MahjongTile tile, List<MahjongTile> hand) {
        int value = 0;

        // 统计相同牌的数量（对子、刻子）
        int sameCount = 0;
        for (MahjongTile t : hand) {
            if (isSameTile(t, tile)) {
                sameCount++;
            }
        }

        // 对子价值: 20, 刻子价值: 50
        if (sameCount == 2) {
            value += 20;
        } else if (sameCount >= 3) {
            value += 50;
        }

        // 检查顺子潜力（只有万、条、筒可以组成顺子）
        if (tile.getType() != MahjongTile.TileType.FENG &&
            tile.getType() != MahjongTile.TileType.JIAN) {

            // 检查前后是否有相邻的牌
            int sequenceValue = 0;

            // 检查 tile-2, tile-1
            if (tile.getValue() >= 3) {
                if (hasTile(hand, tile.getType(), tile.getValue() - 1)) {
                    sequenceValue += 10;
                }
                if (hasTile(hand, tile.getType(), tile.getValue() - 2)) {
                    sequenceValue += 5;
                }
            }

            // 检查 tile+1, tile+2
            if (tile.getValue() <= 7) {
                if (hasTile(hand, tile.getType(), tile.getValue() + 1)) {
                    sequenceValue += 10;
                }
                if (hasTile(hand, tile.getType(), tile.getValue() + 2)) {
                    sequenceValue += 5;
                }
            }

            value += sequenceValue;
        }

        // 中间牌（4-6）比边缘牌（1,2,8,9）更有价值
        if (tile.getType() != MahjongTile.TileType.FENG &&
            tile.getType() != MahjongTile.TileType.JIAN) {
            if (tile.getValue() >= 4 && tile.getValue() <= 6) {
                value += 5;
            }
        }

        return value;
    }

    /**
     * 判断两张牌是否相同
     */
    private boolean isSameTile(MahjongTile t1, MahjongTile t2) {
        return t1.getType() == t2.getType() && t1.getValue() == t2.getValue();
    }

    /**
     * 检查手牌中是否有指定的牌
     */
    private boolean hasTile(List<MahjongTile> hand, MahjongTile.TileType type, int value) {
        for (MahjongTile tile : hand) {
            if (tile.getType() == type && tile.getValue() == value) {
                return true;
            }
        }
        return false;
    }

    /**
     * 机器人决定是否要胡牌
     *
     * @param player 机器人玩家
     * @param tile 打出的牌或摸到的牌
     * @return 是否要胡牌
     */
    public boolean shouldWin(MahjongPlayer player, MahjongTile tile) {
        List<MahjongTile> testHand = new ArrayList<>(player.getHand());
        testHand.add(tile);
        return gameService.canWin(testHand);
    }

    /**
     * 机器人延迟执行动作（模拟思考时间）
     *
     * @return 延迟时间（毫秒）
     */
    public long getThinkingDelay() {
        // 随机1-3秒的思考时间
        return 1000 + new Random().nextInt(2000);
    }
}
