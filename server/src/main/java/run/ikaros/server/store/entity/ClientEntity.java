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
@Table(name = "Client")
@Accessors(chain = true)
public class ClientEntity {
    @Id
    private Long id;
    @Column("client_id")
    private String clientId;
    @Column("client_secret")
    private String clientSecret;
    @Column("redirect_uri")
    private String redirectUri;
    private String name;
    private String description;
}
