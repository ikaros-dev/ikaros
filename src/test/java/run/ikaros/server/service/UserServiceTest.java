package run.ikaros.server.service;


import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import run.ikaros.server.service.UserService;

/**
 * @author guohao
 * @date 2022/10/21
 */
@SpringBootTest
class UserServiceTest {

    @Autowired
    UserService userService;

    @Disabled
    @Test
    void initMasterUserOnlyOnce() {
        userService.initMasterUserOnlyOnce();
    }
}