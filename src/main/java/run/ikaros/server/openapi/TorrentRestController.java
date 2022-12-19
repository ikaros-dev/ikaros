package run.ikaros.server.openapi;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import run.ikaros.server.core.service.TorrentService;
import run.ikaros.server.result.CommonResult;
import run.ikaros.server.utils.AssertUtils;

@RestController
@RequestMapping("/torrent")
public class TorrentRestController {

    private final TorrentService torrentService;

    public TorrentRestController(TorrentService torrentService) {
        this.torrentService = torrentService;
    }

    @PutMapping("/add")
    public CommonResult<Boolean> add(@RequestBody String url) {
        AssertUtils.notBlank(url, "url");
        torrentService.create(url);
        return CommonResult.ok(Boolean.TRUE);
    }

}
