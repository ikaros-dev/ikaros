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
@Table(name = "subject_character")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class SubjectCharacterEntity extends BaseEntity {
    @Column("subject_id")
    private Long subjectId;
    @Column("character_id")
    private Long characterId;
}
