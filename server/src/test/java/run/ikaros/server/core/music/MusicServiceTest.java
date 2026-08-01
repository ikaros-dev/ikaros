package run.ikaros.server.core.music;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.store.enums.SubjectType;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.core.music.service.MusicService;

/**
 * 音乐模块服务测试.
 *
 * @author Nekoli
 */
@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class MusicServiceTest {

    @Autowired
    MusicService musicService;

    @Test
    void createAndFindAlbum() {
        // 创建测试专辑
        Subject subject = new Subject();
        subject.setName("测试专辑");
        subject.setNameCn("Test Album");
        subject.setType(SubjectType.MUSIC);
        subject.setNsfw(false);

        StepVerifier.create(musicService.createAlbum(subject))
            .expectNextMatches(savedSubject -> {
                assertThat(savedSubject.getId()).isNotNull();
                assertThat(savedSubject.getType()).isEqualTo(SubjectType.MUSIC);
                return true;
            })
            .verifyComplete();
    }

    @Test
    void listAlbumsWithPagination() {
        StepVerifier.create(musicService.listAlbums(1, 10))
            .expectNextMatches(wrap -> {
                assertThat(wrap.getPage()).isEqualTo(1);
                assertThat(wrap.getSize()).isEqualTo(10);
                return true;
            })
            .verifyComplete();
    }
}
