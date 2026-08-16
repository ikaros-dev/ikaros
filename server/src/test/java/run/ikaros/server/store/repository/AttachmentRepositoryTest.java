package run.ikaros.server.store.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.api.core.attachment.AttachmentConst;
import run.ikaros.api.infra.utils.FileUtils;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.store.entity.AttachmentEntity;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class AttachmentRepositoryTest {


    @Autowired
    AttachmentRepository repository;

    @Test
    void findById() {
        final String name = String.valueOf(new Random().nextInt(9999));
        AttachmentEntity att = AttachmentEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .name(name)
            .parentId(AttachmentConst.ROOT_DIRECTORY_ID)
            .type(AttachmentType.Directory)
            .path("").fsPath("")
            .build();
        StepVerifier.create(repository.insert(att))
            .expectNext(att).verifyComplete();

        StepVerifier.create(repository.findById(att.getId()))
            .expectNext(att).verifyComplete();
    }

    @Test
    void findAllByTypeAndNameLike() {
        // 保存一些记录
        final String videoAttName =
            "[Airota&LoliHouse] Liz and the Blue Bird "
                + "- Movie [BDRip 1080p HEVC-yuv420p10 FLACx2].mkv";
        final String assScSubtitleAttName =
            "[Airota&LoliHouse] Liz and the Blue Bird - "
                + "Movie [BDRip 1080p HEVC-yuv420p10 FLACx2].sc.ass";
        final String assTcSubtitleAttName =
            "[Airota&LoliHouse] Liz and the Blue Bird - "
                + "Movie [BDRip 1080p HEVC-yuv420p10 FLACx2].tc.ass";

        AttachmentEntity att1 = AttachmentEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .name(videoAttName).type(AttachmentType.File).path(videoAttName)
            .build();
        StepVerifier.create(repository.insert(att1).map(AttachmentEntity::getId))
            .expectNext(att1.getId())
            .verifyComplete();

        AttachmentEntity attAssSc1 = AttachmentEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .name(assScSubtitleAttName).type(AttachmentType.File).path(videoAttName)
            .build();
        StepVerifier.create(repository.insert(attAssSc1).map(AttachmentEntity::getId))
            .expectNext(attAssSc1.getId())
            .verifyComplete();

        AttachmentEntity attAssTc1 = AttachmentEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .name(assTcSubtitleAttName).type(AttachmentType.File).path(videoAttName)
            .build();
        StepVerifier.create(repository.insert(attAssTc1).map(AttachmentEntity::getId))
            .expectNext(attAssTc1.getId())
            .verifyComplete();


        // 查询记录 验证结果
        String fileName = FileUtils.parseFileNameWithoutPostfix(videoAttName);
        Assertions.assertThat(fileName)
            .isEqualTo(
                "[Airota&LoliHouse] Liz and the Blue Bird "
                    + "- Movie [BDRip 1080p HEVC-yuv420p10 FLACx2]");
        StepVerifier.create(repository.findAllByTypeAndNameLike(
                AttachmentType.File, fileName + "%"
            ).collectList().map(List::size))
            .expectNext(3).verifyComplete();
    }

    @Test
    void findAllByParentIdAndDriverId() {
        UUID parentId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        LocalDateTime modifiedTime = LocalDateTime.of(2026, 7, 30, 18, 0);
        AttachmentEntity attachment = AttachmentEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .parentId(parentId)
            .driverId(driverId)
            .name("snapshot.mkv")
            .type(AttachmentType.Driver_File)
            .modifiedTime(modifiedTime)
            .build();

        StepVerifier.create(repository.insert(attachment)
                .thenMany(repository.findAllByParentIdAndDriverId(parentId, driverId)))
            .assertNext(storedAttachment -> {
                Assertions.assertThat(storedAttachment.getId()).isEqualTo(attachment.getId());
                Assertions.assertThat(storedAttachment.getModifiedTime()).isEqualTo(modifiedTime);
            })
            .verifyComplete();
    }

    @Test
    void countKnownFilesAndFolders() {
        long initialFiles = repository.countKnownFiles().blockOptional().orElseThrow();
        long initialFolders = repository.countKnownFolders(
            AttachmentConst.ROOT_DIRECTORY_ID,
            AttachmentConst.COVER_DIRECTORY_ID,
            AttachmentConst.DOWNLOAD_DIRECTORY_ID).blockOptional().orElseThrow();
        UUID driverId = UuidV7Utils.generateUuid();
        AttachmentEntity file = attachment(AttachmentType.File, false, null, null);
        AttachmentEntity driverFile = attachment(AttachmentType.Driver_File, false, null, driverId);
        AttachmentEntity deletedFile = attachment(AttachmentType.File, true, null, null);
        AttachmentEntity folder = attachment(AttachmentType.Directory, false, null, null);
        AttachmentEntity mountFolder = attachment(AttachmentType.Driver_Directory, false,
            AttachmentConst.ROOT_DIRECTORY_ID, driverId);
        AttachmentEntity scannedFolder = attachment(AttachmentType.Driver_Directory, false,
            mountFolder.getId(), driverId);
        List<AttachmentEntity> attachments = List.of(
            file, driverFile, deletedFile, folder, mountFolder, scannedFolder);

        StepVerifier.create(repository.insert(file)
                .then(repository.insert(driverFile))
                .then(repository.insert(deletedFile))
                .then(repository.insert(folder))
                .then(repository.insert(mountFolder))
                .then(repository.insert(scannedFolder))
                .then(repository.countKnownFiles()
                    .zipWith(repository.countKnownFolders(
                        AttachmentConst.ROOT_DIRECTORY_ID,
                        AttachmentConst.COVER_DIRECTORY_ID,
                        AttachmentConst.DOWNLOAD_DIRECTORY_ID))))
            .assertNext(counts -> {
                Assertions.assertThat(counts.getT1()).isEqualTo(initialFiles + 2);
                Assertions.assertThat(counts.getT2()).isEqualTo(initialFolders + 2);
            })
            .verifyComplete();

        StepVerifier.create(repository.deleteAll(attachments)).verifyComplete();
    }

    private AttachmentEntity attachment(AttachmentType type, boolean deleted,
                                        @Nullable UUID parentId, @Nullable UUID driverId) {
        UUID id = UuidV7Utils.generateUuid();
        return AttachmentEntity.builder()
            .id(id)
            .name(id.toString())
            .type(type)
            .deleted(deleted)
            .parentId(parentId)
            .driverId(driverId)
            .path("/" + id)
            .build();
    }
}
