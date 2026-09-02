package run.ikaros.storage;

public enum DeliveryGrantRevocationLevel {
    IMMEDIATE,
    KEY_VERSION_BOUND,
    TTL_BOUNDED,
    NOT_REVOCABLE_BEFORE_EXPIRY
}
