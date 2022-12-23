package run.ikaros.server.openapi;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import run.ikaros.server.entity.EpisodeEntity;
import run.ikaros.server.result.CommonResult;
import run.ikaros.server.core.service.EpisodeService;
import run.ikaros.server.utils.AssertUtils;

/**
 * @author li-guohao
 */
@Tag(name = "剧集")
@RestController
@RequestMapping("/episode")
public class EpisodeRestController {

    private final EpisodeService episodeService;

    public EpisodeRestController(EpisodeService episodeService) {
        this.episodeService = episodeService;
    }

    @PutMapping
    public CommonResult<EpisodeEntity> save(@RequestBody EpisodeEntity entity) {
        AssertUtils.notNull(entity, "episode");
        AssertUtils.notNull(entity.getSeasonId(), "seasonId");
        return CommonResult.ok(episodeService.save(entity));
    }

    @DeleteMapping
    public CommonResult<EpisodeEntity> remove(@RequestParam String id) {
        AssertUtils.notNull(id, "id");
        return CommonResult.ok(episodeService.removeById(Long.valueOf(id)));
    }
}
