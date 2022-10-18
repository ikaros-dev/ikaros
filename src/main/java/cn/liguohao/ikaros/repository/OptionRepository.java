package cn.liguohao.ikaros.repository;


import cn.liguohao.ikaros.model.entity.OptionEntity;
import java.util.List;

/**
 * @author li-guohao
 */
public interface OptionRepository extends BaseRepository<OptionEntity> {

    boolean existsByKeyAndStatus(String key, Boolean status);

    OptionEntity findByKeyAndStatus(String key, Boolean status);

    List<OptionEntity> findByCategoryAndStatus(String category, Boolean status);

}
