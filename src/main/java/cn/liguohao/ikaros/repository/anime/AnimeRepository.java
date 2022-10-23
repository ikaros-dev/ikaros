package cn.liguohao.ikaros.repository.anime;


import cn.liguohao.ikaros.model.entity.anime.AnimeEntity;
import cn.liguohao.ikaros.repository.BaseRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * @author li-guohao
 */
public interface AnimeRepository extends BaseRepository<AnimeEntity> {
    List<AnimeEntity> findByBgmtvIdAndStatus(Long bgmtvId, Boolean status);

    Page<AnimeEntity> findAll(Specification<AnimeEntity> specification, Pageable pageable);

    List<AnimeEntity> findAll(Specification<AnimeEntity> specification);

    Integer count(Specification<AnimeEntity> specification);
}
