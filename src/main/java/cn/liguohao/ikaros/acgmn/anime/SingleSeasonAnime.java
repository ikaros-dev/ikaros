package cn.liguohao.ikaros.acgmn.anime;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 默认的单季度动漫
 *
 * @author li-guohao
 * @date 2022/06/19
 */
public class SingleSeasonAnime implements Anime {

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

    @Override
    public String getMainTitle() {
        return mainTitle;
    }

    public SingleSeasonAnime setMainTitle(String mainTitle) {
        this.mainTitle = mainTitle;
        return this;
    }

    @Override
    public String[] getOtherLocaleTitle() {
        return otherLocaleTitle;
    }

    public SingleSeasonAnime setOtherLocaleTitle(String[] otherLocaleTitle) {
        this.otherLocaleTitle = otherLocaleTitle;
        return this;
    }

    @Override
    public LocalDateTime getAirStartTime() {
        return airStartTime;
    }

    public SingleSeasonAnime setAirStartTime(LocalDateTime airStartTime) {
        this.airStartTime = airStartTime;
        return this;
    }

    @Override
    public Map<String, String> getStaff() {
        return staff;
    }

    public SingleSeasonAnime setStaff(Map<String, String> staff) {
        this.staff = staff;
        return this;
    }

    @Override
    public String getPublishOrganization() {
        return publishOrganization;
    }

    public SingleSeasonAnime setPublishOrganization(String publishOrganization) {
        this.publishOrganization = publishOrganization;
        return this;
    }

    @Override
    public String getOverview() {
        return overview;
    }

    public SingleSeasonAnime setOverview(String overview) {
        this.overview = overview;
        return this;
    }

    @Override
    public String[] getOtherLocaleDescription() {
        return otherLocaleDescription;
    }

    public SingleSeasonAnime setOtherLocaleDescription(String[] otherLocaleDescription) {
        this.otherLocaleDescription = otherLocaleDescription;
        return this;
    }

    @Override
    public Integer getEpisodeCount() {
        return episodeCount;
    }

    public SingleSeasonAnime setEpisodeCount(Integer episodeCount) {
        this.episodeCount = episodeCount;
        return this;
    }

    @Override
    public Integer getAirStatus() {
        return airStatus;
    }

    public SingleSeasonAnime setAirStatus(Integer airStatus) {
        this.airStatus = airStatus;
        return this;
    }

    public List<Long> getEpisodeIds() {
        return episodeIds;
    }

    public SingleSeasonAnime setEpisodeIds(List<Long> episodeIds) {
        this.episodeIds = episodeIds;
        return this;
    }
}
