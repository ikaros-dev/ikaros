package run.ikaros.server.core.binding.handler;

import static run.ikaros.api.infra.utils.ReactiveBeanUtils.copyProperties;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.binding.DirectoryBindingContext;
import run.ikaros.api.core.binding.DirectoryBindingStep;
import run.ikaros.api.core.media.MediaFileCategory;
import run.ikaros.api.core.media.MediaFilePolicy;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.core.attachment.service.AttachmentContentInspectionService;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.repository.AttachmentRepository;

/**
 * Step 5: List child files and SP subdirectories of the directory.
 * Order: 50
 */
@Slf4j
@Component
public class ListFilesStep implements DirectoryBindingStep {

    private final AttachmentRepository attachmentRepository;
    /** 对候选附件执行有限前缀真实格式检查. */
    private final AttachmentContentInspectionService contentInspectionService;

    public ListFilesStep(AttachmentRepository attachmentRepository,
                         AttachmentContentInspectionService contentInspectionService) {
        this.attachmentRepository = attachmentRepository;
        this.contentInspectionService = contentInspectionService;
    }

    @Override
    public String name() {
        return "ListFiles";
    }

    @Override
    public int order() {
        return 50;
    }

    @Override
    public Mono<DirectoryBindingContext> execute(DirectoryBindingContext context) {
        return attachmentRepository.findAllByParentId(context.getDirectoryId())
            .collectList()
            .flatMap(children -> {
                List<AttachmentEntity> spDirEntityList = children.stream()
                    .filter(att -> att.getType() == AttachmentType.Directory
                        || att.getType() == AttachmentType.Driver_Directory)
                    .filter(att -> isSpDirectory(att.getName()))
                    .toList();

                return Flux.fromIterable(children)
                    .filter(att -> att.getType() == AttachmentType.File
                        || att.getType() == AttachmentType.Driver_File)
                    .filter(att -> MediaFilePolicy.isAllowedFileName(att.getName()))
                    .concatMap(entity -> contentInspectionService.inspect(entity)
                        .filter(result -> result.category() == MediaFileCategory.VIDEO)
                        .flatMap(ignored ->
                            copyProperties(entity, Attachment.builder().build())))
                    .collectList()
                    .flatMap(videoFiles -> {
                        context.setChildAttachments(videoFiles);
                        return Flux.fromIterable(spDirEntityList)
                            .concatMap(entity ->
                                copyProperties(entity, Attachment.builder().build()))
                            .collectList();
                    })
                    .map(spDirs -> {
                        context.setSpSubdirectoryAttachments(spDirs);
                        log.info("Found {} video files and {} SP subdirectories in directory: {}",
                            context.getChildAttachments().size(), spDirs.size(),
                            context.getDirectoryName());
                        return context;
                    });
            });
    }

    private boolean isSpDirectory(String name) {
        if (name == null) {
            return false;
        }
        String upper = name.toUpperCase();
        return upper.contains("SP") || upper.contains("OP") || upper.contains("ED")
            || upper.contains("OAD") || upper.contains("OVA") || upper.contains("SPECIAL");
    }

    @Override
    public Mono<Void> rollback(DirectoryBindingContext context) {
        return Mono.empty();
    }
}
