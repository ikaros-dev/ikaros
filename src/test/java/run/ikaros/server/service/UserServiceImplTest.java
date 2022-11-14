package run.ikaros.server.service;


import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author guohao
 * @date 2022/10/21
 */
@SpringBootTest
class UserServiceImplTest {

    @Autowired
    UserServiceImpl userServiceImpl;

    @Disabled
    @Test
    void initMasterUserOnlyOnce() {
        userServiceImpl.initMasterUserOnlyOnce();
    }
}