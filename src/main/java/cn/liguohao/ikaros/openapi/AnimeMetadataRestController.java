package cn.liguohao.ikaros.openapi;

import cn.liguohao.ikaros.common.result.CommonResult;
import cn.liguohao.ikaros.model.dto.AnimeDTO;
import cn.liguohao.ikaros.service.AnimeService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author guohao
 * @date 2022/10/20
 */
@RestController
@RequestMapping("/anime/metadata")
public class AnimeMetadataRestController {
    private final AnimeService animeService;

    public AnimeMetadataRestController(AnimeService animeService) {
        this.animeService = animeService;
    }

    @PutMapping("/network/bgmTvId/{bgmTvId}")
    public CommonResult<AnimeDTO> reqBgmtvBangumiMetadata(@PathVariable Long bgmTvId) {
        return CommonResult.ok(animeService.reqBgmtvBangumiMetadata(bgmTvId));
    }
}
