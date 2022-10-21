package cn.liguohao.ikaros.openapi;

import cn.liguohao.ikaros.common.result.CommonResult;
import cn.liguohao.ikaros.exceptions.RecordNotFoundException;
import cn.liguohao.ikaros.model.dto.AnimeDTO;
import cn.liguohao.ikaros.model.entity.anime.AnimeEntity;
import cn.liguohao.ikaros.model.entity.anime.SeasonEntity;
import cn.liguohao.ikaros.service.AnimeService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @PutMapping("/complex")
    public CommonResult<AnimeDTO> saveAnimeDTO(@RequestBody AnimeDTO animeDTO)
        throws RecordNotFoundException {
        return CommonResult.ok(animeService.saveAnimeDTO(animeDTO));
    }

    @PutMapping
    public CommonResult<AnimeEntity> saveAnimeEntity(@RequestBody AnimeEntity animeEntity)
        throws RecordNotFoundException {
        return CommonResult.ok(animeService.save(animeEntity));
    }

    @PutMapping("/season/animeId/{animeId}")
    public CommonResult<SeasonEntity> saveSeasonEntity(@PathVariable Long animeId,
                                                       @RequestBody SeasonEntity seasonEntity) {
        return CommonResult.ok(animeService.saveSeasonEntity(animeId, seasonEntity));
    }


}
