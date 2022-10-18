package cn.liguohao.ikaros.model.option;

import cn.liguohao.ikaros.common.constants.OptionConstants.Category;
import cn.liguohao.ikaros.common.constants.OptionConstants.Init.Common;
import cn.liguohao.ikaros.core.model.OptionModel;

/**
 * @author guohao
 * @date 2022/10/19
 */
public class CommonOptionModel implements OptionModel {
    private String category = Category.COMMON;
    private String title = Common.TITLE[1];
    private String address = Common.ADDRESS[1];
    private String logo = Common.LOGO[1];
    private String favicon = Common.FAVICON[1];
    private String footer = Common.FOOTER[1];

    @Override
    public String getCategory() {
        return category;
    }

    public CommonOptionModel setCategory(String category) {
        this.category = category;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public CommonOptionModel setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public CommonOptionModel setAddress(String address) {
        this.address = address;
        return this;
    }

    public String getLogo() {
        return logo;
    }

    public CommonOptionModel setLogo(String logo) {
        this.logo = logo;
        return this;
    }

    public String getFavicon() {
        return favicon;
    }

    public CommonOptionModel setFavicon(String favicon) {
        this.favicon = favicon;
        return this;
    }

    public String getFooter() {
        return footer;
    }

    public CommonOptionModel setFooter(String footer) {
        this.footer = footer;
        return this;
    }
}
