package run.ikaros.server.constants;

/**
 * @author guohao
 * @date 2022/10/21
 */
public interface InitConst {
    Long ROOT_ID = 0L;
    String[] PRESET_RESOURCE_TYPES = {"FILE",
        "VIDEO", "VOICE", "PICTURE", "DOCUMENT"};
    String[] PRESET_BOX_TYPES = {"EPISODE", "POSITIVE", "SPECIAL", "OP", "ED", "PV", "CM",
        "ANIME", "COMIC", "GAME", "MUSIC", "NOVELS",
        "IP", "OTHER"};
}
