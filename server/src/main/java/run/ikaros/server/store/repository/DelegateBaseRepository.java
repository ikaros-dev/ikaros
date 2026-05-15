package run.ikaros.server.store.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.repository.support.SimpleR2dbcRepository;
import org.springframework.data.relational.repository.query.RelationalEntityInformation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

@Transactional(readOnly = true)
public class DelegateBaseRepository<T> extends SimpleR2dbcRepository<T, UUID>
    implements BaseRepository<T> {

    private final R2dbcEntityOperations entityOperations;

    public DelegateBaseRepository(
        RelationalEntityInformation<T, UUID> entity,
        R2dbcEntityOperations entityOperations,
        R2dbcConverter converter) {
        super(entity, entityOperations, converter);
        this.entityOperations = entityOperations;
    }

    @Override
    @Transactional
    public Mono<T> insert(T objToInsert) {
        Assert.notNull(objToInsert, "Object to insert must not be null");
        return this.entityOperations.insert(objToInsert);
    }

    @Override
    @Transactional
    public Mono<T> update(T objToUpdate) {
        Assert.notNull(objToUpdate, "Object to update must not be null");
        return this.entityOperations.update(objToUpdate);
    }
}
