package run.ikaros.server.model.bgmtv;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author li-guohao
 */
public class BgmTvSearchFilter {
    /**
     * @see BgmTvSubjectType
     */
    private Integer type = BgmTvSubjectType.NONE.getCode();
    private String tag;
    @JsonProperty("air_date")
    private String airDate = ">1970-01-01";
    private String rating = ">1";
    private String rank = ">1";
    private Boolean nsfw;

    public Integer getType() {
        return type;
    }

    public BgmTvSearchFilter setType(Integer type) {
        this.type = type;
        return this;
    }

    public String getTag() {
        return tag;
    }

    public BgmTvSearchFilter setTag(String tag) {
        this.tag = tag;
        return this;
    }

    public String getAirDate() {
        return airDate;
    }

    public BgmTvSearchFilter setAirDate(String airDate) {
        this.airDate = airDate;
        return this;
    }

    public String getRating() {
        return rating;
    }

    public BgmTvSearchFilter setRating(String rating) {
        this.rating = rating;
        return this;
    }

    public String getRank() {
        return rank;
    }

    public BgmTvSearchFilter setRank(String rank) {
        this.rank = rank;
        return this;
    }

    public Boolean getNsfw() {
        return nsfw;
    }

    public BgmTvSearchFilter setNsfw(Boolean nsfw) {
        this.nsfw = nsfw;
        return this;
    }
}
