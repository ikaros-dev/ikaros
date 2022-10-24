package run.ikaros.server.entity;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author guohao
 * @date 2022/09/10
 */
@Entity
@Table(name = "tag")
public class TagEntity extends BaseEntity {

    private String name;


    public String getName() {
        return name;
    }

    public TagEntity setName(String name) {
        this.name = name;
        return this;
    }
}
