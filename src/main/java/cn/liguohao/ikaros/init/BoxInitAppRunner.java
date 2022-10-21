package cn.liguohao.ikaros.init;

import cn.liguohao.ikaros.service.BoxService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * @author guohao
 * @date 2022/10/18
 */
@Component
public class BoxInitAppRunner implements ApplicationRunner {

    private final BoxService boxService;

    public BoxInitAppRunner(BoxService boxService) {
        this.boxService = boxService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        boxService.initPresetTypeRecords();
    }
}
