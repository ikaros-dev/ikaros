package run.ikaros.server.core.webclient;


import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;
import run.ikaros.api.constant.FileConst;

@SpringBootTest
class WeClientServiceTest {

    @Autowired
    WeClientService weClientService;

    @Test
    @Disabled
    void downloadImageWithGet() {
        String url = "https://lain.bgm.tv/r/400/pic/cover/l/d7/ea/387803_nQONr.jpg";
        StepVerifier.create(weClientService.downloadImageWithGet(FileConst.IMPORT_DIR_NAME, url))
            .expectNextMatches(fileEntity -> fileEntity.getId() != null)
            .verifyComplete();

    }
}