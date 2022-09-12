package cn.liguohao.ikaros.model.entity.anime;

import cn.liguohao.ikaros.model.entity.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author guohao
 * @date 2022/09/10
 */
@Entity
@Table(name = "season_episode")
public class SeasonEpisodeEntity extends BaseEntity {

    @Column(name = "season_id")
    private Long seasonId;

    @Column(name = "episode_id")
    private Long episodeId;


    public Long getSeasonId() {
        return seasonId;
    }

    public SeasonEpisodeEntity setSeasonId(Long seasonId) {
        this.seasonId = seasonId;
        return this;
    }

    public Long getEpisodeId() {
        return episodeId;
    }

    public SeasonEpisodeEntity setEpisodeId(Long episodeId) {
        this.episodeId = episodeId;
        return this;
    }
}
