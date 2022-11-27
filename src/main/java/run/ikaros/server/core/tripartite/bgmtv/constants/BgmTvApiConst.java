package run.ikaros.server.core.tripartite.bgmtv.constants;

/**
 * @see <a href="https://github.com/bangumi/api">bangumi/api</a>
 */
public interface BgmTvApiConst {
    String BASE = "https://api.bgm.tv";
    String SUBJECTS = BASE + "/v0/subjects";
    String EPISODES = BASE + "/v0/episodes";
    /**
     * 实测不支持中文日文查询
     */
    String OLD_SEARCH_SUBJECT = BASE + "/search/subject";
    /**
     * 实测不支持中文日文查询
     */
    String NEXT_SEARCH_SUBJECTS = BASE + "/v0/search/subjects";
    String ME = BASE + "/v0/me";

    Integer DEFAULT_OFFSET = 0;
    Integer DEFAULT_LIMIT = 50;
}
