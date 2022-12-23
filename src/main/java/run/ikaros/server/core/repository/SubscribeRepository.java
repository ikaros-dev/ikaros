package run.ikaros.server.core.repository;

import run.ikaros.server.entity.SubscribeEntity;
import run.ikaros.server.enums.SubscribeType;

import java.util.List;
import java.util.Optional;

public interface SubscribeRepository extends BaseRepository<SubscribeEntity, Long> {
    Optional<SubscribeEntity> findByStatusAndUserIdAndTypeAndTargetId(Boolean status,
                                                                          Long userId,
                                                                          SubscribeType type,
                                                                          Long targetId);

    Optional<SubscribeEntity> findByUserIdAndTypeAndTargetId(Long userId,
                                                                 SubscribeType type,
                                                                 Long targetId);

    List<SubscribeEntity> findByUserIdAndStatus(Long userId, Boolean status);

}
