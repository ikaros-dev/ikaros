package run.ikaros.server.openapi;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import run.ikaros.server.model.dto.SeasonDTO;
import run.ikaros.server.params.SeasonMatchingEpParams;
import run.ikaros.server.result.CommonResult;
import run.ikaros.server.service.SeasonService;
import run.ikaros.server.utils.AssertUtils;

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

    @PutMapping
    public CommonResult<SeasonEntity> save(@RequestBody SeasonEntity seasonEntity) {
        AssertUtils.notNull(seasonEntity, "season");
        AssertUtils.notNull(seasonEntity.getAnimeId(), "animeId");
        return CommonResult.ok(seasonService.save(seasonEntity));
    }

    @DeleteMapping
    public CommonResult<String> remove(@RequestParam("id") Long seasonId)
        throws RecordNotFoundException {
        seasonService.removeById(seasonId);
        return CommonResult.ok();
    }

    @GetMapping("/types")
    public CommonResult<List<String>> findSeasonTypes() {
        return CommonResult.ok(seasonService.finSeasonTypes());
    }

    @PutMapping("/matching/episodes")
    public CommonResult<SeasonDTO> matchingEpisodeUrlByFileIds(
        @RequestBody SeasonMatchingEpParams seasonMatchingEpParams) {
        return CommonResult.ok(seasonService.matchingEpisodeUrlByFileIds(seasonMatchingEpParams));
    }
}
