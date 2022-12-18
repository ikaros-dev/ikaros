package run.ikaros.server.service.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import run.ikaros.server.core.service.OptionService;
import run.ikaros.server.model.request.AppInitRequest;

import jakarta.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author li-guohao
 */
@SpringBootTest
class OptionServiceImplTest {

    @Resource
    OptionService optionService;

    @Test
    void findAppIsInit() {
        boolean appIsInit = optionService.findAppIsInit();
        assertThat(appIsInit).isFalse();
    }

    @Test
    void appInit() {
        boolean appIsInit = optionService.findAppIsInit();
        assertThat(appIsInit).isFalse();
        AppInitRequest appInitRequest = new AppInitRequest()
            .setUsername("ikaros")
            .setPassword("ikaros");
        boolean isSuccess = optionService.appInit(appInitRequest);
        assertThat(isSuccess).isTrue();
        isSuccess = optionService.appInit(new AppInitRequest());
        assertThat(isSuccess).isTrue();
        appIsInit = optionService.findAppIsInit();
        assertThat(appIsInit).isTrue();
    }
}