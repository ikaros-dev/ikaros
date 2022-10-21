package cn.liguohao.ikaros.service;

import cn.liguohao.ikaros.common.JacksonConverter;
import cn.liguohao.ikaros.common.constants.InitConstants;
import cn.liguohao.ikaros.model.entity.ResourceTypeEntity;
import cn.liguohao.ikaros.repository.ResourceRepository;
import cn.liguohao.ikaros.repository.ResourceTypeRepository;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author guohao
 * @date 2022/10/21
 */
@Service
@Transactional(rollbackOn = Exception.class)
public class ResourceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceService.class);

    private final ResourceRepository repository;
    private final ResourceTypeRepository typeRepository;

    public ResourceService(ResourceRepository repository,
                           ResourceTypeRepository typeRepository) {
        this.repository = repository;
        this.typeRepository = typeRepository;
    }

    /**
     * init all preset resource type records
     *
     * @see InitConstants#PRESET_RESOURCE_TYPES
     */
    public void initPresetTypeRecords() {
        for (String presetType : InitConstants.PRESET_RESOURCE_TYPES) {
            if (typeRepository.existsByName(presetType)) {
                ResourceTypeEntity existResourceTypeEntity = typeRepository.findByName(presetType);
                if (existResourceTypeEntity.getStatus()) {
                    continue;
                }
                existResourceTypeEntity.setStatus(true);
                existResourceTypeEntity = typeRepository.saveAndFlush(existResourceTypeEntity);
                LOGGER.debug("update resource type record status to true: {}",
                    JacksonConverter.obj2Json(existResourceTypeEntity));
            } else {
                ResourceTypeEntity resourceTypeEntity = new ResourceTypeEntity()
                    .setName(presetType);
                resourceTypeEntity = typeRepository.saveAndFlush(resourceTypeEntity);
                LOGGER.debug("init add new resource type record: {}",
                    JacksonConverter.obj2Json(resourceTypeEntity));
            }
        }
    }

}
