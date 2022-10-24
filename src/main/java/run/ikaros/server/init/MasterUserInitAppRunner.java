package run.ikaros.server.init;

import run.ikaros.server.service.impl.UserServiceImpl;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * @author li-guohao
 */
@Component
public class MasterUserInitAppRunner implements ApplicationRunner {

    private final UserServiceImpl userServiceImpl;

    public MasterUserInitAppRunner(UserServiceImpl userServiceImpl) {
        this.userServiceImpl = userServiceImpl;
    }

    /**
     * @param args incoming application arguments
     * @throws Exception none
     * @see UserServiceImpl#initMasterUserOnlyOnce() ()
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        userServiceImpl.initMasterUserOnlyOnce();
    }
}
