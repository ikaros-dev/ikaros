package run.ikaros.server.store.entity;

import static jakarta.persistence.FetchType.LAZY;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.ikaros.server.store.enums.MetadataType;

/**
 * option unit entity, key-value type, only save diminutive option config item.
 *
 * @author: li-guohao
 */
@Data
@Entity
@Table(name = "metadata")
@EqualsAndHashCode(callSuper = true)
public class MetadataEntity extends BaseEntity {
    @Column(name = "host_id", nullable = false)
    private Long hostId;
    @Column(name = "meta_key", nullable = false)
    private String key;
    @Basic(fetch = LAZY)
    @Column(name = "meta_value", length = 50000)
    private String value;
    @Enumerated(EnumType.STRING)
    private MetadataType type;
}
