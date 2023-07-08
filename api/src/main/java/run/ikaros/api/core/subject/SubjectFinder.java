package run.ikaros.api.core.subject;

import org.pf4j.ExtensionPoint;
import run.ikaros.api.store.enums.SubjectSyncPlatform;

public interface SubjectFinder extends ExtensionPoint {
    /**
     * 当前插件同步处理的对应的平台，比如是bgmtv的同步插件则返回对应的平台枚举.
     *
     * @see SubjectSyncPlatform
     */
    SubjectSyncPlatform getSyncPlatform();

    Subject findByKeyword(String keyword);
}
