package run.ikaros.music;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.storage.api.StorageService;

@Service
public class PersistentMusicImportService implements MusicImportService {
    private final StorageService storage;
    private final MusicTrackRepository tracks;
    private final MusicAudioSourceService sources;
    private final MusicImportRepository imports;
    private final TransactionalOperator transaction;

    public PersistentMusicImportService(StorageService storage, MusicTrackRepository tracks,
                                       MusicAudioSourceService sources, MusicImportRepository imports,
                                       TransactionalOperator transaction) {
        this.storage = storage; this.tracks = tracks; this.sources = sources; this.imports = imports; this.transaction = transaction;
    }

    @Override
    public Mono<MusicImportView> create(UUID ownerId, CreateMusicImportRequest request, String key) {
        if (key == null || key.isBlank() || key.length() > 256) return Mono.error(new IllegalArgumentException("Idempotency-Key 不合法"));
        Mono<MusicImportView> operation = storage.get(ownerId, request.attachmentId()).flatMap(attachment -> validate(attachment.fileName(), attachment.mediaType())
            .then(tracks.findByOwnerIdAndResourceId(ownerId, attachment.resourceId())
                .flatMap(existing -> Mono.<MusicImportEntity>error(new ConflictException("该 Attachment 已导入为音乐 Track")))
                .switchIfEmpty(Mono.defer(() -> {
                    String title = request.title() == null || request.title().isBlank() ? baseName(attachment.fileName()) : request.title().trim();
                    return tracks.save(new MusicTrackEntity(null, ownerId, attachment.resourceId(), title, request.durationMillis(), null, false, null))
                        .flatMap(track -> sources.add(ownerId, track.id(), new AddMusicAudioSourceRequest(request.attachmentId(), request.codec(), request.container(), request.durationMillis(), request.sampleRate(), request.bitDepth(), request.channels(), request.bitrate(), request.lossless(), 0))
                            .then(imports.save(new MusicImportEntity(null, ownerId, request.attachmentId(), track.id(), "SUCCEEDED", null, null, key, Instant.now(), Instant.now(), null))));
                }))).map(this::view));
        return imports.findByOwnerIdAndIdempotencyKey(ownerId, key).map(this::view).switchIfEmpty(transaction.transactional(operation))
            .onErrorResume(DuplicateKeyException.class, error -> imports.findByOwnerIdAndIdempotencyKey(ownerId, key).switchIfEmpty(Mono.error(error)).map(this::view));
    }

    @Override public Flux<MusicImportView> list(UUID ownerId) { return imports.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).take(100).flatMap(this::viewAsync); }

    private Mono<Void> validate(String fileName, String mediaType) {
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        boolean extension = name.endsWith(".mp3") || name.endsWith(".flac") || name.endsWith(".m4a") || name.endsWith(".aac") || name.endsWith(".ogg") || name.endsWith(".wav") || name.endsWith(".opus");
        boolean audio = mediaType == null || mediaType.toLowerCase(Locale.ROOT).startsWith("audio/");
        return extension && audio ? Mono.empty() : Mono.error(new ConflictException("仅支持常见音频附件"));
    }
    private Mono<MusicImportView> viewAsync(MusicImportEntity value) { return value.trackId() == null ? Mono.just(view(value, null)) : tracks.findById(value.trackId()).map(track -> view(value, track)); }
    private MusicImportView view(MusicImportEntity value) { return view(value, null); }
    private MusicImportView view(MusicImportEntity value, MusicTrackEntity track) { return new MusicImportView(value.id(), value.attachmentId(), value.trackId(), track == null ? null : track.title(), track == null ? null : track.durationMillis(), value.status(), value.errorCode(), value.errorMessage(), value.createdAt(), value.updatedAt()); }
    private String baseName(String value) { String name = value == null || value.isBlank() ? "未命名音乐" : value; int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')); if (slash >= 0) name = name.substring(slash + 1); int dot = name.lastIndexOf('.'); return (dot > 0 ? name.substring(0, dot) : name).trim(); }
}
