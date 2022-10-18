package cn.liguohao.ikaros.common.constants;

import cn.liguohao.ikaros.model.entity.OptionEntity;
import cn.liguohao.ikaros.model.file.IkarosFile;

/**
 * @author guohao
 * @date 2022/10/18
 */
public interface OptionConstants {


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
            String[] IS_INIT = {"is init", "true"};
        }

        interface Common {
            String[] TITLE = {"title", "ikaros cms system"};
            String[] ADDRESS = {"address", "default address url"};
            String[] LOGO = {"logo", "default logo url"};
            String[] FAVICON = {"favicon", "default favicon url"};
            String[] FOOTER = {"footer", "default footer content"};
        }

        interface Seo {
            String[] HIDE_FOR_SE = {"hide for search engine", "false"};
            String[] KEYWORDS = {"keywords", "keyword1;keyword2;keyword3"};
            String[] SITE_DESCRIPTION =
                {"site description", "ikaros, an open source acgmn cms application."};
        }

        interface File {
            String[] PLACE_SELECT = {"place select", IkarosFile.Place.LOCAL.name()};
        }

        interface Other {
            String[] CUSTOMER_GLOBAL_HEADER = {"customer global header", ""};
            String[] STATISTICS_CODE = {"statistics code", ""};
        }

    }

}
