package run.ikaros.server.core.collection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.server.core.subject.event.SubjectRemoveEvent;
import run.ikaros.server.store.entity.SubjectEntity;
import run.ikaros.server.store.repository.SubjectCollectionRepository;

@Slf4j
@Component
public class CollectionSubjectRemoveEventListener {
    private final SubjectCollectionRepository subjectCollectionRepository;

    public CollectionSubjectRemoveEventListener(
        SubjectCollectionRepository subjectCollectionRepository) {
        this.subjectCollectionRepository = subjectCollectionRepository;
    }

    /**
     * Listen subject remove event.
     */
    @EventListener(SubjectRemoveEvent.class)
    public Mono<Void> onSubjectAdd(SubjectRemoveEvent event) {
        SubjectEntity entity = event.getEntity();
        if (entity == null) {
            return Mono.empty();
        }
        return subjectCollectionRepository.removeAllBySubjectId(entity.getId())
            .doOnSuccess(unused -> log.debug("Subject collections removed by subject id: {}",
                entity.getId()))
            .then();
    }
}
