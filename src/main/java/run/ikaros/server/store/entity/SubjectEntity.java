package run.ikaros.server.store.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * a subject contain a file(or folder) and metadata.
 *
 * @author liguohao
 */
@Data
@Table(name = "subject")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class SubjectEntity extends BaseEntity {

    @Column("file_id")
    private Long fileId;
    @Column("box_id")
    private Long boxId;
}