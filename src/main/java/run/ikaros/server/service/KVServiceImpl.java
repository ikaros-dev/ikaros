package run.ikaros.server.service;

import org.springframework.stereotype.Service;
import run.ikaros.server.core.repository.KVRepository;
import run.ikaros.server.core.service.KVService;
import run.ikaros.server.entity.KVEntity;
import run.ikaros.server.enums.KVType;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.BeanUtils;
import run.ikaros.server.utils.StringUtils;

import jakarta.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class KVServiceImpl implements KVService {

    private final KVRepository kvRepository;

    public KVServiceImpl(KVRepository kvRepository) {
        this.kvRepository = kvRepository;
    }

    @Nonnull
    @Override
    public KVEntity save(@Nonnull KVEntity kvEntity) {
        AssertUtils.notNull(kvEntity, "kvEntity");
        Long id = kvEntity.getId();
        KVType type = kvEntity.getType();
        String key = kvEntity.getKey();
        KVEntity existKVEntity = null;
        if (id != null) {
            existKVEntity = kvRepository.getReferenceById(id);
        }

        if (existKVEntity == null && type != null && StringUtils.isNotBlank(key)) {
            Optional<KVEntity> existKVEntityOptional =
                kvRepository.findKVEntityByTypeAndKey(type, key);
            if (existKVEntityOptional.isPresent()) {
                existKVEntity = existKVEntityOptional.get();
            }
        }

        if (existKVEntity == null) {
            kvEntity = kvRepository.saveAndFlush(kvEntity);
        } else {
            BeanUtils.copyProperties(kvEntity, existKVEntity);
            kvEntity = kvRepository.saveAndFlush(existKVEntity);
        }
        return kvEntity;
    }

    @Nonnull
    @Override
    public Map<String, String> findMikanEpUrlBgmTvSubjectIdMap() {
        Map<String, String> map = new HashMap<>();
        kvRepository.findKVEntitiesByType(KVType.MIKAN_EP_URL__BGM_TV_SUBJECT_ID)
            .forEach(kvEntity -> map.put(kvEntity.getKey(), kvEntity.getValue()));
        return map;
    }

    @Nonnull
    @Override
    public Map<String, String> findBgmTvSubjectIdMikanEpUrlMap() {
        Map<String, String> map = new HashMap<>();
        kvRepository.findKVEntitiesByType(KVType.MIKAN_EP_URL__BGM_TV_SUBJECT_ID)
            .forEach(kvEntity -> map.put(kvEntity.getValue(), kvEntity.getKey()));
        return map;
    }

    @Nonnull
    @Override
    public Map<String, String> findMikanTorrentNameBgmTvSubjectIdMap() {
        Map<String, String> map = new HashMap<>();
        kvRepository.findKVEntitiesByType(KVType.MIKAN_TORRENT_NAME__BGM_TV_SUBJECT_ID)
            .forEach(kvEntity -> map.put(kvEntity.getKey(), kvEntity.getValue()));
        return map;
    }

    @Nonnull
    @Override
    public Map<String, String> findBgmTvSubjectIdMikanTorrentNameMap() {
        Map<String, String> map = new HashMap<>();
        kvRepository.findKVEntitiesByType(KVType.MIKAN_TORRENT_NAME__BGM_TV_SUBJECT_ID)
            .forEach(kvEntity -> map.put(kvEntity.getValue(), kvEntity.getKey()));
        return map;
    }

    @Nonnull
    @Override
    public List<KVEntity> findKVEntitiesByTypeAndKeyLike(@Nonnull KVType type,
                                                         @Nonnull String key) {
        AssertUtils.notNull(type, "type");
        AssertUtils.notBlank(key, "key");
        return kvRepository.findKVEntitiesByTypeAndKeyLike(type, StringUtils.addLikeChar(key));
    }
}
