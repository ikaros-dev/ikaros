package run.ikaros.server.store.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Table(name = "subject_image")
@EqualsAndHashCode(callSuper = true)
public class SubjectImageEntity extends BaseEntity {
    @Column("subject_id")
    private Long subjectId;
    private String large;
    private String common;
    private String medium;
    private String small;
    private String grid;
}
