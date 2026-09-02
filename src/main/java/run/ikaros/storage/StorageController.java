package run.ikaros.storage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 提供 Resource Attachment 与 Blob 存储边界的 HTTP 接口。
 */
@RestController
@RequestMapping("/api/resources/{resourceId}/attachments")
public class StorageController {
    private final StorageService storageService;

    /**
     * 创建存储控制器。
     *
     * @param storageService 存储业务服务
     */
    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * 登记一个已进入持久化存储的 Attachment。
     *
     * @param actorId 当前登录用户标识
     * @param resourceId Resource 标识
     * @param request Blob 摘要、附件信息与持久化位置
     * @return 已创建 Attachment 视图
     */
    @Operation(summary = "登记资源附件", description = "根据 SHA-256 复用或创建 Blob，再创建 Attachment 与 Blob Placement。"
        + "接口不接收二进制字节；实际上传由 Storage Provider 能力负责。")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "附件登记成功"),
        @ApiResponse(responseCode = "400", description = "摘要、大小或存储位置参数不合法", content = @Content),
        @ApiResponse(responseCode = "404", description = "资源不存在或无权访问", content = @Content),
        @ApiResponse(responseCode = "409", description = "内容摘要或物理对象键与既有数据冲突", content = @Content)
    })
    @PostMapping
    public Mono<ResponseEntity<AttachmentView>> attach(
        @Parameter(description = "当前认证用户 UUID，由认证层注入", required = true, in = ParameterIn.HEADER)
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID resourceId,
        @Valid @RequestBody AttachBlobRequest request
    ) {
        return storageService.attach(actorId, resourceId, request)
            .map(attachment -> ResponseEntity.created(
                URI.create("/api/resources/" + resourceId + "/attachments/" + attachment.id())
            ).body(attachment));
    }

    /**
     * 登记一个来源明确的派生 Attachment。
     *
     * @param actorId 当前登录用户标识
     * @param resourceId Resource 标识
     * @param request 来源附件与派生内容
     * @return 新建派生 Attachment 视图
     */
    @Operation(summary = "登记派生附件", description = "创建 DERIVED Attachment，并记录其来源 Attachment。"
        + "清理可重建派生内容时不会误删原始附件。")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "派生附件登记成功"),
        @ApiResponse(responseCode = "404", description = "资源或来源附件不存在", content = @Content),
        @ApiResponse(responseCode = "409", description = "派生附件关系冲突", content = @Content)
    })
    @PostMapping("/derived")
    public Mono<ResponseEntity<AttachmentView>> attachDerived(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID resourceId,
        @Valid @RequestBody CreateDerivedAttachmentRequest request
    ) {
        return storageService.attachDerived(actorId, resourceId, request)
            .map(attachment -> ResponseEntity.created(URI.create("/api/resources/" + resourceId + "/attachments/"
                + attachment.id())).body(attachment));
    }

    /**
     * 获取资源附件及其可理解的可用状态与存储位置。
     *
     * @param actorId 当前登录用户标识
     * @param resourceId Resource 标识
     * @return Attachment 列表
     */
    @Operation(summary = "查询资源附件", description = "返回 Resource 的 Attachment、内容摘要和 Placement。"
        + "Hot、Warm、Cold、Archive 是持久化层级，不能与客户端缓存或用户下载混为一谈。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "附件列表查询成功"),
        @ApiResponse(responseCode = "404", description = "资源不存在或无权访问", content = @Content)
    })
    @GetMapping
    public Mono<List<AttachmentView>> list(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @PathVariable UUID resourceId
    ) {
        return storageService.list(actorId, resourceId);
    }
}
