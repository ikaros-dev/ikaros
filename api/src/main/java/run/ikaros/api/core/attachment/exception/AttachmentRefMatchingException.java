package run.ikaros.api.core.attachment.exception;

public class AttachmentRefMatchingException extends RuntimeException {
    public AttachmentRefMatchingException(String message) {
        super(message);
    }

    public AttachmentRefMatchingException(String message, Throwable cause) {
        super(message, cause);
    }
}
