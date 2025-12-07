package com.jeizas.model.mahjong;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 麻将牌组类（碰、杠）
 *
 * @author jeizas
 * @date 2025-12-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MahjongMeld {

    /** 牌组类型 */
    private MeldType type;

    /** 牌组中的牌 */
    private List<MahjongTile> tiles;

    /**
     * 牌组类型枚举
     */
    public enum MeldType {
        PENG("碰"),      // 3张相同的牌
        GANG("杠"),      // 4张相同的牌
        MINGGANG("明杠"), // 从别人打出的牌杠
        ANGANG("暗杠");   // 自己手里的4张牌杠

        private final String displayName;

        MeldType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
