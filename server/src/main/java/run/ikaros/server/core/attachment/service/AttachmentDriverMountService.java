package run.ikaros.server.core.attachment.service;

import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.AttachmentDriverEntity;

/**
 * 附件驱动挂载服务，负责维护目录映射和根附件记录.
 */
public interface AttachmentDriverMountService {

    /**
     * 挂载驱动目录并恢复对应的根附件.
     *
     * @param driver 附件驱动
     * @return 挂载完成信号
     */
    Mono<Void> mount(AttachmentDriverEntity driver);

    /**
     * 卸载驱动目录并隐藏对应的根附件.
     *
     * @param driver 附件驱动
     * @return 卸载完成信号
     */
    Mono<Void> unmount(AttachmentDriverEntity driver);

    /**
     * 将已启用驱动从旧目录同步重绑定到新目录.
     *
     * @param previousDriver 修改前的附件驱动
     * @param currentDriver 修改后的附件驱动
     * @return 重绑定完成信号
     */
    Mono<Void> rebind(AttachmentDriverEntity previousDriver,
                      AttachmentDriverEntity currentDriver);
}
