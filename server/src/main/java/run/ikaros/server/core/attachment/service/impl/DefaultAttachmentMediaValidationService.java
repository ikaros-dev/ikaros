package run.ikaros.server.core.attachment.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Subscription;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.scheduler.Schedulers;
import run.ikaros.api.core.attachment.exception.AttachmentUploadException;
import run.ikaros.api.core.media.MediaFileDetectionResult;
import run.ikaros.api.core.media.MediaFileDetector;
import run.ikaros.api.core.media.MediaFilePolicy;
import run.ikaros.server.core.attachment.service.AttachmentMediaValidationService;
import run.ikaros.server.core.attachment.service.ValidatedMediaStream;

/** 默认附件媒体名称门禁和有限前缀流式验证实现. */
@Service
public class DefaultAttachmentMediaValidationService
    implements AttachmentMediaValidationService {

    @Override
    public String validateFilename(String filename) {
        return MediaFilePolicy.extractExtension(filename)
            .orElseThrow(() -> new IllegalArgumentException("不支持的媒体文件名: " + filename));
    }

    @Override
    public Mono<MediaFileDetectionResult> validate(Path path, String filename) {
        String extension = validateFilename(filename);
        return Mono.fromCallable(() -> readPrefix(path))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(prefix -> detect(prefix, extension, filename));
    }

    @Override
    public Mono<ValidatedMediaStream> validate(Flux<DataBuffer> content, String filename) {
        String extension = validateFilename(filename);
        return Mono.create(sink -> new StreamValidationSession(content, extension, filename, sink)
            .start());
    }

    private static byte[] readPrefix(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return inputStream.readNBytes(MediaFileDetector.MAX_PREFIX_SIZE);
        }
    }

    private static Mono<MediaFileDetectionResult> detect(byte[] prefix, String extension,
                                                          String filename) {
        return Mono.justOrEmpty(MediaFileDetector.detect(prefix, extension))
            .switchIfEmpty(Mono.error(new AttachmentUploadException(
                "无法确认媒体文件真实格式: " + filename, null)));
    }

    /** 连接前缀验证阶段和仅允许订阅一次的后续回放阶段. */
    private static final class StreamValidationSession extends BaseSubscriber<DataBuffer> {

        /** 待验证的原始数据流. */
        private final Flux<DataBuffer> source;
        /** 规范化后的文件扩展名. */
        private final String extension;
        /** 用于错误信息的安全文件名. */
        private final String filename;
        /** 返回验证结果的响应式接收器. */
        private final MonoSink<ValidatedMediaStream> resultSink;
        /** 不超过检测上限的前缀缓存. */
        private final ByteArrayOutputStream prefix =
            new ByteArrayOutputStream(MediaFileDetector.MAX_PREFIX_SIZE);
        /** 回放流累计请求量. */
        private final AtomicLong requested = new AtomicLong();
        /** 防止回放流被重复订阅. */
        private final AtomicBoolean replaySubscribed = new AtomicBoolean();
        /** 前缀回放流的接收器. */
        private volatile @Nullable FluxSink<DataBuffer> replaySink;
        /** 前缀达到上限时同一缓冲区内尚未消费的尾部. */
        private volatile @Nullable DataBuffer boundaryBuffer;
        /** 验证完成后待回放的前缀字节. */
        private volatile byte @Nullable [] replayPrefix;
        /** 原始上游是否已经结束. */
        private volatile boolean sourceCompleted;
        /** 验证结果返回后原始上游延迟报告的错误. */
        private volatile @Nullable Throwable sourceError;
        /** 当前会话是否已经终止. */
        private volatile boolean terminated;
        /** 前缀是否已经完成验证. */
        private volatile boolean validated;

        private StreamValidationSession(Flux<DataBuffer> source, String extension,
                                        String filename,
                                        MonoSink<ValidatedMediaStream> resultSink) {
            this.source = source;
            this.extension = extension;
            this.filename = filename;
            this.resultSink = resultSink;
        }

        private void start() {
            resultSink.onCancel(this::dispose);
            source.doOnDiscard(DataBuffer.class, DataBufferUtils::release).subscribe(this);
        }

        @Override
        protected void hookOnSubscribe(Subscription subscription) {
            request(1);
        }

        @Override
        protected void hookOnNext(DataBuffer dataBuffer) {
            if (terminated) {
                DataBufferUtils.release(dataBuffer);
                return;
            }
            if (!validated) {
                consumePrefix(dataBuffer);
                return;
            }
            emitUpstreamBuffer(dataBuffer);
        }

        private void consumePrefix(DataBuffer dataBuffer) {
            int remaining = MediaFileDetector.MAX_PREFIX_SIZE - prefix.size();
            int copyLength = Math.min(remaining, dataBuffer.readableByteCount());
            byte[] bytes = new byte[copyLength];
            dataBuffer.read(bytes);
            prefix.writeBytes(bytes);
            if (dataBuffer.readableByteCount() == 0) {
                DataBufferUtils.release(dataBuffer);
            } else {
                boundaryBuffer = dataBuffer;
            }
            if (prefix.size() == MediaFileDetector.MAX_PREFIX_SIZE) {
                validatePrefix();
            } else {
                request(1);
            }
        }

        private void validatePrefix() {
            MediaFileDetector.detect(prefix.toByteArray(), extension)
                .ifPresentOrElse(this::validationSucceeded,
                    () -> validationFailed(new AttachmentUploadException(
                        "无法确认媒体文件真实格式: " + filename, null)));
        }

        private void validationSucceeded(MediaFileDetectionResult detectionResult) {
            validated = true;
            replayPrefix = prefix.toByteArray();
            resultSink.success(new ValidatedMediaStream(detectionResult,
                Flux.create(this::connectReplay, FluxSink.OverflowStrategy.ERROR)
                    .doOnDiscard(DataBuffer.class, DataBufferUtils::release)));
        }

        private void validationFailed(Throwable throwable) {
            terminated = true;
            cancel();
            releaseBoundaryBuffer();
            resultSink.error(throwable);
        }

        private void connectReplay(FluxSink<DataBuffer> sink) {
            if (!replaySubscribed.compareAndSet(false, true)) {
                sink.error(new IllegalStateException("媒体回放流只允许订阅一次"));
                return;
            }
            replaySink = sink;
            sink.onRequest(this::requestReplay);
            sink.onCancel(this::dispose);
            sink.onDispose(this::dispose);
        }

        private synchronized void requestReplay(long demand) {
            if (terminated || demand <= 0) {
                return;
            }
            requested.getAndAccumulate(demand, StreamValidationSession::addCap);
            drainReplay();
        }

        private synchronized void drainReplay() {
            FluxSink<DataBuffer> sink = replaySink;
            if (sink == null || terminated || requested.get() == 0) {
                return;
            }
            if (replayPrefix != null) {
                byte[] bytes = replayPrefix;
                replayPrefix = null;
                requested.decrementAndGet();
                sink.next(DefaultDataBufferFactory.sharedInstance.wrap(bytes));
                if (requested.get() == 0) {
                    return;
                }
            }
            if (boundaryBuffer != null) {
                DataBuffer dataBuffer = boundaryBuffer;
                boundaryBuffer = null;
                requested.decrementAndGet();
                sink.next(dataBuffer);
                if (requested.get() == 0) {
                    return;
                }
            }
            if (sourceCompleted) {
                terminated = true;
                sink.complete();
                return;
            }
            if (sourceError != null) {
                terminated = true;
                sink.error(sourceError);
                return;
            }
            request(1);
        }

        private synchronized void emitUpstreamBuffer(DataBuffer dataBuffer) {
            FluxSink<DataBuffer> sink = replaySink;
            if (sink == null || requested.get() == 0 || terminated) {
                DataBufferUtils.release(dataBuffer);
                validationFailed(new IllegalStateException("媒体回放流缺少下游请求"));
                return;
            }
            requested.decrementAndGet();
            sink.next(dataBuffer);
            drainReplay();
        }

        @Override
        protected void hookOnComplete() {
            sourceCompleted = true;
            if (!validated) {
                validatePrefix();
                return;
            }
            drainReplay();
        }

        @Override
        protected void hookOnError(Throwable throwable) {
            if (!validated) {
                terminated = true;
                releaseBoundaryBuffer();
                resultSink.error(throwable);
            } else {
                sourceError = throwable;
                if (replaySink != null) {
                    terminated = true;
                    replaySink.error(throwable);
                }
            }
        }

        public synchronized void dispose() {
            if (terminated) {
                return;
            }
            terminated = true;
            cancel();
            releaseBoundaryBuffer();
            replayPrefix = null;
        }

        private void releaseBoundaryBuffer() {
            DataBuffer dataBuffer = boundaryBuffer;
            boundaryBuffer = null;
            if (dataBuffer != null) {
                DataBufferUtils.release(dataBuffer);
            }
        }

        private static long addCap(long current, long demand) {
            long result = current + demand;
            return result < 0 ? Long.MAX_VALUE : result;
        }
    }
}
