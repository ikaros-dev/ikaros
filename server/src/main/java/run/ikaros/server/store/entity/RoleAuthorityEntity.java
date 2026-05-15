package run.ikaros.server.store.entity;

import jakarta.persistence.Id;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "role_authority")
@Accessors(chain = true)
public class RoleAuthorityEntity {
    @Id
    private UUID id;
    @Column("role_id")
    private UUID roleId;
    @Column("authority_id")
    private UUID authorityId;
}
