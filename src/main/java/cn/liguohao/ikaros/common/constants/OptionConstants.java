package cn.liguohao.ikaros.common.constants;

import cn.liguohao.ikaros.model.entity.OptionEntity;
import cn.liguohao.ikaros.model.file.IkarosFile;
import cn.liguohao.ikaros.model.option.AppOptionModel;

/**
 * @author guohao
 * @date 2022/10/18
 */
public interface OptionConstants {

    String MODEL_CLASS_PACKAGE_NAME = AppOptionModel.class.getPackageName();

    interface Category {
        String DEFAULT = "default";
        String APP = Init.App.class.getSimpleName().toLowerCase();
        String COMMON = Init.Common.class.getSimpleName().toLowerCase();
        String SEO = Init.Seo.class.getSimpleName().toLowerCase();
        String FILE = Init.File.class.getSimpleName().toLowerCase();
        String OTHER = Init.Other.class.getSimpleName().toLowerCase();
    }

    interface Type {
        OptionEntity.Type DEFAULT = OptionEntity.Type.INTERNAL;
    }

    interface Init {

        interface App {
            String[] IS_INIT = {"isInit", "true"};
        }

        interface Common {
            String[] TITLE = {"title", "ikaros cms system"};
            String[] ADDRESS = {"address", "/"};
            String[] LOGO = {"logo", "/logo.jpg"};
            String[] FAVICON = {"favicon", "/favicon.ico"};
            String[] FOOTER = {"footer", ""};
        }

        interface Seo {
            String[] HIDE_FOR_SE = {"hideForSearchEngine", "false"};
            String[] KEYWORDS = {"keywords", "ikaros;cms;opensource"};
            String[] SITE_DESCRIPTION =
                {"siteDescription", "ikaros, an open source acgmn cms application."};
        }

        interface File {
            String[] PLACE_SELECT = {"placeSelect", IkarosFile.Place.LOCAL.name()};
        }

        interface Other {
            String[] CUSTOMER_GLOBAL_HEADER = {"customerGlobalHeader", ""};
            String[] STATISTICS_CODE = {"statisticsCode", ""};
        }

    }

}
