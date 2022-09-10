package cn.liguohao.ikaros.entity.anime;

import cn.liguohao.ikaros.entity.BaseEntity;
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

    @Column(name = "file_id")
    private Long fileId;

    private String title;

    @Column(name = "original_title")
    private String originalTitle;

    @Column(name = "air_time")
    private Date airTime;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private String overview;

    private Long duration;


    public Long getFileId() {
        return fileId;
    }

    public EpisodeEntity setFileId(Long fileId) {
        this.fileId = fileId;
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
