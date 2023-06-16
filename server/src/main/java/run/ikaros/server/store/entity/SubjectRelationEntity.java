package run.ikaros.server.store.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import run.ikaros.api.store.entity.BaseEntity;
import run.ikaros.api.store.enums.SubjectRelationType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "subject_relation")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class SubjectRelationEntity extends BaseEntity {
    @Column("subject_id")
    private Long subjectId;
    @Column("relation_type")
    private SubjectRelationType relationType;
    @Column("relation_subject_id")
    private Long relationSubjectId;
}
