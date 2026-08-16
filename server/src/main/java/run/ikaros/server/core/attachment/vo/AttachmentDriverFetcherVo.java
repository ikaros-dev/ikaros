package run.ikaros.server.core.attachment.vo;

import lombok.Data;
import org.jspecify.annotations.Nullable;
import run.ikaros.api.store.enums.AttachmentDriverType;

@Data
public class AttachmentDriverFetcherVo {
    private @Nullable AttachmentDriverType type;
    private @Nullable String name;
}
