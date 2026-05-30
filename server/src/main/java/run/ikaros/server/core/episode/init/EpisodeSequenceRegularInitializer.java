package run.ikaros.server.core.episode.init;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.EpisodeGroup;
import run.ikaros.server.store.entity.EpisodeSequenceRegularEntity;
import run.ikaros.server.store.repository.EpisodeSequenceRegularRepository;

@Slf4j
@Component
public class EpisodeSequenceRegularInitializer {

    private final EpisodeSequenceRegularRepository repository;

    public EpisodeSequenceRegularInitializer(EpisodeSequenceRegularRepository repository) {
        this.repository = repository;
    }

    /**
     * Insert built-in episode sequence regular rules on startup.
     * Idempotent: only inserts rules whose name does not already exist in the database.
     */
    @EventListener(ApplicationReadyEvent.class)
    public Mono<Void> initialize() {
        List<EpisodeSequenceRegularEntity> builtInRules = buildBuiltInRules();

        return Flux.fromIterable(builtInRules)
            .flatMap(this::insertIfNotExists)
            .doOnEach(signal -> {
                if (signal.isOnNext() && signal.get() != null) {
                    log.debug("Inserted built-in episode sequence regular rule: [{}] {}",
                        signal.get().getName(), signal.get().getRegex());
                }
            })
            .then();
    }

    private Mono<EpisodeSequenceRegularEntity> insertIfNotExists(
        EpisodeSequenceRegularEntity rule) {
        return repository.findByName(rule.getName())
            .switchIfEmpty(Mono.defer(() -> {
                log.info("Inserting built-in rule: name=[{}] regex=[{}] epGroup=[{}]",
                    rule.getName(), rule.getRegex(), rule.getEpGroup());
                return repository.insert(rule);
            }));
    }

