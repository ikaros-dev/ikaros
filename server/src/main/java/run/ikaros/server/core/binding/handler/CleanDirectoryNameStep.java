package run.ikaros.server.core.binding.handler;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.binding.DirectoryBindingContext;
import run.ikaros.api.core.binding.DirectoryBindingStep;

/**
 * Step: Clean special characters from directory clean name.
 * Run after ParseDirectoryNameStep (order=10), before FindSubjectInfoStep (order=20).
 * Order: 15
 */
@Component
public class CleanDirectoryNameStep implements DirectoryBindingStep {

    private static final String SPECIAL_CHARS_REGEX =
        "[？！!?,.:：;；、……（）()【】《》\"\"''~～+＝=*＆&%￥#@^]";

    @Override
    public String name() {
        return "CleanDirectoryName";
    }

    @Override
    public int order() {
        return 15;
    }

    @Override
    public boolean shouldSkip(DirectoryBindingContext context) {
        return context.getCleanName() == null || context.getCleanName().isBlank()
            || context.getSubjectId() != null;
    }

    @Override
    public Mono<DirectoryBindingContext> execute(DirectoryBindingContext context) {
        String cleanName = context.getCleanName();

        // Remove words containing apostrophe
        cleanName = Arrays.stream(cleanName.split("\\s+"))
            .filter(word -> !word.contains("'"))
            .collect(Collectors.joining(" "));

        // Remove remaining special characters
        cleanName = cleanName.replaceAll(SPECIAL_CHARS_REGEX, "").trim();

        //

        context.setCleanName(cleanName);
        return Mono.just(context);
    }

    @Override
    public Mono<Void> rollback(DirectoryBindingContext context) {
        return Mono.empty();
    }
}
