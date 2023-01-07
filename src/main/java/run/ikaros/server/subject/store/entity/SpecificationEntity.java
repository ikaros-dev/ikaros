package run.ikaros.server.subject.store.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * SpecificationEntity is an entity for storing Specification data into database.
 *
 * @author: li-guohao
 */
@Data
@Table("specification")
public class SpecificationEntity {
    @Id
    private Long id;
    /**
     * This field only for serving optimistic lock value.
     */
    @Version
    private Long version;
    @Column("subject_id")
    private Long subjectId;
    @Column("spec_key")
    private String specKey;
    @Column("spec_value")
    private String specValue;
}
