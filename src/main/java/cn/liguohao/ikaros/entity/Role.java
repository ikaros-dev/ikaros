package cn.liguohao.ikaros.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Objects;

/**
 * @author liguohao
 */
@Entity
@Table(name = "role")
public class Role extends Base {

    private String name;

    public String getName() {
        return name;
    }

    public Role setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Role role)) {
            return false;
        }
        return Objects.equals(name, role.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}