package cn.liguohao.ikaros.model.entity.anime;

import cn.liguohao.ikaros.model.entity.BaseEntity;
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
@Table(name = "season")
public class SeasonEntity extends BaseEntity {

    public enum Type {
        FIRST,
        SECOND,
        THIRD,
        FOUR,
        FIFTH,
        SIXTH,

        PROMOTION_VIDEO,
        OPENING_SONG,
        ENDING_SONG,

        SPECIAL_PROMOTION_VIDEO_FIRST,
        SPECIAL_PROMOTION_VIDEO_SECOND,

        SMALL_THEATER;
    }


    private String title;

    @Column(name = "original_title")
    private String originalTitle;

    private Type type;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private String overview;


    public String getTitle() {
        return title;
    }

    public SeasonEntity setTitle(String title) {
        this.title = title;
        return this;
    }

    public Type getType() {
        return type;
    }

    public SeasonEntity setType(Type type) {
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
}
