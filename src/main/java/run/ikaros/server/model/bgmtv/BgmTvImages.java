package run.ikaros.server.model.bgmtv;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 */
public class BgmTvImages {
    @JsonProperty("large")
    private String large;
    @JsonProperty("common")
    private String common;
    @JsonProperty("medium")
    private String medium;
    @JsonProperty("small")
    private String small;
    @JsonProperty("grid")
    private String grid;

    public String getLarge() {
        return large;
    }

    public BgmTvImages setLarge(String large) {
        this.large = large;
        return this;
    }

    public String getCommon() {
        return common;
    }

    public BgmTvImages setCommon(String common) {
        this.common = common;
        return this;
    }

    public String getMedium() {
        return medium;
    }

    public BgmTvImages setMedium(String medium) {
        this.medium = medium;
        return this;
    }

    public String getSmall() {
        return small;
    }

    public BgmTvImages setSmall(String small) {
        this.small = small;
        return this;
    }

    public String getGrid() {
        return grid;
    }

    public BgmTvImages setGrid(String grid) {
        this.grid = grid;
        return this;
    }
}
