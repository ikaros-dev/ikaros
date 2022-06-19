package cn.liguohao.ikaros.acgmn.episode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 剧集
 *
 * @author li-guohao
 * @date 2022/06/19
 */
public interface Episode {

    /**
     * 剧集项数据地址
     *
     * @return 剧集文件地址
     */
    String path();

    /**
     * 具体的标题
     *
     * @return 标题
     */
    String title();

    /**
     * 短标题，标题的简化版
     *
     * @return 短标题
     */
    String shortTitle();

    /**
     * 数据添加到系统的时间
     *
     * @return 数据添加到系统的时间
     */
    LocalDateTime dataAddedTime();

    /**
     * 季度号，比如第几季，默认第一季
     *
     * @return 季度号
     */
    default Integer seasonNumber() {
        return 1;
    }

    /**
     * 当前所属剧集号
     *
     * @return 当前所属剧集号
     */
    Integer episodeNumber();

    /**
     * 评分
     *
     * @return 评分
     */
    default BigDecimal communityRating() {
        return null;
    }

    /**
     * 剧集综述(概况)
     *
     * @return 剧集综述
     */
    String overview();

    /**
     * 剧集放送时间
     *
     * @return 剧集放送时间
     */
    LocalDateTime airTime();

    /**
     * 其它拓展的ID，比如其它第三方信息站的ID
     *
     * @return 其它拓展的ID
     */
    default Map<String, Integer> externalIdMap() {
        return Map.of();
    }
}
