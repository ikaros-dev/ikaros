package run.ikaros.server.core.service;

import run.ikaros.server.entity.KVEntity;
import run.ikaros.server.enums.KVType;

import javax.annotation.Nonnull;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;

public interface KVService {
    @Nonnull
    @Transactional(rollbackOn = Exception.class)
    KVEntity save(@Nonnull KVEntity kvEntity);

    @Nonnull
    Map<String, String> findMikanEpUrlBgmTvSubjectIdMap();

    @Nonnull
    Map<String, String> findBgmTvSubjectIdMikanEpUrlMap();

    @Nonnull
    Map<String, String> findMikanTorrentNameBgmTvSubjectIdMap();

    @Nonnull
    Map<String, String> findBgmTvSubjectIdMikanTorrentNameMap();

    @Nonnull
    List<KVEntity> findKVEntitiesByTypeAndKeyLike(@Nonnull KVType type, @Nonnull String key);
}
