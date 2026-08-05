package run.ikaros.server.core.binding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.AttachmentReference;
import run.ikaros.api.core.binding.LocalMediaMode;
import run.ikaros.api.core.binding.LocalScanAssignment;
import run.ikaros.api.core.binding.LocalScanConfirmRequest;
import run.ikaros.api.core.binding.LocalScanItem;
import run.ikaros.api.core.binding.LocalScanPreview;
import run.ikaros.api.core.binding.LocalScanPreviewRequest;
import run.ikaros.api.core.binding.MediaRole;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.api.store.enums.SubjectType;
import run.ikaros.api.store.enums.TaskStatus;
import run.ikaros.server.core.attachment.service.AttachmentReferenceService;
import run.ikaros.server.core.episode.EpisodeService;
import run.ikaros.server.core.subject.service.SubjectService;
import run.ikaros.server.core.task.TaskService;
import run.ikaros.server.infra.utils.JsonUtils;
import run.ikaros.server.store.entity.DirectoryBindingWorkflowEntity;
import run.ikaros.server.store.entity.TaskEntity;
import run.ikaros.server.store.repository.DirectoryBindingWorkflowRepository;
import run.ikaros.server.store.repository.TaskRepository;

/** 本地目录绑定确认和幂等重扫的默认实现. */
@Slf4j
@Service
public class DefaultLocalDirectoryBindingService implements LocalDirectoryBindingService {
    /** 本地媒体扫描器. */
    private final LocalMediaScanner localMediaScanner;
    /** 条目服务. */
    private final SubjectService subjectService;
    /** 剧集服务. */
    private final EpisodeService episodeService;
    /** 附件引用服务. */
    private final AttachmentReferenceService attachmentReferenceService;
    /** 任务提交服务. */
    private final TaskService taskService;
    /** 任务持久化仓储. */
    private final TaskRepository taskRepository;
    /** 本地绑定工作流仓储. */
    private final DirectoryBindingWorkflowRepository workflowRepository;
    /** 扫描状态 JSON 编解码器. */
    private final ObjectMapper objectMapper = JsonUtils.getObjectMapper();

    /**
     * 创建本地目录绑定服务.
     *
     * @param localMediaScanner 本地媒体扫描器
     * @param subjectService 条目服务
     * @param episodeService 剧集服务
     * @param attachmentReferenceService 附件引用服务
     * @param taskService 任务提交服务
     * @param taskRepository 任务持久化仓储
     * @param workflowRepository 本地绑定工作流仓储
     * @param objectMapper 扫描状态 JSON 编解码器
     */
    public DefaultLocalDirectoryBindingService(LocalMediaScanner localMediaScanner,
                                               SubjectService subjectService,
                                               EpisodeService episodeService,
                                               AttachmentReferenceService
                                                   attachmentReferenceService,
                                               TaskService taskService,
                                               TaskRepository taskRepository,
                                               DirectoryBindingWorkflowRepository
                                                   workflowRepository) {
        this.localMediaScanner = localMediaScanner;
        this.subjectService = subjectService;
        this.episodeService = episodeService;
        this.attachmentReferenceService = attachmentReferenceService;
        this.taskService = taskService;
        this.taskRepository = taskRepository;
        this.workflowRepository = workflowRepository;
    }

    @Override
    public Mono<LocalScanPreview> preview(LocalScanPreviewRequest request) {
        if (request == null || request.getDirectoryId() == null || request.getMode() == null) {
            return Mono.error(new IllegalArgumentException("目录和扫描模式不能为空"));
        }
        return localMediaScanner.scan(request);
    }

    @Override
    public Mono<DirectoryBindingWorkflowEntity> confirm(LocalScanConfirmRequest request) {
        if (request == null || !request.isSubjectSelectionValid()
            || request.getDirectoryId() == null
            || request.getMode() == null) {
            return Mono.error(new IllegalArgumentException("必须恰好提供一个条目，且目录和扫描模式不能为空"));
        }
        return preview(LocalScanPreviewRequest.builder().directoryId(request.getDirectoryId())
            .mode(request.getMode()).build())
            .flatMap(preview -> requirePrimaryItems(preview)
                .then(validateAssignments(preview, request.getAssignments()))
                .then(selectSubjectId(request))
                .flatMap(subjectId -> loadWorkflow(
                    request.getDirectoryId(), subjectId, request.getMode())
                    .flatMap(holder -> reconcile(holder.workflow(), preview, subjectId,
                        mergeAssignments(parseState(holder.workflow().getLocalScanState()),
                            request.getAssignments()))
                        .flatMap(result -> persistAndSubmit(holder, preview, result)))));
    }

