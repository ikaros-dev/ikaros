package run.ikaros.server.subject.store.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * StatusEntity is an entity for storing Status data into database.
 *
 * @author: li-guohao
 */
@Data
@Table("status")
public class StatusEntity {
    @Id
    private Long id;
    /**
     * This field only for serving optimistic lock value.
     */
    @Version
    private Long version;
    @Column("subject_id")
    private Long subjectId;
    @Column("status_key")
    private String statusKey;
    @Column("status_value")
    private String statusValue;
}
