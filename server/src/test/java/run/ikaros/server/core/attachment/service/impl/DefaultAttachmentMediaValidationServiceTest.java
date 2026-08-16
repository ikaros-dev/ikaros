package run.ikaros.server.core.attachment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.netty.buffer.UnpooledByteBufAllocator;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import run.ikaros.api.core.media.MediaFileFormat;

/**
 * 验证附件流式前缀检测、回放和失败释放行为.
 */
@org.jspecify.annotations.NullUnmarked
class DefaultAttachmentMediaValidationServiceTest {

    private final DefaultAttachmentMediaValidationService service =
        new DefaultAttachmentMediaValidationService();
    private final NettyDataBufferFactory bufferFactory =
        new NettyDataBufferFactory(UnpooledByteBufAllocator.DEFAULT);

    @Test
    void rejectsUnknownFilenameBeforeContentSubscription() {
        int[] subscriptions = {0};
        Flux<DataBuffer> content = Flux.defer(() -> {
            subscriptions[0]++;
            return Flux.just(bufferFactory.wrap(new byte[] {1, 2, 3}));
        });

        assertThatThrownBy(() -> service.validateFilename("movie.exe"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(subscriptions[0]).isZero();
    }

    @Test
    void validatesAndReplaysPrefixExactlyOnce() {
        byte[] first = pngPrefix();
        byte[] second = "tail".getBytes(StandardCharsets.UTF_8);
        Flux<DataBuffer> content = Flux.just(bufferFactory.wrap(first), bufferFactory.wrap(second));

        StepVerifier
            .create(service.validate(content, "movie.mp4"))
            .assertNext(validated -> {
                assertThat(validated
                    .detectionResult()
                    .format()).isEqualTo(MediaFileFormat.PNG);
                StepVerifier
                    .create(validated
                        .content()
                        .map(buffer -> {
                            byte[] bytes = new byte[buffer.readableByteCount()];
                            buffer.read(bytes);
                            DataBufferUtils.release(buffer);
                            return bytes;
                        })
                        .collectList())
                    .assertNext(parts -> assertThat(parts
                        .stream()
                        .toList()).satisfies(values -> {
                            byte[] actual = new byte[values
                                .stream()
                                .mapToInt(bytes -> bytes.length)
                                .sum()];
                            int offset = 0;
                            for (byte[] value : values) {
                                System.arraycopy(value, 0, actual, offset, value.length);
                                offset += value.length;
                            }
                            assertThat(actual).isEqualTo(join(first, second));
                        }))
                    .verifyComplete();
            })
            .verifyComplete();
    }

    @Test
    void rejectsZipAndReleasesReceivedBuffer() {
        DataBuffer buffer = bufferFactory.wrap(new byte[] {'P', 'K', 3, 4, 1});

        StepVerifier
            .create(service.validate(Flux.just(buffer), "movie.png"))
            .expectError()
            .verify();
        assertThat(((NettyDataBuffer) buffer)
            .getNativeBuffer()
            .refCnt()).isZero();
    }

    private static byte[] pngPrefix() {
        byte[] bytes = new byte[33];
        bytes[0] = (byte) 0x89;
        bytes[1] = 0x50;
        bytes[2] = 0x4e;
        bytes[3] = 0x47;
        bytes[4] = 0x0d;
        bytes[5] = 0x0a;
        bytes[6] = 0x1a;
        bytes[7] = 0x0a;
        bytes[11] = 13;
        bytes[12] = 'I';
        bytes[13] = 'H';
        bytes[14] = 'D';
        bytes[15] = 'R';
        bytes[19] = 1;
        bytes[23] = 1;
        return bytes;
    }

    private static byte[] join(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
