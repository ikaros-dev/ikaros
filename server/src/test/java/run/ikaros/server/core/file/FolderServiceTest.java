package run.ikaros.server.core.file;


import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;
import run.ikaros.api.constant.FileConst;
import run.ikaros.api.core.file.Folder;
import run.ikaros.server.store.entity.FolderEntity;
import run.ikaros.server.store.repository.FileRepository;
import run.ikaros.server.store.repository.FolderRepository;

@SpringBootTest
class FolderServiceTest {

    @Autowired
    FolderService folderService;
    @Autowired
    FolderRepository folderRepository;
    @Autowired
    FileRepository fileRepository;

    @AfterEach
    void tearDown() {
        StepVerifier.create(folderRepository.deleteAll()).verifyComplete();
        StepVerifier.create(fileRepository.deleteAll()).verifyComplete();
    }

    @Test
    void create() {
        String dir1Name = "unit-test-name-" + new Random().nextInt(10000);

        StepVerifier.create(folderService.create(FileConst.DEFAULT_FOLDER_ID, dir1Name)
                .map(FolderEntity::getName))
            .expectNext(dir1Name)
            .verifyComplete();

        StepVerifier.create(
                folderService.findByParentIdAndName(FileConst.DEFAULT_FOLDER_ID, dir1Name)
                    .map(Folder::getName))
            .expectNext(dir1Name)
            .verifyComplete();

    }

    @Test
    void delete() {

        String dir1Name = "unit-test-name-" + new Random().nextInt(10000);
        AtomicReference<Long> id = createNewDir(dir1Name);

        StepVerifier.create(folderService.delete(id.get(), true))
            .verifyComplete();

        StepVerifier.create(
                folderService.findByParentIdAndName(FileConst.DEFAULT_FOLDER_ID, dir1Name)
                    .map(Folder::getName))
            .verifyComplete();
    }

    @Test
    void updateName() {
        String dir1Name = "unit-test-name-" + new Random().nextInt(10000);
        AtomicReference<Long> id = createNewDir(dir1Name);


        String dir1NewName = "unit-test-name-" + new Random().nextInt(10000);

        StepVerifier.create(folderService.updateName(id.get(), dir1NewName)
                .map(Folder::getName))
            .expectNext(dir1NewName)
            .verifyComplete();

        StepVerifier.create(
                folderService.findByParentIdAndName(FileConst.DEFAULT_FOLDER_ID, dir1Name)
                    .map(Folder::getName))
            .verifyComplete();

        StepVerifier.create(
                folderService.findByParentIdAndName(FileConst.DEFAULT_FOLDER_ID, dir1NewName)
                    .map(Folder::getName))
            .expectNext(dir1NewName)
            .verifyComplete();

    }

    @Test
    void move() {
        String dir1Name = "unit-test-name-" + new Random().nextInt(10000);
        AtomicReference<Long> dir1Id = createNewDir(dir1Name);
        String dir11Name = "unit-test-name-" + new Random().nextInt(10000);
        AtomicReference<Long> dir11Id = createNewDir(dir1Id.get(), dir11Name);
        String dir111Name = "unit-test-name-" + new Random().nextInt(10000);
        AtomicReference<Long> dir111Id = createNewDir(dir11Id.get(), dir111Name);
        // dir1
        //  |- dir11
        //  |-   |- dir111

        StepVerifier.create(folderService.findById(dir1Id.get()))
            .expectNextMatches(folder -> folderHasAppointFolderName(folder, dir11Name)
                && folderHasAppointFolderName(folder.getFolders().get(0), dir111Name)
            )
            .verifyComplete();


        String dir2Name = "unit-test-name-" + new Random().nextInt(10000);
        AtomicReference<Long> dir2Id = createNewDir(dir111Name);
        StepVerifier.create(folderService.move(dir111Id.get(), dir2Id.get())
                .map(Folder::getParentId))
            .expectNext(dir2Id.get())
            .verifyComplete();

        // dir1
        //  |- dir11
        // dir2
        //  |- dir111
        StepVerifier.create(folderService.findByParentIdAndName(dir11Id.get(), dir111Name))
            .verifyComplete();
        StepVerifier.create(folderService.findByParentIdAndName(dir2Id.get(), dir111Name)
                .map(Folder::getName))
            .expectNext(dir111Name)
            .verifyComplete();

    }

    private boolean folderHasAppointFolderName(Folder folder, String childFolderName) {
        return Objects.nonNull(folder)
            && Objects.nonNull(folder.getFolders())
            && folder.hasFolder()
            && Objects.nonNull(folder.getFolders().get(0))
            && childFolderName.equals(folder.getFolders().get(0).getName());
    }

    private AtomicReference<Long> createNewDir(String dirName) {
        return createNewDir(FileConst.DEFAULT_FOLDER_ID, dirName);
    }

    private AtomicReference<Long> createNewDir(Long parentId, String dirName) {
        AtomicReference<Long> id = new AtomicReference<>();

        StepVerifier.create(folderService.create(parentId, dirName)
                .map(FolderEntity::getName))
            .expectNext(dirName)
            .verifyComplete();

        StepVerifier.create(
                folderService.findByParentIdAndName(parentId, dirName)
                    .map(folder -> {
                        id.set(folder.getId());
                        return folder.getName();
                    }))
            .expectNext(dirName)
            .verifyComplete();

        Assertions.assertThat(id.get()).isNotNull();
        return id;
    }

    @Test
    void findByParentIdAndNameLike() {
        String dir1Name = "unit-test-name-" + new Random().nextInt(10000);
        AtomicReference<Long> dir1Id = createNewDir(dir1Name);
        String dir2Name = "unit-test-name-" + new Random().nextInt(10000);
        AtomicReference<Long> dir11Id = createNewDir(dir2Name);
        String dir3Name = "unit-test-name-" + new Random().nextInt(10000);
        AtomicReference<Long> dir111Id = createNewDir(dir11Id.get(), dir3Name);
        StepVerifier.create(
                folderService.findByParentIdAndNameLike(FileConst.DEFAULT_FOLDER_ID, "unit-test")
                    .map(Folder::getName))
            .expectNext(dir1Name)
            .expectNext(dir2Name)
            .verifyComplete();

    }
}