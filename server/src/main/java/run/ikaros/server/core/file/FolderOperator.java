package run.ikaros.server.core.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.file.Folder;
import run.ikaros.api.core.file.FolderOperate;
import run.ikaros.server.infra.utils.ReactiveBeanUtils;

@Slf4j
@Component
public class FolderOperator implements FolderOperate {
    private final FolderService folderService;

    public FolderOperator(FolderService folderService) {
        this.folderService = folderService;
    }

    @Override
    public Mono<Folder> create(Long parentId, String name) {
        return folderService.create(parentId, name)
            .flatMap(entity -> ReactiveBeanUtils.copyProperties(entity, new Folder()));
    }

    @Override
    public Mono<Folder> findById(Long id) {
        return folderService.findById(id);
    }

    @Override
    public Mono<Folder> findByParentIdAndName(Long parentId, String name) {
        return folderService.findByParentIdAndName(parentId, name);
    }
}
