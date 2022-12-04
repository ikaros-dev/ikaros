package run.ikaros.server.entity;

import org.hibernate.annotations.Type;

import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.Table;

import static javax.persistence.FetchType.LAZY;

/**
 * @author guohao
 * @date 2022/09/10
 */
@Entity
@Table(name = "episode")
public class EpisodeEntity extends BaseEntity {

    @Column(name = "season_id", nullable = false)
    private Long seasonId = -1L;

    private String url;

    private Long seq = -1L;

    @Column(nullable = false)
    private String title;

    @Column(name = "original_cn")
    private String titleCn;

    @Column(name = "air_time")
    private Date airTime;

    @Lob  @Basic(fetch=LAZY)
    @Type(type = "org.hibernate.type.TextType")
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

    public Long getSeasonId() {
        return seasonId;
    }

    public EpisodeEntity setSeasonId(Long seasonId) {
        this.seasonId = seasonId;
        return this;
    }

    public String getTitleCn() {
        return titleCn;
    }

    public EpisodeEntity setTitleCn(String titleCn) {
        this.titleCn = titleCn;
        return this;
    }
}
