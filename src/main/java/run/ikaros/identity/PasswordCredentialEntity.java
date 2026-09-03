package run.ikaros.identity;
import java.time.Instant; import java.util.UUID; import org.springframework.data.annotation.Id; import org.springframework.data.annotation.Version; import org.springframework.data.relational.core.mapping.Column; import org.springframework.data.relational.core.mapping.Table;
@Table("password_credential") public record PasswordCredentialEntity(@Id UUID id,@Column("user_id") UUID userId,@Column("password_hash") String passwordHash,@Column("created_at") Instant createdAt,@Column("updated_at") Instant updatedAt,@Version Long version) {}
