package run.ikaros.server.core.attachment.listener;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.server.core.user.UserAvatarUpdateEvent;
import run.ikaros.server.store.entity.AttachmentReferenceEntity;
import run.ikaros.server.store.repository.AttachmentReferenceRepository;
import run.ikaros.server.store.repository.AttachmentRepository;

@Slf4j
@Component
public class AttachmentRefUserAvatarUpdateListener {
    private final AttachmentReferenceRepository attachmentReferenceRepository;
    private final AttachmentRepository attachmentRepository;

    public AttachmentRefUserAvatarUpdateListener(
        AttachmentReferenceRepository attachmentReferenceRepository,
        AttachmentRepository attachmentRepository) {
        this.attachmentReferenceRepository = attachmentReferenceRepository;
        this.attachmentRepository = attachmentRepository;
    }

    /**
     * Construct.
     */
    @EventListener(UserAvatarUpdateEvent.class)
    public Mono<Void> onSubjectAdd(UserAvatarUpdateEvent event) {
        final UUID userId = event.getUserId();
        final String oldAvatar = event.getOldAvatar();
        final String avatar = event.getAvatar();
        if (StringUtils.isBlank(avatar)) {
            if (StringUtils.isBlank(oldAvatar)) {
                return Mono.empty();
            }
            // 如果旧的头像是属于附件，并且有一条附件引用记录，则移除这条附件引用记录
            return attachmentRepository.findByUrl(oldAvatar)
                .flatMap(attachmentEntity -> attachmentReferenceRepository
                    .findByTypeAndAttachmentIdAndReferenceId(
                        AttachmentReferenceType.USER_AVATAR,
                        attachmentEntity.getId(), userId
                    ))
                .flatMap(attachmentReferenceRepository::delete)
                .then();
        }
        // 如果新的头像URL是属于附件，则添加新的附件引用，引用类型为 用户头像
        return attachmentRepository.findByUrl(avatar)
            .map(attachmentEntity -> AttachmentReferenceEntity.builder()
                .type(AttachmentReferenceType.USER_AVATAR)
                .attachmentId(attachmentEntity.getId())
                .referenceId(userId)
                .build())
            .flatMap(attachmentReferenceRepository::save)
            .then();
    }

}
