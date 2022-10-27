package run.ikaros.server.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static run.ikaros.server.common.UnitTestConst.PROCESS_SHOUT_NOT_RUN_THIS;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import run.ikaros.server.entity.OptionEntity;
import run.ikaros.server.enums.OptionCategory;
import run.ikaros.server.exceptions.RecordNotFoundException;
import run.ikaros.server.model.dto.OptionItemDTO;
import run.ikaros.server.service.impl.OptionServiceImpl;

/**
 * @author guohao
 * @date 2022/10/18
 */
@SpringBootTest
class OptionServiceImplTest {

    @Autowired
    OptionServiceImpl optionServiceImpl;

    private String buildStr() {
        return UUID.randomUUID().toString().substring(0, 20);
    }

    @Test
    void crud() throws RecordNotFoundException {
        String key = buildStr();
        try {
            optionServiceImpl.findOptionItemByKey(key);
            fail(PROCESS_SHOUT_NOT_RUN_THIS);
        } catch (RecordNotFoundException e) {
            assertNotNull(e);
        }

        String value = buildStr();
        OptionItemDTO optionItemDTO = new OptionItemDTO(key, value);
        OptionEntity exceptedOptionEntity = optionServiceImpl.saveOptionItem(optionItemDTO);
        assertNotNull(exceptedOptionEntity);
        assertEquals(key, exceptedOptionEntity.getKey());

        OptionEntity optionEntity = optionServiceImpl.findOptionItemByKey(key);
        assertNotNull(optionEntity);
        assertEquals(exceptedOptionEntity.getKey(), optionEntity.getKey());
        assertEquals(exceptedOptionEntity.getValue(), optionEntity.getValue());
        assertEquals(exceptedOptionEntity.getCreateTime(), optionEntity.getCreateTime());
        assertEquals(exceptedOptionEntity.getUpdateTime().getTime(),
            optionEntity.getUpdateTime().getTime());

        String newValue = buildStr();
        assertNotEquals(value, newValue);

        optionItemDTO.setValue(newValue);
        OptionEntity newValueOptionEntity = optionServiceImpl.saveOptionItem(optionItemDTO);
        assertNotNull(newValueOptionEntity);
        assertEquals(newValue, newValueOptionEntity.getValue());
        assertNotEquals(optionEntity.getUpdateTime().getTime(),
            newValueOptionEntity.getUpdateTime().getTime());
        assertTrue(newValueOptionEntity.getUpdateTime().getTime()
            - optionEntity.getUpdateTime().getTime() > 0);

        OptionEntity newValueOptionEntityByKey = optionServiceImpl.findOptionItemByKey(key);
        assertNotNull(newValueOptionEntityByKey);
        assertEquals(newValue, newValueOptionEntityByKey.getValue());


        optionServiceImpl.deleteOptionItemByKey(key);
        try {
            optionServiceImpl.findOptionItemByKey(key);
            fail(PROCESS_SHOUT_NOT_RUN_THIS);
        } catch (RecordNotFoundException e) {
            assertNotNull(e);
        }

    }

    @Test
    void findOptionByCategory() throws RecordNotFoundException {

        OptionEntity optionEntity1
            = optionServiceImpl.saveOptionItem(new OptionItemDTO(buildStr(), buildStr()));
        OptionEntity optionEntity2
            = optionServiceImpl.saveOptionItem(new OptionItemDTO(buildStr(), buildStr()));
        OptionEntity optionEntity3 = optionServiceImpl.saveOptionItem(
            new OptionItemDTO(buildStr(), buildStr()).setCategory(OptionCategory.OTHER));

        List<OptionEntity> optionListByCategory =
            optionServiceImpl.findOptionByCategory(OptionCategory.OTHER);

        assertNotNull(optionListByCategory);
        assertFalse(optionListByCategory.isEmpty());

        Set<String> keys = optionListByCategory.stream().flatMap(
            (Function<OptionEntity, Stream<String>>) optionEntity
                -> Stream.of(optionEntity.getKey())).collect(Collectors.toSet());

        Assertions.assertTrue(keys.contains(optionEntity1.getKey()));
        Assertions.assertTrue(keys.contains(optionEntity2.getKey()));
        assertFalse(keys.contains(optionEntity3.getKey()));

        optionServiceImpl.deleteOptionItemByKey(optionEntity1.getKey());
        optionServiceImpl.deleteOptionItemByKey(optionEntity2.getKey());
        optionServiceImpl.deleteOptionItemByKey(optionEntity3.getKey());
    }

    @Test
    void findOptionValueByCategoryAndKey() throws RecordNotFoundException {

        String key = buildStr();
        String value = buildStr();
        OptionCategory category = OptionCategory.OTHER;

        try {
            optionServiceImpl.findOptionValueByCategoryAndKey(category, key);
            fail(PROCESS_SHOUT_NOT_RUN_THIS);
        } catch (RecordNotFoundException e) {
            assertNotNull(e);
        }

        OptionItemDTO optionItemDTO = new OptionItemDTO(key, value).setCategory(OptionCategory.OTHER);
        optionServiceImpl.saveOptionItem(optionItemDTO);

        OptionEntity searchOptionEntity =
            optionServiceImpl.findOptionValueByCategoryAndKey(category, key);
        assertNotNull(searchOptionEntity);
        assertEquals(key, searchOptionEntity.getKey());
        assertEquals(value, searchOptionEntity.getValue());

        optionServiceImpl.deleteOptionItemByKey(key);
    }



}