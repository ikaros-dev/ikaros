package run.ikaros.server.entity;

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
    private byte[] data;

    /**
     * 乐观锁预留版本字段
     */
    @Version
    private Long version;
}
