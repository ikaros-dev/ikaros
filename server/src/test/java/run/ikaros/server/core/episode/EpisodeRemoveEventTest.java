package run.ikaros.server.core.episode;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.server.store.entity.EpisodeEntity;

class EpisodeRemoveEventTest {

    private EpisodeEntity buildEpisodeEntity() {
        EpisodeEntity entity = EpisodeEntity.builder()
            .subjectId(UUID.randomUUID())
            .name("Episode 1")
            .nameCn("第一集")
            .description("A test episode")
            .airTime(LocalDateTime.now())
            .sequence(1.0f)
            .build();
        entity.setId(UUID.randomUUID());
        return entity;
    }

    @Test
    void constructorWithSourceAndEntity() {
        Object source = new Object();
        EpisodeEntity entity = buildEpisodeEntity();
        EpisodeRemoveEvent event = new EpisodeRemoveEvent(source, entity);

        assertThat(event.getSource()).isSameAs(source);
        assertThat(event.getEntity()).isNotNull();
        assertThat(event.getEntity()).isEqualTo(entity);
    }

    @Test
    void getSourceReturnsSource() {
        String source = "test-source";
        EpisodeEntity entity = buildEpisodeEntity();
        EpisodeRemoveEvent event = new EpisodeRemoveEvent(source, entity);

        assertThat(event.getSource()).isSameAs(source);
    }

    @Test
    void getEntityReturnsEntity() {
        Object source = new Object();
        EpisodeEntity entity = buildEpisodeEntity();
        EpisodeRemoveEvent event = new EpisodeRemoveEvent(source, entity);

        assertThat(event.getEntity()).isSameAs(entity);
    }

    @Test
    void multipleInstancesShareNoState() {
        Object source = new Object();
        EpisodeEntity entity1 = buildEpisodeEntity();
        EpisodeEntity entity2 = buildEpisodeEntity();

        EpisodeRemoveEvent event1 = new EpisodeRemoveEvent(source, entity1);
        EpisodeRemoveEvent event2 = new EpisodeRemoveEvent(source, entity2);

        assertThat(event1.getEntity()).isSameAs(entity1);
        assertThat(event2.getEntity()).isSameAs(entity2);
        assertThat(event1.getEntity()).isNotSameAs(event2.getEntity());
    }
}
