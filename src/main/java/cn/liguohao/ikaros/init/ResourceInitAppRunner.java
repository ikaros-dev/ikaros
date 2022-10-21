package cn.liguohao.ikaros.init;

import cn.liguohao.ikaros.service.ResourceService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * @author guohao
 * @date 2022/10/18
 */
@Component
public class ResourceInitAppRunner implements ApplicationRunner {

    private final ResourceService resourceService;

    public ResourceInitAppRunner(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        resourceService.initPresetTypeRecords();
    }
}
