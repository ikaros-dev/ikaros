package run.ikaros.server.openapi;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import run.ikaros.server.exceptions.RecordNotFoundException;
import run.ikaros.server.model.dto.AnimeDTO;
import run.ikaros.server.result.CommonResult;
import run.ikaros.server.service.BgmTvService;

/**
 * @author li-guohao
 */
@RestController
@RequestMapping("/network/metadata")
public class NetworkMetadataRestController {

    private final BgmTvService bgmTvService;

    public NetworkMetadataRestController(BgmTvService bgmTvService) {
        this.bgmTvService = bgmTvService;
    }

    @PutMapping("/bgmTv/subject")
    public CommonResult<AnimeDTO> reqBgmtvBangumiMetadata(@RequestParam("id") Long subjectId)
        throws RecordNotFoundException {
        return CommonResult.ok(bgmTvService.reqBgmtvSubject(subjectId));
    }
}
