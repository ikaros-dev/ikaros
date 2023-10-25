package run.ikaros.api.infra.exception;

public class NotFoundException extends IkarosException {
    public NotFoundException(String reason) {
        super(reason);
    }
}
