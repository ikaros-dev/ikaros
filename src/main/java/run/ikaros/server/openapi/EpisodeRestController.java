package run.ikaros.server.openapi;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import run.ikaros.server.entity.EpisodeEntity;
import run.ikaros.server.model.dto.EpisodeDTO;
import run.ikaros.server.result.CommonResult;
import run.ikaros.server.service.EpisodeService;

/**
 * @author li-guohao
 */
@RestController
@RequestMapping("/episode")
public class EpisodeRestController {

    private final EpisodeService episodeService;

    public EpisodeRestController(EpisodeService episodeService) {
        this.episodeService = episodeService;
    }

    @PutMapping
    public CommonResult<EpisodeEntity> save(@RequestBody EpisodeEntity entity) {
        return CommonResult.ok(episodeService.save(entity));
    }

    @DeleteMapping
    public CommonResult<EpisodeEntity> remove(@RequestParam String id) {
        return CommonResult.ok(episodeService.removeById(Long.valueOf(id)));
    }
}
