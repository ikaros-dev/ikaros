package run.ikaros.api.infra.exception.user;

import run.ikaros.api.infra.exception.NotFoundException;

public class UserAuthException extends NotFoundException {
    public UserAuthException(String message) {
        super(message);
    }
}
