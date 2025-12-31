package run.ikaros.server.core.subject.listener;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.server.core.collection.event.SubjectCollectionScoreUpdateEvent;
import run.ikaros.server.store.entity.BaseEntity;
import run.ikaros.server.store.entity.SubjectCollectionEntity;
import run.ikaros.server.store.repository.SubjectCollectionRepository;
import run.ikaros.server.store.repository.SubjectRepository;
import run.ikaros.server.store.repository.UserRepository;

@Slf4j
@Component
public class SubjectCollectionScoreUpdateEventListener {
    private final UserRepository userRepository;
    private final SubjectCollectionRepository subjectCollectionRepository;
    private final SubjectRepository subjectRepository;

    /**
     * Construct.
     */
    public SubjectCollectionScoreUpdateEventListener(
        UserRepository userRepository, SubjectCollectionRepository subjectCollectionRepository,
        SubjectRepository subjectRepository) {
        this.userRepository = userRepository;
        this.subjectCollectionRepository = subjectCollectionRepository;
        this.subjectRepository = subjectRepository;
    }

    /**
     * 监听条目收藏分数更新事件，并重新计算条目的综合分数并更新.
     */
    @EventListener(SubjectCollectionScoreUpdateEvent.class)
    public Mono<Void> onSubjectCollectionScoreUpdateEvent(SubjectCollectionScoreUpdateEvent event) {
        UUID subjectId = event.getSubjectId();
        if (subjectId == null) {
            return Mono.empty();
        }
        log.debug("Receive event with subjectId={}.", subjectId);
        return userRepository.findAll()
            .map(BaseEntity::getId)
            .flatMap(uid ->
                subjectCollectionRepository.findByUserIdAndSubjectId(uid, subjectId))
            .map(SubjectCollectionEntity::getScore)
            .collectList()
            .map(list -> list.stream().mapToInt(Integer::intValue).average().orElse(0.0))
            .flatMap(average -> subjectRepository.findById(subjectId)
                .map(subjectEntity -> {
                    subjectEntity.setScore(average);
                    return subjectEntity;
                }))
            .flatMap(subjectRepository::save)
            .then();
    }
}
