package run.ikaros.server.subject.store.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * MetadataEntity is an entity for storing Metadata data into database.
 *
 * @author: li-guohao
 */
@Data
@Table("metadata")
public class MetadataEntity {
    @Id
    private Long id;
    /**
     * This field only for serving optimistic lock value.
     */
    @Version
    private Long version;
    @Column("subject_id")
    private Long subjectId;
    @Column("meta_key")
    private String metaKey;
    @Column("meta_value")
    private String metaValue;
}
