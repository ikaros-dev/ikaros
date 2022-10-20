package cn.liguohao.ikaros.model.bgmtv;

public enum EpisodeType {
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

    EpisodeType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
