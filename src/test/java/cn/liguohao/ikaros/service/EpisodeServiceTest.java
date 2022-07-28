package cn.liguohao.ikaros.service;


import cn.liguohao.ikaros.persistence.structural.entity.EpisodeEntity;
import cn.liguohao.ikaros.persistence.incompact.file.FileData;
import cn.liguohao.ikaros.persistence.incompact.file.FileDataHandler;
import cn.liguohao.ikaros.persistence.incompact.file.FileDataOperateResult;
import cn.liguohao.ikaros.persistence.incompact.file.LocalFileDataHandler;
import cn.liguohao.ikaros.persistence.structural.repository.EpisodeRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author li-guohao
 * @date 2022/06/19
 */
@SpringBootTest
class EpisodeServiceTest {

    static EpisodeEntity episodeEntity;
    static final String content = "Hello World";
    static byte[] datum = content.getBytes(StandardCharsets.UTF_8);
    static FileDataHandler fileDataHandler = new LocalFileDataHandler();
    @Autowired
    EpisodeService episodeService;
    @Autowired
    EpisodeRepository episodeRepository;

    void assertEpisode(EpisodeEntity expected, EpisodeEntity actual) {
        Assertions.assertNotNull(expected);
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(expected.title(), actual.title());
        Assertions.assertEquals(expected.shortTitle(), actual.shortTitle());
        Assertions.assertEquals(expected.overview(), actual.overview());
        Assertions.assertEquals(expected.episodeNumber(), actual.episodeNumber());
    }

    void resetEpisode(EpisodeEntity episodeEntity) {
        episodeRepository.deleteById(episodeEntity.getId());
        fileDataHandler.delete(
            FileData.parseEpisodePath(episodeEntity.path(), episodeEntity.dataAddedTime()));
    }

    @BeforeAll
    public static void setUp() {
        episodeEntity = new EpisodeEntity()
            .setTitle(" test episode title S1E01    ")
            .setShortTitle(" test ep ")
            .setDataAddedTime(LocalDateTime.now())
            .setOverview(" this is test episode....")
            .setAirTime(LocalDateTime.now())
            .setEpisodeNumber(1);
    }

    @Test
    void add() {
        episodeService.addOrUpdate(episodeEntity, datum);

        Long eid = episodeEntity.getId();
        Assertions.assertNotNull(eid);

        // 验证元数据
        Optional<EpisodeEntity> episodeEntityRecordOptional = episodeRepository.findById(eid);

        Assertions.assertTrue(episodeEntityRecordOptional.isPresent());

        EpisodeEntity episodeEntityRecord = episodeEntityRecordOptional.get();

        assertEpisode(episodeEntity, episodeEntityRecord);
        Assertions.assertNotNull(episodeEntityRecord.path());

        // 验证项数据
        String path = episodeEntityRecord.path();
        Assertions.assertTrue(fileDataHandler.exist(path));

        // 重置
        resetEpisode(episodeEntityRecord);
    }

    @Test
    void updateMetaDataOnly() {
        // 准备旧元数据
        episodeService.addOrUpdate(episodeEntity, datum);

        // 验证旧元数据
        Long eid = episodeEntity.getId();
        Assertions.assertNotNull(eid);

        Optional<EpisodeEntity> episodeEntityRecordOptional = episodeRepository.findById(eid);

        Assertions.assertTrue(episodeEntityRecordOptional.isPresent());

        EpisodeEntity episodeEntityRecord = episodeEntityRecordOptional.get();

        assertEpisode(episodeEntity, episodeEntityRecord);

        // 更新旧元数据
        String newTitle = "new title";
        episodeEntity.setTitle(newTitle);

        // 操作
        episodeService.addOrUpdate(episodeEntity);

        // 验证更新是否生效
        Optional<EpisodeEntity> newEpisodeEntityOptional = episodeRepository.findById(eid);
        Assertions.assertTrue(newEpisodeEntityOptional.isPresent());
        EpisodeEntity newEpisodeEntity = newEpisodeEntityOptional.get();

        assertEpisode(episodeEntity, newEpisodeEntity);

        // 重置
        resetEpisode(episodeEntityRecord);
    }

    @Test
    void updateDatum() {
        // 准备旧元数据
        episodeService.addOrUpdate(episodeEntity, datum);

        // 验证旧元数据
        Long eid = episodeEntity.getId();
        Assertions.assertNotNull(eid);

        Optional<EpisodeEntity> episodeEntityRecordOptional = episodeRepository.findById(eid);

        Assertions.assertTrue(episodeEntityRecordOptional.isPresent());

        EpisodeEntity episodeEntityRecord = episodeEntityRecordOptional.get();

        assertEpisode(episodeEntity, episodeEntityRecord);

        FileDataOperateResult fileDataOperateResult = fileDataHandler.download(FileData
            .parseEpisodePath(episodeEntityRecord.path(), episodeEntityRecord.dataAddedTime()));
        byte[] downloadedDatum = fileDataOperateResult.itemData().datum();
        Assertions.assertEquals(new String(datum, StandardCharsets.UTF_8),
            new String(downloadedDatum, StandardCharsets.UTF_8));

        // 操作
        // 更新旧项数据
        byte[] newDatum = "New Hello World".getBytes(StandardCharsets.UTF_8);
        episodeService.addOrUpdate(episodeEntity, newDatum);

        // 验证更新是否生效
        byte[] newDownloadedDatum = fileDataHandler.download(FileData
                .parseEpisodePath(episodeEntityRecord.path(), episodeEntityRecord.dataAddedTime()))
            .itemData().datum();
        Assertions.assertEquals(new String(newDatum, StandardCharsets.UTF_8),
            new String(newDownloadedDatum, StandardCharsets.UTF_8));

        // 重置
        resetEpisode(episodeEntityRecord);

    }


    @Test
    void delete() {
        // 准备旧元数据
        episodeService.addOrUpdate(episodeEntity, datum);

        // 验证旧元数据
        Long eid = episodeEntity.getId();
        Assertions.assertNotNull(eid);

        Optional<EpisodeEntity> episodeEntityRecordOptional = episodeRepository.findById(eid);

        Assertions.assertTrue(episodeEntityRecordOptional.isPresent());

        EpisodeEntity episodeEntityRecord = episodeEntityRecordOptional.get();

        assertEpisode(episodeEntity, episodeEntityRecord);


        // 操作
        episodeService.delete(eid);

        // 验证
        Optional<EpisodeEntity> deletedEpisodeEntityOptional = episodeService.findByEid(eid);
        Assertions.assertTrue(deletedEpisodeEntityOptional.isEmpty());
        Assertions.assertFalse(fileDataHandler.exist(episodeEntityRecord.path()));

        // 重置
        resetEpisode(episodeEntityRecord);
    }

    @Test
    void findByEid() {
        // 准备旧元数据
        episodeService.addOrUpdate(episodeEntity, datum);

        // 操作
        Long eid = episodeEntity.getId();
        Assertions.assertNotNull(eid);

        Optional<EpisodeEntity> episodeEntityRecordOptional = episodeRepository.findById(eid);

        // 验证
        Assertions.assertTrue(episodeEntityRecordOptional.isPresent());
        EpisodeEntity episodeEntityRecord = episodeEntityRecordOptional.get();
        assertEpisode(episodeEntity, episodeEntityRecord);


        // 重置
        resetEpisode(episodeEntityRecord);
    }
}