    static List<EpisodeSequenceRegularEntity> buildBuiltInRules() {
        List<EpisodeSequenceRegularEntity> rules = new ArrayList<>();

        // Specific [01]..[100] rules with sequence and descending priority
        for (int i = 1; i <= 100; i++) {
            String num = String.format("%02d", i); // "01", "02", ..., "99"
            String regex = "\\[" + num + "\\]";
            int priority = 201 - i * 2; // [01]=199, [02]=197, ..., [100]=1
            String name = "序号[" + num + "]";
            String desc = "匹配文件名中的 [" + num + "] 标记，集序号=" + i;
            rules.add(buildRule(name, regex, EpisodeGroup.MAIN, (float) i, priority, desc));
        }

        // Specific [EP01]..[EP100] rules with sequence and descending priority
        for (int i = 1; i <= 100; i++) {
            String num = String.format("%02d", i); // "01", "02", ..., "99"
            String regex = "\\[EP" + num + "\\]";
            int priority = 200 - i * 2; // [EP01]=198, [EP02]=196, ..., [EP100]=0
            String name = "序号[EP" + num + "]";
            String desc = "匹配文件名中的 [EP" + num + "] 标记，集序号=" + i;
            rules.add(buildRule(name, regex, EpisodeGroup.MAIN, (float) i, priority, desc));
        }

        // Specific [01v2]..[100vX] rules — strip vX, keep preceding number
        for (int i = 1; i <= 100; i++) {
            String num = String.format("%02d", i);
            String regex = "\\[" + num + "v[0-9]+\\]";
            int priority = 300 - i; // [01vX]=299, [02vX]=298, ..., [100vX]=200
            String name = "序号[" + num + "v*]";
            String desc = "匹配文件名中的 [" + num + "v数字] 标记，去掉v后缀保留集序号=" + i;
            rules.add(buildRule(name, regex, EpisodeGroup.MAIN, (float) i, priority, desc));
        }

        // [NCOP] → OPENING_SONG, sequence=0
        rules.add(buildRule("[NCOP]转OP",
            "\\[NCOP\\]", EpisodeGroup.OPENING_SONG, 0f, 1000,
            "匹配 [NCOP] 标记为片头曲，序号=0"));

        // [NCED] → ENDING_SONG, sequence=0
        rules.add(buildRule("[NCED]转ED",
            "\\[NCED\\]", EpisodeGroup.ENDING_SONG, 0f, 999,
            "匹配 [NCED] 标记为片尾曲，序号=0"));

        // [SP] → SPECIAL_PROMOTION, sequence=0
        rules.add(buildRule("[SP]转SP",
            "\\[SP\\]", EpisodeGroup.SPECIAL_PROMOTION, 0f, 998,
            "匹配 [SP] 标记为特典，序号=0"));

        // [CM] → COMMERCIAL_MESSAGE, sequence=0
        rules.add(buildRule("[CM]转CM",
            "\\[CM\\]", EpisodeGroup.COMMERCIAL_MESSAGE, 0f, 997,
            "匹配 [CM] 标记为广告，序号=0"));

        // [OVA] → ORIGINAL_VIDEO_ANIMATION, sequence=0
        rules.add(buildRule("[OVA]转OVA",
            "\\[OVA\\]", EpisodeGroup.ORIGINAL_VIDEO_ANIMATION, 0f, 996,
            "匹配 [OVA] 标记为原版视频动画，序号=0"));

        // [OAD] → ORIGINAL_ANIMATION_DISC, sequence=0
        rules.add(buildRule("[OAD]转OAD",
            "\\[OAD\\]", EpisodeGroup.ORIGINAL_ANIMATION_DISC, 0f, 995,
            "匹配 [OAD] 标记为原版动画光盘，序号=0"));

        // [OAD01]..[OAD100] → ORIGINAL_ANIMATION_DISC, sequence=i
        for (int i = 1; i <= 100; i++) {
            String num = String.format("%02d", i);
            String regex = "\\[OAD" + num + "\\]";
            int priority = 1100 - i; // [OAD01]=1099, [OAD02]=1098, ..., [OAD100]=1000
            String name = "序号[OAD" + num + "]";
            String desc = "匹配 [OAD" + num + "] 为原版动画光盘，集序号=" + i;
            rules.add(buildRule(name, regex, EpisodeGroup.ORIGINAL_ANIMATION_DISC,
                (float) i, priority, desc));
        }

        // General [OAD数字] fallback
        rules.add(buildRule("方括号OAD序号通用匹配", "\\[OAD[0-9]{1,3}\\]",
            EpisodeGroup.ORIGINAL_ANIMATION_DISC, null, Integer.MIN_VALUE,
            "匹配 [OAD01] [OAD134] 等带 OAD 前缀的序号（最末位兜底）"));

        // [OVA01]..[OVA100] → ORIGINAL_VIDEO_ANIMATION, sequence=i
        for (int i = 1; i <= 100; i++) {
            String num = String.format("%02d", i);
            String regex = "\\[OVA" + num + "\\]";
            int priority = 1000 - i; // [OVA01]=999, [OVA02]=998, ..., [OVA100]=900
            String name = "序号[OVA" + num + "]";
            String desc = "匹配 [OVA" + num + "] 为原版视频动画，集序号=" + i;
            rules.add(buildRule(name, regex, EpisodeGroup.ORIGINAL_VIDEO_ANIMATION,
                (float) i, priority, desc));
        }

        // General [OVA数字] fallback
        rules.add(buildRule("方括号OVA序号通用匹配", "\\[OVA[0-9]{1,3}\\]",
            EpisodeGroup.ORIGINAL_VIDEO_ANIMATION, null, Integer.MIN_VALUE,
            "匹配 [OVA01] [OVA134] 等带 OVA 前缀的序号（最末位兜底）"));

        // [CM01]..[CM100] → COMMERCIAL_MESSAGE, sequence=i
        for (int i = 1; i <= 100; i++) {
            String num = String.format("%02d", i);
            String regex = "\\[CM" + num + "\\]";
            int priority = 900 - i; // [CM01]=899, [CM02]=898, ..., [CM100]=800
            String name = "序号[CM" + num + "]";
            String desc = "匹配 [CM" + num + "] 为广告，集序号=" + i;
            rules.add(buildRule(name, regex, EpisodeGroup.COMMERCIAL_MESSAGE,
                (float) i, priority, desc));
        }

        // General [CM数字] fallback
        rules.add(buildRule("方括号CM序号通用匹配", "\\[CM[0-9]{1,3}\\]",
            EpisodeGroup.COMMERCIAL_MESSAGE, null, Integer.MIN_VALUE,
            "匹配 [CM01] [CM134] 等带 CM 前缀的序号（最末位兜底）"));

        // [SP01]..[SP100] → SPECIAL_PROMOTION, sequence=i
        for (int i = 1; i <= 100; i++) {
            String num = String.format("%02d", i);
            String regex = "\\[SP" + num + "\\]";
            int priority = 800 - i; // [SP01]=799, [SP02]=798, ..., [SP100]=700
            String name = "序号[SP" + num + "]";
            String desc = "匹配 [SP" + num + "] 为特典，集序号=" + i;
            rules.add(buildRule(name, regex, EpisodeGroup.SPECIAL_PROMOTION,
                (float) i, priority, desc));
        }

        // General [SP数字] fallback
        rules.add(buildRule("方括号SP序号通用匹配", "\\[SP[0-9]{1,3}\\]",
            EpisodeGroup.SPECIAL_PROMOTION, null, Integer.MIN_VALUE,
            "匹配 [SP01] [SP134] 等带 SP 前缀的序号（最末位兜底）"));

        // [NCOP01]..[NCOP100] → OPENING_SONG, sequence=i
        for (int i = 1; i <= 100; i++) {
            String num = String.format("%02d", i);
            String regex = "\\[NCOP" + num + "\\]";
            int priority = 700 - i; // [NCOP01]=699, [NCOP02]=698, ..., [NCOP100]=600
            String name = "序号[NCOP" + num + "]";
            String desc = "匹配 [NCOP" + num + "] 为片头曲，集序号=" + i;
            rules.add(buildRule(name, regex, EpisodeGroup.OPENING_SONG,
                (float) i, priority, desc));
        }

        // [NCED01]..[NCED100] → ENDING_SONG, sequence=i
        for (int i = 1; i <= 100; i++) {
            String num = String.format("%02d", i);
            String regex = "\\[NCED" + num + "\\]";
            int priority = 599 - i; // [NCED01]=598, [NCED02]=597, ..., [NCED100]=499
            String name = "序号[NCED" + num + "]";
            String desc = "匹配 [NCED" + num + "] 为片尾曲，集序号=" + i;
            rules.add(buildRule(name, regex, EpisodeGroup.ENDING_SONG,
                (float) i, priority, desc));
        }

        // General [NCOP数字] fallback
        rules.add(buildRule("方括号NCOP序号通用匹配", "\\[NCOP[0-9]{1,3}\\]",
            EpisodeGroup.OPENING_SONG, null, Integer.MIN_VALUE,
            "匹配 [NCOP01] [NCOP134] 等带 NCOP 前缀的序号（最末位兜底）"));

        // General [NCED数字] fallback
        rules.add(buildRule("方括号NCED序号通用匹配", "\\[NCED[0-9]{1,3}\\]",
            EpisodeGroup.ENDING_SONG, null, Integer.MIN_VALUE,
            "匹配 [NCED01] [NCED134] 等带 NCED 前缀的序号（最末位兜底）"));

        // Specific " 01 ".." 99 " rules — number padded by spaces
        for (int i = 1; i <= 99; i++) {
            String num = String.format("%02d", i);
            String regex = "\\s" + num + "\\s";
            int priority = 400 - i; // " 01 "=399, " 02 "=398, ..., " 99 "=301
            String name = "空串" + num + "空串";
            String desc = "匹配文件名中空格包裹的 " + num + " 标记，集序号=" + i;
            rules.add(buildRule(name, regex, EpisodeGroup.MAIN, (float) i, priority, desc));
        }

        // General [数字] fallback at lowest priority
        rules.add(buildRule("方括号序号通用匹配", "\\[[0-9]{1,3}(\\.[0-9]+)?\\]",
            EpisodeGroup.MAIN, null, Integer.MIN_VALUE,
            "匹配 [01] [134] [15.5] 等方括号内的集序号（最末位兜底）"));

        // General [EP数字] fallback at lowest priority
        rules.add(buildRule("方括号EP序号通用匹配", "\\[EP[0-9]{1,3}\\]",
            EpisodeGroup.MAIN, null, Integer.MIN_VALUE,
            "匹配 [EP01] [EP134] 等方括号内 EP 前缀的集序号（最末位兜底）"));

        return rules;
    }

    private static EpisodeSequenceRegularEntity buildRule(
        String name, String regex, EpisodeGroup epGroup,
        Float sequence, int priority, String description) {
        LocalDateTime now = LocalDateTime.now();
        EpisodeSequenceRegularEntity entity = EpisodeSequenceRegularEntity.builder()
            .name(name)
            .regex(regex)
            .epGroup(epGroup)
            .sequence(sequence)
            .priority(priority)
            .description(description)
            .enabled(true)
            .build();
        entity.setCreateTime(now)
            .setUpdateTime(now)
            .setDeleteStatus(false);
        return entity;
    }
}
