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
@Table(name = "anime_season")
public class AnimeSeasonEntity extends BaseEntity {

    @Column(name = "season_id", nullable = false)
    private Long seasonId;

    @Column(name = "anime_id", nullable = false)
    private Long animeId;


    public Long getSeasonId() {
        return seasonId;
    }

    public AnimeSeasonEntity setSeasonId(Long seasonId) {
        this.seasonId = seasonId;
        return this;
    }

    public Long getAnimeId() {
        return animeId;
    }

    public AnimeSeasonEntity setAnimeId(Long animeId) {
        this.animeId = animeId;
        return this;
    }
}
