package run.ikaros.server.store.enums;

public enum SubjectType {
    OTHER(1),
    ANIME(2),
    COMIC(3),
    GAME(4),
    MUSIC(5),
    NOVEL(6),
    REAL(7);

    private final int code;

    SubjectType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
