package com.jeizas.model.mahjong;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 麻将牌实体类
 *
 * @author jeizas
 * @date 2025-12-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MahjongTile {

    /** 牌的类型：万(WAN)、条(TIAO)、筒(TONG)、风(FENG)、箭(JIAN) */
    private TileType type;

    /** 牌的值：1-9 (万/条/筒), 1-4 (风：东南西北), 1-3 (箭：中发白) */
    private int value;

    /**
     * 牌的类型枚举
     */
    public enum TileType {
        WAN("万"),
        TIAO("条"),
        TONG("筒"),
        FENG("风"),
        JIAN("箭");

        private final String displayName;

        TileType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 获取牌的字符串表示
     *
     * @return 牌的字符串表示
     */
    @Override
    public String toString() {
        if (type == TileType.FENG) {
            String[] fengNames = {"东", "南", "西", "北"};
            return fengNames[value - 1];
        } else if (type == TileType.JIAN) {
            String[] jianNames = {"中", "发", "白"};
            return jianNames[value - 1];
        } else {
            return value + type.getDisplayName();
        }
    }

    /**
     * 创建牌的唯一ID
     *
     * @return 牌的唯一标识符
     */
    public String getTileId() {
        return type.name() + "_" + value;
    }

    /**
     * 重写equals方法，基于type和value进行比较
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MahjongTile that = (MahjongTile) o;
        return value == that.value && type == that.type;
    }

    /**
     * 重写hashCode方法，基于type和value计算哈希值
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(type, value);
    }
}
