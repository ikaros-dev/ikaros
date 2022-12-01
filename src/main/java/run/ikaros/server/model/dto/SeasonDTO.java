package run.ikaros.server.model.dto;

import run.ikaros.server.entity.SeasonEntity;
import java.util.List;

/**
 * @author guohao
 * @date 2022/09/10
 */
public class SeasonDTO extends SeasonEntity {

    private List<EpisodeDTO> episodeDTOS;

    public List<EpisodeDTO> getEpisodes() {
        return episodeDTOS;
    }

    public SeasonDTO setEpisodes(List<EpisodeDTO> episodeDTOS) {
        this.episodeDTOS = episodeDTOS;
        return this;
    }
}
