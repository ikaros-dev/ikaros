package cn.liguohao.ikaros.model;

import cn.liguohao.ikaros.entity.anime.AnimeEntity;
import java.util.List;

/**
 * @author guohao
 * @date 2022/09/10
 */
public class Anime extends AnimeEntity {

    private List<Season> seasons;


    public List<Season> getSeasons() {
        return seasons;
    }

    public Anime setSeasons(List<Season> seasons) {
        this.seasons = seasons;
        return this;
    }
}
