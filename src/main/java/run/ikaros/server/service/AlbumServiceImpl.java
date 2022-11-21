package run.ikaros.server.service;

import org.springframework.stereotype.Service;
import run.ikaros.server.core.repository.AlbumRepository;
import run.ikaros.server.core.service.AlbumService;
import run.ikaros.server.entity.AlbumEntity;

@Service
public class AlbumServiceImpl
    extends AbstractCrudService<AlbumEntity, Long>
    implements AlbumService {
    private final AlbumRepository albumRepository;

    public AlbumServiceImpl(AlbumRepository albumRepository) {
        super(albumRepository);
        this.albumRepository = albumRepository;
    }


}
