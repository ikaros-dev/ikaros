package run.ikaros.server.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import run.ikaros.server.entity.KVEntity;
import run.ikaros.server.enums.KVType;

import java.util.List;
import java.util.Optional;

public interface KVRepository extends JpaRepository<KVEntity, Long> {

    Optional<KVEntity> findKVEntityByTypeAndKey(KVType type, String key);

    List<KVEntity> findKVEntitiesByType(KVType type);

    Optional<KVEntity> findKVEntitiesByTypeAndKey(KVType type, String key);

    List<KVEntity> findKVEntitiesByTypeAndKeyLike(KVType type, String key);
}
