package cn.liguohao.ikaros.persistence.structural.entity;

import cn.liguohao.ikaros.acgmn.episode.Episode;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * @author li-guohao
 * @date 2022/06/19
 */
@Entity
@Table(name = "episode")
public class EpisodeEntity extends BaseEntity implements Episode {

    private String path;
    private String title;
    private String shortTitle;
    private LocalDateTime dataAddedTime;
    private Integer episodeNumber;
    private String overview;
    private LocalDateTime airTime;

    @Override
    public String path() {
        return path;
    }

    public EpisodeEntity setPath(String path) {
        this.path = path;
        return this;
    }

    @Override
    public String title() {
        return title;
    }

    public EpisodeEntity setTitle(String title) {
        this.title = title;
        return this;
    }

    @Override
    public String shortTitle() {
        return shortTitle;
    }

    public EpisodeEntity setShortTitle(String shortTitle) {
        this.shortTitle = shortTitle;
        return this;
    }

    @Override
    public LocalDateTime dataAddedTime() {
        return dataAddedTime;
    }

    public EpisodeEntity setDataAddedTime(LocalDateTime dataAddedTime) {
        this.dataAddedTime = dataAddedTime;
        return this;
    }

    @Override
    public Integer episodeNumber() {
        return episodeNumber;
    }

    public EpisodeEntity setEpisodeNumber(Integer episodeNumber) {
        this.episodeNumber = episodeNumber;
        return this;
    }

    @Override
    public String overview() {
        return overview;
    }

    public EpisodeEntity setOverview(String overview) {
        this.overview = overview;
        return this;
    }

    @Override
    public LocalDateTime airTime() {
        return airTime;
    }

    public EpisodeEntity setAirTime(LocalDateTime airTime) {
        this.airTime = airTime;
        return this;
    }
}
