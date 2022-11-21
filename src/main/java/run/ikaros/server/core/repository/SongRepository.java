package run.ikaros.server.core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import run.ikaros.server.entity.SeasonEntity;
import run.ikaros.server.entity.SongEntity;

public interface SongRepository extends BaseRepository<SongEntity, Long> {

    Page<SongEntity> findAll(Specification<SongEntity> specification, Pageable pageable);
}
