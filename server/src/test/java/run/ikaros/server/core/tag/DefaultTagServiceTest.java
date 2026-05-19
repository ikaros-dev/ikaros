package run.ikaros.server.core.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.TagType;
import run.ikaros.server.core.tag.event.TagRemoveEvent;
import run.ikaros.server.store.entity.TagEntity;
import run.ikaros.server.store.repository.TagRepository;

class DefaultTagServiceTest {
    private TagRepository tagRepository;
    private R2dbcEntityTemplate r2dbcEntityTemplate;
    private ApplicationEventPublisher eventPublisher;
    private DefaultTagService defaultTagService;

    @BeforeEach
    void setUp() {
        tagRepository = Mockito.mock(TagRepository.class);
        r2dbcEntityTemplate =
            Mockito.mock(R2dbcEntityTemplate.class);
        eventPublisher =
            Mockito.mock(ApplicationEventPublisher.class);
        defaultTagService = new DefaultTagService(
            tagRepository, r2dbcEntityTemplate,
            eventPublisher);
    }

    @Test
    void findAll_withType() {
        UUID masterId = UuidV7Utils.generateUuid();
        TagEntity tagEntity = TagEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .type(TagType.SUBJECT)
            .name("testTag")
            .masterId(masterId)
            .userId(UuidV7Utils.generateUuid())
            .createTime(LocalDateTime.now())
            .color("#FF0000")
            .build();

        when(r2dbcEntityTemplate.select(
            any(Query.class), any(Class.class)))
            .thenReturn(Flux.just(tagEntity));

        StepVerifier.create(
                defaultTagService.findAll(
                    TagType.SUBJECT, masterId, null, null))
            .assertNext(tag -> {
                assertThat(tag.getName())
                    .isEqualTo("testTag");
                assertThat(tag.getType())
                    .isEqualTo(TagType.SUBJECT);
                assertThat(tag.getMasterId())
                    .isEqualTo(masterId);
            })
            .verifyComplete();
    }

    @Test
    void removeById_found() {
        UUID tagId = UuidV7Utils.generateUuid();
        TagEntity tagEntity = TagEntity.builder()
            .id(tagId)
            .type(TagType.SUBJECT)
            .name("testTag")
            .masterId(UuidV7Utils.generateUuid())
            .userId(UuidV7Utils.generateUuid())
            .createTime(LocalDateTime.now())
            .color("#FF0000")
            .build();

        when(tagRepository.findById(tagId))
            .thenReturn(Mono.just(tagEntity));
        when(tagRepository.deleteById(tagId))
            .thenReturn(Mono.empty());

        StepVerifier.create(
                defaultTagService.removeById(tagId))
            .verifyComplete();

        verify(tagRepository).deleteById(tagId);

        ArgumentCaptor<TagRemoveEvent> captor =
            ArgumentCaptor.forClass(TagRemoveEvent.class);
        verify(eventPublisher)
            .publishEvent(captor.capture());
        assertThat(captor.getValue().getEntity())
            .isEqualTo(tagEntity);
    }

    @Test
    void removeById_notFound() {
        UUID tagId = UuidV7Utils.generateUuid();
        when(tagRepository.findById(tagId))
            .thenReturn(Mono.empty());

        StepVerifier.create(
                defaultTagService.removeById(tagId))
            .expectError(NotFoundException.class)
            .verify();
    }
}
