package run.ikaros.server.core.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.TagType;
import run.ikaros.server.core.tag.event.TagRemoveEvent;
import run.ikaros.server.store.entity.TagEntity;
import run.ikaros.server.store.repository.TagRepository;

class DefaultTagServiceMoreTest {
    private TagRepository tagRepository;
    private R2dbcEntityTemplate r2dbcEntityTemplate;
    private ApplicationEventPublisher eventPublisher;
    private DefaultTagService defaultTagService;

    @BeforeEach
    void setUp() {
        tagRepository = Mockito.mock(TagRepository.class);
        r2dbcEntityTemplate = Mockito.mock(R2dbcEntityTemplate.class);
        eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        defaultTagService = new DefaultTagService(tagRepository, r2dbcEntityTemplate, eventPublisher);
    }

    @Test
    void findSubjectTags() {
        UUID subjectId = UuidV7Utils.generateUuid();
        TagEntity tagEntity = TagEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .type(TagType.SUBJECT)
            .name("anime")
            .masterId(subjectId)
            .userId(UuidV7Utils.generateUuid())
            .color("#FF0000")
            .createTime(LocalDateTime.now())
            .build();

        when(r2dbcEntityTemplate.select(any(Query.class), any(Class.class)))
            .thenReturn(Flux.just(tagEntity));

        StepVerifier.create(defaultTagService.findSubjectTags(subjectId))
            .assertNext(subjectTag -> {
                assertThat(subjectTag.getName()).isEqualTo("anime");
                assertThat(subjectTag.getSubjectId()).isEqualTo(subjectId);
            })
            .verifyComplete();
    }

    @Test
    void findSubjectTagsEmpty() {
        UUID subjectId = UuidV7Utils.generateUuid();
        when(r2dbcEntityTemplate.select(any(Query.class), any(Class.class)))
            .thenReturn(Flux.empty());
        StepVerifier.create(defaultTagService.findSubjectTags(subjectId))
            .expectNextCount(0)
            .verifyComplete();
    }

    @Test
    void findAttachmentTags() {
        UUID attachmentId = UuidV7Utils.generateUuid();
        TagEntity tagEntity = TagEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .type(TagType.ATTACHMENT)
            .name("cover")
            .masterId(attachmentId)
            .userId(UuidV7Utils.generateUuid())
            .createTime(LocalDateTime.now())
            .build();

        when(r2dbcEntityTemplate.select(any(Query.class), any(Class.class)))
            .thenReturn(Flux.just(tagEntity));

        StepVerifier.create(defaultTagService.findAttachmentTags(attachmentId))
            .assertNext(attachmentTag -> {
                assertThat(attachmentTag.getName()).isEqualTo("cover");
                assertThat(attachmentTag.getAttachmentId()).isEqualTo(attachmentId);
            })
            .verifyComplete();
    }

    @Test
    void remove() {
        UUID masterId = UuidV7Utils.generateUuid();
        UUID userId = UuidV7Utils.generateUuid();
        UUID tagId = UuidV7Utils.generateUuid();

        TagEntity tagEntity = TagEntity.builder()
            .id(tagId)
            .type(TagType.SUBJECT)
            .masterId(masterId)
            .name("action")
            .userId(userId)
            .createTime(LocalDateTime.now())
            .build();

        when(r2dbcEntityTemplate.select(any(Query.class), any(Class.class)))
            .thenReturn(Flux.just(tagEntity));
        when(tagRepository.findById(tagId)).thenReturn(Mono.just(tagEntity));
        when(tagRepository.deleteById(tagId)).thenReturn(Mono.empty());

        StepVerifier.create(defaultTagService.remove(TagType.SUBJECT, masterId, "action", userId))
            .verifyComplete();

        verify(eventPublisher).publishEvent(any(TagRemoveEvent.class));
    }

    @Test
    void removeById() {
        UUID tagId = UuidV7Utils.generateUuid();
        TagEntity tagEntity = TagEntity.builder()
            .id(tagId)
            .type(TagType.SUBJECT)
            .name("test")
            .createTime(LocalDateTime.now())
            .build();

        when(tagRepository.findById(tagId)).thenReturn(Mono.just(tagEntity));
        when(tagRepository.deleteById(tagId)).thenReturn(Mono.empty());

        StepVerifier.create(defaultTagService.removeById(tagId))
            .verifyComplete();

        verify(eventPublisher).publishEvent(any(TagRemoveEvent.class));
    }
}
