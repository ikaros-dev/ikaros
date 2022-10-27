package run.ikaros.server.repository;


import run.ikaros.server.entity.OptionEntity;
import java.util.List;
import run.ikaros.server.enums.OptionCategory;

/**
 * @author li-guohao
 */
public interface OptionRepository extends BaseRepository<OptionEntity, Long> {

    boolean existsByKeyAndStatus(String key, Boolean status);

    OptionEntity findByKeyAndStatus(String key, Boolean status);

    List<OptionEntity> findByCategoryAndStatus(OptionCategory category, Boolean status);

    OptionEntity findByCategoryAndKeyAndStatus(OptionCategory category, String key, Boolean status);

}
