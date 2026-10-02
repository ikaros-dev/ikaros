package run.ikaros.ingestion;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.operations.api.BackgroundTask;
import run.ikaros.operations.api.BackgroundTaskDispatcher;
import run.ikaros.operations.api.BackgroundTaskService;

/** Scans explicitly configured local roots without reading file contents or following links. */
@Component
public final class LocalFilesystemScanTaskHandler {
    private static final int CHECKPOINT_INTERVAL = 100;
    private static final Set<String> SENSITIVE_DIRECTORY_NAMES = Set.of(
        "appdata", ".ssh", ".gnupg", ".aws", ".azure", ".config"
    );
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mkv", "mp4", "avi", "mov", "webm", "m4v", "mpeg", "mpg");
    private static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "flac", "m4a", "aac", "ogg", "opus", "wav", "aiff");
    private static final Set<String> PHOTO_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "heic", "tif", "tiff", "bmp");
    private static final Set<String> BOOK_EXTENSIONS = Set.of("epub", "mobi", "azw", "azw3", "fb2");
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of("pdf", "doc", "docx", "odt", "rtf", "txt", "md");
    private static final Set<String> ARCHIVE_EXTENSIONS = Set.of("zip", "7z", "rar", "tar", "gz", "cbz", "cbr");

    private final BackgroundTaskDispatcher dispatcher;
    private final BackgroundTaskService tasks;
    private final IngestionSourceRepository sources;
    private final ScanRunService scanRuns;
    private final DiscoveredItemService discoveredItems;
    private final IngestionCandidateService candidates;
    private final long maxFiles;

    public LocalFilesystemScanTaskHandler(BackgroundTaskDispatcher dispatcher, BackgroundTaskService tasks,
        IngestionSourceRepository sources, ScanRunService scanRuns, DiscoveredItemService discoveredItems,
        IngestionCandidateService candidates,
        @org.springframework.beans.factory.annotation.Value("${ikaros.ingestion.local-scan.max-files:100000}")
        long maxFiles) {
        if (maxFiles < 1 || maxFiles > 1_000_000) {
            throw new IllegalArgumentException("本地扫描文件数量上限必须在 1 到 1000000 之间");
        }
        this.dispatcher = dispatcher;
        this.tasks = tasks;
        this.sources = sources;
        this.scanRuns = scanRuns;
        this.discoveredItems = discoveredItems;
        this.candidates = candidates;
        this.maxFiles = maxFiles;
    }

    @PostConstruct
    void register() {
        dispatcher.register("ingestion.scan", this::handle);
    }

    Mono<Map<String, Object>> handle(BackgroundTask task) {
        UUID ownerId = uuid(task, "actor_id");
        UUID sourceId = uuid(task, "source_id");
        UUID scanRunId = uuid(task, "scan_run_id");
        AtomicLong discoveredCount = new AtomicLong();
        AtomicLong candidateCount = new AtomicLong();
        AtomicLong skippedCount = new AtomicLong();

        return Mono.zip(scanRuns.get(ownerId, scanRunId), sources.findByIdAndOwnerId(sourceId, ownerId)
                .switchIfEmpty(Mono.error(new NotFoundException("导入来源不存在"))))
            .flatMap(pair -> {
                ScanRunView scan = pair.getT1();
                IngestionSourceEntity source = pair.getT2();
                if (!source.id().equals(scan.sourceId())) {
                    return Mono.error(new NotFoundException("扫描运行不存在"));
                }
                if (scan.status() == ScanRunStatus.CANCELLED) {
                    return Mono.just(Map.<String, Object>of("scan_run_id", scanRunId.toString(), "cancelled", true));
                }
                if (!IngestionSourceType.LOCAL_FILESYSTEM.name().equals(source.sourceType())) {
                    return Mono.error(new ConflictException("当前扫描任务只支持本地文件系统来源"));
                }
                if (!IngestionSourceStatus.ENABLED.name().equals(source.status())) {
                    return Mono.error(new ConflictException("本地来源当前未启用"));
                }
                return resolveRoot(source.rootReference())
                    .flatMap(root -> scanRuns.checkpoint(scanRunId, "starting", 0, 0, 0, null)
                        .then(checkCancellation(task))
                        .then(scanTree(task, ownerId, scanRunId, root,
                            discoveredCount, candidateCount, skippedCount)))
                    .then(tasks.get(task.id()))
                    .flatMap(current -> {
                        ScanRunStatus status = current.cancelRequestedAt() == null
                            ? ScanRunStatus.SUCCEEDED : ScanRunStatus.CANCELLED;
                        return scanRuns.finish(scanRunId, status, "completed", discoveredCount.get(),
                            candidateCount.get(), skippedCount.get(), null)
                            .thenReturn(Map.<String, Object>of("scan_run_id", scanRunId.toString(),
                                "discovered_count", discoveredCount.get(), "candidate_count", candidateCount.get(),
                                "skipped_count", skippedCount.get(), "cancelled", status == ScanRunStatus.CANCELLED));
                    });
            })
            .onErrorResume(error -> {
                if (error instanceof ScanCancelledException) {
                    return scanRuns.finish(scanRunId, ScanRunStatus.CANCELLED, "cancelled", discoveredCount.get(),
                        candidateCount.get(), skippedCount.get(), null)
                        .thenReturn(Map.<String, Object>of("scan_run_id", scanRunId.toString(), "cancelled", true));
                }
                return scanRuns.finish(scanRunId, ScanRunStatus.FAILED, "failed", discoveredCount.get(),
                    candidateCount.get(), skippedCount.get(), "本地来源扫描失败")
                    .then(Mono.error(error));
            });
    }

    private Mono<Void> scanTree(BackgroundTask task, UUID ownerId, UUID scanRunId, Path root,
        AtomicLong discoveredCount, AtomicLong candidateCount, AtomicLong skippedCount) {
        return Flux.using(
                () -> Files.walk(root),
                Flux::fromStream,
                Stream::close
            )
            .subscribeOn(Schedulers.boundedElastic())
            .filter(path -> !path.equals(root) && !hasSensitiveDirectory(root.relativize(path)))
            .concatMap(path -> Mono.fromCallable(() -> fileInfo(root, path))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(info -> info == null ? Mono.empty() : processFile(task, ownerId, scanRunId,
                    info, discoveredCount, candidateCount, skippedCount)), 1)
            .onErrorMap(IOException.class, ignored -> new ConflictException("本地来源读取失败"))
            .onErrorMap(java.io.UncheckedIOException.class, ignored -> new ConflictException("本地来源读取失败"))
            .then();
    }

    private Mono<Void> processFile(BackgroundTask task, UUID ownerId, UUID scanRunId,
        FileInfo file, AtomicLong discoveredCount, AtomicLong candidateCount, AtomicLong skippedCount) {
        if (discoveredCount.get() >= maxFiles) {
            return Mono.error(new ConflictException("本地扫描超过配置的文件数量上限"));
        }
        long count = discoveredCount.incrementAndGet();
        return discoveredItems.record(ownerId, scanRunId, new DiscoveredItemRequest(file.relativeKey(),
                file.sizeBytes(), file.modifiedAt(), file.etag(), file.mediaType(), "AVAILABLE", 0))
            .then(Mono.defer(() -> {
                String candidateType = resourceType(file.extension());
                if (candidateType == null) {
                    skippedCount.incrementAndGet();
                    return Mono.empty();
                }
                return candidates.create(ownerId, scanRunId, new CreateCandidateRequest(candidateType,
                    file.title(), null, 75, fingerprint(file.relativeKey(), file.sizeBytes(), file.modifiedAt())))
                    .doOnNext(ignored -> candidateCount.incrementAndGet())
                    .then();
            }))
            .then(Mono.defer(() -> {
                if (count % CHECKPOINT_INTERVAL != 0) return Mono.empty();
                String checkpoint = "files=" + count;
                return checkCancellation(task)
                    .then(scanRuns.checkpoint(scanRunId, checkpoint, count, candidateCount.get(), skippedCount.get(), null))
                    .then(tasks.updateProgress(task.id(), task.leaseToken(), Map.of(
                        "discovered_count", count, "candidate_count", candidateCount.get(),
                        "skipped_count", skippedCount.get()))).then();
            }));
    }

    private Mono<Void> checkCancellation(BackgroundTask task) {
        return tasks.get(task.id()).flatMap(current -> current.cancelRequestedAt() == null
            ? Mono.empty() : Mono.error(new ScanCancelledException()));
    }

    private Mono<Path> resolveRoot(String rootReference) {
        return Mono.fromCallable(() -> {
            if (rootReference == null || rootReference.isBlank()) {
                throw new ConflictException("本地来源缺少根目录配置");
            }
            Path configured = Path.of(rootReference).toAbsolutePath().normalize();
            Path root = configured.toRealPath();
            if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) || root.getParent() == null
                || isSensitiveRoot(root)) {
                throw new ConflictException("本地来源根目录不可扫描");
            }
            return root;
        }).subscribeOn(Schedulers.boundedElastic())
            .onErrorMap(error -> error instanceof ConflictException ? error
                : new ConflictException("本地来源根目录不可访问"));
    }

    private boolean isSensitiveRoot(Path root) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            for (String property : new String[] {"java.home"}) {
                if (isWithin(root, System.getProperty(property))) return true;
            }
            for (String variable : new String[] {"SystemRoot", "WINDIR", "ProgramFiles", "ProgramFiles(x86)", "ProgramData"}) {
                if (isWithin(root, System.getenv(variable))) return true;
            }
            return false;
        }
        for (String sensitive : new String[] {"/", "/etc", "/proc", "/sys", "/dev", "/run", "/root", "/boot", "/usr", "/bin", "/sbin", "/var/lib"}) {
            if (root.startsWith(Path.of(sensitive))) return true;
        }
        return false;
    }

    private boolean isWithin(Path root, String candidate) {
        if (candidate == null || candidate.isBlank()) return false;
        try {
            return root.startsWith(Path.of(candidate).toRealPath());
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }

    private boolean hasSensitiveDirectory(Path relative) {
        for (Path segment : relative) {
            if (SENSITIVE_DIRECTORY_NAMES.contains(segment.toString().toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private FileInfo fileInfo(Path root, Path path) throws IOException {
        if (Files.isSymbolicLink(path)) return null;
        Path realPath = path.toRealPath();
        if (!realPath.startsWith(root)) return null;
        BasicFileAttributes attributes = Files.readAttributes(realPath, BasicFileAttributes.class,
            LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isRegularFile()) return null;
        Path relative = root.relativize(realPath);
        String relativeKey = relative.toString().replace(FileSystems.getDefault().getSeparator(), "/");
        String fileName = relative.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String extension = dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        String title = dot > 0 ? fileName.substring(0, dot) : fileName;
        Instant modifiedAt = attributes.lastModifiedTime().toInstant();
        return new FileInfo(relativeKey, attributes.size(), modifiedAt,
            attributes.size() + "-" + attributes.lastModifiedTime().toMillis(), mediaType(extension), extension, title);
    }

    private String resourceType(String extension) {
        if (VIDEO_EXTENSIONS.contains(extension)) return "VIDEO";
        if (AUDIO_EXTENSIONS.contains(extension)) return "MUSIC";
        if (PHOTO_EXTENSIONS.contains(extension)) return "PHOTO";
        if (BOOK_EXTENSIONS.contains(extension)) return "BOOK";
        if (DOCUMENT_EXTENSIONS.contains(extension)) return "DOCUMENT";
        if (ARCHIVE_EXTENSIONS.contains(extension)) return "ARCHIVE";
        return null;
    }

    private String mediaType(String extension) {
        if (VIDEO_EXTENSIONS.contains(extension)) return "video/" + (extension.equals("mkv") ? "x-matroska" : "mp4");
        if (AUDIO_EXTENSIONS.contains(extension)) return "audio/" + extension;
        if (PHOTO_EXTENSIONS.contains(extension)) return "image/" + (extension.equals("jpg") ? "jpeg" : extension);
        if (extension.equals("pdf")) return "application/pdf";
        if (extension.equals("epub")) return "application/epub+zip";
        if (ARCHIVE_EXTENSIONS.contains(extension) || BOOK_EXTENSIONS.contains(extension)) return "application/octet-stream";
        if (extension.equals("txt") || extension.equals("md")) return "text/plain";
        if (extension.equals("doc")) return "application/msword";
        if (extension.equals("docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        return null;
    }

    private String fingerprint(String relativeKey, long size, Instant modifiedAt) {
        try {
            byte[] input = (relativeKey + "\0" + size + "\0" + modifiedAt.toEpochMilli())
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 不可用", impossible);
        }
    }

    private UUID uuid(BackgroundTask task, String key) {
        Object value = task.payload().get(key);
        if (value == null) throw new IllegalArgumentException("扫描任务缺少必要参数");
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("扫描任务参数无效");
        }
    }

    private record FileInfo(String relativeKey, long sizeBytes, Instant modifiedAt, String etag,
                            String mediaType, String extension, String title) { }

    private static final class ScanCancelledException extends RuntimeException { }
}
