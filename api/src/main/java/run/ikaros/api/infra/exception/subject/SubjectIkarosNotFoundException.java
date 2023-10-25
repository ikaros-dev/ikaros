package run.ikaros.api.infra.exception.subject;

import run.ikaros.api.infra.exception.IkarosNotFoundException;

public class SubjectIkarosNotFoundException extends IkarosNotFoundException {
    public SubjectIkarosNotFoundException(String message) {
        super(message);
    }
}
