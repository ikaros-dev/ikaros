package cn.liguohao.ikaros.service;

import cn.liguohao.ikaros.common.JacksonConverter;
import cn.liguohao.ikaros.common.constants.InitConstants;
import cn.liguohao.ikaros.model.entity.BoxTypeEntity;
import cn.liguohao.ikaros.repository.BoxRepository;
import cn.liguohao.ikaros.repository.BoxTypeRepository;
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
public class BoxService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BoxService.class);

    private final BoxRepository repository;
    private final BoxTypeRepository typeRepository;

    public BoxService(BoxRepository repository, BoxTypeRepository typeRepository) {
        this.repository = repository;
        this.typeRepository = typeRepository;
    }

    /**
     * init all preset box type records
     *
     * @see InitConstants#PRESET_BOX_TYPES
     */
    public void initPresetTypeRecords() {
        for (String presetType : InitConstants.PRESET_BOX_TYPES) {
            if (typeRepository.existsByName(presetType)) {
                BoxTypeEntity existBoxTypeEntity = typeRepository.findByName(presetType);
                if (existBoxTypeEntity.getStatus()) {
                    continue;
                }
                existBoxTypeEntity.setStatus(true);
                existBoxTypeEntity = typeRepository.saveAndFlush(existBoxTypeEntity);
                LOGGER.debug("update box type record status to true: {}",
                    JacksonConverter.obj2Json(existBoxTypeEntity));
            } else {
                BoxTypeEntity boxTypeEntity = new BoxTypeEntity()
                    .setName(presetType);
                boxTypeEntity = typeRepository.saveAndFlush(boxTypeEntity);
                LOGGER.debug("init add new box type record: {}",
                    JacksonConverter.obj2Json(boxTypeEntity));
            }
        }
    }
}
