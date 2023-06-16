package run.ikaros.server.search.subject;

import java.util.List;
import java.util.Set;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.search.subject.SubjectSearchService;
import run.ikaros.server.core.subject.event.SubjectAddEvent;
import run.ikaros.server.core.subject.event.SubjectRemoveEvent;

@Component
public class SubjectEventListener {

    private final SubjectSearchService subjectSearchService;

    public SubjectEventListener(SubjectSearchService subjectSearchService) {
        this.subjectSearchService = subjectSearchService;
    }

    /**
     * {@link SubjectAddEvent} listener.
     */
    @EventListener(SubjectAddEvent.class)
    public Mono<Void> handleFileAddEvent(SubjectAddEvent event) {
        return ReactiveSubjectDocConverter.fromEntity(event.getEntity())
            .doOnNext(subjectDoc -> {
                try {
                    subjectSearchService.addDocuments(List.of(subjectDoc));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .then();
    }

    /**
     * {@link SubjectRemoveEvent} listener.
     */
    @EventListener(SubjectRemoveEvent.class)
    public Mono<Void> handleFileRemoveEvent(SubjectRemoveEvent event) {
        return ReactiveSubjectDocConverter.fromEntity(event.getEntity())
            .doOnNext(subjectDoc -> {
                try {
                    subjectSearchService.removeDocuments(Set.of(subjectDoc.getName()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .then();
    }

}
