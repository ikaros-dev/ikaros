package run.ikaros.server.core.meta;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.meta.MetaOperate;
import run.ikaros.api.core.subject.Subject;

@Slf4j
@Component
public class MetaOperator implements MetaOperate {
    private final MetaService metaService;

    public MetaOperator(MetaService metaService) {
        this.metaService = metaService;
    }

    @Override
    public Mono<Subject> findOne(String keyword) {
        return metaService.findOne(keyword);
    }

    @Override
    public Flux<Subject> findAll(String keyword) {
        return metaService.findAll(keyword);
    }
}
