package run.ikaros.server.core.meta;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.meta.DelegateMetaService;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.infra.utils.AssertUtils;
import run.ikaros.server.plugin.ExtensionComponentsFinder;

@Slf4j
@Service
public class DefaultMetaService implements MetaService {

    private final ExtensionComponentsFinder extensionComponentsFinder;

    public DefaultMetaService(ExtensionComponentsFinder extensionComponentsFinder) {
        this.extensionComponentsFinder = extensionComponentsFinder;
    }

    @Override
    public Mono<Subject> findOne(String keyword) {
        AssertUtils.notBlank(keyword, "'keyword' has not blank.");
        return findAll(keyword)
            .collectList()
            .filter(list -> !list.isEmpty())
            .flatMap(list -> Mono.justOrEmpty(list.get(0)));
    }

    @Override
    public Flux<Subject> findAll(String keyword) {
        AssertUtils.notBlank(keyword, "'keyword' has not blank.");
        List<DelegateMetaService> metaService =
            extensionComponentsFinder.getExtensions(DelegateMetaService.class);
        return Flux.fromStream(metaService.stream())
            .flatMap(service -> service.findAll(keyword).collectList())
            .collectList()
            .map(list -> {
                List<Subject> subjectMetas = new ArrayList<>();
                for (List<Subject> metas : list) {
                    subjectMetas.addAll(metas);
                }
                return subjectMetas;
            })
            .flatMapMany(Flux::fromIterable);
    }
}
