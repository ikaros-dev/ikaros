package cn.liguohao.ikaros.acgmn.subject;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author li-guohao
 * @date 2022/06/19
 */
public class SimpleAnimeSubject implements AnimeSubject {

    private String mainTitle;
    private String[] otherLocaleTitle;
    private LocalDateTime airStartTime;
    private Map<String, String> staff;
    private String publishOrganization;
    private String overview;
    private String[] otherLocaleDescription;
    private Integer episodeCount;
    private Integer airStatus;

    @Override
    public Integer airStatus() {
        return airStatus;
    }

    public SimpleAnimeSubject setAirStatus(Integer airStatus) {
        this.airStatus = airStatus;
        return this;
    }

    @Override
    public String mainTitle() {
        return mainTitle;
    }

    public SimpleAnimeSubject setMainTitle(String mainTitle) {
        this.mainTitle = mainTitle;
        return this;
    }

    @Override
    public String[] otherLocaleTitle() {
        return otherLocaleTitle;
    }

    public SimpleAnimeSubject setOtherLocaleTitle(String[] otherLocaleTitle) {
        this.otherLocaleTitle = otherLocaleTitle;
        return this;
    }

    @Override
    public LocalDateTime airStartTime() {
        return airStartTime;
    }

    public SimpleAnimeSubject setAirStartTime(LocalDateTime airStartTime) {
        this.airStartTime = airStartTime;
        return this;
    }

    @Override
    public Map<String, String> staff() {
        return staff;
    }

    public SimpleAnimeSubject setStaff(Map<String, String> staff) {
        this.staff = staff;
        return this;
    }

    @Override
    public String publishOrganization() {
        return publishOrganization;
    }

    public SimpleAnimeSubject setPublishOrganization(String publishOrganization) {
        this.publishOrganization = publishOrganization;
        return this;
    }

    @Override
    public String overview() {
        return overview;
    }

    public SimpleAnimeSubject setOverview(String overview) {
        this.overview = overview;
        return this;
    }

    @Override
    public String[] otherLocaleDescription() {
        return otherLocaleDescription;
    }

    public SimpleAnimeSubject setOtherLocaleDescription(String[] otherLocaleDescription) {
        this.otherLocaleDescription = otherLocaleDescription;
        return this;
    }

    @Override
    public Integer episodeCount() {
        return episodeCount;
    }

    public SimpleAnimeSubject setEpisodeCount(Integer episodeCount) {
        this.episodeCount = episodeCount;
        return this;
    }
}
