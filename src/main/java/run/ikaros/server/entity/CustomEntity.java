package run.ikaros.server.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

/**
 * 可定制的数据持久化模型，参考自 Halo Extension 和 K8s CRD
 *
 * @author: li-guohao
 */
@Data
@Entity
@Table(name = "custom")
public class CustomEntity {
    /**
     * 全局唯一名称，使用左前查询匹配
     */
    @Id
    private String name;

    /**
     * 数据，使用 Base64 格式编码存储
     */
    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] data;

    /**
     * 乐观锁预留版本字段
     */
    @Version
    private Long version;

    public CustomEntity() {
    }

    public CustomEntity(String name, byte[] data) {
        this.name = name;
        this.data = data;
    }

    public CustomEntity(String name, Long version) {
        this.name = name;
        this.version = version;
    }

    public CustomEntity(String name, byte[] data, Long version) {
        this.name = name;
        this.data = data;
        this.version = version;
    }
}
