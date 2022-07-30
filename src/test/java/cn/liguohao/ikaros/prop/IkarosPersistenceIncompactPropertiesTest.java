package cn.liguohao.ikaros.prop;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author li-guohao
 * @date 2022/05/28
 */
@SpringBootTest
class IkarosPersistenceIncompactPropertiesTest {

    @Autowired
    private IkarosPersistenceIncompactProperties ikarosPersistenceIncompactProperties;

    @Test
    void test1() {
        Assertions.assertNotNull(ikarosPersistenceIncompactProperties);
        Assertions.assertNotNull(ikarosPersistenceIncompactProperties.getFileDirPrefix());
    }
}