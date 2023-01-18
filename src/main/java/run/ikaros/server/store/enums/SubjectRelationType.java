package run.ikaros.server.store.enums;

public enum SubjectRelationType {
    OTHER(1),
    ANIME(2),
    COMIC(3),
    GAME(4),
    MUSIC(5),
    NOVEL(6),
    REAL(7),
    /**
     * 前传.
     */
    BEFORE(8),
    /**
     * 后传.
     */
    AFTER(9),
    /**
     * 相同世界观.
     */
    SAME_WORLDVIEW(10),
    /**
     * 原声集.
     */
    ORIGINAL_SOUND_TRACK(11);
    // todo more subject relation type.

    private final int code;

    SubjectRelationType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
