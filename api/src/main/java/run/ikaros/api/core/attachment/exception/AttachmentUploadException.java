package run.ikaros.api.core.attachment.exception;

/**
 * Please use
 * {@link run.ikaros.api.infra.exception.attachment.exception.AttachmentUploadException}.
 */
@Deprecated
public class AttachmentUploadException extends AttachmentException {
    public AttachmentUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
