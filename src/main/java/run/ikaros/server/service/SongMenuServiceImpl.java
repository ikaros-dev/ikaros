package run.ikaros.server.service;

import org.springframework.stereotype.Service;
import run.ikaros.server.core.repository.SongMenuRepository;
import run.ikaros.server.core.repository.SongRepository;
import run.ikaros.server.core.service.SongMenuService;
import run.ikaros.server.core.service.SongService;
import run.ikaros.server.entity.SongEntity;
import run.ikaros.server.entity.SongMenuEntity;

@Service
public class SongMenuServiceImpl
    extends AbstractCrudService<SongMenuEntity, Long>
    implements SongMenuService {
    private final SongMenuRepository songMenuRepository;

    public SongMenuServiceImpl(SongMenuRepository songMenuRepository) {
        super(songMenuRepository);
        this.songMenuRepository = songMenuRepository;
    }


}
