package run.ikaros.server.store.entity;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "character")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class CharacterEntity extends BaseEntity {
    private String name;
    private String infobox;
    private String summary;
    private UUID uuid;
}
