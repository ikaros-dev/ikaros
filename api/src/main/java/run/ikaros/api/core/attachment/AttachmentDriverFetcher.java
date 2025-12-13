package run.ikaros.api.core.attachment;

import java.util.List;
import org.pf4j.ExtensionPoint;
import run.ikaros.api.store.enums.AttachmentDriverType;

public interface AttachmentDriverFetcher extends ExtensionPoint {
    default AttachmentDriverType getDriverType() {
        return AttachmentDriverType.CUSTOM;
    }

    String getDriverName();

    void setDriver(AttachmentDriver driver);

    List<Attachment> getChildAttachments(Long pid, String remotePath);
}
