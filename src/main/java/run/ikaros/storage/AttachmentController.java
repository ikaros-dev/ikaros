package run.ikaros.storage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Attachment 身份级读取接口。 */
@RestController
@RequestMapping({"/api/attachments", "/api/v2/attachments"})
public class AttachmentController {
    private final StorageService storageService;
    private final DeliveryGrantService deliveryGrantService;

    public AttachmentController(StorageService storageService, DeliveryGrantService deliveryGrantService) {
        this.storageService = storageService;
        this.deliveryGrantService = deliveryGrantService;
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

    @Operation(summary = "读取附件内容", description = "通过 Storage Provider 读取附件内容，支持单一 bytes Range。")
    @GetMapping("/{attachmentId}/content")
    public Mono<ResponseEntity<Flux<DataBuffer>>> content(
        @RequestHeader(value = "X-Ikaros-Actor-Id", required = false) UUID actorId,
        @PathVariable UUID attachmentId,
        @RequestHeader(value = "Range", required = false) String range,
        @RequestHeader(value = "X-Ikaros-Delivery-Grant", required = false) String deliveryGrant,
        @RequestParam(value = "delivery_grant", required = false) String deliveryGrantQuery
    ) {
        String effectiveGrant = deliveryGrant == null || deliveryGrant.isBlank() ? deliveryGrantQuery : deliveryGrant;
        Mono<UUID> authorizedActor = effectiveGrant == null || effectiveGrant.isBlank()
            ? (actorId == null ? Mono.error(new run.ikaros.common.NotFoundException("需要 Actor 或 Delivery Grant")) : Mono.just(actorId))
            : deliveryGrantService.authorize(actorId, attachmentId, effectiveGrant, range);
        return authorizedActor.flatMap(a -> storageService.readContent(a, attachmentId, range)).map(content -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(content.mediaType()));
            headers.setContentLength(content.length());
            if (content.partial()) {
                headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + content.start() + "-" + content.end()
                    + "/" + content.totalLength());
            }
            return ResponseEntity.status(content.partial() ? 206 : 200).headers(headers).body(content.body());
        });
    }
}
