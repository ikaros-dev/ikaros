package run.ikaros.server.store.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * setting entity.
 *
 * @author: li-guohao
 */
@Data
@Table(name = "setting")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class SettingEntity extends BaseEntity {
    private String category;
    @Column("setting_key")
    private String key;
    @Column("setting_value")
    private String value;
}
