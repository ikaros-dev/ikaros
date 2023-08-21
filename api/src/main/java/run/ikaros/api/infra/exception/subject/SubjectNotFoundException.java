package run.ikaros.api.infra.exception.subject;

import run.ikaros.api.infra.exception.NotFoundException;

public class SubjectNotFoundException extends NotFoundException {
    public SubjectNotFoundException(String message) {
        super(message);
    }
}
