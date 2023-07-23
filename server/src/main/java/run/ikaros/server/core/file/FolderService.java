package run.ikaros.server.core.file;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.file.Folder;
import run.ikaros.api.infra.exception.file.FolderHasChildException;
import run.ikaros.server.store.entity.FolderEntity;

public interface FolderService {
    /**
     * Create a folder in parent folder that id is parentId.
     * if folder exists, return it directly.
     *
     * @param parentId parent folder id
     * @param name     new folder name
     * @return folder
     */
    Mono<FolderEntity> create(Long parentId, String name);

    /**
     * Delete folder by id.
     * delete all children(files and folder) when allowDeleteWhenChildExists is true.
     * throw {@link FolderHasChildException} when allowDeleteWhenChildExists is false
     * and folder has children.
     *
     * @param id                         folder id
     * @param allowDeleteWhenChildExists allow to delete when child exists
     * @throws FolderHasChildException folder has child, such as files or folders
     */
    Mono<Void> delete(Long id, boolean allowDeleteWhenChildExists)
        throws FolderHasChildException;

    /**
     * Update folder name. skip when same with old.
     *
     * @param id      folder id
     * @param newName new folder name
     * @return folder
     */
    Mono<Folder> updateName(Long id, String newName);

    /**
     * Move folder to new location.
     *
     * @param id          folder id
     * @param newParentId new parent folder id
     * @return folder
     */
    Mono<Folder> move(Long id, Long newParentId);

    Mono<Folder> findById(Long id);

    Mono<Folder> findByIdShallow(Long id);

    Mono<Folder> findByParentIdAndName(Long parentId, String name);

    Flux<Folder> findByParentIdAndNameLike(Long parentId, String nameKeyWord);

    Mono<Void> pushRemote(Long folderId, String remote);

    Mono<Void> pullRemote(Long folderId, String remote);
}
