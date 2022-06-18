package cn.liguohao.ikaros.subject;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 动漫的条目，这里定义的是标准的单季度多集数的动漫的条目。
 *
 * @author li-guohao
 * @date 2022/06/19
 */
public interface AnimeSubject extends Subject {

    /**
     * 条目主标题
     *
     * @return 标题
     */
    String mainTitle();

    /**
     * 其它语言的标题
     *
     * @return 标题数组
     */
    default String[] otherLocaleTitle() {
        return new String[] {};
    }

    /**
     * 放送开始日期
     *
     * @return 放送开始日期
     */
    LocalDateTime airStartTime();

    /**
     * 制作组全体职员
     *
     * @return 制作组全体职员
     */
    Map<String, String> staff();

    /**
     * 制作组
     *
     * @return 制作组
     */
    String publishOrganization();

    /**
     * 描述(简介)
     *
     * @return 描述
     */
    String description();

    /**
     * 其它语言的描述(简介)
     *
     * @return 描述数组
     */
    default String[] otherLocaleDescription() {
        return new String[] {};
    }

    /**
     * 总剧集数
     *
     * @return 剧集数
     */
    int episodeCount();
}
