package run.ikaros.api.core.attachment.exception;

import run.ikaros.api.infra.exception.IkarosException;

/**
 * Please use {@link run.ikaros.api.infra.exception.attachment.exception.AttachmentException}.
 */
@Deprecated
public class AttachmentException extends IkarosException {


    public AttachmentException(String message) {
        super(message);
    }

    public AttachmentException(String message, Throwable cause) {
        super(message, cause);
    }

}
