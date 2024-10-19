package run.ikaros.api.core.subject;

import java.util.List;
import org.pf4j.ExtensionPoint;
import run.ikaros.api.core.character.Character;
import run.ikaros.api.core.person.Person;
import run.ikaros.api.core.tag.Tag;
import run.ikaros.api.store.enums.SubjectSyncPlatform;

public interface SubjectSynchronizer extends ExtensionPoint {
    /**
     * 当前插件同步处理的对应的平台，比如是bgmtv的同步插件则返回对应的平台枚举.
     *
     * @see SubjectSyncPlatform
     */
    SubjectSyncPlatform getSyncPlatform();

    /**
     * 根据平台ID获取条目.
     */
    Subject fetchSubjectWithPlatformId(String platformId);

    /**
     * 根据平台ID获取剧集.
     */
    List<Episode> fetchEpisodesWithPlatformId(String platformId);

    /**
     * 根据平台ID获取标签.
     */
    List<Tag> fetchTagsWithPlatformId(String platformId);

    /**
     * 根据平台ID获取角色.
     */
    List<Person> fetchPersonsWithPlatformId(String platformId);

    /**
     * 根据平台ID获取人物.
     */
    List<Character> fetchCharactersWithPlatformId(String platformId);
}
