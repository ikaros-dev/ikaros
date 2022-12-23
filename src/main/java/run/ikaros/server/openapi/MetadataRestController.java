package run.ikaros.server.openapi;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import run.ikaros.server.core.service.MetadataService;
import run.ikaros.server.model.response.MetadataSearchResponse;
import run.ikaros.server.result.CommonResult;

import java.util.List;

@Tag(name = "元数据")
@RestController
@RequestMapping("/metadata")
public class MetadataRestController {

    private final MetadataService metadataService;

    public MetadataRestController(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @GetMapping("/search")
    public CommonResult<List<MetadataSearchResponse>> searchAnime(
        @RequestParam("keyword") String keyword) {
        return CommonResult.ok(metadataService.search(keyword));
    }
}
