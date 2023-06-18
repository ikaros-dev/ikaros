package run.ikaros.api.core.subject;

import org.pf4j.ExtensionPoint;
import run.ikaros.api.store.enums.SubjectSyncPlatform;

public interface SubjectSynchronizer extends ExtensionPoint {
    /**
     * 当前插件同步处理的对应的平台，比如是bgmtv的同步插件则返回对应的平台枚举.
     *
     * @see SubjectSyncPlatform
     */
    SubjectSyncPlatform getSyncPlatform();

    /**
     * 根据对应API从对应的平台拉取数据并转化成Ikaros的条目格式.
     *
     * @param platformId 对应平台的条目Id.
     * @return 从平台拉取的数据，进行格式转化。
     * @see Subject
     */
    Subject pull(String platformId);
}
