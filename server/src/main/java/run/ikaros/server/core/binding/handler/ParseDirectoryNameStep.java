package run.ikaros.server.core.binding.handler;

import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.binding.DirectoryBindingContext;
import run.ikaros.api.core.binding.DirectoryBindingStep;
import run.ikaros.api.infra.utils.RegexUtils;

/**
 * Step 1: Parse directory name to extract bracket tags and clean name.
 * Order: 10
 */
@Component
public class ParseDirectoryNameStep implements DirectoryBindingStep {

    @Override
    public String name() {
        return "ParseDirectoryName";
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Mono<DirectoryBindingContext> execute(DirectoryBindingContext context) {
        String dirName = context.getDirectoryName();
        List<String> tags = RegexUtils.getFileTag(dirName);
        String cleanName = dirName.replaceAll("\\[[^\\[\\]]+\\]", "").trim();

        context.setBracketTags(tags);
        context.setCleanName(cleanName);
        return Mono.just(context);
    }

    @Override
    public Mono<Void> rollback(DirectoryBindingContext context) {
        return Mono.empty();
    }
}