    @Override
    public Mono<DirectoryBindingWorkflowEntity> rescan(UUID directoryId, UUID subjectId,
                                                        LocalMediaMode mode) {
        if (directoryId == null || subjectId == null || mode == null) {
            return Mono.error(new IllegalArgumentException("目录、条目和扫描模式不能为空"));
        }
        return workflowRepository.findLocalWorkflow(directoryId, subjectId, mode.name())
            .switchIfEmpty(Mono.error(new IllegalArgumentException("未找到本地目录绑定工作流")))
            .flatMap(workflow -> preview(LocalScanPreviewRequest.builder().directoryId(directoryId)
                .mode(mode).build()).flatMap(preview -> reconcile(workflow, preview, subjectId,
                    parseState(workflow.getLocalScanState()).manualAssignments())
                .flatMap(result -> persistAndSubmit(
                    new WorkflowHolder(workflow, true), preview, result))));
    }

    private Mono<Void> requirePrimaryItems(LocalScanPreview preview) {
        if (preview == null || preview.getItems() == null || preview.getItems().stream()
            .noneMatch(item -> item.getRole() == MediaRole.PRIMARY)) {
            return Mono.error(new IllegalArgumentException("扫描结果中没有可确认的主资源"));
        }
        return Mono.empty();
    }

