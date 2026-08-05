package run.ikaros.server.core.episode;

import static run.ikaros.api.constant.OpenApiConst.ATT_STREAM_ENDPOINT_PREFIX;
import static run.ikaros.api.infra.utils.ReactiveBeanUtils.copyProperties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.binding.MediaPhysicalType;
import run.ikaros.api.core.binding.MediaRole;
import run.ikaros.api.core.binding.MediaTrack;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.core.subject.EpisodeRecord;
import run.ikaros.api.core.subject.EpisodeResource;
import run.ikaros.api.infra.utils.ReflectUtils;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.EpisodeGroup;
import run.ikaros.server.cache.annotation.FluxCacheEvict;
import run.ikaros.server.cache.annotation.FluxCacheable;
import run.ikaros.server.cache.annotation.MonoCacheEvict;
import run.ikaros.server.cache.annotation.MonoCacheable;
import run.ikaros.server.infra.utils.JsonUtils;
import run.ikaros.server.store.entity.EpisodeEntity;
import run.ikaros.server.store.repository.AttachmentReferenceRepository;
import run.ikaros.server.store.repository.AttachmentRepository;
import run.ikaros.server.store.repository.EpisodeRepository;

/** 提供剧集持久化、查询及媒体资源投影能力。 */
@Slf4j
@Service
public class DefaultEpisodeService implements EpisodeService {
    /** 本地扫描状态查询语句。 */
    private static final String LOCAL_SCAN_STATE_QUERY = "select local_scan_state "
        + "from directory_binding_workflow where platform is null "
        + "and local_scan_state is not null and local_scan_state like :attachmentId";
    /** 剧集数据仓储。 */
    private final EpisodeRepository episodeRepository;
    /** 附件引用数据仓储。 */
    private final AttachmentReferenceRepository attachmentReferenceRepository;
    /** 附件数据仓储。 */
    private final AttachmentRepository attachmentRepository;
    /** 应用事件发布器。 */
    private final ApplicationEventPublisher applicationEventPublisher;
    /** 响应式数据库客户端。 */
    private final DatabaseClient databaseClient;
    /** 本地扫描状态 JSON 转换器。 */
    private final ObjectMapper objectMapper = JsonUtils.getObjectMapper();

    /**
     * Construct.
     */
    public DefaultEpisodeService(EpisodeRepository episodeRepository,
                                 AttachmentReferenceRepository attachmentReferenceRepository,
                                 AttachmentRepository attachmentRepository,
                                 ApplicationEventPublisher applicationEventPublisher,
                                 DatabaseClient databaseClient) {
        this.episodeRepository = episodeRepository;
        this.attachmentReferenceRepository = attachmentReferenceRepository;
        this.attachmentRepository = attachmentRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.databaseClient = databaseClient;
    }


    @Override
    @MonoCacheEvict
    public Mono<Episode> save(Episode episode) {
        Assert.notNull(episode, "episode must not be null");
        UUID episodeId = episode.getId();
        if (episodeId != null) {
            return episodeRepository.findById(episodeId)
                .flatMap(entity -> copyProperties(episode, entity))
                .flatMap(episodeRepository::update)
                .flatMap(e -> copyProperties(e, episode));
        } else {
            return copyProperties(episode, new EpisodeEntity())
                .map(e -> {
                    e.setId(UuidV7Utils.generateUuid());
                    return e;
                })
                .flatMap(episodeRepository::insert)
                .flatMap(e -> copyProperties(e, episode));
        }
    }

    @Override
    @MonoCacheable(value = "episode:id:", key = "#episodeId")
    public Mono<Episode> findById(UUID episodeId) {
        Assert.notNull(episodeId, "episode must not null.");
        return episodeRepository.findById(episodeId)
            .flatMap(episodeEntity -> copyProperties(episodeEntity, new Episode()));
    }

    @Override
    @FluxCacheable(value = "episodes:subjectId:", key = "#subjectId")
    public Flux<Episode> findAllBySubjectId(UUID subjectId) {
        Assert.notNull(subjectId, "subjectId must not null.");
        return episodeRepository.findAllBySubjectId(subjectId)
            .flatMap(episodeEntity -> copyProperties(episodeEntity, new Episode()));
    }

