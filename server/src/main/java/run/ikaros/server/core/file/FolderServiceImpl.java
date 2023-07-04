package run.ikaros.server.core.file;

import static run.ikaros.server.infra.utils.ReactiveBeanUtils.copyProperties;

import java.time.LocalDateTime;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.FileConst;
import run.ikaros.api.core.file.File;
import run.ikaros.api.core.file.Folder;
import run.ikaros.server.infra.exception.file.FolderExistsException;
import run.ikaros.server.infra.exception.file.FolderHasChildException;
import run.ikaros.server.infra.exception.file.ParentFolderNotExistsException;
import run.ikaros.server.store.entity.FileEntity;
import run.ikaros.server.store.entity.FolderEntity;
import run.ikaros.server.store.repository.FileRepository;
import run.ikaros.server.store.repository.FolderRepository;

@Slf4j
@Service
public class FolderServiceImpl implements FolderService {
    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;

    public FolderServiceImpl(FolderRepository folderRepository,
                             FileRepository fileRepository) {
        this.folderRepository = folderRepository;
        this.fileRepository = fileRepository;
    }


    @Override
    public Mono<FolderEntity> create(Long parentId, String name)
        throws FolderExistsException {
        Assert.hasText(name, "folder name must has text.");
        if (Objects.isNull(parentId) || parentId < 0) {
            parentId = FileConst.DEFAULT_FOLDER_ROOT_ID;
        }
        return folderRepository.findByNameAndParentId(name, parentId)
            .switchIfEmpty(folderRepository.save(FolderEntity.builder()
                .parentId(parentId).name(name)
                .createTime(LocalDateTime.now()).updateTime(LocalDateTime.now()).build()));
    }

    @Override
    public Mono<Void> delete(Long id, boolean allowDeleteWhenChildExists)
        throws FolderHasChildException {
        Assert.isTrue(id >= 0, "folder id must >= 0.");
        return folderRepository.findAllByParentId(id)
            .flatMap(folderEntity -> allowDeleteWhenChildExists
                ? delete(folderEntity.getId(), true)
                :
                Mono.error(new FolderHasChildException(
                    "current folder has children folders when delete.")))
            .thenMany(fileRepository.findAllByFolderId(id))
            .flatMap(fileEntity -> allowDeleteWhenChildExists
                ? deleteFile(fileEntity)
                :
                Mono.error(new FolderHasChildException(
                    "current folder has children files when delete.")))
            .then(folderRepository.deleteById(id));
    }

    private Mono<FileEntity> deleteFile(FileEntity fileEntity) {
        java.io.File file = new java.io.File(fileEntity.getOriginalPath());
        if (file.exists()) {
            file.delete();
            log.debug("delete file in path: {}", fileEntity.getOriginalPath());
        }
        return folderRepository.deleteById(fileEntity.getId())
            .then(Mono.just(fileEntity));
    }

    @Override
    public Mono<Folder> updateName(Long id, String newName) {
        Assert.isTrue(id > 0, "folder id must gt 0.");
        Assert.hasText(newName, "newFolderName must hast text.");
        return folderRepository.findById(id)
            .filter(folderEntity -> !newName.equals(folderEntity.getName()))
            .map(folderEntity -> folderEntity.setName(newName))
            .flatMap(folderRepository::save)
            .flatMap(folderEntity -> copyProperties(folderEntity, new Folder()));
    }

    @Override
    public Mono<Folder> move(Long id, Long newParentId) {
        Assert.isTrue(id >= 0, "folder id must >= 0.");
        Assert.isTrue(newParentId >= 0, "newParentFolderId id must >= 0.");
        return folderRepository.findById(id)
            .filter(folderEntity -> !newParentId.equals(folderEntity.getParentId()))
            .flatMap(folderEntity -> folderRepository.existsById(newParentId)
                .filter(exists -> exists)
                .switchIfEmpty(Mono.error(new ParentFolderNotExistsException(
                    "target parent folder not exists,"
                        + "please create parent folder before move.")))
                .flatMap(exists ->
                    folderRepository.save(folderEntity.setParentId(newParentId))))
            .flatMap(folderEntity -> findById(folderEntity.getId()));
    }

    @Override
    public Mono<Folder> findById(Long id) {
        Assert.isTrue(id > -1, "folder id must gt -1.");
        return folderRepository.findById(id)
            .flatMap(folderEntity -> copyProperties(folderEntity, new Folder()))
            .flatMap(folder -> folderRepository.findAllByParentId(folder.getId())
                .flatMap(folderEntity -> findById(folderEntity.getId()))
                .map(folder1 -> folder1.setParentId(folder.getId())
                    .setParentName(folder.getName()))
                .collectList()
                .map(folders -> folder.setFolders(folders).updateCanRead()))
            .flatMap(folder -> fileRepository.findAllByFolderId(id)
                .flatMap(fileEntity -> copyProperties(fileEntity, new File()))
                .collectList()
                .map(files -> folder.setFiles(files).updateCanRead()));
    }

    @Override
    public Mono<Folder> findByParentIdAndName(Long parentId, String name) {
        Assert.isTrue(parentId > -2, "parent folder id must gt -2.");
        Assert.hasText(name, "name must hast text.");
        return folderRepository.findByNameAndParentId(name, parentId)
            .flatMap(folderEntity -> findById(folderEntity.getId()));
    }

    @Override
    public Flux<Folder> findByParentIdAndNameLike(Long parentId, String nameKeyWord) {
        Assert.isTrue(parentId > -1, "parent folder id must gt 0.");
        Assert.hasText(nameKeyWord, "nameKeyWord must hast text.");
        String nameLike = '%' + nameKeyWord + '%';
        return folderRepository.findAllByNameLikeAndParentId(nameLike, parentId)
            .flatMap(folderEntity -> copyProperties(folderEntity, new Folder()));
    }

}
