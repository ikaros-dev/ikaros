package run.ikaros.server.core.attachment.service;

import java.util.List;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentDriverFetcher;

/**
 * 附件 SHA-1 后台计算服务.
 */
public interface AttachmentSha1Service {

    /**
     * 异步计算附件 SHA-1，并在附件未发生变化时回填结果.
     *
     * @param fetcher 附件驱动读取器
     * @param attachments 待计算 SHA-1 的附件
     */
    void calculateAsync(AttachmentDriverFetcher fetcher, List<Attachment> attachments);
}
