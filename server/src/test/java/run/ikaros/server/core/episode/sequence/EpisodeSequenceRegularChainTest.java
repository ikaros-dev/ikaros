package run.ikaros.server.core.episode.sequence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import run.ikaros.api.core.episode.EpisodeSequenceRegularHandler;
import run.ikaros.api.core.episode.EpisodeSequenceRegularPluginHook;
import run.ikaros.api.core.episode.EpisodeSequenceRegularResult;
import run.ikaros.api.store.enums.EpisodeGroup;
import run.ikaros.server.plugin.ExtensionComponentsFinder;
import run.ikaros.server.store.entity.EpisodeSequenceRegularEntity;
import run.ikaros.server.store.repository.EpisodeSequenceRegularRepository;

class EpisodeSequenceRegularChainTest {

    @Mock
    private EpisodeSequenceRegularRepository repository;
    @Mock
    private ExtensionComponentsFinder extensionComponentsFinder;
    private EpisodeSequenceRegularChain chain;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(extensionComponentsFinder.getExtensions(EpisodeSequenceRegularPluginHook.class))
            .thenReturn(List.of());
        chain = new EpisodeSequenceRegularChain(repository, extensionComponentsFinder);
    }

    private static EpisodeSequenceRegularEntity createRule(String name, String regex,
                                                           EpisodeGroup group, Float seq,
                                                           int priority, boolean enabled) {
        EpisodeSequenceRegularEntity entity = EpisodeSequenceRegularEntity.builder()
            .name(name).regex(regex).epGroup(group).sequence(seq)
            .priority(priority).enabled(enabled).build();
        entity.setId(UUID.randomUUID());
        return entity;
    }

    @Test
    void match_returnsFirstMatchByPriority() {
        EpisodeSequenceRegularEntity opRule = createRule(
            "OP Match", "NCOP", EpisodeGroup.OPENING_SONG, null, 50, true);
        EpisodeSequenceRegularEntity edRule = createRule(
            "ED Match", "NCED", EpisodeGroup.ENDING_SONG, null, 100, true);

        when(repository.findAllByEnabledTrueOrderByPriorityDesc())
            .thenReturn(Flux.just(edRule, opRule));

        StepVerifier.create(chain.match("[NCED] Ending.mkv"))
            .assertNext(result -> {
                assertThat(result.isMatched()).isTrue();
                assertThat(result.getEpGroup()).isEqualTo(EpisodeGroup.ENDING_SONG);
                assertThat(result.getMatchedRuleName()).isEqualTo("ED Match");
            })
            .verifyComplete();
    }

    @Test
    void match_noRules_returnsUnmatched() {
        when(repository.findAllByEnabledTrueOrderByPriorityDesc())
            .thenReturn(Flux.empty());

        StepVerifier.create(chain.match("EP01.mkv"))
            .assertNext(result -> {
                assertThat(result.isMatched()).isFalse();
                assertThat(result.getAttachmentName()).isEqualTo("EP01.mkv");
            })
            .verifyComplete();
    }

    @Test
    void match_nullAttachmentName_returnsUnmatched() {
        StepVerifier.create(chain.match(null))
            .assertNext(result -> {
                assertThat(result.isMatched()).isFalse();
                assertThat(result.getAttachmentName()).isNull();
            })
            .verifyComplete();
    }

    @Test
    void match_blankAttachmentName_returnsUnmatched() {
        StepVerifier.create(chain.match(""))
            .assertNext(result -> {
                assertThat(result.isMatched()).isFalse();
                assertThat(result.getAttachmentName()).isEqualTo("");
            })
            .verifyComplete();
    }

    @Test
    void match_noMatchingRule_returnsUnmatched() {
        EpisodeSequenceRegularEntity opRule = createRule(
            "OP Match", "NCOP", EpisodeGroup.OPENING_SONG, null, 50, true);

        when(repository.findAllByEnabledTrueOrderByPriorityDesc())
            .thenReturn(Flux.just(opRule));

        StepVerifier.create(chain.match("EP01.mkv"))
            .assertNext(result -> {
                assertThat(result.isMatched()).isFalse();
                assertThat(result.getAttachmentName()).isEqualTo("EP01.mkv");
            })
            .verifyComplete();
    }

    @Test
    void match_multipleMatchingRules_returnsHighestPriority() {
        EpisodeSequenceRegularEntity generalRule = createRule(
            "General Match", "SP", EpisodeGroup.SPECIAL_PROMOTION, null, 10, true);
        EpisodeSequenceRegularEntity specificRule = createRule(
            "Specific SP", "SP01", EpisodeGroup.SPECIAL_PROMOTION, 1f, 50, true);

        when(repository.findAllByEnabledTrueOrderByPriorityDesc())
            .thenReturn(Flux.just(specificRule, generalRule));

        StepVerifier.create(chain.match("SP01.mkv"))
            .assertNext(result -> {
                assertThat(result.isMatched()).isTrue();
                assertThat(result.getMatchedRuleName()).isEqualTo("Specific SP");
                assertThat(result.getSequence()).isEqualTo(1f);
            })
            .verifyComplete();
    }

    @Test
    void match_pluginHandler_mergesWithDbHandlers() {
        EpisodeSequenceRegularEntity dbRule = createRule(
            "DB Rule", "NCOP", EpisodeGroup.OPENING_SONG, null, 50, true);

        when(repository.findAllByEnabledTrueOrderByPriorityDesc())
            .thenReturn(Flux.just(dbRule));

        EpisodeSequenceRegularHandler pluginHandler = new EpisodeSequenceRegularHandler() {
            @Override
            public int order() {
                return 100;
            }

            @Override
            public Mono<EpisodeSequenceRegularResult> match(String attachmentName) {
                if (attachmentName != null && attachmentName.contains("PLUGIN")) {
                    return Mono.just(EpisodeSequenceRegularResult.builder()
                        .matched(true)
                        .attachmentName(attachmentName)
                        .epGroup(EpisodeGroup.SPECIAL_PROMOTION)
                        .matchedRuleName("PluginRule")
                        .build());
                }
                return Mono.empty();
            }
        };

        EpisodeSequenceRegularPluginHook pluginHook =
            () -> List.of(pluginHandler);

        when(extensionComponentsFinder.getExtensions(EpisodeSequenceRegularPluginHook.class))
            .thenReturn(List.of(pluginHook));

        // Plugin handler has higher priority (100 > 50), so it should match first
        StepVerifier.create(chain.match("PLUGIN_SP.mkv"))
            .assertNext(result -> {
                assertThat(result.isMatched()).isTrue();
                assertThat(result.getMatchedRuleName()).isEqualTo("PluginRule");
                assertThat(result.getEpGroup()).isEqualTo(EpisodeGroup.SPECIAL_PROMOTION);
            })
            .verifyComplete();
    }

    @Test
    void match_pluginHandler_noMatch_fallsBackToDbHandlers() {
        EpisodeSequenceRegularEntity dbRule = createRule(
            "DB NCED", "NCED", EpisodeGroup.ENDING_SONG, null, 50, true);

        when(repository.findAllByEnabledTrueOrderByPriorityDesc())
            .thenReturn(Flux.just(dbRule));

        EpisodeSequenceRegularHandler pluginHandler = new EpisodeSequenceRegularHandler() {
            @Override
            public int order() {
                return 100;
            }

            @Override
            public Mono<EpisodeSequenceRegularResult> match(String attachmentName) {
                return Mono.empty(); // plugin doesn't match
            }
        };

        EpisodeSequenceRegularPluginHook pluginHook =
            () -> List.of(pluginHandler);

        when(extensionComponentsFinder.getExtensions(EpisodeSequenceRegularPluginHook.class))
            .thenReturn(List.of(pluginHook));

        // Plugin doesn't match, falls back to DB handler
        StepVerifier.create(chain.match("[NCED] Song.mkv"))
            .assertNext(result -> {
                assertThat(result.isMatched()).isTrue();
                assertThat(result.getMatchedRuleName()).isEqualTo("DB NCED");
                assertThat(result.getEpGroup()).isEqualTo(EpisodeGroup.ENDING_SONG);
            })
            .verifyComplete();
    }

    @Test
    void match_onlyPluginHandlers_worksWithoutDbRules() {
        when(repository.findAllByEnabledTrueOrderByPriorityDesc())
            .thenReturn(Flux.empty());

        EpisodeSequenceRegularHandler pluginHandler = new EpisodeSequenceRegularHandler() {
            @Override
            public int order() {
                return 10;
            }

            @Override
            public Mono<EpisodeSequenceRegularResult> match(String attachmentName) {
                return Mono.just(EpisodeSequenceRegularResult.builder()
                    .matched(true)
                    .attachmentName(attachmentName)
                    .epGroup(EpisodeGroup.OPENING_SONG)
                    .matchedRuleName("OnlyPlugin")
                    .build());
            }
        };

        EpisodeSequenceRegularPluginHook pluginHook =
            () -> List.of(pluginHandler);

        when(extensionComponentsFinder.getExtensions(EpisodeSequenceRegularPluginHook.class))
            .thenReturn(List.of(pluginHook));

        StepVerifier.create(chain.match("NCOP.mkv"))
            .assertNext(result -> {
                assertThat(result.isMatched()).isTrue();
                assertThat(result.getMatchedRuleName()).isEqualTo("OnlyPlugin");
            })
            .verifyComplete();
    }
}
