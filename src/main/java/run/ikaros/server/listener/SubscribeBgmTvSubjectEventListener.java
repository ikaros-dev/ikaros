package run.ikaros.server.listener;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import run.ikaros.server.core.service.AnimeService;
import run.ikaros.server.event.SubscribeBgmTvSubjectEvent;

import javax.annotation.Nonnull;

/**
 * <ol>
 *     <li>查询剧集信息</li>
 *     <li>根据关键词查询萌番组换取标签</li>
 *     <li>根据标签查询萌番组换取Torrent列表</li>
 *     <li>根据用户配置的优先级别筛选种子</li>
 *     <li>下载筛选后的种子</li>
 *     <li>将筛选后的种子添加到下载模块</li>
 *     <li>监听下载完成</li>
 *     <li>下载完成后，创建硬链接，匹配对应的动漫剧集</li>
 *     <li>给用户发送通知，告知更新</li>
 * </ol>
 */
@Component
public class SubscribeBgmTvSubjectEventListener implements
    ApplicationListener<SubscribeBgmTvSubjectEvent> {

    private final AnimeService animeService;

    public SubscribeBgmTvSubjectEventListener(AnimeService animeService) {
        this.animeService = animeService;
    }

    @Override
    public void onApplicationEvent(@Nonnull SubscribeBgmTvSubjectEvent event) {
        // todo impl all steps
    }
}
