package run.ikaros.server.store.entity;

import java.util.UUID;
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
@Table(name = "person_character")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class PersonCharacterEntity extends BaseEntity {
    @Column("person_id")
    private UUID personId;
    @Column("character_id")
    private UUID characterId;
}
