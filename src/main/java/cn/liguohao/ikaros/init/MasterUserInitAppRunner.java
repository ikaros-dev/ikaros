package cn.liguohao.ikaros.init;

import cn.liguohao.ikaros.service.UserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 第一次运行会初始化一个管理员用户，用户名密码都是 admin 。
 *
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
