package run.ikaros.server.store.entity;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@NoArgsConstructor
@AllArgsConstructor
public class CustomMetadataEntity {
    @Id
    private UUID id;
    @Column("custom_id")
    private UUID customId;
    @Column("cm_key")
    private String key;
    @Column("cm_value")
    private byte[] value;
}
