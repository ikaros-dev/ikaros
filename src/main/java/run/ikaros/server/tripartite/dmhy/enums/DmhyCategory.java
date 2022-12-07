package run.ikaros.server.tripartite.dmhy.enums;

import lombok.Getter;

@Getter
public enum DmhyCategory {
    /**
     * 其他
     */
    OTHER(1),
    /**
     * 動畫
     */
    ANIME(2),
    /**
     * 季度全集
     */
    SEASON_COLLECTION(31)
    ;
    private final int code;

    DmhyCategory(int code) {
        this.code = code;
    }
}
