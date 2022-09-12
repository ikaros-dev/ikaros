package cn.liguohao.ikaros.model.dto;

import cn.liguohao.ikaros.model.entity.anime.SeasonEntity;
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
