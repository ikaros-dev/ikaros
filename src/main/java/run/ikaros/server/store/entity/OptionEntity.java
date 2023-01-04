package run.ikaros.server.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.relational.core.mapping.Table;

/**
 * option entity.
 *
 * @author: li-guohao
 */
@Data
@Entity
@Table(name = "option")
@EqualsAndHashCode(callSuper = true)
public class OptionEntity extends BaseEntity {
    @Column(nullable = false)
    private String name;
}
