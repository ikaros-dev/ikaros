package run.ikaros.server.constants;

import run.ikaros.server.enums.FilePlace;
import run.ikaros.server.enums.MailProtocol;
import run.ikaros.server.enums.NotifyMethod;

public interface DefaultConst {
    String OPTION_APP_IS_INIT = Boolean.TRUE.toString();
    String OPTION_APP_THEME = "simple";
    String OPTION_APP_ENABLE_AUTO_ANIME_SUB_TASK = Boolean.FALSE.toString();
    String OPTION_APP_ENABLE_GENERATE_MEDIA_DIR_TASK = Boolean.FALSE.toString();


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
    String OPTION_NETWORK_CONNECT_TIMEOUT = "5000";
    String OPTION_NETWORK_READ_TIMEOUT = "5000";


    String OPTION_BGMTV_ENABLE_PROXY = Boolean.FALSE.toString();
    String OPTION_BGMTV_ACCESS_TOKEN = "";

    String OPTION_MIKAN_MY_SUBSCRIBE_RSS = "";
    String OPTION_MIKAN_ENABLE_PROXY = Boolean.FALSE.toString();


    String OPTION_JELLYFIN_MEDIA_DIR_PATH = "/media";


    String OPTION_QBITTORRENT_API_BASE = "/api/v2";
    String OPTION_QBITTORRENT_CATEGORY = "ikaros";
    String OPTION_QBITTORRENT_CATEGORY_SAVE_PATH = "/downloads/ikaros/";
    String OPTION_QBITTORRENT_URL = "";
    String OPTION_QBITTORRENT_ENABLE_AUTH = Boolean.TRUE.toString();
    String OPTION_QBITTORRENT_USERNAME = "admin";
    String OPTION_QBITTORRENT_PASSWORD = "adminadmin";

    String OPTION_NOTIFY_MAIL_ENABLE = Boolean.FALSE.toString();
    String OPTION_NOTIFY_MAIL_PROTOCOL = MailProtocol.SMTP.name();
    String OPTION_NOTIFY_MAIL_SMTP_HOST = "";
    String OPTION_NOTIFY_MAIL_SMTP_PORT = "";
    String OPTION_NOTIFY_MAIL_SMTP_ACCOUNT = "";
    String OPTION_NOTIFY_MAIL_SMTP_PASSWORD = "";
    String OPTION_NOTIFY_MAIL_SMTP_ACCOUNT_ALIAS = "Ikaros";



}
