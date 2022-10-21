package cn.liguohao.ikaros.repository;


import cn.liguohao.ikaros.model.entity.ResourceTypeEntity;

/**
 * @author li-guohao
 */
public interface ResourceTypeRepository extends BaseRepository<ResourceTypeEntity> {
    boolean existsByName(String name);

    ResourceTypeEntity findByName(String name);
}
