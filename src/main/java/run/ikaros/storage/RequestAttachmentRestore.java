package run.ikaros.storage;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RequestAttachmentRestore(@NotNull UUID attachmentId, String providerRestoreClass,
    String budgetConfirmationToken) {
    public RequestAttachmentRestore(UUID attachmentId, String providerRestoreClass) {
        this(attachmentId, providerRestoreClass, null);
    }
}
