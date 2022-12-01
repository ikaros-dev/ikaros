package run.ikaros.server.core.repository;


import run.ikaros.server.entity.AnimeEntity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author li-guohao
 */
public interface AnimeRepository extends BaseRepository<AnimeEntity, Long> {
    List<AnimeEntity> findByBgmtvIdAndStatus(Long bgmtvId, Boolean status);

    Page<AnimeEntity> findAll(Specification<AnimeEntity> specification, Pageable pageable);

    List<AnimeEntity> findAll(Specification<AnimeEntity> specification);

    Integer count(Specification<AnimeEntity> specification);

    Optional<AnimeEntity> findByTitleAndStatus(String title, Boolean status);

    Optional<AnimeEntity> findByTitleCnAndStatus(String titleCn, Boolean status);

    List<AnimeEntity> findByStatusAndTitleLike(Boolean status, String title);

    List<AnimeEntity> findByStatusAndTitleCnLike(Boolean status, String titleCn);
}
