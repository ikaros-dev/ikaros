package run.ikaros.api.core.attachment;

import reactor.core.publisher.Flux;
import run.ikaros.api.plugin.AllowPluginOperate;
import run.ikaros.api.store.enums.AttachmentRelationType;

public interface AttachmentRelationOperate extends AllowPluginOperate {

    Flux<AttachmentRelation> findAllByTypeAndAttachmentId(AttachmentRelationType type,
                                                          Long attachmentId);
}
