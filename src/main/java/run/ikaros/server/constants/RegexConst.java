package run.ikaros.server.constants;

/**
 * @author guohao
 * @date 2022/10/02
 */
public interface RegexConst {
    String EMAIL = "^([a-zA-Z0-9]*[-_]?[a-zA-Z0-9]+)*@([a-zA-Z0-9]*[-_]?"
        + "[a-zA-Z0-9]+)+[\\\\.][A-Za-z]{2,3}([\\\\.][A-Za-z]{2})?$";

    String TELEPHONE = "0\\\\d{2,3}-\\\\d{7,8}";

    String MOBILE_PHONE_NUMBER = "^((13[0-9])|(15[^4,\\\\D])|(18[0,5-9]))\\\\d{8}$";

    String FILE_NAME_TAG_EPISODE_SEQUENCE = "\\[[0-9]{1,2}\\]";
    String FILE_NAME_TAG = "\\[[A-Za-z0-9_-]+\\]";
    String FILE_POSTFIX = "\\.[A-Za-z0-9_-]+$";
    String NUMBER_EPISODE_SEQUENCE = "[0-9]{1,2}";

}