    @Override
    @FluxCacheable(value = "episodeRecords:subjectId:", key = "#subjectId")
    public Flux<EpisodeRecord> findRecordsBySubjectId(UUID subjectId) {
        Assert.notNull(subjectId, "subjectId must not null.");
        return findAllBySubjectId(subjectId)
            .flatMap(episode -> findResourcesById(episode.getId())
                .collectList()
                .flatMap(resources -> Mono.just(new EpisodeRecord(episode, resources)))
            );
    }

    @Override
    @MonoCacheable(value = "episode:subjectId_group_sequence_name",
        key = "#subjectId + #group + #sequence + #name")
    public Mono<Episode> findBySubjectIdAndGroupAndSequenceAndName(
        UUID subjectId, EpisodeGroup group, Float sequence, String name) {
        Assert.notNull(subjectId, "subjectId must not null.");
        Assert.notNull(group, "'group' must not be null");
        Assert.isTrue(sequence >= 0, "'sequence' must >= 0.");
        Assert.hasText(name, "'name' must not be empty.");
        return episodeRepository.findBySubjectIdAndGroupAndSequenceAndName(
                subjectId, group, sequence, name)
            .flatMap(episodeEntity -> copyProperties(episodeEntity, new Episode()));
    }

    @Override
    @FluxCacheable(value = "episode:subjectId_group_sequence",
        key = "#subjectId + #group + #sequence")
    public Flux<Episode> findBySubjectIdAndGroupAndSequence(UUID subjectId, EpisodeGroup group,
                                                            Float sequence) {
        Assert.notNull(subjectId, "subjectId must not null.");
        Assert.notNull(group, "'group' must not be null");
        Assert.isTrue(sequence >= 0, "'sequence' must >= 0.");
        return episodeRepository.findBySubjectIdAndGroupAndSequence(
            subjectId, group, sequence
        ).flatMap(episodeEntity -> copyProperties(episodeEntity, new Episode()));
    }

    @Override
    @MonoCacheEvict
    public Mono<Void> deleteById(UUID episodeId) {
        Assert.notNull(episodeId, "episodeId must not null.");
        return episodeRepository.findById(episodeId)
            .flatMap(entity -> episodeRepository.delete(entity)
                .doOnSuccess(v -> {
                    log.debug("Remove exists episode: {}", entity);
                    EpisodeRemoveEvent event = new EpisodeRemoveEvent(this, entity);
                    applicationEventPublisher.publishEvent(event);
                }));
    }

    @Override
    @MonoCacheable(value = "episode:count:subjectId", key = "#subjectId")
    public Mono<Long> countBySubjectId(UUID subjectId) {
        Assert.notNull(subjectId, "subjectId must not null.");
        return episodeRepository.countBySubjectId(subjectId);
    }

    @Override
    @MonoCacheable(value = "episode:countMatching:subjectId", key = "#subjectId")
    public Mono<Long> countMatchingBySubjectId(UUID subjectId) {
        Assert.notNull(subjectId, "'subjectId' must not null.");
        return databaseClient.sql("select count(e.ID) from EPISODE e, ATTACHMENT_REFERENCE ar "
                + "where ar.TYPE = 'EPISODE' and e.ID = ar.REFERENCE_ID "
                + "and e.SUBJECT_ID = :subjectId")
            .bind("subjectId", subjectId)
            .map(row -> row.get(0, Long.class))
            .one();
    }


    @Override
    @FluxCacheable(value = "episode_resources:episodeId", key = "#episodeId")
    public Flux<EpisodeResource> findResourcesById(UUID episodeId) {
        Assert.notNull(episodeId, "'episodeId' must not null.");
        return databaseClient.sql("select att_ref.ATTACHMENT_ID as attachment_id, "
                + "att.PARENT_ID as parent_attachment_id, "
                + "att_ref.REFERENCE_ID as episode_id, "
                + "att.URL as url, "
                + "att.NAME as name "
                + "from ATTACHMENT_REFERENCE att_ref, ATTACHMENT att "
                + "where att_ref.TYPE = 'EPISODE' and att_ref.REFERENCE_ID = :episodeId "
                + "and att_ref.ATTACHMENT_ID = att.ID "
                + "order by att_ref.TYPE, att_ref.ATTACHMENT_ID")
            .bind("episodeId", episodeId)
            .fetch()
            .all()
            .map(row -> ReflectUtils.mapToClass(row, EpisodeResource.class, true))
            .map(this::initializeResourceProjection)
            .concatMap(this::projectLocalMedia)
            .sort(Comparator.comparing(ResourceProjection::sortPath, this::compareNaturalPath))
            .map(ResourceProjection::resource);
    }

