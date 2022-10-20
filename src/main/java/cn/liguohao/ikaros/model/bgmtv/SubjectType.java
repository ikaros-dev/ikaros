package cn.liguohao.ikaros.model.bgmtv;

/**
 * 条目类型
 *
 * @author <a href="http://liguohao.cn" target="_blank">liguohao</a>
 * @date 2022/4/4
 */
public enum SubjectType {
    /**
     * 书籍
     */
    BOOK(1),

    /**
     * 动画
     */
    ANIME(2),

    /**
     * 音乐
     */
    MUSIC(3),

    /**
     * 游戏
     */
    GAME(4),

    /**
     * 三次元
     */
    REAL(6)
    ;
    private final int code;

    SubjectType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
