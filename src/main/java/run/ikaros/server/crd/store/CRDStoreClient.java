package run.ikaros.server.crd.store;

import jakarta.annotation.Nonnull;
import org.springframework.transaction.annotation.Transactional;
import run.ikaros.server.crd.CustomResourceDefinition;
import run.ikaros.server.entity.CustomEntity;

import java.util.List;
import java.util.Optional;

/**
 * @author: li-guohao
 */
public interface CRDStoreClient {

    List<CustomEntity> listByNamePrefix(@Nonnull String prefix);

    Optional<CustomEntity> fetchByName(String name);

    @Transactional
    CustomEntity create(String name, byte[] data);

    @Transactional
    CustomEntity update(String name, byte[] data, Long version);

    @Transactional
    CustomEntity delete(String name, Long version);
}
