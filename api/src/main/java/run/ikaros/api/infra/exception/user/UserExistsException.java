package run.ikaros.api.infra.exception.user;

import run.ikaros.api.infra.exception.NotFoundException;

public class UserExistsException extends NotFoundException {
    public UserExistsException(String message) {
        super(message);
    }

    public UserExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
