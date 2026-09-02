package run.ikaros.plugin;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/** 插件生命周期持久化记录；Manifest 作为版本化 JSON 保存。 */
@Table("plugin")
public record PluginEntity(@Id UUID id, @Column("plugin_id") String pluginId,
                           @Column("manifest_json") String manifestJson,
                           String status, @Column("granted_permissions_json") String grantedPermissionsJson,
                           @Column("created_at") Instant createdAt, @Column("updated_at") Instant updatedAt) {
}
