package run.ikaros.server.store.enums;

public enum CollectionStatus {
    /**
     * 未收藏.
     */
    NOT(0),
    /**
     * 想看.
     */
    WISH(1),
    /**
     * 在看.
     */
    DOING(2),
    /**
     * 看过.
     */
    DONE(3),
    /**
     * 搁置.
     */
    SHELVE(4),
    /**
     * 抛弃.
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
