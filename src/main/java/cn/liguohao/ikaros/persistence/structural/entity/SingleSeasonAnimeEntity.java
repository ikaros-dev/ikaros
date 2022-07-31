package cn.liguohao.ikaros.persistence.structural.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author liguohao
 * @date 2022/07/31
 */
@Entity
@Table(name = "default_anime")
public class SingleSeasonAnimeEntity extends BaseEntity {
    private String mainTitle;
    private String[] otherLocaleTitle;
    private LocalDateTime airStartTime;
    private Map<String, String> staff;
    private String publishOrganization;
    private String overview;
    private String[] otherLocaleDescription;
    private Integer episodeCount;
    private Integer airStatus;
    private List<Long> episodeIds;

    public String getMainTitle() {
        return mainTitle;
    }

    public SingleSeasonAnimeEntity setMainTitle(String mainTitle) {
        this.mainTitle = mainTitle;
        return this;
    }

    public String[] getOtherLocaleTitle() {
        return otherLocaleTitle;
    }

    public SingleSeasonAnimeEntity setOtherLocaleTitle(String[] otherLocaleTitle) {
        this.otherLocaleTitle = otherLocaleTitle;
        return this;
    }

    public LocalDateTime getAirStartTime() {
        return airStartTime;
    }

    public SingleSeasonAnimeEntity setAirStartTime(LocalDateTime airStartTime) {
        this.airStartTime = airStartTime;
        return this;
    }

    public Map<String, String> getStaff() {
        return staff;
    }

    public SingleSeasonAnimeEntity setStaff(Map<String, String> staff) {
        this.staff = staff;
        return this;
    }

    public String getPublishOrganization() {
        return publishOrganization;
    }

    public SingleSeasonAnimeEntity setPublishOrganization(String publishOrganization) {
        this.publishOrganization = publishOrganization;
        return this;
    }

    public String getOverview() {
        return overview;
    }

    public SingleSeasonAnimeEntity setOverview(String overview) {
        this.overview = overview;
        return this;
    }

    public String[] getOtherLocaleDescription() {
        return otherLocaleDescription;
    }

    public SingleSeasonAnimeEntity setOtherLocaleDescription(String[] otherLocaleDescription) {
        this.otherLocaleDescription = otherLocaleDescription;
        return this;
    }

    public Integer getEpisodeCount() {
        return episodeCount;
    }

    public SingleSeasonAnimeEntity setEpisodeCount(Integer episodeCount) {
        this.episodeCount = episodeCount;
        return this;
    }

    public Integer getAirStatus() {
        return airStatus;
    }

    public SingleSeasonAnimeEntity setAirStatus(Integer airStatus) {
        this.airStatus = airStatus;
        return this;
    }

    public List<Long> getEpisodeIds() {
        return episodeIds;
    }

    public SingleSeasonAnimeEntity setEpisodeIds(List<Long> episodeIds) {
        this.episodeIds = episodeIds;
        return this;
    }
}
