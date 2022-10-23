package cn.liguohao.ikaros.model.bgmtv;

public enum BgmTvEpisodeType {
    /**
     * 正篇
     */
    POSITIVE(0),

    /**
     * 特别篇
     */
    SPECIAL(1),


    OP(2),


    ED(3),


    PV(4)
    ;
    private final int code;

    BgmTvEpisodeType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
