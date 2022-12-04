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
 * @author li-guohao
 * @date 2022/09/10
 */
@Entity
@Table(name = "anime")
public class AnimeEntity extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(name = "title_cn", nullable = false)
    private String titleCn;

    @Column(name = "bgmtv_id")
    private Long bgmtvId;

    private String platform;

    @Lob  @Basic(fetch=LAZY)
    @Type(type = "org.hibernate.type.TextType")
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

    public String getTitleCn() {
        return titleCn;
    }

    public AnimeEntity setTitleCn(String titleCn) {
        this.titleCn = titleCn;
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
