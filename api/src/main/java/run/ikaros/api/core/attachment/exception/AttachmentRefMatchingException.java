package run.ikaros.api.core.attachment.exception;

/**
 * Please use
 * {@link run.ikaros.api.infra.exception.attachment.exception.AttachmentRefMatchingException}.
 */
@Deprecated
public class AttachmentRefMatchingException extends AttachmentException {
    public AttachmentRefMatchingException(String message) {
        super(message);
    }

    public AttachmentRefMatchingException(String message, Throwable cause) {
        super(message, cause);
    }
}
