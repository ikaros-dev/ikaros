package run.ikaros.api.infra.exception.attachment.exception;

public class AttachmentRefMatchingException extends AttachmentException {
    public AttachmentRefMatchingException(String message) {
        super(message);
    }

    public AttachmentRefMatchingException(String message, Throwable cause) {
        super(message, cause);
    }
}
