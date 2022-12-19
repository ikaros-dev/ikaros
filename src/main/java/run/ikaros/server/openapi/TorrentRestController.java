package run.ikaros.server.openapi;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import run.ikaros.server.core.service.TorrentService;
import run.ikaros.server.enums.TorrentType;
import run.ikaros.server.model.request.TorrentAddRequest;
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
    public CommonResult<Boolean> add(@RequestBody TorrentAddRequest torrent) {
        AssertUtils.notNull(torrent, "torrent");

        String torrentTypeStr = torrent.getType();
        AssertUtils.notBlank(torrentTypeStr, "torrentTypeStr");
        TorrentType type = TorrentType.valueOf(torrentTypeStr.toUpperCase());

        return CommonResult.ok(torrentService.create(type, torrent.getContent()));
    }

}
