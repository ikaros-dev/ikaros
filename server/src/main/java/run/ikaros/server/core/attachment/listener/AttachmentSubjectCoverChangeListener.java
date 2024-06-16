package run.ikaros.server.core.attachment.listener;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.infra.utils.FileUtils;
import run.ikaros.server.core.subject.event.SubjectRemoveEvent;
import run.ikaros.server.store.entity.SubjectEntity;
import run.ikaros.server.store.repository.AttachmentRepository;

@Slf4j
@Component
public class AttachmentSubjectCoverChangeListener {
    private final AttachmentRepository attachmentRepository;

    public AttachmentSubjectCoverChangeListener(
        AttachmentRepository attachmentRepository) {
        this.attachmentRepository = attachmentRepository;
    }

    /**
     * Construct.
     */
    @EventListener(SubjectRemoveEvent.class)
    public Mono<Void> onSubjectRemove(SubjectRemoveEvent event) {
        SubjectEntity subjectEntity = event.getEntity();
        Long subjectId = subjectEntity.getId();
        String cover = subjectEntity.getCover();
        if (Objects.isNull(subjectId) || subjectId < 0 || StringUtils.isBlank(cover)) {
            return Mono.empty();
        }
        return attachmentRepository.findByUrl(cover)
            .map(attachmentEntity -> {
                String fsPath = attachmentEntity.getFsPath();
                if (StringUtils.isBlank(fsPath) || fsPath.startsWith("http")) {
                    return attachmentEntity;
                }
                try {
                    FileUtils.deletePathAndContentIfExists(Path.of(fsPath));
                    log.debug("Delete subject cover url success in fs path for fsPath[{}].",
                        fsPath);
                } catch (IOException e) {
                    log.error("Delete subject cover url failed in fs path for fsPath[{}].",
                        fsPath, e);
                }
                return attachmentEntity;
            })
            .flatMap(attachmentRepository::delete);
    }
}
