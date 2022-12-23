package run.ikaros.server.openapi;

import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import run.ikaros.server.tripartite.mikan.model.MikanRssItem;
import run.ikaros.server.utils.JsonUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author guohao
 * @date 2022/10/20
 */
@RestController
@RequestMapping("/anime")
@Tag(name = "动漫")
public class AnimeRestController {

    private final AnimeService animeService;

    public AnimeRestController(AnimeService animeService) {
        this.animeService = animeService;
    }

    @Operation(summary = "保存动漫")
    @PutMapping("/dto")
    public CommonResult<AnimeDTO> saveAnimeDTO(@RequestBody AnimeDTO animeDTO)
        throws RecordNotFoundException {
        return CommonResult.ok(animeService.saveAnimeDTO(animeDTO));
    }

    @Operation(summary = "根据ID查询动漫")
    @GetMapping("/dto/id/{animeId}")
    public CommonResult<AnimeDTO> findAnimeDTO(@PathVariable Long animeId)
        throws RecordNotFoundException {
        return CommonResult.ok(animeService.findAnimeDTOById(animeId));
    }

    @Operation(summary = "添加查询动漫")
    @GetMapping("/dtos")
    public CommonResult<PagingWrap<AnimeDTO>> findAnimeDTOS(
        SearchAnimeDTOSParams searchAnimeDTOSParams) {
        return CommonResult.ok(animeService.findAnimeDTOS(searchAnimeDTOSParams));
    }

    @Operation(summary = "只保存动漫实体")
    @PutMapping
    public CommonResult<AnimeEntity> saveAnimeEntity(@RequestBody AnimeEntity animeEntity) {
        return CommonResult.ok(animeService.save(animeEntity));
    }


    @Operation(summary = "根据ID删除动漫实体")
    @DeleteMapping("/id/{id}")
    public CommonResult<AnimeEntity> deleteAnimeById(@PathVariable("id") Long animeId) {
        return CommonResult.ok(animeService.deleteByIdLogically(animeId));
    }


    @Operation(summary = "根据ID批量删除", description = "需要Base64格式编码的ID数组")
    @DeleteMapping("/ids/{ids}")
    public CommonResult<Boolean> deleteWithBatchByIds(
        @PathVariable("ids") String animeIdsJsonBase64) {
        byte[] bytes = Base64.getDecoder().decode(animeIdsJsonBase64);
        String animeIdsJson = new String(bytes, StandardCharsets.UTF_8);
        Long[] ids = JsonUtils.json2ObjArr(animeIdsJson, new TypeReference<>() {
        });
        if (ids == null) {
            throw new IllegalArgumentException("anime id array is null");
        }
        animeService.deleteWithBatchLogicallyByIds(ids);
        return CommonResult.ok(Boolean.TRUE);
    }


}
