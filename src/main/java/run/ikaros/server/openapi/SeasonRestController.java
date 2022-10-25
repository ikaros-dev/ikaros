package run.ikaros.server.openapi;

import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import run.ikaros.server.entity.SeasonEntity;
import run.ikaros.server.exceptions.RecordNotFoundException;
import run.ikaros.server.result.CommonResult;
import run.ikaros.server.service.SeasonService;

/**
 * @author li-guohao
 */
@RestController
@RequestMapping("/season")
public class SeasonRestController {

    private final SeasonService seasonService;

    public SeasonRestController(SeasonService seasonService) {
        this.seasonService = seasonService;
    }

    @PutMapping("/animeId/{animeId}")
    public CommonResult<SeasonEntity> saveSeasonEntity(@PathVariable Long animeId,
                                                       @RequestBody SeasonEntity seasonEntity) {
        return CommonResult.ok(seasonService.save(seasonEntity.setAnimeId(animeId)));
    }

    @DeleteMapping
    public CommonResult<String> removeAnimeSeason(@RequestParam("id") Long seasonId)
        throws RecordNotFoundException {
        seasonService.removeById(seasonId);
        return CommonResult.ok();
    }

    @GetMapping("/types")
    public CommonResult<List<String>> findSeasonTypes() {
        return CommonResult.ok(seasonService.finSeasonTypes());
    }
}
