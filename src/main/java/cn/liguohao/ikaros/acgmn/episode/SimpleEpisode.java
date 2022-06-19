package cn.liguohao.ikaros.acgmn.episode;

import java.time.LocalDateTime;

/**
 * @author li-guohao
 * @date 2022/06/19
 */
public class SimpleEpisode implements Episode {

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

    public SimpleEpisode setPath(String path) {
        this.path = path;
        return this;
    }

    @Override
    public String title() {
        return title;
    }

    public SimpleEpisode setTitle(String title) {
        this.title = title;
        return this;
    }

    @Override
    public String shortTitle() {
        return shortTitle;
    }

    public SimpleEpisode setShortTitle(String shortTitle) {
        this.shortTitle = shortTitle;
        return this;
    }

    @Override
    public LocalDateTime dataAddedTime() {
        return dataAddedTime;
    }

    public SimpleEpisode setDataAddedTime(LocalDateTime dataAddedTime) {
        this.dataAddedTime = dataAddedTime;
        return this;
    }

    @Override
    public Integer episodeNumber() {
        return episodeNumber;
    }

    public SimpleEpisode setEpisodeNumber(Integer episodeNumber) {
        this.episodeNumber = episodeNumber;
        return this;
    }

    @Override
    public String overview() {
        return overview;
    }

    public SimpleEpisode setOverview(String overview) {
        this.overview = overview;
        return this;
    }

    @Override
    public LocalDateTime airTime() {
        return airTime;
    }

    public SimpleEpisode setAirTime(LocalDateTime airTime) {
        this.airTime = airTime;
        return this;
    }
}
