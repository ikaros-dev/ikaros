package run.ikaros.server.openapi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import run.ikaros.server.core.service.AnimeService;
import run.ikaros.server.entity.AnimeEntity;
import run.ikaros.server.exceptions.RecordNotFoundException;
import run.ikaros.server.model.dto.AnimeDTO;
import run.ikaros.server.params.SearchAnimeDTOSParams;
import run.ikaros.server.result.CommonResult;
import run.ikaros.server.result.PagingWrap;

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


}
