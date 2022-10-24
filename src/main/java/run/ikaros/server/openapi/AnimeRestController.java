package run.ikaros.server.openapi;

import run.ikaros.server.result.CommonResult;
import run.ikaros.server.result.PagingWrap;
import run.ikaros.server.exceptions.RecordNotFoundException;
import run.ikaros.server.model.dto.AnimeDTO;
import run.ikaros.server.entity.anime.AnimeEntity;
import run.ikaros.server.entity.anime.EpisodeEntity;
import run.ikaros.server.entity.anime.SeasonEntity;
import run.ikaros.server.params.SearchAnimeDTOSParams;
import run.ikaros.server.service.AnimeService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author guohao
 * @date 2022/10/20
 */
@RestController
@RequestMapping("/anime")
public class AnimeRestController {

    private final AnimeService animeService;

    public AnimeRestController(AnimeService animeService) {
        this.animeService = animeService;
    }

    @PutMapping("/metadata/network/bgmTvId/{bgmTvId}")
    public CommonResult<AnimeDTO> reqBgmtvBangumiMetadata(@PathVariable Long bgmTvId)
        throws RecordNotFoundException {
        return CommonResult.ok(animeService.reqBgmtvBangumiMetadata(bgmTvId));
    }

    @PutMapping("/dto")
    public CommonResult<AnimeDTO> saveAnimeDTO(@RequestBody AnimeDTO animeDTO)
        throws RecordNotFoundException {
        return CommonResult.ok(animeService.saveAnimeDTO(animeDTO));
    }

    @GetMapping("/dto/id/{animeId}")
    public CommonResult<AnimeDTO> findAnimeDTO(@PathVariable Long animeId)
        throws RecordNotFoundException {
        return CommonResult.ok(animeService.findAnimeDTOById(animeId));
    }

    @GetMapping("/dtos")
    public CommonResult<PagingWrap<AnimeDTO>> findAnimeDTOS(
        SearchAnimeDTOSParams searchAnimeDTOSParams) {
        return CommonResult.ok(animeService.findAnimeDTOS(searchAnimeDTOSParams));
    }

    @PutMapping
    public CommonResult<AnimeEntity> saveAnimeEntity(@RequestBody AnimeEntity animeEntity) {
        return CommonResult.ok(animeService.save(animeEntity));
    }

    @PutMapping("/season/animeId/{animeId}")
    public CommonResult<SeasonEntity> saveSeasonEntity(@PathVariable Long animeId,
                                                       @RequestBody SeasonEntity seasonEntity) {
        return CommonResult.ok(animeService.saveSeasonEntity(animeId, seasonEntity));
    }

    @DeleteMapping("/season/animeId/{animeId}")
    public CommonResult<String> removeAnimeSeason(@PathVariable Long animeId,
                                                  @RequestParam Long seasonId)
        throws RecordNotFoundException {
        animeService.removeAnimeSeason(animeId, seasonId);
        return CommonResult.ok();
    }

    @GetMapping("/season/types")
    public CommonResult<List<String>> findSeasonTypes() {
        return CommonResult.ok(animeService.finSeasonTypes());
    }

    @PutMapping("/episode/seasonId/{seasonId}")
    public CommonResult<EpisodeEntity> saveEpisodeEntity(@PathVariable Long seasonId,
                                                         @RequestBody EpisodeEntity episodeEntity) {
        return CommonResult.ok(animeService.saveEpisodeEntity(seasonId, episodeEntity));
    }

    @DeleteMapping("/season/episode/seasonId/{seasonId}")
    public CommonResult<String> removeSeasonEpisode(@PathVariable Long seasonId,
                                                    @RequestParam Long episodeId) {
        animeService.removeSeasonEpisode(seasonId, episodeId);
        return CommonResult.ok();
    }
}
