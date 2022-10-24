package run.ikaros.server.entity;

import java.util.Comparator;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.apache.juli.logging.Log;
import run.ikaros.server.enums.SeasonType;

/**
 * @author guohao
 * @date 2022/09/10
 */
@Entity
@Table(name = "season", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"type", "original_title"})})
public class SeasonEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    private SeasonType type = SeasonType.FIRST;

    @Column(name = "anime_id", nullable = false)
    private Long animeId;

    private String title;

    @Column(name = "original_title")
    private String originalTitle;

    @Lob @Basic(fetch = FetchType.LAZY)
    private String overview;


    public String getTitle() {
        return title;
    }

    public SeasonEntity setTitle(String title) {
        this.title = title;
        return this;
    }

    public SeasonType getType() {
        return type;
    }

    public SeasonEntity setType(SeasonType type) {
        this.type = type;
        return this;
    }

    public String getOverview() {
        return overview;
    }

    public SeasonEntity setOverview(String overview) {
        this.overview = overview;
        return this;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public SeasonEntity setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
        return this;
    }

    public Long getAnimeId() {
        return animeId;
    }

    public SeasonEntity setAnimeId(Long animeId) {
        this.animeId = animeId;
        return this;
    }
}
