package run.ikaros.server.custom.exception;

import org.springframework.http.HttpStatus;
import run.ikaros.server.custom.GroupVersionKind;

public class CustomSchemeNotFoundException extends CustomException {
    public CustomSchemeNotFoundException(String message) {
        super(message);
    }

    public CustomSchemeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public CustomSchemeNotFoundException(GroupVersionKind gvk) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "Scheme not found for " + gvk, null, null,
            new Object[] {gvk});
    }
}
