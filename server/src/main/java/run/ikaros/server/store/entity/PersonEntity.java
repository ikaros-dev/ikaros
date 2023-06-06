package run.ikaros.server.store.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Table;
import run.ikaros.api.store.entity.BaseEntity;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "person")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class PersonEntity extends BaseEntity {
    private String name;
    private String infobox;
    private String summary;
}
