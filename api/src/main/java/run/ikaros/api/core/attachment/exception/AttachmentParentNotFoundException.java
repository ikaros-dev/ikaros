package run.ikaros.api.core.attachment.exception;

/**
 * Please use
 * {@link run.ikaros.api.infra.exception.attachment.exception.AttachmentParentNotFoundException}.
 */
@Deprecated
public class AttachmentParentNotFoundException extends AttachmentException {
    public AttachmentParentNotFoundException(String message) {
        super(message);
    }
}
