package run.ikaros.server.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.relational.core.mapping.Table;
import run.ikaros.server.store.enums.SubjectType;

/**
 * a subject contain a file(or folder) and metadata.
 *
 * @author liguohao
 */
@Data
@Entity
@Table(name = "subject")
@EqualsAndHashCode(callSuper = true)
public class SubjectEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    private SubjectType type;

    private Long fid;

    @Column(name = "box_id")
    private Long boxId;
}