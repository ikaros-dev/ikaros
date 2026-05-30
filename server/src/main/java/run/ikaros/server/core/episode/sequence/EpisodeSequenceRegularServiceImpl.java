package run.ikaros.server.core.episode.sequence;

import static run.ikaros.api.infra.utils.ReactiveBeanUtils.copyProperties;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.episode.EpisodeSequenceRegular;
import run.ikaros.api.core.episode.EpisodeSequenceRegularResult;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.store.entity.EpisodeSequenceRegularEntity;
import run.ikaros.server.store.repository.EpisodeSequenceRegularRepository;

@Slf4j
@Service
public class EpisodeSequenceRegularServiceImpl implements EpisodeSequenceRegularService {

    private final EpisodeSequenceRegularRepository repository;
    private final EpisodeSequenceRegularChain chain;

    public EpisodeSequenceRegularServiceImpl(EpisodeSequenceRegularRepository repository,
                                             EpisodeSequenceRegularChain chain) {
        this.repository = repository;
        this.chain = chain;
    }

    @Override
    public Mono<EpisodeSequenceRegular> save(EpisodeSequenceRegular regular) {
        if (regular.getId() == null) {
            return copyProperties(regular, EpisodeSequenceRegularEntity.builder().build())
                .flatMap(repository::insert)
                .flatMap(saved -> copyProperties(saved,
                    EpisodeSequenceRegular.builder().build()));
        }
        // For updates, fetch existing entity first to preserve optimisticLockVersion
        return repository.findById(regular.getId())
            .switchIfEmpty(Mono.error(
                new IllegalArgumentException("Rule not found for id: " + regular.getId())))
            .flatMap(existing -> copyProperties(regular, existing))
            .flatMap(repository::update)
            .flatMap(saved -> copyProperties(saved,
                EpisodeSequenceRegular.builder().build()));
    }

    @Override
    public Mono<Void> removeById(UUID id) {
        return repository.deleteById(id);
    }

    @Override
    public Mono<EpisodeSequenceRegular> findById(UUID id) {
        return repository.findById(id)
            .flatMap(entity -> copyProperties(entity, EpisodeSequenceRegular.builder().build()));
    }

    @Override
    public Flux<EpisodeSequenceRegular> findAll() {
        return repository.findAll()
            .flatMap(entity -> copyProperties(entity, EpisodeSequenceRegular.builder().build()));
    }

    @Override
    public Mono<PagingWrap<EpisodeSequenceRegular>> findAll(Integer page, Integer size) {
        int pageNum = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : size;
        PageRequest pageRequest = PageRequest.of(pageNum - 1, pageSize);

        Flux<EpisodeSequenceRegular> regularFlux = repository
            .findAllByEnabledTrueOrderByPriorityDesc(pageRequest)
            .flatMap(entity -> copyProperties(entity, EpisodeSequenceRegular.builder().build()));

        Mono<Long> countMono = repository.countAllByEnabledTrue();

        return regularFlux.collectList()
            .flatMap(list -> countMono
                .map(count -> new PagingWrap<>(pageNum, pageSize, count, list)));
    }

    @Override
    public Mono<EpisodeSequenceRegularResult> match(String attachmentName) {
        return chain.match(attachmentName);
    }
}
