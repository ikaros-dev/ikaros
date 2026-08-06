package run.ikaros.server.core.subsonic.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.core.episode.EpisodeService;
import run.ikaros.server.core.music.service.MusicService;
import run.ikaros.server.core.subject.service.SubjectService;
import run.ikaros.server.store.repository.EpisodeListEpisodeRepository;
import run.ikaros.server.store.repository.EpisodeListRepository;
import run.ikaros.server.store.repository.SubjectRepository;

/** Subsonic 歌曲标识语义测试. */
class DefaultSubsonicServiceTest {

    @Test
    void getSongShouldExposeEpisodeIdUsedByStream() {
        EpisodeService episodeService = mock(EpisodeService.class);
        SubjectService subjectService = mock(SubjectService.class);
        DefaultSubsonicService service = new DefaultSubsonicService(
            mock(MusicService.class), subjectService, mock(SubjectRepository.class),
            episodeService, mock(AttachmentService.class), mock(EpisodeListRepository.class),
            mock(EpisodeListEpisodeRepository.class));
        UUID episodeId = UUID.randomUUID();
        Episode episode = Episode.defaultEpisode(UUID.randomUUID())
            .setId(episodeId).setName("Track 1");
        when(episodeService.findById(episodeId)).thenReturn(Mono.just(episode));
        when(subjectService.findById(episode.getSubjectId())).thenReturn(Mono.empty());

        var response = service.getSong(episodeId.toString()).block();

        assertThat(response).isNotNull();
        assertThat(response.getSong().getId()).isEqualTo(episodeId.toString());
    }
}
