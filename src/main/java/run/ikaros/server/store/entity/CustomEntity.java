package run.ikaros.server.store.entity;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Table;

/**
 * custom resource definition entity.
 *
 * @author: li-guohao
 * @see <a href="https://kubernetes.io/zh-cn/docs/tasks/extend-kubernetes/custom-resources/custom-resource-definitions/">KubernatesCRD</a>
 * @see <a href="https://spec.openapis.org/oas/v3.1.0">OpenAPI v3</a>
 * @see <a href="https://github.com/halo-dev/rfcs/tree/main/extension">Halo Extension</a>
 */
@Data
@Builder
@Table("custom")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class CustomEntity extends BaseEntity {
    private String name;
    private byte[] data;
}
