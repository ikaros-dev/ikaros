package run.ikaros.api.core.attachment.exception;

public class AttachmentParentNotFoundException extends RuntimeException {
    public AttachmentParentNotFoundException(String message) {
        super(message);
    }
}
