package run.ikaros.server.openapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import run.ikaros.server.core.service.AnimeService;
import run.ikaros.server.core.tripartite.bgmtv.service.BgmTvService;
import run.ikaros.server.entity.AnimeEntity;
import run.ikaros.server.exceptions.QbittorrentRequestException;
import run.ikaros.server.exceptions.RecordNotFoundException;
import run.ikaros.server.result.CommonResult;
import run.ikaros.server.tripartite.bgmtv.model.BgmTvUserInfo;
import run.ikaros.server.tripartite.dmhy.DmhyClient;
import run.ikaros.server.tripartite.dmhy.enums.DmhyCategory;
import run.ikaros.server.tripartite.dmhy.model.DmhyRssItem;
import run.ikaros.server.tripartite.qbittorrent.QbittorrentClient;
import run.ikaros.server.utils.AssertUtils;

import java.util.List;

@RestController
@RequestMapping("/tripartite")
public class TripartiteRestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(TripartiteRestController.class);

    private final QbittorrentClient qbittorrentClient;
    private final BgmTvService bgmTvService;
    private final AnimeService animeService;
    private final DmhyClient dmhyClient;

    public TripartiteRestController(QbittorrentClient qbittorrentClient,
                                    BgmTvService bgmTvService, AnimeService animeService,
                                    DmhyClient dmhyClient) {
        this.qbittorrentClient = qbittorrentClient;
        this.bgmTvService = bgmTvService;
        this.animeService = animeService;
        this.dmhyClient = dmhyClient;
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

    @GetMapping("/dmhy/rss/items/anime/{id}")
    public CommonResult<List<DmhyRssItem>> findDmhyRssItems(
        @PathVariable("id") Long animeId,
        @RequestParam(required = false, name = "seq") Long seq) {
        AssertUtils.notNull(animeId, "anime id");
        AnimeEntity animeEntity = animeService.getById(animeId);
        if (null == animeEntity || !animeEntity.getStatus()) {
            throw new RecordNotFoundException("not found for animeId=" + animeId);
        }
        if (seq != null && seq <= 0) {
            throw new IllegalArgumentException("seq must > 0");
        }
        String titleCn = animeEntity.getTitleCn();
        String keywords =
            titleCn + (null == seq ? "" : " " + ((seq > 0 && seq < 10) ? "0" + seq : seq));
        return CommonResult.ok(dmhyClient.findRssItems(keywords, DmhyCategory.ANIME));
    }
}
