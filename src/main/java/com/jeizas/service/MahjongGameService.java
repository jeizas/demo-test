package com.jeizas.service;

import com.jeizas.model.mahjong.MahjongTile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 麻将游戏服务类，处理游戏逻辑
 *
 * @author jeizas
 * @date 2025-12-07
 */
@Service
public class MahjongGameService {

    /**
     * 检查是否可以胡牌（简化规则）
     * 胡牌条件：14张牌，满足以下之一：
     * 1. 一对将 + 4组顺子/刻子
     * 2. 七对子
     *
     * @param hand 手牌
     * @return 是否可以胡牌
     */
    public boolean canWin(List<MahjongTile> hand) {
        if (hand.size() != 14) {
            return false;
        }

        // 检查七对子
        if (isSevenPairs(hand)) {
            return true;
        }

        // 检查标准胡牌（一对将 + 4组顺子/刻子）
        return isStandardWin(hand);
    }

    /**
     * 检查是否是七对子
     *
     * @param hand 手牌
     * @return 是否是七对子
     */
    private boolean isSevenPairs(List<MahjongTile> hand) {
        Map<String, Integer> tileCount = countTiles(hand);

        if (tileCount.size() != 7) {
            return false;
        }

        for (int count : tileCount.values()) {
            if (count != 2) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查是否是标准胡牌（一对将 + 4组顺子/刻子）
     *
     * @param hand 手牌
     * @return 是否是标准胡牌
     */
    private boolean isStandardWin(List<MahjongTile> hand) {
        Map<String, Integer> tileCount = countTiles(hand);

        // 尝试每一种牌作为将牌
        for (String tileId : tileCount.keySet()) {
            if (tileCount.get(tileId) >= 2) {
                // 创建副本并移除将牌
                Map<String, Integer> remaining = new HashMap<>(tileCount);
                remaining.put(tileId, remaining.get(tileId) - 2);
                if (remaining.get(tileId) == 0) {
                    remaining.remove(tileId);
                }

                // 检查剩余牌是否能组成4组顺子/刻子
                if (canFormMelds(remaining)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 检查是否能组成顺子/刻子
     *
     * @param tileCount 牌的数量统计
     * @return 是否能组成顺子/刻子
     */
    private boolean canFormMelds(Map<String, Integer> tileCount) {
        if (tileCount.isEmpty()) {
            return true;
        }

        // 获取第一张牌
        String firstTileId = tileCount.keySet().iterator().next();
        MahjongTile firstTile = parseTileId(firstTileId);

        // 尝试组成刻子（3张相同的牌）
        if (tileCount.get(firstTileId) >= 3) {
            Map<String, Integer> remaining = new HashMap<>(tileCount);
            remaining.put(firstTileId, remaining.get(firstTileId) - 3);
            if (remaining.get(firstTileId) == 0) {
                remaining.remove(firstTileId);
            }
            if (canFormMelds(remaining)) {
                return true;
            }
        }

        // 尝试组成顺子（只有万、条、筒可以组成顺子）
        if (firstTile.getType() != MahjongTile.TileType.FENG &&
            firstTile.getType() != MahjongTile.TileType.JIAN) {

            // 检查是否有连续的3张牌
            if (firstTile.getValue() <= 7) {
                String tile2Id = getTileId(firstTile.getType(), firstTile.getValue() + 1);
                String tile3Id = getTileId(firstTile.getType(), firstTile.getValue() + 2);

                if (tileCount.containsKey(tile2Id) && tileCount.containsKey(tile3Id)) {
                    Map<String, Integer> remaining = new HashMap<>(tileCount);
                    remaining.put(firstTileId, remaining.get(firstTileId) - 1);
                    if (remaining.get(firstTileId) == 0) {
                        remaining.remove(firstTileId);
                    }
                    remaining.put(tile2Id, remaining.get(tile2Id) - 1);
                    if (remaining.get(tile2Id) == 0) {
                        remaining.remove(tile2Id);
                    }
                    remaining.put(tile3Id, remaining.get(tile3Id) - 1);
                    if (remaining.get(tile3Id) == 0) {
                        remaining.remove(tile3Id);
                    }

                    if (canFormMelds(remaining)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 统计每种牌的数量
     *
     * @param hand 手牌
     * @return 牌的数量统计
     */
    private Map<String, Integer> countTiles(List<MahjongTile> hand) {
        Map<String, Integer> count = new HashMap<>();
        for (MahjongTile tile : hand) {
            String id = tile.getTileId();
            count.put(id, count.getOrDefault(id, 0) + 1);
        }
        return count;
    }

    /**
     * 从牌ID解析出牌对象
     *
     * @param tileId 牌ID
     * @return 牌对象
     */
    private MahjongTile parseTileId(String tileId) {
        String[] parts = tileId.split("_");
        MahjongTile.TileType type = MahjongTile.TileType.valueOf(parts[0]);
        int value = Integer.parseInt(parts[1]);
        return new MahjongTile(type, value);
    }

    /**
     * 获取牌ID
     *
     * @param type 牌类型
     * @param value 牌值
     * @return 牌ID
     */
    private String getTileId(MahjongTile.TileType type, int value) {
        return type.name() + "_" + value;
    }

    /**
     * 计算分数（简化规则：基础分10分，自摸加倍）
     *
     * @param hand 手牌
     * @param isSelfDrawn 是否自摸
     * @return 分数
     */
    public int calculateScore(List<MahjongTile> hand, boolean isSelfDrawn) {
        int baseScore = 10;

        // 七对子加倍
        if (isSevenPairs(hand)) {
            baseScore *= 2;
        }

        // 自摸加倍
        if (isSelfDrawn) {
            baseScore *= 2;
        }

        return baseScore;
    }

    /**
     * 获取听牌列表（可以胡哪些牌）
     *
     * @param hand 手牌
     * @return 可以胡的牌列表
     */
    public List<MahjongTile> getTingPai(List<MahjongTile> hand) {
        List<MahjongTile> tingPai = new ArrayList<>();

        if (hand.size() != 13) {
            return tingPai;
        }

        // 尝试所有可能的牌
        for (MahjongTile.TileType type : MahjongTile.TileType.values()) {
            int maxValue = 9;
            if (type == MahjongTile.TileType.FENG) {
                maxValue = 4;
            } else if (type == MahjongTile.TileType.JIAN) {
                maxValue = 3;
            }

            for (int value = 1; value <= maxValue; value++) {
                MahjongTile testTile = new MahjongTile(type, value);
                List<MahjongTile> testHand = new ArrayList<>(hand);
                testHand.add(testTile);

                if (canWin(testHand)) {
                    tingPai.add(testTile);
                }
            }
        }

        return tingPai;
    }
}
