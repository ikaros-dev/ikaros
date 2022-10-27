package run.ikaros.server.enums;

import java.util.Comparator;
import run.ikaros.server.entity.SeasonEntity;

/**
 * @author li-guohao
 */
public enum SeasonType {
    FIRST(1),
    /**
     * 正篇第二季
     */
    SECOND(2),
    /**
     * 正篇第三季
     */
    THIRD(3),
    /**
     * 正篇第四季
     */
    FOUR(4),
    /**
     * 正篇第五季
     */
    FIFTH(5),
    /**
     * 正篇第六季
     */
    SIXTH(6),

    /**
     * 特别篇 其一
     */
    SPECIAL_FIRST(11),
    /**
     * 特别篇 其二
     */
    SPECIAL_SECOND(12),
    /**
     * 特别篇 其三
     */
    SPECIAL_THIRD(12),


    /**
     * 宣传短片 (PV)
     */
    PROMOTION_VIDEO(61),

    /**
     * 片头曲 (OP)
     */
    OPENING_SONG(71),
    /**
     * 片尾曲 (ED)
     */
    ENDING_SONG(72),

    /**
     * SP
     */
    SPECIAL_PROMOTION(81),
    /**
     * 小剧场
     */
    SMALL_THEATER(82),
    /**
     * LIVE
     */
    LIVE(83),
    /**
     * CM
     */
    CM(84),

    /**
     * 其它
     */
    OTHER(90);

    /**
     * 从小到大 => 值越小越考前
     */
    private final int order;

    SeasonType(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }

    /**
     * 从小到大 => order值越小越考前
     */
    public static class OrderComparator implements Comparator<SeasonType> {
        @Override
        public int compare(SeasonType o1, SeasonType o2) {
            if (o1.getOrder() == o2.getOrder()) {
                return 0;
            }
            return o1.getOrder() > o2.getOrder() ? 1 : -1;
        }
    }

}
