package run.ikaros.server.core.subsonic.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.core.episode.EpisodeService;
import run.ikaros.server.core.music.service.MusicService;
import run.ikaros.server.core.subject.service.SubjectService;
import run.ikaros.server.core.subsonic.service.SubsonicService;
import run.ikaros.server.store.entity.EpisodeListEntity;
import run.ikaros.server.store.entity.EpisodeListEpisodeEntity;
import run.ikaros.server.store.repository.EpisodeListEpisodeRepository;
import run.ikaros.server.store.repository.EpisodeListRepository;
import run.ikaros.server.store.repository.SubjectRepository;

/** Subsonic 歌单写入和歌曲标识语义测试. */
class DefaultSubsonicServiceTest {

    @Test
    void getSongShouldExposeEpisodeIdUsedByStream() {
        EpisodeService episodeService = mock(EpisodeService.class);
        SubjectService subjectService = mock(SubjectService.class);
        DefaultSubsonicService service = service(
            episodeService, subjectService, mock(EpisodeListRepository.class),
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

    @Test
    void createPlaylistShouldUpdateExistingPlaylist() {
        EpisodeListRepository listRepository = mock(EpisodeListRepository.class);
        EpisodeListEpisodeRepository relationRepository =
            mock(EpisodeListEpisodeRepository.class);
        DefaultSubsonicService service = service(listRepository, relationRepository);
        EpisodeListEntity existing = playlist("旧名称");
        when(listRepository.findById(existing.getId())).thenReturn(Mono.just(existing));
        when(listRepository.update(existing)).thenReturn(Mono.just(existing));

        StepVerifier.create(service.createPlaylist(
                existing.getId().toString(), "新名称", null))
            .assertNext(response -> assertThat(response.getStatus()).isEqualTo("ok"))
            .verifyComplete();

        verify(listRepository).update(existing);
        verify(listRepository, never()).insert(any());
        assertThat(existing.getName()).isEqualTo("新名称");
    }

    @Test
    void createPlaylistShouldInsertNewPlaylist() {
        EpisodeListRepository listRepository = mock(EpisodeListRepository.class);
        EpisodeListEpisodeRepository relationRepository =
            mock(EpisodeListEpisodeRepository.class);
        DefaultSubsonicService service = service(listRepository, relationRepository);
        when(listRepository.insert(any())).thenAnswer(invocation ->
            Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.createPlaylist(null, "新歌单", null))
            .assertNext(response -> assertThat(response.getStatus()).isEqualTo("ok"))
            .verifyComplete();

        verify(listRepository).insert(any(EpisodeListEntity.class));
        verify(listRepository, never()).update(any());
    }

    @Test
    void createPlaylistShouldDeleteBeforeBatchInsert() {
        EpisodeListRepository listRepository = mock(EpisodeListRepository.class);
        EpisodeListEpisodeRepository relationRepository =
            mock(EpisodeListEpisodeRepository.class);
        DefaultSubsonicService service = service(listRepository, relationRepository);
        EpisodeListEntity existing = playlist("歌单");
        UUID episodeId = UuidV7Utils.generateUuid();
        when(listRepository.findById(existing.getId())).thenReturn(Mono.just(existing));
        when(listRepository.update(existing)).thenReturn(Mono.just(existing));
        when(relationRepository.deleteByEpisodeListId(existing.getId())).thenReturn(Mono.empty());
        when(relationRepository.insertAll(existing.getId(), List.of(episodeId)))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.createPlaylist(existing.getId().toString(),
                "歌单", List.of(episodeId.toString())))
            .expectNextCount(1)
            .verifyComplete();

        InOrder order = inOrder(relationRepository);
        order.verify(relationRepository).deleteByEpisodeListId(existing.getId());
        order.verify(relationRepository).insertAll(existing.getId(), List.of(episodeId));
    }

    @Test
    void createPlaylistShouldNotChangeRelationsForNullOrEmptySongs() {
        EpisodeListRepository listRepository = mock(EpisodeListRepository.class);
        EpisodeListEpisodeRepository relationRepository =
            mock(EpisodeListEpisodeRepository.class);
        DefaultSubsonicService service = service(listRepository, relationRepository);
        EpisodeListEntity existing = playlist("歌单");
        when(listRepository.findById(existing.getId())).thenReturn(Mono.just(existing));
        when(listRepository.update(existing)).thenReturn(Mono.just(existing));

        StepVerifier.create(service.createPlaylist(existing.getId().toString(), "歌单", null))
            .expectNextCount(1)
            .verifyComplete();
        StepVerifier.create(service.createPlaylist(
                existing.getId().toString(), "歌单", List.of()))
            .expectNextCount(1)
            .verifyComplete();

        verify(relationRepository, never()).deleteByEpisodeListId(any());
        verify(relationRepository, never()).insertAll(any(), any());
    }

    @Test
    void createPlaylistShouldRejectInvalidSongBeforeDelete() {
        EpisodeListRepository listRepository = mock(EpisodeListRepository.class);
        EpisodeListEpisodeRepository relationRepository =
            mock(EpisodeListEpisodeRepository.class);
        DefaultSubsonicService service = service(listRepository, relationRepository);
        EpisodeListEntity existing = playlist("歌单");
        when(listRepository.findById(existing.getId())).thenReturn(Mono.just(existing));
        when(listRepository.update(existing)).thenReturn(Mono.just(existing));

        StepVerifier.create(service.createPlaylist(
                existing.getId().toString(), "歌单", List.of("invalid")))
            .expectError(NullPointerException.class)
            .verify();

        verify(relationRepository, never()).deleteByEpisodeListId(any());
        verify(relationRepository, never()).insertAll(any(), any());
    }

    @Test
    void createPlaylistShouldPropagateBatchInsertError() {
        EpisodeListRepository listRepository = mock(EpisodeListRepository.class);
        EpisodeListEpisodeRepository relationRepository =
            mock(EpisodeListEpisodeRepository.class);
        DefaultSubsonicService service = service(listRepository, relationRepository);
        EpisodeListEntity existing = playlist("歌单");
        UUID episodeId = UuidV7Utils.generateUuid();
        IllegalStateException failure = new IllegalStateException("批量写入失败");
        when(listRepository.findById(existing.getId())).thenReturn(Mono.just(existing));
        when(listRepository.update(existing)).thenReturn(Mono.just(existing));
        when(relationRepository.deleteByEpisodeListId(existing.getId())).thenReturn(Mono.empty());
        when(relationRepository.insertAll(existing.getId(), List.of(episodeId)))
            .thenReturn(Mono.error(failure));

        StepVerifier.create(service.createPlaylist(existing.getId().toString(),
                "歌单", List.of(episodeId.toString())))
            .expectErrorMatches(error -> error == failure)
            .verify();
    }

    private DefaultSubsonicService service(EpisodeListRepository listRepository,
                                           EpisodeListEpisodeRepository relationRepository) {
        return service(mock(EpisodeService.class), mock(SubjectService.class),
            listRepository, relationRepository);
    }

    private DefaultSubsonicService service(EpisodeService episodeService,
                                           SubjectService subjectService,
                                           EpisodeListRepository listRepository,
                                           EpisodeListEpisodeRepository relationRepository) {
        return new DefaultSubsonicService(
            mock(MusicService.class), subjectService, mock(SubjectRepository.class),
            episodeService, mock(AttachmentService.class), listRepository, relationRepository);
    }

    private EpisodeListEntity playlist(String name) {
        EpisodeListEntity playlist = EpisodeListEntity.builder()
            .name(name)
            .description("")
            .nsfw(false)
            .build();
        playlist.setId(UuidV7Utils.generateUuid());
        return playlist;
    }

    /** 使用 PostgreSQL 验证歌单关系写入的响应式事务. */
    @Nested
    @SpringBootTest
    @Testcontainers
    @Import(IkarosTestcontainersConfiguration.class)
    class TransactionIntegrationTests {

        /** 用于验证响应式事务的真实服务代理. */
        @Autowired
        SubsonicService transactionalService;

        /** 用于准备和核对事务数据的歌单仓储. */
        @Autowired
        EpisodeListRepository realEpisodeListRepository;

        /** 可注入批量写入失败的关系仓储代理. */
        @MockitoSpyBean
        EpisodeListEpisodeRepository realEpisodeListEpisodeRepository;

        @AfterEach
        void tearDown() {
            StepVerifier.create(realEpisodeListEpisodeRepository.deleteAll()
                .then(realEpisodeListRepository.deleteAll()))
                .verifyComplete();
        }

        @Test
        void createPlaylistShouldRollbackPlaylistAndRelationsWhenBatchInsertFails() {
            EpisodeListEntity existing = playlist("旧名称");
            UUID oldEpisodeId = UuidV7Utils.generateUuid();
            UUID newEpisodeId = UuidV7Utils.generateUuid();
            EpisodeListEpisodeEntity oldRelation = EpisodeListEpisodeEntity.builder()
                .id(UuidV7Utils.generateUuid())
                .episodeListId(existing.getId())
                .episodeId(oldEpisodeId)
                .build();
            StepVerifier.create(realEpisodeListRepository.insert(existing)
                    .then(realEpisodeListEpisodeRepository.insert(oldRelation)))
                .expectNext(oldRelation)
                .verifyComplete();
            doReturn(Mono.error(new IllegalStateException("批量写入失败")))
                .when(realEpisodeListEpisodeRepository)
                .insertAll(eq(existing.getId()), eq(List.of(newEpisodeId)));

            StepVerifier.create(transactionalService.createPlaylist(existing.getId().toString(),
                    "新名称", List.of(newEpisodeId.toString())))
                .expectError(IllegalStateException.class)
                .verify();

            StepVerifier.create(realEpisodeListRepository.findById(existing.getId()))
                .assertNext(found -> assertThat(found.getName()).isEqualTo("旧名称"))
                .verifyComplete();
            StepVerifier.create(realEpisodeListEpisodeRepository
                    .findAllByEpisodeListId(existing.getId()).collectList())
                .assertNext(relations -> {
                    assertThat(relations).hasSize(1);
                    assertThat(relations.getFirst().getEpisodeId()).isEqualTo(oldEpisodeId);
                })
                .verifyComplete();
        }
    }
}
