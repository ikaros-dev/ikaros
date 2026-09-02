package run.ikaros.storage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Attachment 身份级读取接口。 */
@RestController
@RequestMapping({"/api/attachments", "/api/v2/attachments"})
public class AttachmentController {
    private final StorageService storageService;

    public AttachmentController(StorageService storageService) {
        this.storageService = storageService;
    }

    @Operation(summary = "查询附件元数据", description = "按 Attachment 身份读取元数据，并校验所属 Resource 的访问权。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "附件查询成功"),
        @ApiResponse(responseCode = "404", description = "附件不存在或无权访问")
    })
    @GetMapping("/{attachmentId}")
    public Mono<AttachmentView> get(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
                                    @PathVariable UUID attachmentId) {
        return storageService.get(actorId, attachmentId);
    }
}
