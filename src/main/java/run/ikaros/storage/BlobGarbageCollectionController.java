package run.ikaros.storage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 提供 Blob 物理清理前的候选扫描与人工决策审计接口。
 */
@RestController
@RequestMapping("/api/storage/gc")
public class BlobGarbageCollectionController {
    private final StorageService storageService;

    /**
     * 创建 Blob GC 控制器。
     *
     * @param storageService 存储业务服务
     */
    public BlobGarbageCollectionController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * 扫描没有有效 Attachment 引用且已经超过最小保留期的 Blob。
     *
     * @param actorId 当前认证用户标识
     * @param limit 最多返回的候选数量
     * @param minimumAgeSeconds Blob 至少需要保留的秒数
     * @return Blob GC 候选摘要
     */
    @Operation(summary = "扫描 Blob 清理候选", description = "候选必须没有有效 Attachment 引用，且已超过指定最小保留期。"
        + "原始附件和派生附件均通过有效 Attachment 引用保护；本接口只扫描，不执行物理删除。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "候选扫描成功"),
        @ApiResponse(responseCode = "400", description = "扫描参数不合法", content = @Content)
    })
    @GetMapping("/candidates")
    public Mono<List<BlobGcCandidateView>> findCandidates(
        @Parameter(description = "当前认证用户 UUID，由认证层注入", required = true, in = ParameterIn.HEADER)
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @RequestParam(defaultValue = "100") int limit,
        @RequestParam(defaultValue = "86400") long minimumAgeSeconds
    ) {
        return storageService.findGarbageCollectionCandidates(limit, Duration.ofSeconds(minimumAgeSeconds));
    }

    /**
     * 记录 Blob GC 的人工批准或拒绝结果。
     *
     * @param actorId 当前认证用户标识
     * @param blobId Blob 标识
     * @param request GC 决策
     * @return 空响应
     */
    @Operation(summary = "记录 Blob 清理决策", description = "将 Blob GC 的人工批准或拒绝写入审计日志。"
        + "本接口不直接删除物理对象，物理清理由后续受控任务执行。")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "决策记录成功"),
        @ApiResponse(responseCode = "404", description = "Blob 不存在", content = @Content)
    })
    @PostMapping("/{blobId}/decision")
    public Mono<ResponseEntity<Void>> recordDecision(
        @Parameter(description = "当前认证用户 UUID，由认证层注入", required = true, in = ParameterIn.HEADER)
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID blobId,
        @Valid @RequestBody GarbageCollectionDecisionRequest request
    ) {
        return storageService.recordGarbageCollectionDecision(actorId, blobId, request.approved())
            .thenReturn(ResponseEntity.noContent().build());
    }
}
