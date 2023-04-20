package run.ikaros.api.store.enums;

public enum CollectionStatus {
    /**
     * Not collection.
     */
    NOT(0),
    /**
     * Wist watch.
     */
    WISH(1),
    /**
     * Watching.
     */
    DOING(2),
    /**
     * Watch done.
     */
    DONE(3),
    /**
     * No time to watch it.
     */
    SHELVE(4),
    /**
     * Discard it.
     */
    DISCARD(5);

    private final int code;

    CollectionStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
