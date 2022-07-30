package cn.liguohao.ikaros.openapi;

import cn.liguohao.ikaros.acgmn.episode.SimpleEpisode;
import cn.liguohao.ikaros.service.EpisodeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController("/episode")
public class EpisodeRestController {

    private final EpisodeService episodeService;

    public EpisodeRestController(EpisodeService episodeService) {
        this.episodeService = episodeService;
    }

    @PostMapping
    public void addSimpleEpisode(@RequestBody SimpleEpisode simpleEpisode) {
        episodeService.addSimpleEpisode(simpleEpisode);
    }

    @GetMapping("/{eid}")
    public Object findOneById(Long eid) {
        return episodeService.findByEid(eid).orElseThrow();
    }

    @PutMapping("/datum/{eid}")
    public void uploadDatum(MultipartFile multipartFile,
                            @PathVariable("eid") Long eid)
            throws IOException {
        episodeService.uploadDatum(multipartFile, eid);
    }

}
