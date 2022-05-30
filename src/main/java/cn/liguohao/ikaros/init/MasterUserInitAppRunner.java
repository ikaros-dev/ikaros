package cn.liguohao.ikaros.init;

import cn.liguohao.ikaros.service.UserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * @author li-guohao
 */
@Component
public class MasterUserInitAppRunner implements ApplicationRunner {

    private final UserService userService;

    public MasterUserInitAppRunner(UserService userService) {
        this.userService = userService;
    }

    /**
     * @param args incoming application arguments
     * @throws Exception none
     * @see UserService#initMasterUserOnlyOnce() ()
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        userService.initMasterUserOnlyOnce();
    }
}
