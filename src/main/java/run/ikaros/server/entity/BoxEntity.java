package run.ikaros.server.entity;

import run.ikaros.server.constants.InitConst;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "box", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"parent_id", "name"})})
public class BoxEntity extends BaseEntity {
    public enum Type {
        EPISODE(1),
        POSITIVE(2),
        SPECIAL(3),
        OP(4),
        ED(5),
        PV(6),
        CM(7),

        ANIME(11),
        COMIC(12),
        GAME(13),
        MUSIC(14),
        NOVELS(15),

        IP(21),

        OTHER(99);

        private final int code;

        Type(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

    }

    @Column(name = "parent_id", nullable = false)
    private Long parentId = InitConst.ROOT_ID;

    private Integer type = Type.OTHER.getCode();

    /**
     * 盒子名称，要求兄弟盒子不允许重名，即当 parent_id 相同时，不允许 name 重复
     */
    @Column(nullable = false)
    private String name;

    public Long getParentId() {
        return parentId;
    }

    public BoxEntity setParentId(Long parentId) {
        this.parentId = parentId;
        return this;
    }

    public Integer getType() {
        return type;
    }

    public BoxEntity setType(Integer type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public BoxEntity setName(String name) {
        this.name = name;
        return this;
    }
}