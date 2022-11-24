package run.ikaros.server.openapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import run.ikaros.server.core.tripartite.bgmtv.service.BgmTvService;
import run.ikaros.server.exceptions.QbittorrentRequestException;
import run.ikaros.server.result.CommonResult;
import run.ikaros.server.tripartite.bgmtv.model.BgmTvUserInfo;
import run.ikaros.server.tripartite.qbittorrent.QbittorrentClient;

@RestController
@RequestMapping("/tripartite")
public class TripartiteRestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(TripartiteRestController.class);

    private final QbittorrentClient qbittorrentClient;
    private final BgmTvService bgmTvService;

    public TripartiteRestController(QbittorrentClient qbittorrentClient, BgmTvService bgmTvService) {
        this.qbittorrentClient = qbittorrentClient;
        this.bgmTvService = bgmTvService;
    }

    @GetMapping("/qbittorrent/connect/test")
    public CommonResult<Boolean> testQbittorrentConnect() {
        boolean connectSuccess = false;
        try {
            qbittorrentClient.getApiVersion();
            connectSuccess = true;
        } catch (QbittorrentRequestException qbittorrentRequestException) {
            LOGGER.warn("connect qbittorrent fail", qbittorrentRequestException);
        }
        return CommonResult.ok(connectSuccess);
    }

    @GetMapping("/bgmtv/token/user/me")
    public CommonResult<BgmTvUserInfo> bgmTvTokenUserMe() {
        return CommonResult.ok(bgmTvService.getMe());
    }

}
