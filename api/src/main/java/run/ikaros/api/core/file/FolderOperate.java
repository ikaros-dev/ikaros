package run.ikaros.api.core.file;

import reactor.core.publisher.Mono;
import run.ikaros.api.plugin.AllowPluginOperate;

public interface FolderOperate extends AllowPluginOperate {
    /**
     * Create a folder in parent folder that id is parentId.
     * if folder exists, return it directly.
     *
     * @param parentId parent folder id
     * @param name     new folder name
     * @return folder
     */
    Mono<Folder> create(Long parentId, String name);

    Mono<Folder> findById(Long id);

    Mono<Folder> findByParentIdAndName(Long parentId, String name);

}
