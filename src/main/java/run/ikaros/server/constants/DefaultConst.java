package run.ikaros.server.constants;

import run.ikaros.server.enums.FilePlace;

public interface DefaultConst {
    String OPTION_APP_IS_INIT = Boolean.TRUE.toString();
    String OPTION_APP_THEME = "simple";


    String OPTION_COMMON_TITLE = "ikaros cms system";
    String OPTION_COMMON_DESCRIPTION = "专注于ACGMN的内容管家(CMS)，——伊卡洛斯。";
    String OPTION_COMMON_ADDRESS = "/";
    String OPTION_COMMON_LOGO = "/logo.jpg";
    String OPTION_COMMON_FAVICON = "/favicon.ico";
    String OPTION_COMMON_HEADER = "";
    String OPTION_COMMON_FOOTER = "";
    String OPTION_COMMON_STATISTICS_CODE = "";


    String OPTION_SEO_HIDE_FOR_SEARCH_ENGINE = Boolean.FALSE.toString();
    String OPTION_SEO_KEYWORDS = "ikaros;cms;opensource";
    String OPTION_SEO_SITE_DESCRIPTION = "ikaros, an open source acgmn cms application.";


    String OPTION_FILE_PLACE_SELECT = FilePlace.LOCAL.name();


    String OPTION_NETWORK_PROXY_HTTP_HOST = "";
    String OPTION_NETWORK_PROXY_HTTP_PORT = "";


    String OPTION_BGMTV_API_BASE = "https://api.bgm.tv";
    String OPTION_BGMTV_API_SUBJECTS = "/v0/subjects";
    String OPTION_BGMTV_API_EPISODES = "/v0/episodes";
    String OPTION_BGMTV_API_SEARCH_SUBJECT = "/search/subject";
    String OPTION_BGMTV_ENABLE_PROXY = Boolean.FALSE.toString();


    String OPTION_MIKAN_MY_SUBSCRIBE_RSS = "";
    String OPTION_MIKAN_ENABLE_PROXY = Boolean.FALSE.toString();


    String OPTION_JELLYFIN_MEDIA_DIR_PATH = "/media";


    String OPTION_QBITTORRENT_API_BASE = "/api/v2";
    String OPTION_QBITTORRENT_CATEGORY = "ikaros";
    String OPTION_QBITTORRENT_CATEGORY_SAVE_PATH = "/downloads/ikaros/";
    String OPTION_QBITTORRENT_URL = "";
    String OPTION_QBITTORRENT_ENABLE_AUTH = "false";
    String OPTION_QBITTORRENT_USERNAME = "";
    String OPTION_QBITTORRENT_PASSWORD = "";



}
