package run.ikaros.api.infra.exception.user;

import run.ikaros.api.infra.exception.NotFoundException;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
