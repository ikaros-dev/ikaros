package run.ikaros.server.entity;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity(name = "song")
public class SongEntity extends BaseEntity {
    @Column(nullable = false)
    private String url;
    /**
     * Map的JSON格式
     */
    private String metadata;
    /**
     * @see SongMenuEntity#id
     */
    private Long menuId;
    /**
     * @see AlbumEntity#id
     */
    private Long albumId;

    public String getUrl() {
        return url;
    }

    public SongEntity setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getMetadata() {
        return metadata;
    }

    public SongEntity setMetadata(String metadata) {
        this.metadata = metadata;
        return this;
    }

    public Long getMenuId() {
        return menuId;
    }

    public SongEntity setMenuId(Long menuId) {
        this.menuId = menuId;
        return this;
    }

    public Long getAlbumId() {
        return albumId;
    }

    public SongEntity setAlbumId(Long albumId) {
        this.albumId = albumId;
        return this;
    }
}
