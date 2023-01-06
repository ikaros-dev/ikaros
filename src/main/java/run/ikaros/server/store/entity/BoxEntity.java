package run.ikaros.server.store.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * a container for store resource logically.
 *
 * @author liguohao
 */
@Data
@Table(name = "box")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class BoxEntity extends BaseEntity {

    @Column("parent_id")
    private Long parentId;

    private String name;
}