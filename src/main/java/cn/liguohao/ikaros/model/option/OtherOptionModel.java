package cn.liguohao.ikaros.model.option;

import cn.liguohao.ikaros.common.constants.OptionConstants.Category;
import cn.liguohao.ikaros.common.constants.OptionConstants.Init.Other;
import cn.liguohao.ikaros.core.model.OptionModel;

/**
 * @author guohao
 * @date 2022/10/19
 */
public class OtherOptionModel implements OptionModel {
    private String category = Category.OTHER;
    private String customerGlobalHeader = Other.CUSTOMER_GLOBAL_HEADER[1];
    private String statisticsCode = Other.STATISTICS_CODE[1];

    @Override
    public String getCategory() {
        return category;
    }

    public OtherOptionModel setCategory(String category) {
        this.category = category;
        return this;
    }

    public String getCustomerGlobalHeader() {
        return customerGlobalHeader;
    }

    public OtherOptionModel setCustomerGlobalHeader(String customerGlobalHeader) {
        this.customerGlobalHeader = customerGlobalHeader;
        return this;
    }

    public String getStatisticsCode() {
        return statisticsCode;
    }

    public OtherOptionModel setStatisticsCode(String statisticsCode) {
        this.statisticsCode = statisticsCode;
        return this;
    }
}
