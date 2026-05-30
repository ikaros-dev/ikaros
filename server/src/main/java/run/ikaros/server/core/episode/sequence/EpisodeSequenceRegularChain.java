package run.ikaros.server.core.episode.sequence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.episode.EpisodeSequenceRegularHandler;
import run.ikaros.api.core.episode.EpisodeSequenceRegularPluginHook;
import run.ikaros.api.core.episode.EpisodeSequenceRegularResult;
import run.ikaros.server.plugin.ExtensionComponentsFinder;
import run.ikaros.server.store.entity.EpisodeSequenceRegularEntity;
import run.ikaros.server.store.repository.EpisodeSequenceRegularRepository;

/**
 * Chain of Responsibility orchestrator for episode sequence regular matching.
 * Merges DB-configured rules with plugin-provided handlers,
 * sorts by priority descending, and tries each handler until one matches.
 */
@Slf4j
@Component
public class EpisodeSequenceRegularChain {

    private final EpisodeSequenceRegularRepository repository;
    private final ExtensionComponentsFinder extensionComponentsFinder;

    public EpisodeSequenceRegularChain(EpisodeSequenceRegularRepository repository,
                                       ExtensionComponentsFinder extensionComponentsFinder) {
        this.repository = repository;
        this.extensionComponentsFinder = extensionComponentsFinder;
    }

    /**
     * Match an attachment name against all enabled rules in priority order.
     * Returns the first match result, or a default unmatched result.
     */
    public Mono<EpisodeSequenceRegularResult> match(String attachmentName) {
        if (attachmentName == null || attachmentName.isBlank()) {
            return Mono.just(defaultUnmatched(attachmentName));
        }

        return repository.findAllByEnabledTrueOrderByPriorityDesc()
            .collectList()
            .flatMap(entities -> matchWithHandlers(attachmentName, entities));
    }

    private Mono<EpisodeSequenceRegularResult> matchWithHandlers(
        String attachmentName, List<EpisodeSequenceRegularEntity> entities) {

        List<EpisodeSequenceRegularHandler> handlers = new ArrayList<>();

        // DB-configured handlers
        entities.stream()
            .map(RegexSequenceRegularHandler::new)
            .forEach(handlers::add);

        // Plugin-provided handlers
        extensionComponentsFinder
            .getExtensions(EpisodeSequenceRegularPluginHook.class)
            .forEach(hook -> handlers.addAll(hook.getAdditionalHandlers()));

        if (handlers.isEmpty()) {
            return Mono.just(defaultUnmatched(attachmentName));
        }

        handlers.sort(Comparator.comparingInt(EpisodeSequenceRegularHandler::order).reversed());

        return Flux.fromIterable(handlers)
            .concatMap(handler -> handler.match(attachmentName))
            .next()
            .switchIfEmpty(Mono.just(defaultUnmatched(attachmentName)));
    }

    private static EpisodeSequenceRegularResult defaultUnmatched(String attachmentName) {
        return EpisodeSequenceRegularResult.builder()
            .matched(false)
            .attachmentName(attachmentName)
            .build();
    }
}
