package run.ikaros.server.model.bgmtv;

/**
 * @author guohao
 * @date 2022/10/20
 */
public class BgmTvTag {
    private String name;
    private String count;

    public String getName() {
        return name;
    }

    public BgmTvTag setName(String name) {
        this.name = name;
        return this;
    }

    public String getCount() {
        return count;
    }

    public BgmTvTag setCount(String count) {
        this.count = count;
        return this;
    }
}
