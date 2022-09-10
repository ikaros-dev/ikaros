package cn.liguohao.ikaros.model;

import cn.liguohao.ikaros.entity.anime.SeasonEntity;
import java.util.List;

/**
 * @author guohao
 * @date 2022/09/10
 */
public class Season extends SeasonEntity {

    private List<Episode> episodes;


    public List<Episode> getEpisodes() {
        return episodes;
    }

    public Season setEpisodes(List<Episode> episodes) {
        this.episodes = episodes;
        return this;
    }
}
