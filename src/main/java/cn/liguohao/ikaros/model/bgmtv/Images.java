package cn.liguohao.ikaros.model.bgmtv;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 */
public class Images {
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

    public Images setLarge(String large) {
        this.large = large;
        return this;
    }

    public String getCommon() {
        return common;
    }

    public Images setCommon(String common) {
        this.common = common;
        return this;
    }

    public String getMedium() {
        return medium;
    }

    public Images setMedium(String medium) {
        this.medium = medium;
        return this;
    }

    public String getSmall() {
        return small;
    }

    public Images setSmall(String small) {
        this.small = small;
        return this;
    }

    public String getGrid() {
        return grid;
    }

    public Images setGrid(String grid) {
        this.grid = grid;
        return this;
    }
}
