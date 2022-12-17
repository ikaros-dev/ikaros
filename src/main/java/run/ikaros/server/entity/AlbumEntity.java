package run.ikaros.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity(name = "album")
public class AlbumEntity extends BaseEntity {

    @Column(nullable = false)
    private String name;
    private String description;
    /**
     * Map的JSON格式
     */
    private String metadata;

    public String getName() {
        return name;
    }

    public AlbumEntity setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public AlbumEntity setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getMetadata() {
        return metadata;
    }

    public AlbumEntity setMetadata(String metadata) {
        this.metadata = metadata;
        return this;
    }
}
