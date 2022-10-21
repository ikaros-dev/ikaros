package cn.liguohao.ikaros.repository;


import cn.liguohao.ikaros.model.entity.ResourceEntity;

/**
 * @author li-guohao
 */
public interface ResourceRepository extends BaseRepository<ResourceEntity> {

    boolean existsByTypeIdAndName(Long typeId, Long name);

    ResourceEntity findByTypeIdAndName(Long typeId, Long name);
}
