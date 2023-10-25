package run.ikaros.api.infra.exception.user;

import run.ikaros.api.infra.exception.IkarosNotFoundException;

public class UserIkarosNotFoundException extends IkarosNotFoundException {
    public UserIkarosNotFoundException(String message) {
        super(message);
    }
}
