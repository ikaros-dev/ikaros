package cn.liguohao.ikaros.repository;


import cn.liguohao.ikaros.model.entity.BoxTypeEntity;

/**
 * @author li-guohao
 */
public interface BoxTypeRepository extends BaseRepository<BoxTypeEntity> {

    boolean existsByName(String name);


    BoxTypeEntity findByName(String name);
}