    private EpisodeResource initializeResourceProjection(EpisodeResource resource) {
        resource.setTracks(List.of());
        resource.setImageSequence(false);
        return resource;
    }

    private Mono<ResourceProjection> projectLocalMedia(EpisodeResource resource) {
        if (resource.getAttachmentId() == null || resource.getEpisodeId() == null) {
            return Mono.just(defaultProjection(resource));
        }
        return findLocalState(resource)
            .flatMap(state -> applyLocalState(resource, state)
                .map(projected -> new ResourceProjection(projected,
                    resourceSortPath(state, resource))))
            .defaultIfEmpty(defaultProjection(resource))
            .onErrorResume(error -> {
                log.warn("投影本地剧集媒体状态失败: episodeId={}, attachmentId={}",
                    resource.getEpisodeId(), resource.getAttachmentId(), error);
                return Mono.just(defaultProjection(resource));
            });
    }

    private ResourceProjection defaultProjection(EpisodeResource resource) {
        return new ResourceProjection(resource, safeText(resource.getName()));
    }

    private String resourceSortPath(JsonNode state, EpisodeResource resource) {
        JsonNode primaryItem = findPrimaryItem(state, resource);
        String relativePath = textValue(primaryItem, "relative_path");
        return safeText(relativePath == null ? resource.getName() : relativePath);
    }

    private Mono<JsonNode> findLocalState(EpisodeResource resource) {
        return databaseClient.sql(LOCAL_SCAN_STATE_QUERY)
            .bind("attachmentId", '%' + resource.getAttachmentId().toString() + '%')
            .fetch()
            .all()
            .map(row -> row.get("local_scan_state"))
            .filter(String.class::isInstance)
            .cast(String.class)
            .flatMap(json -> Mono.justOrEmpty(parseState(json)))
            .filter(state -> findPrimaryItem(state, resource) != null)
            .next()
            .onErrorResume(error -> {
                log.warn("查询本地剧集媒体状态失败: episodeId={}, attachmentId={}",
                    resource.getEpisodeId(), resource.getAttachmentId(), error);
                return Mono.empty();
            });
    }