    private Mono<Void> validateAssignments(LocalScanPreview preview,
                                           List<LocalScanAssignment> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return Mono.empty();
        }
        Map<UUID, LocalScanItem> items = new LinkedHashMap<>();
        preview.getItems().stream().filter(item -> item.getAttachmentId() != null)
            .forEach(item -> items.put(item.getAttachmentId(), item));
        Set<UUID> assignedAttachmentIds = new HashSet<>();
        for (LocalScanAssignment assignment : assignments) {
            if (assignment == null || assignment.getAttachmentId() == null) {
                return Mono.error(new IllegalArgumentException("人工关联的附件标识不能为空"));
            }
            if (!assignedAttachmentIds.add(assignment.getAttachmentId())) {
                return Mono.error(new IllegalArgumentException("同一附件不能重复指定人工关联"));
            }
            LocalScanItem item = items.get(assignment.getAttachmentId());
            if (item == null || item.getRole() != MediaRole.PENDING_CONFIRMATION) {
                return Mono.error(new IllegalArgumentException("人工关联只能用于待确认扫描项"));
            }
            UUID primaryAttachmentId = assignment.getPrimaryAttachmentId();
            if (primaryAttachmentId != null) {
                LocalScanItem primary = items.get(primaryAttachmentId);
                if (primary == null || primary.getRole() != MediaRole.PRIMARY) {
                    return Mono.error(new IllegalArgumentException("人工关联的主资源不存在"));
                }
            }
        }
        return Mono.empty();
    }

    private Mono<UUID> selectSubjectId(LocalScanConfirmRequest request) {
        if (request.getSubjectId() != null) {
            if (request.getMode() == LocalMediaMode.AUDIO) {
                return subjectService.findById(request.getSubjectId())
                    .filter(subject -> subject.getType() == SubjectType.MUSIC)
                    .map(Subject::getId)
                    .switchIfEmpty(Mono.error(
                        new IllegalArgumentException("音频扫描只能绑定音乐条目")));
            }
            return Mono.just(request.getSubjectId());
        }
        Subject subject = request.getSubject();
        if (request.getMode() == LocalMediaMode.AUDIO && subject.getType() != SubjectType.MUSIC) {
            return Mono.error(new IllegalArgumentException("音频扫描只能创建音乐条目"));
        }
        return subjectService.create(subject).map(Subject::getId)
            .filter(Objects::nonNull)
            .switchIfEmpty(Mono.error(new IllegalStateException("创建本地条目后未返回标识")));
    }

    private Mono<WorkflowHolder> loadWorkflow(UUID directoryId, UUID subjectId,
                                              LocalMediaMode mode) {
        return workflowRepository.findLocalWorkflow(directoryId, subjectId, mode.name())
            .map(workflow -> new WorkflowHolder(workflow, true))
            .switchIfEmpty(Mono.fromSupplier(() -> new WorkflowHolder(
                DirectoryBindingWorkflowEntity.builder()
                    .id(UuidV7Utils.generateUuid())
                    .directoryId(directoryId)
                    .directoryName(directoryId.toString())
                    .subjectId(subjectId)
                    .localMode(mode.name())
                    .status(TaskStatus.CREATE)
                    .createTime(LocalDateTime.now())
                    .build(), false)));
    }

    private Mono<ReconcileResult> reconcile(DirectoryBindingWorkflowEntity workflow,
                                             LocalScanPreview preview, UUID subjectId,
                                             Map<UUID, UUID> manualAssignments) {
        LocalState state = parseState(workflow.getLocalScanState());
        List<LocalScanItem> primaries = preview.getItems().stream()
            .filter(item -> item.getRole() == MediaRole.PRIMARY).toList();
        return Flux.fromIterable(primaries).index()
            .concatMap(indexed -> bindPrimary(indexed.getT2(), subjectId,
                indexed.getT1().floatValue() + 1F, state.episodeMappings()))
            .collectList()
            .map(bindings -> new ReconcileResult(bindings, manualAssignments,
                state.relativePaths(), state.episodeMappings(), state.items()));
    }

    private Mono<PrimaryBinding> bindPrimary(LocalScanItem item, UUID subjectId, Float sequence,
                                             Map<UUID, UUID> episodeMappings) {
        UUID mappedEpisodeId = episodeMappings.get(item.getAttachmentId());
        return attachmentReferenceService.findAllByTypeAndAttachmentId(
                AttachmentReferenceType.EPISODE,
                item.getAttachmentId())
            .collectList()
            .flatMap(references -> {
                if (mappedEpisodeId != null) {
                    boolean conflicts = references.stream().anyMatch(reference ->
                        !mappedEpisodeId.equals(reference.getReferenceId()));
                    if (conflicts) {
                        return Mono.just(PrimaryBinding.pending(
                            item.getAttachmentId(), mappedEpisodeId));
                    }
                    if (!references.isEmpty()) {
                        return Mono.just(PrimaryBinding.mapped(
                            item.getAttachmentId(), mappedEpisodeId));
                    }
                    return saveReference(item.getAttachmentId(), mappedEpisodeId)
                        .thenReturn(PrimaryBinding.mapped(item.getAttachmentId(), mappedEpisodeId));
                }
                if (!references.isEmpty()) {
                    return Mono.just(PrimaryBinding.pending(item.getAttachmentId()));
                }
                Episode episode = Episode.defaultEpisode(subjectId)
                    .setSequence(sequence)
                    .setName(item.getRelativePath())
                    .setNameCn(item.getRelativePath());
                return episodeService.save(episode)
                    .flatMap(saved -> saveReference(item.getAttachmentId(), saved.getId())
                        .thenReturn(PrimaryBinding.mapped(item.getAttachmentId(), saved.getId())));
            });
    }

    private Mono<AttachmentReference> saveReference(UUID attachmentId, UUID episodeId) {
        return attachmentReferenceService.save(AttachmentReference.builder()
            .type(AttachmentReferenceType.EPISODE)
            .attachmentId(attachmentId)
            .referenceId(episodeId)
            .build());
    }

    private Mono<DirectoryBindingWorkflowEntity> persistAndSubmit(WorkflowHolder holder,
                                                                    LocalScanPreview preview,
                                                                    ReconcileResult result) {
        DirectoryBindingWorkflowEntity workflow = holder.workflow();
        workflow.setLocalScanState(writeState(preview, result));
        Mono<DirectoryBindingWorkflowEntity> persist = holder.exists()
            ? workflowRepository.update(workflow) : workflowRepository.insert(workflow);
        return persist.flatMap(this::submitTask);
    }

    private Mono<DirectoryBindingWorkflowEntity> submitTask(
        DirectoryBindingWorkflowEntity workflow) {
        TaskEntity entity = TaskEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .status(TaskStatus.CREATE)
            .createTime(LocalDateTime.now())
            .build();
        workflow.setTaskId(entity.getId()).setStatus(TaskStatus.CREATE).setEndTime(null)
            .setFailMessage(null);
        return workflowRepository.update(workflow)
            .then(taskService.submit(new LocalDirectoryBindingTask(entity, taskRepository, workflow,
                workflowRepository)))
            .thenReturn(workflow);
    }

    private String writeState(LocalScanPreview preview, ReconcileResult result) {
        ObjectNode state = objectMapper.createObjectNode();
        state.put("mode", preview.getMode().name());
        ArrayNode items = objectMapper.valueToTree(preview.getItems());
        Set<UUID> currentAttachmentIds = new HashSet<>();
        for (JsonNode node : items) {
            UUID attachmentId = uuidValue(node, "attachment_id");
            if (attachmentId != null) {
                currentAttachmentIds.add(attachmentId);
            }
        }
        result.previousItems().forEach((attachmentId, previousItem) -> {
            if (!currentAttachmentIds.contains(attachmentId)) {
                ObjectNode retainedItem = previousItem.deepCopy();
                retainedItem.put("missing", true);
                items.add(retainedItem);
            }
        });
        Map<UUID, PrimaryBinding> bindings = new LinkedHashMap<>();
        result.bindings().forEach(binding -> bindings.put(binding.attachmentId(), binding));
        for (JsonNode node : items) {
            if (!(node instanceof ObjectNode item)) {
                continue;
            }
            UUID attachmentId = uuidValue(item, "attachment_id");
            PrimaryBinding binding = bindings.get(attachmentId);
            if (binding != null) {
                if (binding.pending()) {
                    item.put("role", MediaRole.PENDING_CONFIRMATION.name());
                    if (binding.episodeId() == null) {
                        item.remove("episode_id");
                    } else {
                        item.put("episode_id", binding.episodeId().toString());
                    }
                } else {
                    item.put("episode_id", binding.episodeId().toString());
                }
            }
            if (result.manualAssignments().containsKey(attachmentId)) {
                UUID primaryId = result.manualAssignments().get(attachmentId);
                item.put("manual_override", true);
                if (primaryId == null) {
                    item.put("role", MediaRole.UNASSOCIATED.name());
                    item.putNull("candidate_primary_attachment_id");
                } else {
                    item.put("role", MediaRole.AUTO_ASSOCIATED.name());
                    item.put("candidate_primary_attachment_id", primaryId.toString());
                }
            }
        }
        state.set("items", items);
        ArrayNode overrides = state.putArray("manual_overrides");
        result.manualAssignments().forEach((attachmentId, primaryId) -> {
            ObjectNode override = overrides.addObject();
            override.put("attachment_id", attachmentId.toString());
            if (primaryId == null) {
                override.putNull("primary_attachment_id");
            } else {
                override.put("primary_attachment_id", primaryId.toString());
            }
        });
        Map<UUID, UUID> episodeMappings = new LinkedHashMap<>(result.previousMappings());
        result.bindings().stream().filter(binding -> binding.episodeId() != null)
            .forEach(binding -> episodeMappings.put(binding.attachmentId(), binding.episodeId()));
        ArrayNode mappingNodes = state.putArray("episode_mappings");
        episodeMappings.forEach((attachmentId, episodeId) -> {
            ObjectNode mapping = mappingNodes.addObject();
            mapping.put("attachment_id", attachmentId.toString());
            mapping.put("episode_id", episodeId.toString());
        });
        long missing = result.previousPaths().entrySet().stream()
            .filter(entry -> preview.getItems().stream().noneMatch(item ->
                entry.getKey().equals(item.getAttachmentId())
                    && entry.getValue().equals(item.getRelativePath())))
            .count();
        state.put("report", missing == 0 ? "重扫完成，未发现消失或重命名文件"
            : "重扫完成，发现 " + missing + " 个消失或重命名文件，已保留原有关联");
        try {
            return objectMapper.writeValueAsString(state);
        } catch (Exception exception) {
            throw new IllegalStateException("无法保存本地扫描状态", exception);
        }
    }

    private LocalState parseState(String json) {
        if (json == null || json.isBlank()) {
            return LocalState.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            Map<UUID, UUID> mappings = new LinkedHashMap<>();
            Map<UUID, String> paths = new LinkedHashMap<>();
            Map<UUID, ObjectNode> itemSnapshots = new LinkedHashMap<>();
            JsonNode items = root.path("items");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    UUID attachmentId = uuidValue(item, "attachment_id");
                    UUID episodeId = uuidValue(item, "episode_id");
                    if (attachmentId != null && episodeId != null) {
                        mappings.put(attachmentId, episodeId);
                    }
                    if (attachmentId != null && item.hasNonNull("relative_path")) {
                        paths.put(attachmentId, item.get("relative_path").asText());
                    }
                    if (attachmentId != null && item instanceof ObjectNode objectNode) {
                        itemSnapshots.put(attachmentId, objectNode.deepCopy());
                    }
                }
            }
            JsonNode mappingNodes = root.path("episode_mappings");
            if (mappingNodes.isArray()) {
                for (JsonNode mapping : mappingNodes) {
                    UUID attachmentId = uuidValue(mapping, "attachment_id");
                    UUID episodeId = uuidValue(mapping, "episode_id");
                    if (attachmentId != null && episodeId != null) {
                        mappings.put(attachmentId, episodeId);
                    }
                }
            }
            Map<UUID, UUID> assignments = new LinkedHashMap<>();
            JsonNode overrides = root.path("manual_overrides");
            if (overrides.isArray()) {
                for (JsonNode override : overrides) {
                    UUID attachmentId = uuidValue(override, "attachment_id");
                    if (attachmentId != null) {
                        assignments.put(attachmentId, uuidValue(override, "primary_attachment_id"));
                    }
                }
            }
            return new LocalState(mappings, assignments, paths, itemSnapshots);
        } catch (Exception exception) {
            log.warn("无法解析本地目录扫描状态，将按待确认处理");
            return LocalState.empty();
        }
    }

    private Map<UUID, UUID> mergeAssignments(LocalState state,
                                              List<LocalScanAssignment> assignments) {
        Map<UUID, UUID> result = new LinkedHashMap<>(state.manualAssignments());
        if (assignments != null) {
            assignments.stream().filter(Objects::nonNull)
                .filter(assignment -> assignment.getAttachmentId() != null)
                .forEach(assignment -> result.put(assignment.getAttachmentId(),
                    assignment.getPrimaryAttachmentId()));
        }
        return result;
    }

    private UUID uuidValue(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        try {
            return UUID.fromString(node.get(field).asText());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private record WorkflowHolder(DirectoryBindingWorkflowEntity workflow, boolean exists) {
    }

    private record PrimaryBinding(UUID attachmentId, UUID episodeId, boolean pending) {
        private static PrimaryBinding mapped(UUID attachmentId, UUID episodeId) {
            return new PrimaryBinding(attachmentId, episodeId, false);
        }

        private static PrimaryBinding pending(UUID attachmentId) {
            return new PrimaryBinding(attachmentId, null, true);
        }

        private static PrimaryBinding pending(UUID attachmentId, UUID episodeId) {
            return new PrimaryBinding(attachmentId, episodeId, true);
        }
    }

    private record ReconcileResult(List<PrimaryBinding> bindings,
                                   Map<UUID, UUID> manualAssignments,
                                   Map<UUID, String> previousPaths,
                                   Map<UUID, UUID> previousMappings,
                                   Map<UUID, ObjectNode> previousItems) {
    }

    private record LocalState(Map<UUID, UUID> episodeMappings,
                              Map<UUID, UUID> manualAssignments,
                              Map<UUID, String> relativePaths,
                              Map<UUID, ObjectNode> items) {
        private static LocalState empty() {
            return new LocalState(Map.of(), Map.of(), Map.of(), Map.of());
        }
    }
}
