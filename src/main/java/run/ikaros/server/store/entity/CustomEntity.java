package run.ikaros.server.store.entity;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * [WIP] custom resource definition entity.
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
    @Column("api_version")
    private String apiVersion;
    private String kind;
    /**
     * The name is unique globally.
     */
    private String name;
    /**
     * Generate name is for generating metadata name automatically.
     */
    private String generateName;
    /**
     * Labels are like key-value format.
     */
    private byte[] labels;
    /**
     * Annotations are like key-value format.
     */
    private byte[] annotations;

}
