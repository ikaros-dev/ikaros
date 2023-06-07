package run.ikaros.server.store.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.http.HttpMethod;
import run.ikaros.api.store.entity.BaseEntity;

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
    /**
     * Control api operate method.
     *
     * @see HttpMethod#name()
     */
    private String method;
    private Boolean allow;
    private String url;
}
