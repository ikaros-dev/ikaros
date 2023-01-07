package run.ikaros.server.store.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * option unit entity, key-value type, only save diminutive option config item.
 *
 * @author: li-guohao
 */
@Data
@Table(name = "metadata")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class MetadataEntity extends BaseEntity {
    @Column("host_id")
    private Long hostId;
    @Column("meta_key")
    private String key;
    @Column("meta_value")
    private String value;
    private String type;
}
