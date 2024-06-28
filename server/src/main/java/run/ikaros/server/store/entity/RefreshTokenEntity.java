package run.ikaros.server.store.entity;

import java.time.LocalDateTime;
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
@Table(name = "refresh_token")
@Accessors(chain = true)
public class RefreshTokenEntity {
    @Id
    private Long id;
    @Column("access_token ")
    private String accessToken;
    @Column("client_id")
    private String clientId;
    @Column("user_id")
    private Long userId;
    private String scope;
    @Column("expires_at")
    private LocalDateTime expiresAt;
}
