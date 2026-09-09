package run.ikaros.media;

import java.time.Instant;
import java.util.UUID;
import java.util.HashSet;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.resource.api.CreateResourceRequest;
import run.ikaros.resource.api.ResourceService;
import run.ikaros.resource.api.ResourceType;

@Service
public class PersistentMediaCatalogService implements MediaCatalogService {
    private final ResourceService resources;
    private final MediaSubjectRepository subjects;
    private final MediaSeasonRepository seasons;
    private final MediaEpisodeRepository episodes;

    public PersistentMediaCatalogService(ResourceService resources, MediaSubjectRepository subjects,
        MediaSeasonRepository seasons, MediaEpisodeRepository episodes) {
        this.resources = resources; this.subjects = subjects; this.seasons = seasons; this.episodes = episodes;
    }

    @Override public Mono<MediaSubjectView> createSubject(UUID ownerId, CreateMediaSubjectRequest request) {
        String locale = request.locale() == null || request.locale().isBlank() ? "en-US" : request.locale();
        return resources.create(ownerId, new CreateResourceRequest(ResourceType.VIDEO, request.title(), locale))
            .flatMap(resource -> subjects.save(new MediaSubjectEntity(null, ownerId, resource.id(), request.kind(),
                Instant.now(), Instant.now(), null))).map(this::subjectView);
    }

    @Override public Flux<MediaSubjectView> listSubjects(UUID ownerId) { return subjects.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).take(100).map(this::subjectView); }

    @Override public Mono<MediaSeasonView> createSeason(UUID ownerId, UUID subjectId, CreateMediaSeasonRequest request) {
        return ownedSubject(ownerId, subjectId).flatMap(subject -> {
            String locale = request.locale() == null || request.locale().isBlank() ? "en-US" : request.locale();
            return resources.create(ownerId, new CreateResourceRequest(ResourceType.VIDEO, request.title(), locale))
                .flatMap(resource -> seasons.save(new MediaSeasonEntity(null, ownerId, subject.id(), resource.id(),
                    request.seasonNumber(), request.title().trim(), Instant.now(), Instant.now(), null)));
        }).map(this::seasonView);
    }

    @Override public Flux<MediaSeasonView> listSeasons(UUID ownerId, UUID subjectId) {
        return ownedSubject(ownerId, subjectId).flatMapMany(s -> seasons.findAllByOwnerIdAndSubjectIdOrderBySeasonNumberAsc(ownerId, subjectId).take(100).map(this::seasonView));
    }

    @Override public Mono<MediaEpisodeView> createEpisode(UUID ownerId, UUID subjectId, UUID seasonId, CreateMediaEpisodeRequest request) {
        return ownedSubject(ownerId, subjectId).flatMap(subject -> ownedSeason(ownerId, seasonId).flatMap(season -> {
            if (!season.subjectId().equals(subject.id())) return Mono.error(new ConflictException("Season 不属于该 Media Subject"));
            String locale = request.locale() == null || request.locale().isBlank() ? "en-US" : request.locale();
            return resources.create(ownerId, new CreateResourceRequest(ResourceType.VIDEO, request.title(), locale))
                .flatMap(resource -> episodes.save(new MediaEpisodeEntity(null, ownerId, subject.id(), season.id(), resource.id(),
                    request.episodeNumber(), request.absoluteNumber(), null, Instant.now(), Instant.now(), null)));
        })).map(this::episodeView);
    }

    @Override public Flux<MediaEpisodeView> listEpisodes(UUID ownerId, UUID subjectId, UUID seasonId) {
        return ownedSubject(ownerId, subjectId).flatMapMany(s -> {
            if (seasonId == null) return episodes.findAllByOwnerIdAndSubjectIdOrderByEpisodeNumberAsc(ownerId, subjectId).take(100).map(this::episodeView);
            return ownedSeason(ownerId, seasonId).flatMapMany(season -> {
                if (!season.subjectId().equals(subjectId)) return Flux.error(new ConflictException("Season 不属于该 Media Subject"));
                return episodes.findAllByOwnerIdAndSeasonIdOrderByEpisodeNumberAsc(ownerId, seasonId).take(100).map(this::episodeView);
            });
        });
    }

    @Override
    @Transactional
    public Mono<Void> reorderEpisodes(UUID ownerId, UUID subjectId, UUID seasonId, ReorderMediaEpisodesRequest request) {
        if (request == null || request.episodeIds() == null || request.episodeIds().isEmpty()) {
            return Mono.error(new IllegalArgumentException("剧集顺序不能为空"));
        }
        List<UUID> orderedIds = request.episodeIds();
        if (orderedIds.size() != new HashSet<>(orderedIds).size()) {
            return Mono.error(new IllegalArgumentException("剧集顺序不能包含重复条目"));
        }
        return ownedSubject(ownerId, subjectId).then(ownedSeason(ownerId, seasonId)).flatMapMany(ignored ->
            episodes.findAllByOwnerIdAndSeasonIdOrderByEpisodeNumberAsc(ownerId, seasonId).collectList()
        ).flatMap(existing -> {
            if (existing.size() != orderedIds.size()
                || existing.stream().anyMatch(episode -> !episode.subjectId().equals(subjectId)
                    || !orderedIds.contains(episode.id()))) {
                return Flux.error(new ConflictException("剧集顺序必须包含该 Season 的全部剧集"));
            }
            return episodes.maxEpisodeNumber(ownerId, seasonId).flatMapMany(max -> {
                int offset = Math.addExact(max, existing.size() + 1);
                return episodes.shiftEpisodeNumbers(ownerId, seasonId, offset)
                    .thenMany(Flux.range(0, orderedIds.size())
                        .concatMap(index -> episodes.updateEpisodeNumber(ownerId, orderedIds.get(index), index)))
                    .then();
            });
        }).then();
    }

    private Mono<MediaSubjectEntity> ownedSubject(UUID ownerId, UUID id) { return subjects.findById(id).filter(s -> s.ownerId().equals(ownerId)).switchIfEmpty(Mono.error(new NotFoundException("Media Subject 不存在"))); }
    private Mono<MediaSeasonEntity> ownedSeason(UUID ownerId, UUID id) { return seasons.findById(id).filter(s -> s.ownerId().equals(ownerId)).switchIfEmpty(Mono.error(new NotFoundException("Season 不存在"))); }
    private MediaSubjectView subjectView(MediaSubjectEntity e) { return new MediaSubjectView(e.id(), e.resourceId(), e.kind(), e.createdAt()); }
    private MediaSeasonView seasonView(MediaSeasonEntity e) { return new MediaSeasonView(e.id(), e.subjectId(), e.resourceId(), e.seasonNumber(), e.name()); }
    private MediaEpisodeView episodeView(MediaEpisodeEntity e) { return new MediaEpisodeView(e.id(), e.subjectId(), e.seasonId(), e.resourceId(), e.episodeNumber(), e.absoluteNumber()); }
}
