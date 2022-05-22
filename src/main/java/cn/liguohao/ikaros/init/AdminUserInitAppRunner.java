package cn.liguohao.ikaros.init;

import cn.liguohao.ikaros.handler.UserHandler;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * @author li-guohao
 */
@Component
public class AdminUserInitAppRunner implements ApplicationRunner {

    private final UserHandler userHandler;

    public AdminUserInitAppRunner(UserHandler userHandler) {
        this.userHandler = userHandler;
    }

    /**
     * @param args incoming application arguments
     * @throws Exception none
     * @see UserHandler#initAdminUserOnlyOnce()
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        userHandler.initAdminUserOnlyOnce();
    }
}
