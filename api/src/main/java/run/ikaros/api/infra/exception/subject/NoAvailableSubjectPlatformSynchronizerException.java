package run.ikaros.api.infra.exception.subject;

import run.ikaros.api.infra.exception.IkarosException;

public class NoAvailableSubjectPlatformSynchronizerException extends IkarosException {
    public NoAvailableSubjectPlatformSynchronizerException(String message) {
        super(message);
    }

}
