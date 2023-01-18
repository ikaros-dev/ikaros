package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import run.ikaros.server.store.entity.AuthorityEntity;

public interface AuthorityRepository extends R2dbcRepository<AuthorityEntity, Long> {
}
