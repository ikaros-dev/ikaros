package run.ikaros.server.test.store.repository.base;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import run.ikaros.server.store.entity.BaseEntity;

/**
 * BaseTestEntity use by BaseRepositoryTests.
 *
 * @see run.ikaros.server.store.repository.BaseRepositoryTests
 * @author: li-guohao
 */
@Entity
@Table(name = "unit_test_base")
public class BaseTestEntity extends BaseEntity {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
