package run.ikaros.api.infra.exception.custom;

import run.ikaros.api.custom.GroupVersionKind;

public class CustomSchemeNotFoundException extends CustomException {

    public CustomSchemeNotFoundException(GroupVersionKind gvk) {
        super("Scheme not found for " + gvk);
    }
}
