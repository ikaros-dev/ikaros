package run.ikaros.server.core.episode.sequence;

import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.episode.EpisodeSequenceRegularHandler;
import run.ikaros.api.core.episode.EpisodeSequenceRegularResult;
import run.ikaros.server.store.entity.EpisodeSequenceRegularEntity;

/**
 * A handler that matches an attachment name against a single regex rule.
 * Created dynamically by {@link EpisodeSequenceRegularChain} from DB entities.
 */
@Slf4j
public class RegexSequenceRegularHandler implements EpisodeSequenceRegularHandler {

    private final EpisodeSequenceRegularEntity rule;
    private final Pattern pattern;

    public RegexSequenceRegularHandler(EpisodeSequenceRegularEntity rule) {
        this.rule = rule;
        this.pattern = Pattern.compile(rule.getRegex());
    }

    public EpisodeSequenceRegularEntity getRule() {
        return rule;
    }

    @Override
    public int order() {
        return rule.getPriority();
    }

    @Override
    public Mono<EpisodeSequenceRegularResult> match(String attachmentName) {
        if (attachmentName == null || !rule.getEnabled()) {
            return Mono.empty();
        }
        try {
            if (pattern.matcher(attachmentName).find()) {
                log.debug("Regex [{}] matched attachment [{}]", rule.getRegex(), attachmentName);
                return Mono.just(EpisodeSequenceRegularResult.builder()
                    .matched(true)
                    .attachmentName(attachmentName)
                    .epGroup(rule.getEpGroup())
                    .sequence(rule.getSequence())
                    .matchedRuleName(rule.getName())
                    .matchedRegex(rule.getRegex())
                    .build());
            }
        } catch (Exception e) {
            log.warn("Failed to match regex [{}] for attachment [{}]: {}",
                rule.getRegex(), attachmentName, e.getMessage());
        }
        return Mono.empty();
    }
}
