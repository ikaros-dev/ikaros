package cn.liguohao.ikaros.model.entity.anime;

import cn.liguohao.ikaros.model.entity.BaseEntity;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * @author guohao
 * @date 2022/09/10
 */
@Entity
@Table(name = "episode")
public class EpisodeEntity extends BaseEntity {

    private String url;

    /**
     * 第几集
     */
    private Long seq;

    private String title;

    @Column(name = "original_title")
    private String originalTitle;

    @Column(name = "air_time")
    private Date airTime;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private String overview;

    /**
     * 时长，单位秒
     */
    private Long duration;


    public String getUrl() {
        return url;
    }

    public EpisodeEntity setUrl(String url) {
        this.url = url;
        return this;
    }

    public Long getSeq() {
        return seq;
    }

    public EpisodeEntity setSeq(Long seq) {
        this.seq = seq;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public EpisodeEntity setTitle(String title) {
        this.title = title;
        return this;
    }

    public Date getAirTime() {
        return airTime;
    }

    public EpisodeEntity setAirTime(Date airTime) {
        this.airTime = airTime;
        return this;
    }

    public String getOverview() {
        return overview;
    }

    public EpisodeEntity setOverview(String overview) {
        this.overview = overview;
        return this;
    }

    public Long getDuration() {
        return duration;
    }

    public EpisodeEntity setDuration(Long duration) {
        this.duration = duration;
        return this;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public EpisodeEntity setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
        return this;
    }

}
