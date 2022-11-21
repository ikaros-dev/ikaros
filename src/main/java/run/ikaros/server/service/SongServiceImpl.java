package run.ikaros.server.service;

import org.springframework.stereotype.Service;
import run.ikaros.server.core.repository.SongRepository;
import run.ikaros.server.core.service.SongService;
import run.ikaros.server.entity.SongEntity;

@Service
public class SongServiceImpl extends AbstractCrudService<SongEntity, Long> implements SongService {
    private final SongRepository songRepository;

    public SongServiceImpl(SongRepository songRepository) {
        super(songRepository);
        this.songRepository = songRepository;
    }


}
