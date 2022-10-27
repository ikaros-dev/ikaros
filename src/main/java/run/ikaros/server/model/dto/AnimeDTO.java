package run.ikaros.server.model.dto;

import run.ikaros.server.entity.AnimeEntity;
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
