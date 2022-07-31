package cn.liguohao.ikaros.acgmn.anime;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 动漫
 *
 * @author li-guohao
 * @date 2022/06/19
 */
public interface Anime {

    /**
     * 主标题
     *
     * @return 标题
     */
    String getMainTitle();

    /**
     * 其它语言的标题
     *
     * @return 标题数组
     */
    default String[] getOtherLocaleTitle() {
        return new String[] {};
    }

    /**
     * 放送开始日期
     *
     * @return 放送开始日期
     */
    LocalDateTime getAirStartTime();

    /**
     * 制作组全体职员
     *
     * @return 制作组全体职员
     */
    Map<String, String> getStaff();

    /**
     * 制作组
     *
     * @return 制作组
     */
    String getPublishOrganization();

    /**
     * 综述(概况)
     *
     * @return 综述
     */
    String getOverview();

    /**
     * 其它语言的描述(简介)
     *
     * @return 描述数组
     */
    default String[] getOtherLocaleDescription() {
        return new String[] {};
    }

    /**
     * 总剧集数
     *
     * @return 剧集数
     */
    Integer getEpisodeCount();

    /**
     * 动画状态，0-放送中，1-已经完结
     *
     * @return 动画放送状态
     */
    Integer getAirStatus();
}
