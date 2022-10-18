package cn.liguohao.ikaros.service;


import static cn.liguohao.ikaros.common.UnitTestConstants.PROCESS_SHOUT_NOT_RUN_THIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import cn.liguohao.ikaros.exceptions.RecordNotFoundException;
import cn.liguohao.ikaros.model.dto.OptionItemDTO;
import cn.liguohao.ikaros.model.entity.OptionEntity;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author guohao
 * @date 2022/10/18
 */
@SpringBootTest
class OptionServiceTest {

    @Autowired
    OptionService optionService;

    private String buildStr() {
        return UUID.randomUUID().toString().substring(0, 20);
    }

    @Test
    void crud() throws RecordNotFoundException {
        String key = buildStr();
        try {
            optionService.findOptionItemByKey(key);
            fail(PROCESS_SHOUT_NOT_RUN_THIS);
        } catch (RecordNotFoundException e) {
            assertNotNull(e);
        }

        String value = buildStr();
        OptionItemDTO optionItemDTO = new OptionItemDTO(key, value);
        OptionEntity exceptedOptionEntity = optionService.saveOptionItem(optionItemDTO);
        assertNotNull(exceptedOptionEntity);
        assertEquals(key, exceptedOptionEntity.getKey());

        OptionEntity optionEntity = optionService.findOptionItemByKey(key);
        assertNotNull(optionEntity);
        assertEquals(exceptedOptionEntity.getKey(), optionEntity.getKey());
        assertEquals(exceptedOptionEntity.getValue(), optionEntity.getValue());
        assertEquals(exceptedOptionEntity.getCreateTime(), optionEntity.getCreateTime());
        assertEquals(exceptedOptionEntity.getUpdateTime().getTime(),
            optionEntity.getUpdateTime().getTime());

        String newValue = buildStr();
        assertNotEquals(value, newValue);

        optionItemDTO.setValue(newValue);
        OptionEntity newValueOptionEntity = optionService.saveOptionItem(optionItemDTO);
        assertNotNull(newValueOptionEntity);
        assertEquals(newValue, newValueOptionEntity.getValue());
        assertNotEquals(optionEntity.getUpdateTime().getTime(),
            newValueOptionEntity.getUpdateTime().getTime());
        assertTrue(newValueOptionEntity.getUpdateTime().getTime()
            - optionEntity.getUpdateTime().getTime() > 0);

        OptionEntity newValueOptionEntityByKey = optionService.findOptionItemByKey(key);
        assertNotNull(newValueOptionEntityByKey);
        assertEquals(newValue, newValueOptionEntityByKey.getValue());


        optionService.deleteOptionItemByKey(key);
        try {
            optionService.findOptionItemByKey(key);
            fail(PROCESS_SHOUT_NOT_RUN_THIS);
        } catch (RecordNotFoundException e) {
            assertNotNull(e);
        }

    }


}