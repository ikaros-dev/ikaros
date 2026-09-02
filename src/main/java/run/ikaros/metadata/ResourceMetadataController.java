package run.ikaros.metadata;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** 提供 Resource 元数据来源和人工覆盖的接口。 */
@RestController @RequestMapping({"/api/resources/{resourceId}/metadata", "/api/v2/resources/{resourceId}/metadata"})
public class ResourceMetadataController {
    private final ResourceMetadataService service;
    public ResourceMetadataController(ResourceMetadataService service) { this.service=service; }
    /** 人工写入并锁定字段。 */
    @Operation(summary="人工写入元数据",description="人工写入会锁定字段，自动同步不可静默覆盖。")
    @ApiResponses({@ApiResponse(responseCode="200",description="字段已写入")})
    @PutMapping("/{fieldKey}") public Mono<ResourceMetadataView> setManual(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,@PathVariable UUID resourceId,@PathVariable String fieldKey,@Valid @RequestBody MetadataValueRequest request){return service.setManual(ownerId,resourceId,fieldKey,request);}
    /** 应用非人工来源的字段建议。 */
    @Operation(summary="应用自动元数据",description="字段被人工锁定时返回当前值和 applied=false。")
    @ApiResponses({@ApiResponse(responseCode="200",description="自动更新已处理")})
    @PostMapping("/{fieldKey}/automatic") public Mono<ResourceMetadataView> applyAutomatic(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,@PathVariable UUID resourceId,@PathVariable String fieldKey,@Valid @RequestBody AutomaticMetadataRequest request){return service.applyAutomatic(ownerId,resourceId,fieldKey,request);}
    /** 恢复字段的自动管理状态。 */
    @Operation(summary="恢复自动管理",description="解除人工锁定，后续自动来源可更新该字段。")
    @ApiResponses({@ApiResponse(responseCode="200",description="自动管理已恢复"),@ApiResponse(responseCode="404",description="字段不存在",content=@Content)})
    @PostMapping("/{fieldKey}/restore-automatic") public Mono<ResourceMetadataView> restore(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,@PathVariable UUID resourceId,@PathVariable String fieldKey){return service.restoreAutomatic(ownerId,resourceId,fieldKey);}
    /** 查询字段及其来源解释。 */
    @Operation(summary="查看元数据来源",description="返回字段的当前值、来源、来源引用和人工锁定状态。")
    @ApiResponses({@ApiResponse(responseCode="200",description="字段列表查询成功")})
    @GetMapping public Flux<ResourceMetadataView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,@PathVariable UUID resourceId){return service.list(ownerId,resourceId);}
}