    private JsonNode parseState(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            log.warn("忽略无法解析的本地剧集媒体状态", exception);
            return null;
        }
    }

    private Mono<EpisodeResource> applyLocalState(EpisodeResource resource, JsonNode state) {
        JsonNode primaryItem = findPrimaryItem(state, resource);
        if (primaryItem == null) {
            return Mono.just(resource);
        }
        resource.setImageSequence(mediaType(primaryItem) == MediaPhysicalType.IMAGE);
        List<MediaTrack> embeddedTracks = embeddedTracks(primaryItem);
        return externalTracks(state, resource.getAttachmentId())
            .collectList()
            .map(externalTracks -> {
                List<MediaTrack> tracks = new ArrayList<>(embeddedTracks.size()
                    + externalTracks.size());
                tracks.addAll(embeddedTracks);
                tracks.addAll(externalTracks);
                resource.setTracks(List.copyOf(tracks));
                return resource;
            });
    }

    private JsonNode findPrimaryItem(JsonNode state, EpisodeResource resource) {
        JsonNode items = state.path("items");
        if (!items.isArray()) {
            return null;
        }
        for (JsonNode item : items) {
            if (resource.getAttachmentId().equals(uuidValue(item, "attachment_id"))
                && isMappedToEpisode(state, item, resource.getEpisodeId())) {
                return item;
            }
        }
        return null;
    }

    private boolean isMappedToEpisode(JsonNode state, JsonNode item, UUID episodeId) {
        if (episodeId.equals(uuidValue(item, "episode_id"))) {
            return true;
        }
        UUID attachmentId = uuidValue(item, "attachment_id");
        JsonNode mappings = state.path("episode_mappings");
        if (attachmentId == null || !mappings.isArray()) {
            return false;
        }
        for (JsonNode mapping : mappings) {
            if (attachmentId.equals(uuidValue(mapping, "attachment_id"))
                && episodeId.equals(uuidValue(mapping, "episode_id"))) {
                return true;
            }
        }
        return false;
    }

    private List<MediaTrack> embeddedTracks(JsonNode primaryItem) {
        List<MediaTrack> tracks = new ArrayList<>();
        JsonNode trackNodes = primaryItem.path("tracks");
        if (trackNodes.isArray()) {
            for (JsonNode trackNode : trackNodes) {
                try {
                    MediaTrack track = objectMapper.treeToValue(trackNode, MediaTrack.class);
                    track.setAttachmentId(null);
                    track.setUrl(null);
                    track.setPlayable(false);
                    tracks.add(track);
                } catch (Exception exception) {
                    log.warn("忽略无法解析的内嵌媒体轨道", exception);
                }
            }
        }
        String failureReason = textValue(primaryItem, "probe_failure_reason");
        if (tracks.isEmpty() && failureReason != null) {
            tracks.add(MediaTrack.builder().playable(false).failureReason(failureReason).build());
        }
        return tracks;
    }

    private Flux<MediaTrack> externalTracks(JsonNode state, UUID primaryAttachmentId) {
        JsonNode items = state.path("items");
        if (!items.isArray()) {
            return Flux.empty();
        }
        return Flux.fromIterable(items)
            .filter(item -> isPlayableExternalTrack(item, primaryAttachmentId))
            .sort(Comparator.comparing(item -> safeText(textValue(item, "relative_path")),
                this::compareNaturalPath))
            .concatMap(this::toExternalTrack);
    }

    private boolean isPlayableExternalTrack(JsonNode item, UUID primaryAttachmentId) {
        MediaPhysicalType physicalType = mediaType(item);
        return !item.path("missing").asBoolean(false)
            && MediaRole.AUTO_ASSOCIATED.name().equals(textValue(item, "role"))
            && primaryAttachmentId.equals(uuidValue(item, "candidate_primary_attachment_id"))
            && (physicalType == MediaPhysicalType.AUDIO
                || physicalType == MediaPhysicalType.SUBTITLE);
    }

    private Mono<MediaTrack> toExternalTrack(JsonNode item) {
        UUID attachmentId = uuidValue(item, "attachment_id");
        if (attachmentId == null) {
            return Mono.empty();
        }
        return attachmentRepository.findById(attachmentId)
            .map(attachment -> MediaTrack.builder()
                .attachmentId(attachmentId)
                .url(ATT_STREAM_ENDPOINT_PREFIX + '/' + attachmentId)
                .kind(mediaType(item) == MediaPhysicalType.AUDIO ? "audio" : "subtitle")
                .language(metadataText(item, "language"))
                .title(attachment.getName())
                .defaultTrack(false)
                .codec(externalCodec(item, attachment.getName()))
                .playable(true)
                .build())
            .onErrorResume(error -> {
                log.warn("忽略无法读取的外置媒体轨道: attachmentId={}", attachmentId, error);
                return Mono.empty();
            });
    }

    private String externalCodec(JsonNode item, String filename) {
        String extension = metadataText(item, "extension");
        if (extension == null) {
            int extensionStart = filename == null ? -1 : filename.lastIndexOf('.');
            extension = extensionStart < 0 ? null : filename.substring(extensionStart);
        }
        return extension == null ? null : extension.replaceFirst("^\\.", "")
            .toLowerCase(Locale.ROOT);
    }

    private String metadataText(JsonNode item, String field) {
        return textValue(item.path("display_metadata"), field);
    }

    private MediaPhysicalType mediaType(JsonNode item) {
        String value = textValue(item, "physical_type");
        if (value == null) {
            return MediaPhysicalType.UNKNOWN;
        }
        try {
            return MediaPhysicalType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return MediaPhysicalType.UNKNOWN;
        }
    }

    private UUID uuidValue(JsonNode node, String field) {
        String value = textValue(node, field);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String textValue(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        String value = node.get(field).asText();
        return value.isBlank() ? null : value;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private int compareNaturalPath(String left, String right) {
        int leftIndex = 0;
        int rightIndex = 0;
        while (leftIndex < left.length() && rightIndex < right.length()) {
            char leftCharacter = Character.toLowerCase(left.charAt(leftIndex));
            char rightCharacter = Character.toLowerCase(right.charAt(rightIndex));
            if (Character.isDigit(leftCharacter) && Character.isDigit(rightCharacter)) {
                int compared = compareNumericPart(left, right, leftIndex, rightIndex);
                if (compared != 0) {
                    return compared;
                }
                leftIndex = numericEnd(left, leftIndex);
                rightIndex = numericEnd(right, rightIndex);
                continue;
            }
            if (leftCharacter != rightCharacter) {
                return Character.compare(leftCharacter, rightCharacter);
            }
            leftIndex++;
            rightIndex++;
        }
        int result = Integer.compare(left.length(), right.length());
        return result != 0 ? result : left.compareTo(right);
    }

    private int compareNumericPart(String left, String right, int leftStart, int rightStart) {
        int leftEnd = numericEnd(left, leftStart);
        int rightEnd = numericEnd(right, rightStart);
        String leftNumber = stripLeadingZeros(left.substring(leftStart, leftEnd));
        String rightNumber = stripLeadingZeros(right.substring(rightStart, rightEnd));
        int lengthComparison = Integer.compare(leftNumber.length(), rightNumber.length());
        if (lengthComparison != 0) {
            return lengthComparison;
        }
        int valueComparison = leftNumber.compareTo(rightNumber);
        return valueComparison != 0 ? valueComparison : Integer.compare(leftEnd - leftStart,
            rightEnd - rightStart);
    }

    private int numericEnd(String value, int start) {
        int index = start;
        while (index < value.length() && Character.isDigit(value.charAt(index))) {
            index++;
        }
        return index;
    }

    private String stripLeadingZeros(String value) {
        int index = 0;
        while (index < value.length() - 1 && value.charAt(index) == '0') {
            index++;
        }
        return value.substring(index);
    }

    /** 携带本地相对路径排序键的剧集资源投影。 */
    private record ResourceProjection(EpisodeResource resource, String sortPath) {
    }

    @Override
    @FluxCacheEvict
    public Flux<Episode> updateEpisodesWithSubjectId(UUID subjectId, List<Episode> episodes) {
        Assert.notNull(subjectId, "'subjectId' must not null.");
        Assert.notNull(episodes, "'episodes' must not be null.");

        episodes.forEach(episode -> episode.setSubjectId(subjectId));

        // 移除新的列表里不存在的过期剧集
        //        return episodeRepository.findAllBySubjectId(subjectId)
        //            .filter(entity -> {
        //                Optional<Episode> episodeOptional = episodes.stream()
        //                  .filter(episode -> episode.getSubjectId().equals(entity.getSubjectId())
        //                        && episode.getSequence().equals(entity.getSequence())
        //                        && episode.getGroup().equals(entity.getGroup())
        //                    ).findFirst();
        //                return episodeOptional.isEmpty();
        //            })
        //            .flatMap(entity -> episodeRepository.delete(entity)
        //                .doOnSuccess(v -> {
        //                    log.debug("Remove exists episode: {}", entity);
        //                    EpisodeRemoveEvent event = new EpisodeRemoveEvent(this, entity);
        //                    applicationEventPublisher.publishEvent(event);
        //                }))
        //            // 更新或新增剧集
        //            .thenMany(Flux.fromIterable(episodes))
        return Flux.fromIterable(episodes)
            .flatMap(episode -> episodeRepository.findBySubjectIdAndGroupAndSequence(
                    subjectId, episode.getGroup(), episode.getSequence())
                .collectList().filter(entities -> !entities.isEmpty())
                .map(entities -> entities.get(0))
                .flatMap(entity -> copyProperties(episode, entity, "id")
                    .flatMap(episodeRepository::update))
                .switchIfEmpty(copyProperties(episode, new EpisodeEntity())
                    .flatMap(episodeRepository::insert)) // 如果没找到，则新增
            )
            .flatMap(entity -> copyProperties(entity, new Episode()));
    }
}
