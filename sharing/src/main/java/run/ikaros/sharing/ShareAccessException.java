package run.ikaros.sharing;

/** A safe, user-facing share access failure that never contains the token. */
public class ShareAccessException extends RuntimeException {
    private final ShareAccessFailureReason reason;

    public ShareAccessException(ShareAccessFailureReason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public ShareAccessFailureReason reason() {
        return reason;
    }

    public String code() {
        return "share.access." + reason.name().toLowerCase(java.util.Locale.ROOT);
    }
}
