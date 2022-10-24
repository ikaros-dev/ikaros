package run.ikaros.server.entity.anime;

import run.ikaros.server.entity.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author guohao
 * @date 2022/09/10
 */
@Entity
@Table(name = "anime_tag")
public class AnimeTagEntity extends BaseEntity {

    @Column(name = "anime_id")
    private Long animeId;

    @Column(name = "tag_id")
    private Long tagId;

    public Long getAnimeId() {
        return animeId;
    }

    public AnimeTagEntity setAnimeId(Long animeId) {
        this.animeId = animeId;
        return this;
    }

    public Long getTagId() {
        return tagId;
    }

    public AnimeTagEntity setTagId(Long tagId) {
        this.tagId = tagId;
        return this;
    }
}
