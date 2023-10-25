package run.ikaros.api.core.attachment.exception;

/**
 * Please use {@link run.ikaros.api.infra.exception.attachment.exception.AttachmentExistsException}.
 */
@Deprecated
public class AttachmentExistsException extends AttachmentException {
    public AttachmentExistsException(String message) {
        super(message);
    }
}
