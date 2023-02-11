package run.ikaros.server.store.enums;

import java.util.Arrays;
import org.springframework.util.Assert;

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

    /**
     * Get {@link SubjectType} by type code.
     *
     * @param code type code, a {@link Integer} instance
     * @return {@link SubjectType} instance
     */
    public static SubjectType codeOf(Integer code) {
        Assert.isTrue(code >= 0, "'code' mus gt 0");
        return Arrays.stream(SubjectType.values())
            .filter(subjectType -> code.equals(subjectType.getCode()))
            .findFirst().orElse(SubjectType.OTHER);
    }
}
