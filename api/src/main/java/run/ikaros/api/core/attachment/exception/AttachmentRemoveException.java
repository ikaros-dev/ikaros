package run.ikaros.api.core.attachment.exception;

/**
 * Please use
 * {@link run.ikaros.api.infra.exception.attachment.exception.AttachmentRemoveException}.
 */
@Deprecated
public class AttachmentRemoveException extends AttachmentException {
    public AttachmentRemoveException(String message) {
        super(message);
    }

    public AttachmentRemoveException(String message, Throwable cause) {
        super(message, cause);
    }
}
