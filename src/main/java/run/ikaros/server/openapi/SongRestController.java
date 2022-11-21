package run.ikaros.server.openapi;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import run.ikaros.server.core.service.SongService;
import run.ikaros.server.model.dto.SongDTO;
import run.ikaros.server.model.request.SaveSongRequest;
import run.ikaros.server.model.request.SearchSongRequest;
import run.ikaros.server.result.CommonResult;
import run.ikaros.server.result.PagingWrap;
import run.ikaros.server.utils.AssertUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/song")
public class SongRestController {
    private final SongService songService;

    public SongRestController(SongService songService) {
        this.songService = songService;
    }

    @GetMapping("/list")
    public CommonResult<PagingWrap<SongDTO>> findSongs(
        @RequestBody(required = false) @Valid SearchSongRequest searchSongRequest) {
        return CommonResult.ok(songService.findSongs(searchSongRequest));
    }

    @PostMapping
    public CommonResult<SongDTO> saveSong(
        @RequestBody @Nonnull @Valid SaveSongRequest saveSongRequest) {
        AssertUtils.notNull(saveSongRequest, "saveSongRequest");
        // todo impl save songs
        return CommonResult.ok();
    }

    @DeleteMapping("/id/{id}")
    public CommonResult<Void> deleteSongById(@PathVariable("id") Long id) {
        AssertUtils.isPositive(id, "id");
        songService.removeById(id);
        return CommonResult.ok();
    }

}
