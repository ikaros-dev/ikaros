package run.ikaros.server.repository;


import run.ikaros.server.entity.OptionEntity;
import java.util.List;

/**
 * @author li-guohao
 */
public interface OptionRepository extends BaseRepository<OptionEntity, Long> {

    boolean existsByKeyAndStatus(String key, Boolean status);

    OptionEntity findByKeyAndStatus(String key, Boolean status);

    List<OptionEntity> findByCategoryAndStatus(String category, Boolean status);

    OptionEntity findByCategoryAndKeyAndStatus(String category, String key, Boolean status);

}
