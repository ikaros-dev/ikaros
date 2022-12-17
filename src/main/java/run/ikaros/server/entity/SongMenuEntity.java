package run.ikaros.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity(name = "song_menu")
public class SongMenuEntity extends BaseEntity {

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

    public SongMenuEntity setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public SongMenuEntity setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getMetadata() {
        return metadata;
    }

    public SongMenuEntity setMetadata(String metadata) {
        this.metadata = metadata;
        return this;
    }
}
