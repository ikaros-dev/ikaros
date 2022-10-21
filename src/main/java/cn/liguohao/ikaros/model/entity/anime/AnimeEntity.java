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
@Table(name = "anime")
public class AnimeEntity extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(name = "bgmtv_id")
    private Long bgmtvId;

    @Column(name = "original_title", nullable = false)
    private String originalTitle;

    private String platform;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private String overview;

    private String producer;

    @Column(name = "cover_url")
    private String coverUrl;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private String staffs;

    private Date airTime;


    public String getTitle() {
        return title;
    }

    public AnimeEntity setTitle(String title) {
        this.title = title;
        return this;
    }

    public Long getBgmtvId() {
        return bgmtvId;
    }

    public AnimeEntity setBgmtvId(Long bgmtvId) {
        this.bgmtvId = bgmtvId;
        return this;
    }

    public String getOverview() {
        return overview;
    }

    public AnimeEntity setOverview(String overview) {
        this.overview = overview;
        return this;
    }

    public String getProducer() {
        return producer;
    }

    public AnimeEntity setProducer(String producer) {
        this.producer = producer;
        return this;
    }

    public String getStaffs() {
        return staffs;
    }

    public AnimeEntity setStaffs(String staffs) {
        this.staffs = staffs;
        return this;
    }

    public Date getAirTime() {
        return airTime;
    }

    public AnimeEntity setAirTime(Date airTime) {
        this.airTime = airTime;
        return this;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public AnimeEntity setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
        return this;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public AnimeEntity setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
        return this;
    }

    public String getPlatform() {
        return platform;
    }

    public AnimeEntity setPlatform(String platform) {
        this.platform = platform;
        return this;
    }
}
