package cn.liguohao.ikaros.repository;

import cn.liguohao.ikaros.entity.EpisodeEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author li-guohao
 * @date 2022/06/19
 */
public interface EpisodeRepository extends JpaRepository<EpisodeEntity, Long> {

    /**
     * 根据状态和ID查询记录
     *
     * @param eid    剧集ID
     * @param status 剧集状态
     * @return 剧集记录
     */
    Optional<EpisodeEntity> findByIdAndStatus(Long eid, Boolean status);
}
