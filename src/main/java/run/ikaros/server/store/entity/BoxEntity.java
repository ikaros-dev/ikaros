package run.ikaros.server.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * a container for store resource logically.
 *
 * @author liguohao
 */
@Data
@Entity
@Table(name = "box")
@EqualsAndHashCode(callSuper = true)
public class BoxEntity extends BaseEntity {

    @Column(name = "parent_id", nullable = false)
    private Long parentId;

    @Column(nullable = false)
    private String name;
}