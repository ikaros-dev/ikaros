package run.ikaros.server.init.option;

import run.ikaros.server.enums.OptionCategory;

import javax.annotation.Nonnull;

/**
 * @author li-guohao
 */
public class OtherPresetOption implements PresetOption {

    private String customerGlobalHeader = "";
    private String statisticsCode = "";

    @Nonnull
    @Override
    public OptionCategory getCategory() {
        return OptionCategory.OTHER;
    }

    public String getCustomerGlobalHeader() {
        return customerGlobalHeader;
    }

    public OtherPresetOption setCustomerGlobalHeader(String customerGlobalHeader) {
        this.customerGlobalHeader = customerGlobalHeader;
        return this;
    }

    public String getStatisticsCode() {
        return statisticsCode;
    }

    public OtherPresetOption setStatisticsCode(String statisticsCode) {
        this.statisticsCode = statisticsCode;
        return this;
    }

}
