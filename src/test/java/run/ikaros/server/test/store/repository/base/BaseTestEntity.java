package run.ikaros.server.test.store.repository.base;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.ikaros.server.store.entity.BaseEntity;

/**
 * @author: li-guohao
 */
@Data
@Entity
@Table(name = "unit_test_base")
@EqualsAndHashCode(callSuper = true)
public class BaseTestEntity extends BaseEntity {
    private String name;
}
