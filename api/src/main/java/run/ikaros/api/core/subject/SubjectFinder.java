package run.ikaros.api.core.subject;

import run.ikaros.api.plugin.IkarosExtensionPoint;
import run.ikaros.api.store.enums.SubjectSyncPlatform;

public interface SubjectFinder extends IkarosExtensionPoint {
    /**
     * 当前插件同步处理的对应的平台，比如是bgmtv的同步插件则返回对应的平台枚举.
     *
     * @see SubjectSyncPlatform
     */
    SubjectSyncPlatform getSyncPlatform();

    Subject findByKeyword(String keyword);
}
