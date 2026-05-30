package run.ikaros.api.core.meta;

import reactor.core.publisher.Flux;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.plugin.IkarosExtensionPoint;

public interface DelegateMetaService extends IkarosExtensionPoint {
    Flux<Subject> findAll(String keyword);
}
