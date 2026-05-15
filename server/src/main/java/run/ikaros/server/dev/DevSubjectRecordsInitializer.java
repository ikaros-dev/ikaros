package run.ikaros.server.dev;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.SubjectType;
import run.ikaros.server.store.entity.SubjectEntity;
import run.ikaros.server.store.repository.SubjectRepository;

public class DevSubjectRecordsInitializer {
    private final SubjectRepository repository;

    public DevSubjectRecordsInitializer(SubjectRepository repository) {
        this.repository = repository;
    }

    /**
     * 本地开发时插入大量数据.
     */
    @EventListener(ApplicationReadyEvent.class)
    public Mono<Void> insertRecords() {
        // 本地测试时，生成1000条条目数据
        final int count = 1000;
        final Random random = new Random();
        List<SubjectEntity> subjectEntities = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            SubjectEntity entity = SubjectEntity.builder().build();
            entity.setId(UuidV7Utils.generateUuid());
            entity.setCover("/static/dev/subject/cover/default.jpg");
            entity.setAirTime(LocalDateTime.now());
            entity.setName("name" + i);
            entity.setNsfw(false);
            entity.setScore(random.nextDouble(9));
            entity.setType(SubjectType.ANIME);
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            subjectEntities.add(entity);
        }
        return repository.count()
            .filter(c -> c < count)
            .flatMapMany(c -> Flux.fromStream(subjectEntities.stream()))
            .flatMap(repository::insert)
            .then();
    }
}
