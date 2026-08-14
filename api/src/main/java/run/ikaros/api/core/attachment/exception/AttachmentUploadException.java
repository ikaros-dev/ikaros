package run.ikaros.api.core.attachment.exception;

import org.jspecify.annotations.Nullable;

public class AttachmentUploadException extends AttachmentException {
    public AttachmentUploadException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
