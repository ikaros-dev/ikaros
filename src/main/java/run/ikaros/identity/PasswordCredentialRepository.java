package run.ikaros.identity;
import java.util.UUID; import org.springframework.data.repository.reactive.ReactiveCrudRepository; import reactor.core.publisher.Mono;
public interface PasswordCredentialRepository extends ReactiveCrudRepository<PasswordCredentialEntity,UUID>{Mono<PasswordCredentialEntity> findByUserId(UUID userId);}
