package run.ikaros.api.core.attachment.exception;

public class AttachmentExistsException extends RuntimeException {
    public AttachmentExistsException(String message) {
        super(message);
    }
}
