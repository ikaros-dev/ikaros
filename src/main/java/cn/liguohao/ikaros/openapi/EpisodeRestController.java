package cn.liguohao.ikaros.openapi;

import cn.liguohao.ikaros.acgmn.episode.SimpleEpisode;
import cn.liguohao.ikaros.exceptions.IkarosRuntimeException;
import cn.liguohao.ikaros.exceptions.NotFoundRecordException;
import cn.liguohao.ikaros.service.EpisodeService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.function.Supplier;

@RestController
@RequestMapping("/episode")
public class EpisodeRestController {

    private final EpisodeService episodeService;

    public EpisodeRestController(EpisodeService episodeService) {
        this.episodeService = episodeService;
    }

    @PostMapping
    public void addSimpleEpisode(@RequestBody SimpleEpisode simpleEpisode) {
        episodeService.addSimpleEpisode(simpleEpisode);
    }

    /**
     * 根据剧集ID查询剧集信息
     *
     * @param eid 剧集ID
     * @return 剧集信息
     * @throws NotFoundRecordException 找不到对应剧集ID的剧集信息
     */
    @GetMapping("/{eid}")
    public Object findOneById(@PathVariable Long eid) {
        return episodeService.findByEid(eid)
                .orElseThrow(((Supplier<IkarosRuntimeException>) () ->
                        new NotFoundRecordException("episode id is " + eid)));
    }

    @PutMapping("/datum/{eid}")
    public void uploadDatum(@RequestParam("file") MultipartFile multipartFile,
                            @PathVariable("eid") Long eid)
            throws IOException {
        episodeService.uploadDatum(multipartFile, eid);
    }

    @DeleteMapping("/{eid}")
    public void deleteOneById(@PathVariable Long eid) {
        episodeService.delete(eid);
    }

}
