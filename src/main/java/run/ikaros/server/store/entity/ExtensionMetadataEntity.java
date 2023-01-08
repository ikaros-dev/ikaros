package run.ikaros.server.store.entity;

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
@Table("extension_metadata")
@Accessors(chain = true)
public class ExtensionMetadataEntity {
    @Id
    private Long id;
    @Column("e_id")
    private Long extensionId;
    @Column("em_key")
    private String key;
    @Column("em_value")
    private byte[] value;
}
