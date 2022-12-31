package run.ikaros.server.crd.store;

import org.springframework.stereotype.Service;
import run.ikaros.server.core.repository.CustomRepository;
import run.ikaros.server.crd.CustomResourceDefinition;
import run.ikaros.server.entity.CustomEntity;

import java.util.List;
import java.util.Optional;

/**
 * @author: li-guohao
 */
@Service
public class CRDStoreClientJpaImpl implements CRDStoreClient {
    private final CustomRepository repository;

    public CRDStoreClientJpaImpl(CustomRepository repository) {
        this.repository = repository;
    }


    @Override
    public List<CustomEntity> listByNamePrefix(String prefix) {
        return repository.findAllByNameStartingWith(prefix);
    }

    @Override
    public Optional<CustomEntity> fetchByName(String name) {
        return repository.findById(name);
    }

    @Override
    public CustomEntity create(String name, byte[] data) {
        CustomEntity customEntity = new CustomEntity(name, data);
        return repository.save(customEntity);
    }

    @Override
    public CustomEntity update(String name, byte[] data, Long version) {
        CustomEntity customEntity = new CustomEntity(name, data, version);
        return repository.save(customEntity);
    }

    @Override
    public CustomEntity delete(String name, Long version) {
        CustomEntity customEntity = new CustomEntity(name, version);
        return repository.save(customEntity);
    }
}
