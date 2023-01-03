package run.ikaros.server.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.ikaros.server.store.enums.ResourceType;

/**
 * a resource contain a file(or folder) and metadata.
 *
 * @author liguohao
 */
@Data
@Entity
@Table(name = "resource")
@EqualsAndHashCode(callSuper = true)
public class ResourceEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    private ResourceType type;

    private Long fid;

    @Column(name = "box_id")
    private Long boxId;
}