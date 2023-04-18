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
@Table(name = "profile")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class ProfileEntity extends BaseEntity {
    @Column("p_name")
    private String name;
    @Column("p_key")
    private String key;
    @Column("p_value")
    private String value;
}
