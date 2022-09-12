package cn.liguohao.ikaros.model.dto;

import cn.liguohao.ikaros.model.entity.anime.AnimeEntity;
import java.util.List;

/**
 * @author guohao
 * @date 2022/09/10
 */
public class AnimeDTO extends AnimeEntity {

    private List<SeasonDTO> seasonDTOS;


    public List<SeasonDTO> getSeasons() {
        return seasonDTOS;
    }

    public AnimeDTO setSeasons(List<SeasonDTO> seasonDTOS) {
        this.seasonDTOS = seasonDTOS;
        return this;
    }
}
