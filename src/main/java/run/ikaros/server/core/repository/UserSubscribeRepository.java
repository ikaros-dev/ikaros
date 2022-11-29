package run.ikaros.server.core.repository;

import run.ikaros.server.entity.UserSubscribeEntity;
import run.ikaros.server.enums.SubscribeProgress;
import run.ikaros.server.enums.SubscribeType;

import java.util.Optional;

public interface UserSubscribeRepository extends BaseRepository<UserSubscribeEntity, Long> {

    Optional<UserSubscribeEntity> findByStatusAndUserIdAndTypeAndTargetId(Boolean status,
                                                                          Long userId,
                                                                          SubscribeType type,
                                                                          Long targetId);

}
