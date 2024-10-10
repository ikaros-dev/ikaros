package run.ikaros.api.core.meta;

import org.pf4j.ExtensionPoint;
import reactor.core.publisher.Flux;
import run.ikaros.api.core.subject.SubjectMeta;

public interface DelegateMetaService extends ExtensionPoint {
    Flux<SubjectMeta> findAll(String keyword);
}
