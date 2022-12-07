package run.ikaros.server.constants;

/**
 * @author guohao
 * @date 2022/10/20
 */
public interface IkarosConst {
    String HOME_PAGE = "https://ikaros.run";
    String ORGANIZATION_GITHUB_URL = "https://github.com/ikaros-dev";
    String REPO_GITHUB_NAME = "ikaros-dev/ikaros";
    String REPO_GITHUB_URL = "https://github.com/ikaros-dev/ikaros";

    // todo 目前设置成GitHub仓库地址，后续官网上线设置成官网地址
    // 当前 User-Agent格式 ikaros-dev/ikaros (https://github.com/ikaros-dev/ikaros)
    String REST_TEMPLATE_USER_AGENT = IkarosConst.REPO_GITHUB_NAME
        + " (" + IkarosConst.REPO_GITHUB_URL + ")";
}
