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
@Table(name = "config")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class ConfigEntity extends BaseEntity {
    @Column("c_name")
    private String name;
    @Column("c_key")
    private String key;
    @Column("c_value")
    private String value;
}
