package run.ikaros.sharing;

/** Stable, non-secret reasons a share token cannot be redeemed. */
public enum ShareAccessFailureReason {
    MISSING_TOKEN,
    INVALID_TOKEN,
    REVOKED,
    EXPIRED
}
