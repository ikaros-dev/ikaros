package run.ikaros.server.core.attachment.vo;

import lombok.Data;
import run.ikaros.api.store.enums.AttachmentDriverType;

@Data
public class AttachmentDriverFetcherVo {
    private AttachmentDriverType type;
    private String name;
}
