package run.ikaros.server.store.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ikuser_role")
@Accessors(chain = true)
public class UserRoleEntity {
    @Id
    private Long id;
    @Column("user_id")
    private Long userId;
    @Column("role_id")
    private Long roleId;
}
