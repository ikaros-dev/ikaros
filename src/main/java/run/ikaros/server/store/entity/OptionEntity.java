package run.ikaros.server.store.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Table;

/**
 * option entity.
 *
 * @author: li-guohao
 */
@Data
@Table(name = "option")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class OptionEntity extends BaseEntity {
    private String name;
}
