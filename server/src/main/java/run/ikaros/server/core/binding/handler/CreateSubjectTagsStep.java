package run.ikaros.server.core.binding.handler;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.binding.DirectoryBindingContext;
import run.ikaros.api.core.binding.DirectoryBindingStep;
import run.ikaros.api.core.tag.Tag;
import run.ikaros.api.store.enums.TagType;
import run.ikaros.server.core.tag.TagService;

/**
 * Step 4: Create subject tags from directory bracket content.
 * Order: 40
 */
@Slf4j
@Component
public class CreateSubjectTagsStep implements DirectoryBindingStep {

    private final TagService tagService;

    public CreateSubjectTagsStep(TagService tagService) {
        this.tagService = tagService;
    }

    @Override
    public String name() {
        return "CreateSubjectTags";
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public boolean shouldSkip(DirectoryBindingContext context) {
        return context.getBracketTags() == null || context.getBracketTags().isEmpty()
            || context.getSubjectId() == null;
    }

    @Override
    public Mono<DirectoryBindingContext> execute(DirectoryBindingContext context) {
        UUID subjectId = context.getSubjectId();
        return Flux.fromIterable(context.getBracketTags())
            .concatMap(tagName ->
                tagService.findAll(TagType.SUBJECT, subjectId, null, tagName)
                    .next()
                    .flatMap(existing -> {
                        log.info("Tag [{}] already exists for subject [{}], skip",
                            tagName, subjectId);
                        return Mono.just(existing);
                    })
                    .switchIfEmpty(Mono.defer(() -> {
                        Tag tag = Tag.builder()
                            .type(TagType.SUBJECT)
                            .masterId(subjectId)
                            .name(tagName)
                            .build();
                        return tagService.create(tag)
                            .doOnSuccess(t ->
                                log.info("Created subject tag: name={}, subjectId={}",
                                    t.getName(), subjectId));
                    }))
                    .doOnNext(t -> {
                        context.getCreatedTags().add(t);
                    })
            )

            .then(Mono.just(context));
    }

    @Override
    public Mono<Void> rollback(DirectoryBindingContext context) {
        return Flux.fromIterable(context.getCreatedTags())
            .concatMap(tag -> tagService.removeById(tag.getId())
                .onErrorResume(e -> {
                    log.warn("Failed to remove tag during rollback: {}", tag.getName(), e);
                    return Mono.empty();
                }))
            .then();
    }
}
