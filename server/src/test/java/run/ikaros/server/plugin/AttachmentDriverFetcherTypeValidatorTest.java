package run.ikaros.server.plugin;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentDriverFetcher;
import run.ikaros.api.store.enums.AttachmentDriverType;

/**
 * 插件附件驱动类型校验器测试.
 */
@org.jspecify.annotations.NullUnmarked
class AttachmentDriverFetcherTypeValidatorTest {
    /** 被测试的插件标识. */
    private static final String PLUGIN_ID = "test-plugin";
    /** 被测试的附件驱动类型校验器. */
    private final AttachmentDriverFetcherTypeValidator validator =
        new AttachmentDriverFetcherTypeValidator();

    @Test
    void validateExtensionClassesAllowsDefaultCustomType() {
        assertThatCode(() -> validator.validateExtensionClasses(PLUGIN_ID,
            List.of(DefaultCustomFetcher.class))).doesNotThrowAnyException();
    }

    @Test
    void validateExtensionClassesAllowsExplicitCustomType() {
        assertThatCode(() -> validator.validateExtensionClasses(PLUGIN_ID,
            List.of(ExplicitCustomFetcher.class))).doesNotThrowAnyException();
    }

    @Test
    void validateExtensionClassesRejectsLocalType() {
        assertThatThrownBy(() -> validator.validateExtensionClasses(PLUGIN_ID,
            List.of(LocalFetcher.class)))
            .isInstanceOf(PluginValidationException.class)
            .hasMessageContaining("仅允许声明 CUSTOM 类型");
    }

    @Test
    void validateExtensionClassesRejectsDynamicType() {
        assertThatThrownBy(() -> validator.validateExtensionClasses(PLUGIN_ID,
            List.of(DynamicCustomFetcher.class)))
            .isInstanceOf(PluginValidationException.class)
            .hasMessageContaining("仅允许声明 CUSTOM 类型");
    }

    /**
     * 测试附件驱动扩展的公共空实现.
     */
    private abstract static class TestAttachmentDriverFetcher
        implements AttachmentDriverFetcher {
        @Override
        public String getDriverName() {
            return "test";
        }

        @Override
        public Flux<Attachment> getChildren(UUID driverId, UUID parentAttId,
                                            String remotePath) {
            return Flux.empty();
        }

        @Override
        public Mono<String> parseReadUrl(Attachment attachment) {
            return Mono.empty();
        }

        @Override
        public Mono<String> parseDownloadUrl(Attachment attachment) {
            return Mono.empty();
        }

        @Override
        public Flux<DataBuffer> getSteam(Attachment attachment) {
            return Flux.empty();
        }

        @Override
        public Flux<DataBuffer> getSteam(Attachment attachment, long start, long end) {
            return Flux.empty();
        }
    }

    /** 使用接口默认 CUSTOM 类型的测试驱动. */
    private static final class DefaultCustomFetcher extends TestAttachmentDriverFetcher {
    }

    /** 明确声明 CUSTOM 类型的测试驱动. */
    private static final class ExplicitCustomFetcher extends TestAttachmentDriverFetcher {
        @Override
        public AttachmentDriverType getDriverType() {
            return AttachmentDriverType.CUSTOM;
        }
    }

    /** 非法声明 LOCAL 类型的测试驱动. */
    private static final class LocalFetcher extends TestAttachmentDriverFetcher {
        @Override
        public AttachmentDriverType getDriverType() {
            return AttachmentDriverType.LOCAL;
        }
    }

    /** 动态声明驱动类型的测试驱动. */
    private static final class DynamicCustomFetcher extends TestAttachmentDriverFetcher {
        @Override
        public AttachmentDriverType getDriverType() {
            return System.nanoTime() > 0
                ? AttachmentDriverType.CUSTOM : AttachmentDriverType.LOCAL;
        }
    }
}
