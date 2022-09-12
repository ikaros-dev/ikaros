package cn.liguohao.ikaros.model.entity;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Objects;

/**
 * @author liguohao
 */
@Entity
@Table(name = "role")
public class RoleEntity extends BaseEntity {

    private String name;

    public String getName() {
        return name;
    }

    public RoleEntity setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoleEntity roleEntity)) {
            return false;
        }
        return Objects.equals(name, roleEntity.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}