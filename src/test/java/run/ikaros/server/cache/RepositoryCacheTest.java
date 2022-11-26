package run.ikaros.server.cache;

import run.ikaros.server.service.UserService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author li-guohao
 * @date 2022/06/03
 */
@Disabled
@SpringBootTest
public class RepositoryCacheTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryCacheTest.class);

    @Autowired
    UserService userService;

    @Test
    void findByUsername() {
        LOGGER.info("exec {}#findByUsername", this.getClass().getName());
        final String username = "admin";
        userService.findByUsername(username);
        userService.findByUsername(username);
        userService.findByUsername(username);
    }

}
