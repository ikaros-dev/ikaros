package run.ikaros.server.store.entity;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * custom resource definition entity metadata.
 *
 * @author: li-guohao
 */
@Data
@Builder
@Table("custom_metadata")
@Accessors(chain = true)
public class CustomMetadataEntity {
    @Id
    private Long id;
    @Column("custom_id")
    private Long customId;
    @Column("cm_key")
    private String key;
    @Column("cm_value")
    private byte[] value;
    private UUID uuid;
}
