package run.ikaros.server.core.webclient;

public interface WebClientConst {
    String HOME_PAGE = "https://ikaros.run";
    String REPO_GITHUB_NAME = "ikaros-dev/ikaros";

    // 当前 User-Agent格式 ikaros-dev/ikaros (https://ikaros.run)
    String REST_TEMPLATE_USER_AGENT = REPO_GITHUB_NAME + " (" + HOME_PAGE + ")";
    String TOKEN_PREFIX = "Bearer ";
}
