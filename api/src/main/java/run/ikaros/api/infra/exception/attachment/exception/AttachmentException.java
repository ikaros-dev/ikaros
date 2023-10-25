package run.ikaros.api.infra.exception.attachment.exception;

import run.ikaros.api.infra.exception.IkarosException;

public class AttachmentException extends IkarosException {


    public AttachmentException(String message) {
        super(message);
    }

    public AttachmentException(String message, Throwable cause) {
        super(message, cause);
    }

}
