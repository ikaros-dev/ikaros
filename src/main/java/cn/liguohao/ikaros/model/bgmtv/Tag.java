package cn.liguohao.ikaros.model.bgmtv;

/**
 * @author guohao
 * @date 2022/10/20
 */
public class Tag {
    private String name;
    private String count;

    public String getName() {
        return name;
    }

    public Tag setName(String name) {
        this.name = name;
        return this;
    }

    public String getCount() {
        return count;
    }

    public Tag setCount(String count) {
        this.count = count;
        return this;
    }
}
