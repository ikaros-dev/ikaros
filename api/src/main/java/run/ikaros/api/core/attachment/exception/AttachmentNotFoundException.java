package run.ikaros.api.core.attachment.exception;

/**
 * Please use
 * {@link run.ikaros.api.infra.exception.attachment.exception.AttachmentNotFoundException}.
 */
@Deprecated
public class AttachmentNotFoundException extends AttachmentException {
    public AttachmentNotFoundException(String message) {
        super(message);
    }
}
