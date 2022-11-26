package run.ikaros.server.service;

import org.springframework.stereotype.Service;
import run.ikaros.server.core.service.AnimeService;
import run.ikaros.server.core.service.MediaService;
import run.ikaros.server.entity.AnimeEntity;

import java.util.List;

@Service
public class MediaServiceImpl implements MediaService {
    private final AnimeService animeService;

    public MediaServiceImpl(AnimeService animeService) {
        this.animeService = animeService;
    }

    @Override
    public void generateMediaDir() {
        List<AnimeEntity> animeEntityList = animeService.listAll();
        for (AnimeEntity animeEntity : animeEntityList) {


        }
    }
}
