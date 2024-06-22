package run.ikaros.server.store.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import run.ikaros.api.store.enums.AuthorityType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "authority")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class AuthorityEntity extends BaseEntity {
    @Column("role_id")
    private Long roleId;
    private Boolean allow;
    private AuthorityType type;
    private String target;
    private String authority;
}
