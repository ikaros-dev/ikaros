package run.ikaros.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import run.ikaros.server.constants.HttpConst;
import run.ikaros.server.core.service.OptionService;
import run.ikaros.server.core.service.RssService;
import run.ikaros.server.enums.OptionCategory;
import run.ikaros.server.enums.OptionMikan;
import run.ikaros.server.enums.OptionNetwork;
import run.ikaros.server.exceptions.RecordNotFoundException;
import run.ikaros.server.exceptions.RssOperateException;
import run.ikaros.server.model.dto.OptionNetworkDTO;
import run.ikaros.server.tripartite.mikan.model.MikanRssItem;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.RestTemplateUtils;
import run.ikaros.server.utils.StringUtils;
import run.ikaros.server.utils.SystemVarUtils;
import run.ikaros.server.utils.XmlUtils;

import jakarta.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author li-guohao
 */
@Service
public class RssServiceImpl implements RssService, InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(RssServiceImpl.class);

    private final OptionService optionService;
    private Proxy proxy = null;

    public RssServiceImpl(OptionService optionService) {
        this.optionService = optionService;
    }

    public void setProxy(@Nullable Proxy proxy) {
        this.proxy = proxy;
    }

    @Nonnull
    @Override
    public List<MikanRssItem> parseMikanMySubscribeRss(@Nonnull String mikanMySubscribeRssUrl) {
        AssertUtils.notBlank(mikanMySubscribeRssUrl, "mikanMySubscribeRssUrl");
        // 1. 先下载RSS的XML文件到缓存目录
        String rssXmlFilePath = downloadRssXmlFile(mikanMySubscribeRssUrl);
        // 2. 调用 DOM 库解析缓存文件
        List<MikanRssItem> mikanRssItemList = XmlUtils.parseMikanRssXmlFile(rssXmlFilePath);
        LOGGER.info("parse xml file get mikan rss item size: {}", mikanRssItemList.size());
        // 3. 删除对应的缓存文件
        if (StringUtils.isNotBlank(rssXmlFilePath)) {
            File file = new File(rssXmlFilePath);
            if (file.exists()) {
                file.delete();
                LOGGER.info("delete rss xml cache file for path: {}", rssXmlFilePath);
            }
        }
        return mikanRssItemList;


        //        List<MikanRssItem> mikanRssItemList = new ArrayList<>();
        //        SyndFeed syndFeed = RssUtils.parseFeed(mikanMySubscribeRssUrl);
        //        if (syndFeed == null || syndFeed.getEntries().size() == 0) {
        //            LOGGER.warn("fetch content fail, current rss subscribe address is {}",
        //                mikanMySubscribeRssUrl);
        //            return mikanRssItemList;
        //        }
        //
        //        for (SyndEntry syndEntry : syndFeed.getEntries()) {
        //
        //            String title = syndEntry.getTitle();
        //            List<SyndEnclosure> enclosures = syndEntry.getEnclosures();
        //            if (enclosures.isEmpty()) {
        //                LOGGER.warn("enclosures is empty, break current title={}", title);
        //                break;
        //            }
        //            SyndEnclosure syndEnclosure = enclosures.get(0);
        //            if (!"application/x-bittorrent".equalsIgnoreCase(syndEnclosure.getType())) {
        //            LOGGER.warn("enclosures first element type is not 'application/x-bittorrent',"
        //                    + " break current title={}", title);
        //                break;
        //            }
        //
        //            String torrentUrl = syndEnclosure.getUrl();
        //            MikanRssItem mikanRssItem = new MikanRssItem();
        //            mikanRssItem.setTitle(title)
        //                .setTorrentUrl(torrentUrl)
        //                .setEpisodePageUrl(syndEntry.getLink());
        //            mikanRssItemList.add(mikanRssItem);
        //        }
        //
        //        return mikanRssItemList;
    }

    @Override
    public String downloadRssXmlFile(@Nonnull String url) {
        AssertUtils.notBlank(url, "url");
        String cacheFilePath = SystemVarUtils.getOsCacheDirPath() + File.separator
            + UUID.randomUUID().toString().replace("-", "") + ".xml";

        InputStream inputStream = null;
        BufferedInputStream bufferedInputStream = null;
        OutputStream outputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        try {
            URLConnection urlConnection;
            if (proxy == null) {
                urlConnection = new URL(url).openConnection();
            } else {
                urlConnection = new URL(url).openConnection(proxy);
            }
            urlConnection.connect();
            inputStream = urlConnection.getInputStream();
            bufferedInputStream = new BufferedInputStream(inputStream);

            File cacheFile = new File(cacheFilePath);
            if (!cacheFile.exists()) {
                cacheFile.createNewFile();
            }
            outputStream = Files.newOutputStream(cacheFile.toPath());
            bufferedOutputStream = new BufferedOutputStream(outputStream);

            byte[] buffer = new byte[128];
            int len;
            while ((len = bufferedInputStream.read(buffer)) != -1) {
                bufferedOutputStream.write(buffer, 0, len);
            }
            LOGGER.info("download rss xml file to cache path: {}", cacheFilePath);
        } catch (IOException e) {
            String msg = "fail write rss url xml file bytes to cache path: " + cacheFilePath;
            LOGGER.warn(msg, e);
            throw new RssOperateException(msg, e);
        } finally {
            try {
                bufferedOutputStream.close();
                outputStream.close();
                bufferedInputStream.close();
                inputStream.close();
            } catch (IOException e) {
                throw new RssOperateException(e);
            }
        }
        return cacheFilePath;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        OptionNetworkDTO optionNetworkDTO = optionService.getOptionNetworkDTO();
        String proxyHttpHost = optionNetworkDTO.getProxyHttpHost();
        Integer proxyHttpPort = optionNetworkDTO.getProxyHttpPort();
        if (StringUtils.isNotBlank(proxyHttpHost) && Objects.nonNull(proxyHttpPort)) {
            setProxy(new Proxy(Proxy.Type.HTTP,
                new InetSocketAddress(proxyHttpHost,
                    proxyHttpPort)));
        }
    }
}
