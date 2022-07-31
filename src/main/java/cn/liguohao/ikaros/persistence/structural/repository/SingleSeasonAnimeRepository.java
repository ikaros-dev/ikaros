package cn.liguohao.ikaros.persistence.structural.repository;

import cn.liguohao.ikaros.persistence.structural.entity.SingleSeasonAnimeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author li-guohao
 * @date 2022/06/19
 */
public interface SingleSeasonAnimeRepository extends JpaRepository<SingleSeasonAnimeEntity, Long> {

    /**
     * 根据状态和ID查询记录
     *
     * @param aid    ID
     * @param status 状态
     * @return 记录
     */
    Optional<SingleSeasonAnimeEntity> findByIdAndStatus(Long aid, Boolean status);
}
