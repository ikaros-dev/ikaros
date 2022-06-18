package cn.liguohao.ikaros.entity;

import cn.liguohao.ikaros.file.ItemDataType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 条目数据项表
 *
 * @author li-guohao
 * @date 2022/06/19
 */
@Entity
@Table(name = "item_data")
public class ItemDataEntity extends BaseEntity {


    /**
     * 类型
     */
    private ItemDataType type;

    /**
     * 名称，不带后缀
     */
    private String name;

    /**
     * 后缀，比如.txt .mp4 或者 txt mp4等
     */
    private String postfix;

    /**
     * 访问路径，如是本地存储则是相对路径
     */
    private String url;

    public ItemDataType getType() {
        return type;
    }

    public ItemDataEntity setType(ItemDataType type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public ItemDataEntity setName(String name) {
        this.name = name;
        return this;
    }

    public String getPostfix() {
        return postfix;
    }

    public ItemDataEntity setPostfix(String postfix) {
        this.postfix = postfix;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public ItemDataEntity setUrl(String url) {
        this.url = url;
        return this;
    }
}
