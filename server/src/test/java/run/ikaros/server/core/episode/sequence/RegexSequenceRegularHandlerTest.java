package run.ikaros.server.core.episode.sequence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import run.ikaros.api.store.enums.EpisodeGroup;
import run.ikaros.server.store.entity.EpisodeSequenceRegularEntity;

class RegexSequenceRegularHandlerTest {

    private static EpisodeSequenceRegularEntity createRule(String name, String regex,
                                                           EpisodeGroup group, Float seq,
                                                           int priority, boolean enabled) {
        EpisodeSequenceRegularEntity rule = EpisodeSequenceRegularEntity.builder()
            .name(name).regex(regex).epGroup(group).sequence(seq)
            .priority(priority).enabled(enabled).build();
        rule.setId(java.util.UUID.randomUUID());
        return rule;
    }

    @Test
    void match_whenMatchesRegex_returnsResult() {
        EpisodeSequenceRegularEntity rule = createRule(
            "NCED Match", "NCED", EpisodeGroup.ENDING_SONG, 1f, 100, true);

        RegexSequenceRegularHandler handler = new RegexSequenceRegularHandler(rule);

        StepVerifier.create(handler.match("[NCED] Song.mkv"))
            .assertNext(result -> {
                assertThat(result.isMatched()).isTrue();
                assertThat(result.getAttachmentName()).isEqualTo("[NCED] Song.mkv");
                assertThat(result.getEpGroup()).isEqualTo(EpisodeGroup.ENDING_SONG);
                assertThat(result.getSequence()).isEqualTo(1f);
                assertThat(result.getMatchedRuleName()).isEqualTo("NCED Match");
            })
            .verifyComplete();
    }

    @Test
    void match_whenNotMatches_returnsEmpty() {
        EpisodeSequenceRegularEntity rule = createRule(
            "NCED Match", "NCED", EpisodeGroup.ENDING_SONG, 1f, 100, true);

        RegexSequenceRegularHandler handler = new RegexSequenceRegularHandler(rule);

        StepVerifier.create(handler.match("EP01.mkv"))
            .verifyComplete();
    }

    @Test
    void match_whenDisabled_returnsEmpty() {
        EpisodeSequenceRegularEntity rule = createRule(
            "Disabled Rule", "NCED", EpisodeGroup.ENDING_SONG, null, 100, false);

        RegexSequenceRegularHandler handler = new RegexSequenceRegularHandler(rule);

        StepVerifier.create(handler.match("[NCED] Song.mkv"))
            .verifyComplete();
    }

    @Test
    void match_whenNullAttachmentName_returnsEmpty() {
        EpisodeSequenceRegularEntity rule = createRule(
            "Test", "test", null, null, 0, true);

        RegexSequenceRegularHandler handler = new RegexSequenceRegularHandler(rule);

        StepVerifier.create(handler.match(null))
            .verifyComplete();
    }

    @Test
    void match_sequenceIsNull_returnsResultWithoutSequence() {
        EpisodeSequenceRegularEntity rule = createRule(
            "OP Match", "NCOP", EpisodeGroup.OPENING_SONG, null, 90, true);

        RegexSequenceRegularHandler handler = new RegexSequenceRegularHandler(rule);

        StepVerifier.create(handler.match("NCOP.mkv"))
            .assertNext(result -> {
                assertThat(result.isMatched()).isTrue();
                assertThat(result.getSequence()).isNull();
                assertThat(result.getEpGroup()).isEqualTo(EpisodeGroup.OPENING_SONG);
            })
            .verifyComplete();
    }

    @Test
    void order_returnsRulePriority() {
        EpisodeSequenceRegularEntity rule = createRule(
            "Test", "test", null, null, 50, true);

        RegexSequenceRegularHandler handler = new RegexSequenceRegularHandler(rule);
        assertThat(handler.order()).isEqualTo(50);
    }
}
