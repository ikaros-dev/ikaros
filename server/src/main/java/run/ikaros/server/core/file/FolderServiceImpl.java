package run.ikaros.server.core.file;

import java.time.LocalDateTime;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.FileConst;
import run.ikaros.api.core.file.Folder;
import run.ikaros.server.infra.exception.file.FolderExistsException;
import run.ikaros.server.infra.exception.file.FolderHasChildException;
import run.ikaros.server.infra.exception.file.ParentFolderNotExistsException;
import run.ikaros.server.infra.utils.ReactiveBeanUtils;
import run.ikaros.server.store.entity.FolderEntity;
import run.ikaros.server.store.repository.FolderRepository;

@Slf4j
@Service
public class FolderServiceImpl implements FolderService {
    private final FolderRepository folderRepository;

    public FolderServiceImpl(FolderRepository folderRepository) {
        this.folderRepository = folderRepository;
    }


    @Override
    public Mono<Folder> create(Long parentId, String name)
        throws FolderExistsException {
        Assert.hasText(name, "folder name must has text.");
        if (Objects.isNull(parentId) || parentId < 0) {
            parentId = FileConst.DEFAULT_FOLDER_ROOT_ID;
        }
        Long finalParentFolderId = parentId;
        return folderRepository.findByNameAndParentId(name, parentId)
            .switchIfEmpty(folderRepository.save(FolderEntity.builder()
                .parentId(parentId).name(name)
                .createTime(LocalDateTime.now()).updateTime(LocalDateTime.now()).build()))
            .flatMap(folderEntity -> Mono.error(
                new FolderExistsException(
                    "folder exists "
                        + "for parent id:[" + finalParentFolderId + "] "
                        + "and name:[" + name + "]")));
    }

    @Override
    public Mono<Void> delete(Long id, boolean allowDeleteWhenChildExists)
        throws FolderHasChildException {
        Assert.isTrue(id > 0, "folder id must gt 0.");
        return folderRepository.findAllByParentId(id)
            .flatMap(folderEntity -> allowDeleteWhenChildExists
                ? delete(folderEntity.getId(), true)
                :
                Mono.error(new FolderHasChildException("current folder has children when delete.")))
            .then(folderRepository.deleteById(id));
    }

    @Override
    public Mono<Folder> updateName(Long id, String newName) {
        Assert.isTrue(id > 0, "folder id must gt 0.");
        Assert.hasText(newName, "newFolderName must hast text.");
        return folderRepository.findById(id)
            .filter(folderEntity -> !newName.equals(folderEntity.getName()))
            .map(folderEntity -> folderEntity.setName(newName))
            .flatMap(folderRepository::save)
            .flatMap(folderEntity -> ReactiveBeanUtils.copyProperties(folderEntity, new Folder()));
    }

    @Override
    public Mono<Folder> move(Long id, Long newParentId) {
        Assert.isTrue(id > 0, "folder id must gt 0.");
        Assert.isTrue(newParentId > 0, "newParentFolderId id must gt 0.");
        return folderRepository.findById(id)
            .filter(folderEntity -> !newParentId.equals(folderEntity.getParentId()))
            .flatMap(folderEntity -> folderRepository.existsById(newParentId)
                .filter(exists -> exists)
                .switchIfEmpty(Mono.error(new ParentFolderNotExistsException(
                    "target parent folder not exists,"
                        + "please create parent folder before move.")))
                .flatMap(exists ->
                    folderRepository.save(folderEntity.setParentId(newParentId))))
            .flatMap(folderEntity -> ReactiveBeanUtils.copyProperties(folderEntity, new Folder()));
    }

}
