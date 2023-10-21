package run.ikaros.api.core.attachment.exception;

public class AttachmentRemoveException extends AttachmentException {
    public AttachmentRemoveException(String message) {
        super(message);
    }

    public AttachmentRemoveException(String message, Throwable cause) {
        super(message, cause);
    }
}
