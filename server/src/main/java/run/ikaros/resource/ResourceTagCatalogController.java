package run.ikaros.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import run.ikaros.common.PageResponse;

/** 当前用户 Resource 标签目录。 */
@Validated
@RestController
@RequestMapping({"/api/tags"})
public class ResourceTagCatalogController {
    private final ResourceTagService service;

    public ResourceTagCatalogController(ResourceTagService service) {
        this.service = service;
    }

    @Operation(summary = "查询标签目录", description = "按名称去重并分页返回当前用户使用过的 Resource 标签。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "标签目录查询成功"))
    @GetMapping
    public Mono<PageResponse<ResourceTagView>> list(
        @RequestHeader("X-Ikaros-Actor-Id") UUID ownerId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) int size
    ) {
        return service.listCatalog(ownerId, page, size);
    }
}
