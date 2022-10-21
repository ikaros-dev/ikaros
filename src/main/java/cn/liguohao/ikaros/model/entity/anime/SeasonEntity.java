package cn.liguohao.ikaros.model.entity.anime;

import cn.liguohao.ikaros.model.entity.BaseEntity;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * @author guohao
 * @date 2022/09/10
 */
@Entity
@Table(name = "season", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"type", "original_title"})})
public class SeasonEntity extends BaseEntity {

    public enum Type {
        /**
         * 正篇第一季
         */
        FIRST(1),
        /**
         * 正篇第二季
         */
        SECOND(2),
        /**
         * 正篇第三季
         */
        THIRD(3),
        /**
         * 正篇第四季
         */
        FOUR(4),
        /**
         * 正篇第五季
         */
        FIFTH(5),
        /**
         * 正篇第六季
         */
        SIXTH(6),

        /**
         * 特别篇 其一
         */
        SPECIAL_FIRST(11),
        /**
         * 特别篇 其二
         */
        SPECIAL_SECOND(12),
        /**
         * 特别篇 其三
         */
        SPECIAL_THIRD(12),


        /**
         * 宣传短片 (PV)
         */
        PROMOTION_VIDEO(61),

        OPENING_SONG(71),
        ENDING_SONG(72),

        SPECIAL_PROMOTION_VIDEO(81),
        SMALL_THEATER(82),

        OTHER(90);

        private final int code;

        Type(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }


    private String title;

    @Column(name = "original_title")
    private String originalTitle;

    private Integer type = Type.FIRST.getCode();

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

    public Integer getType() {
        return type;
    }

    public SeasonEntity setType(Integer type) {
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
