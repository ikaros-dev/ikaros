package cn.liguohao.ikaros.service;


import static cn.liguohao.ikaros.common.UnitTestConstants.PROCESS_SHOUT_NOT_RUN_THIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import cn.liguohao.ikaros.common.constants.OptionConstants;
import cn.liguohao.ikaros.core.model.OptionModel;
import cn.liguohao.ikaros.exceptions.NotSupportException;
import cn.liguohao.ikaros.exceptions.RecordNotFoundException;
import cn.liguohao.ikaros.model.dto.OptionItemDTO;
import cn.liguohao.ikaros.model.entity.OptionEntity;
import cn.liguohao.ikaros.model.option.SeoOptionModel;
import java.io.IOException;
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

    @Test
    void findOptionByCategory() throws RecordNotFoundException {

        OptionEntity optionEntity1
            = optionService.saveOptionItem(new OptionItemDTO(buildStr(), buildStr()));
        OptionEntity optionEntity2
            = optionService.saveOptionItem(new OptionItemDTO(buildStr(), buildStr()));
        OptionEntity optionEntity3 = optionService.saveOptionItem(
            new OptionItemDTO(buildStr(), buildStr()).setCategory(buildStr()));

        List<OptionEntity> optionListByCategory =
            optionService.findOptionByCategory(OptionConstants.Category.DEFAULT);

        assertNotNull(optionListByCategory);
        assertFalse(optionListByCategory.isEmpty());

        Set<String> keys = optionListByCategory.stream().flatMap(
            (Function<OptionEntity, Stream<String>>) optionEntity
                -> Stream.of(optionEntity.getKey())).collect(Collectors.toSet());

        Assertions.assertTrue(keys.contains(optionEntity1.getKey()));
        Assertions.assertTrue(keys.contains(optionEntity2.getKey()));
        assertFalse(keys.contains(optionEntity3.getKey()));

        optionService.deleteOptionItemByKey(optionEntity1.getKey());
        optionService.deleteOptionItemByKey(optionEntity2.getKey());
        optionService.deleteOptionItemByKey(optionEntity3.getKey());
    }

    @Test
    void findOptionValueByCategoryAndKey() throws RecordNotFoundException {

        String key = buildStr();
        String value = buildStr();
        String category = buildStr();

        try {
            optionService.findOptionValueByCategoryAndKey(category, key);
            fail(PROCESS_SHOUT_NOT_RUN_THIS);
        } catch (RecordNotFoundException e) {
            assertNotNull(e);
        }

        OptionItemDTO optionItemDTO = new OptionItemDTO(key, value).setCategory(category);
        optionService.saveOptionItem(optionItemDTO);

        OptionEntity searchOptionEntity =
            optionService.findOptionValueByCategoryAndKey(category, key);
        assertNotNull(searchOptionEntity);
        assertEquals(key, searchOptionEntity.getKey());
        assertEquals(value, searchOptionEntity.getValue());

        optionService.deleteOptionItemByKey(key);
    }

    @Test
    void initPresetOptionItems()
        throws NotSupportException, IllegalAccessException, RecordNotFoundException {
        String category = OptionConstants.Category.APP;
        String key = OptionConstants.Init.App.IS_INIT[0];

        // OptionItemInitAppRunner has run to init
        OptionEntity optionEntity =
            optionService.findOptionValueByCategoryAndKey(category, key);
        assertNotNull(optionEntity);
        Assertions.assertEquals("true", optionEntity.getValue());

        optionService.initPresetOptionItems();
    }

    @Test
    void findAllOptionModel()
        throws NoSuchFieldException, InstantiationException, IllegalAccessException, IOException {
        List<OptionModel> optionModels = optionService.findAllOptionModel();
        assertNotNull(optionModels);
        assertFalse(optionModels.isEmpty());
    }

    @Test
    void saveOptionModel() throws IllegalAccessException, RecordNotFoundException {
        String newHide4seValue = "true";
        String newSiteDescValue = "update description";
        String newKeywords = "";


        SeoOptionModel seoOptionModel
            = new SeoOptionModel()
            .setHideForSearchEngine(newHide4seValue)
            .setSiteDescription(newSiteDescValue)
            .setKeywords(newKeywords);

        optionService.saveOptionModel(seoOptionModel);

        OptionEntity optionEntity =
            optionService.findOptionItemByKey(OptionConstants.Init.Seo.HIDE_FOR_SE[0]);
        Assertions.assertEquals(newHide4seValue, optionEntity.getValue());

    }

}