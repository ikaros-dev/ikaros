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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "subject_person")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class SubjectPersonEntity extends BaseEntity {
    @Column("subject_id")
    private Long subjectId;
    @Column("person_id")
    private Long personId;
}
