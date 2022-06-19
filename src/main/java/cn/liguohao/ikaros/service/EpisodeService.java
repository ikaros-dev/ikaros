package cn.liguohao.ikaros.service;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.common.BeanKit;
import cn.liguohao.ikaros.common.JacksonConverter;
import cn.liguohao.ikaros.entity.EpisodeEntity;
import cn.liguohao.ikaros.file.ItemData;
import cn.liguohao.ikaros.file.ItemDataHandler;
import cn.liguohao.ikaros.file.ItemDataOperateResult;
import cn.liguohao.ikaros.file.LocalItemDataHandler;
import cn.liguohao.ikaros.repository.EpisodeRepository;
import com.sun.istack.Nullable;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author li-guohao
 * @date 2022/06/19
 */
@Service
public class EpisodeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EpisodeService.class);

    private final EpisodeRepository episodeRepository;
    private ItemDataHandler itemDataHandler = new LocalItemDataHandler();

    public EpisodeService(EpisodeRepository episodeRepository) {
        this.episodeRepository = episodeRepository;
    }

    /**
     * 只更新元数据的时候使用这个
     *
     * @see #addOrUpdate(EpisodeEntity, byte[])
     */
    public void addOrUpdate(EpisodeEntity episodeEntity) {
        Assert.isNotNull(episodeEntity);
        addOrUpdate(episodeEntity, null);
    }

    /**
     * 添加或者更新对应的剧集记录
     *
     * @param episodeEntity 剧集
     * @param datum         项数据
     */
    public void addOrUpdate(EpisodeEntity episodeEntity, @Nullable byte[] datum) {
        Assert.isNotNull(episodeEntity);

        Long eid = episodeEntity.getId();

        if (eid == null || findByEid(eid).isEmpty()) {
            // 新增
            Assert.isNotBlank(episodeEntity.title());

            if (datum == null) {
                throw new IllegalArgumentException("datum is null when add new episode record.");
            }

            // 上传项数据
            ItemDataOperateResult itemDataOperateResult = itemDataHandler.upload(ItemData
                .buildInstanceByDatum(datum, episodeEntity.title().strip()));
            String uploadedPath = itemDataOperateResult.itemData().uploadedPath();
            episodeEntity.setPath(uploadedPath);

            // 保存记录
            episodeRepository.save(episodeEntity);

            LOGGER.debug("add new episode record, new record is [{}]",
                JacksonConverter.obj2Json(episodeEntity));
        } else {
            // 更新
            Assert.isPositive(eid);

            Optional<EpisodeEntity> episodeEntityRecordOptional = findByEid(eid);

            // 更新
            EpisodeEntity episodeEntityRecord = episodeEntityRecordOptional.get();

            // 元数据更新
            BeanKit.copyProperties(episodeEntity, episodeEntityRecord);


            if (datum != null) {
                // 涉及项数据的更新

                // 移除旧的项数据
                itemDataHandler.delete(ItemData.parseEpisodePath(
                    episodeEntityRecord.path(), episodeEntityRecord.dataAddedTime()));

                // 上传新的项数据，并设置上传后的路径
                ItemDataOperateResult itemDataOperateResult = itemDataHandler.upload(ItemData
                    .buildInstanceByDatum(datum, episodeEntityRecord.title().strip()));
                String uploadedPath = itemDataOperateResult.itemData().uploadedPath();
                episodeEntityRecord.setPath(uploadedPath);
            }

            episodeRepository.save(episodeEntityRecord);

            LOGGER.debug("update episode record, eid is [{}]", episodeEntityRecord.getId());

        }

    }

    /**
     * 移除对应的剧集记录
     *
     * @param eid 剧集ID
     */
    public void delete(Long eid) {
        Assert.isPositive(eid);
        Optional<EpisodeEntity> episodeEntityOptional = findByEid(eid);

        if (episodeEntityOptional.isPresent()) {
            EpisodeEntity episodeEntity = episodeEntityOptional.get();

            // 逻辑删除，更新元数据状态
            episodeEntity.setStatus(false);
            episodeRepository.save(episodeEntity);

            // 但是会把项数据(文件)删除
            itemDataHandler.delete(ItemData
                .parseEpisodePath(episodeEntity.path(), episodeEntity.dataAddedTime()));

            LOGGER.debug("delete old episode record, eid is [{}]", eid);
        }
    }

    /**
     * 根据剧集ID查询记录
     *
     * @param eid 剧集ID
     * @return 剧集记录或者null
     */
    public Optional<EpisodeEntity> findByEid(Long eid) {
        Assert.isPositive(eid);

        return episodeRepository.findByIdAndStatus(eid, true);
    }

}
