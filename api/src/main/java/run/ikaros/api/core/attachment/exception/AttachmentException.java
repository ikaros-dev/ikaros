package run.ikaros.api.core.attachment.exception;

import org.jspecify.annotations.Nullable;

public class AttachmentException extends RuntimeException {


    public AttachmentException(String message) {
        super(message);
    }

    public AttachmentException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }

}
