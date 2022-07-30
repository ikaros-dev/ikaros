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
    private String originalFilename;
    private LocalDateTime dataAddedTime;
    private Integer episodeNumber;
    private String overview;
    private LocalDateTime airTime;

    @Override
    public String getPath() {
        return path;
    }

    public EpisodeEntity setPath(String path) {
        this.path = path;
        return this;
    }

    @Override
    public String getTitle() {
        return title;
    }

    public EpisodeEntity setTitle(String title) {
        this.title = title;
        return this;
    }

    @Override
    public String getShortTitle() {
        return shortTitle;
    }

    public EpisodeEntity setShortTitle(String shortTitle) {
        this.shortTitle = shortTitle;
        return this;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public EpisodeEntity setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
        return this;
    }

    @Override
    public LocalDateTime getDataAddedTime() {
        return dataAddedTime;
    }

    public EpisodeEntity setDataAddedTime(LocalDateTime dataAddedTime) {
        this.dataAddedTime = dataAddedTime;
        return this;
    }

    @Override
    public Integer getEpisodeNumber() {
        return episodeNumber;
    }

    public EpisodeEntity setEpisodeNumber(Integer episodeNumber) {
        this.episodeNumber = episodeNumber;
        return this;
    }

    @Override
    public String getOverview() {
        return overview;
    }

    public EpisodeEntity setOverview(String overview) {
        this.overview = overview;
        return this;
    }

    @Override
    public LocalDateTime getAirTime() {
        return airTime;
    }

    public EpisodeEntity setAirTime(LocalDateTime airTime) {
        this.airTime = airTime;
        return this;
    }
}
