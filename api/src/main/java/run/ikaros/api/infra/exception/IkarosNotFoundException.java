package run.ikaros.api.infra.exception;

public class IkarosNotFoundException extends IkarosException {
    public IkarosNotFoundException(String reason) {
        super(reason);
    }
}
