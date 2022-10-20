package cn.liguohao.ikaros.model.bgmtv;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 剧集信息
 */
public class Episode {
    private Integer id;
    private EpisodeType type;
    private String name;
    @JsonProperty("name_cn")
    private String nameCn;
    private Integer sort;
    private Integer ep;
    @JsonProperty("airdate")
    private String airDate;
    private Integer comment;
    /**
     * 维基人填写的原始时长
     */
    private String duration;
    /**
     * 简介
     */
    private String desc;
    /**
     * 音乐曲目的碟片数
     */
    private String disc;
    @JsonProperty("duration_seconds")
    private Integer durationSeconds;


    public Integer getId() {
        return id;
    }

    public Episode setId(Integer id) {
        this.id = id;
        return this;
    }

    public EpisodeType getType() {
        return type;
    }

    public Episode setType(EpisodeType type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public Episode setName(String name) {
        this.name = name;
        return this;
    }

    public String getNameCn() {
        return nameCn;
    }

    public Episode setNameCn(String nameCn) {
        this.nameCn = nameCn;
        return this;
    }

    public Integer getSort() {
        return sort;
    }

    public Episode setSort(Integer sort) {
        this.sort = sort;
        return this;
    }

    public Integer getEp() {
        return ep;
    }

    public Episode setEp(Integer ep) {
        this.ep = ep;
        return this;
    }

    public String getAirDate() {
        return airDate;
    }

    public Episode setAirDate(String airDate) {
        this.airDate = airDate;
        return this;
    }

    public Integer getComment() {
        return comment;
    }

    public Episode setComment(Integer comment) {
        this.comment = comment;
        return this;
    }

    public String getDuration() {
        return duration;
    }

    public Episode setDuration(String duration) {
        this.duration = duration;
        return this;
    }

    public String getDesc() {
        return desc;
    }

    public Episode setDesc(String desc) {
        this.desc = desc;
        return this;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public Episode setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
        return this;
    }

    public String getDisc() {
        return disc;
    }

    public Episode setDisc(String disc) {
        this.disc = disc;
        return this;
    }
}
