package run.ikaros.server.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.annotation.Nonnull;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import run.ikaros.server.core.service.OptionService;
import run.ikaros.server.entity.OptionEntity;
import run.ikaros.server.enums.OptionBgmTv;
import run.ikaros.server.enums.OptionCategory;
import run.ikaros.server.enums.OptionMikan;
import run.ikaros.server.exceptions.MikanRequestException;
import run.ikaros.server.core.service.MikanService;
import run.ikaros.server.exceptions.RecordNotFoundException;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.RestTemplateUtils;

/**
 * @author li-guohao
 */
@Service
public class MikanServiceImpl implements MikanService, InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(MikanServiceImpl.class);
    private RestTemplate restTemplate = new RestTemplate();
    private final OptionService optionService;

    public MikanServiceImpl(OptionService optionService) {
        this.optionService = optionService;
    }

    @Override
    public void setRestTemplate(@Nonnull RestTemplate restTemplate) {
        AssertUtils.notNull(restTemplate, "restTemplate");
        this.restTemplate = restTemplate;
    }

    @Nonnull
    @Override
    public String getAnimePageUrlByEpisodePageUrl(@Nonnull String episodePageUrl) {
        AssertUtils.notBlank(episodePageUrl, "episodePageUrl");
        LOGGER.debug("starting get anime page url by episode page url");
        byte[] bytes = restTemplate.getForObject(episodePageUrl, byte[].class);
        if (bytes == null) {
            throw new MikanRequestException("not found episode page url response data, "
                + "for episode page url: " + episodePageUrl);
        }
        String content = new String(bytes, StandardCharsets.UTF_8);
        Document document = Jsoup.parse(content);
        Element element = document.selectFirst("#sk-container .bangumi-title a");
        if (element == null) {
            throw new MikanRequestException(
                "not found element, for episode page url: " + episodePageUrl);
        }
        String href = element.attr("href");
        LOGGER.debug("completed get anime page url by episode page url");
        return BASE_URL + href;
    }

    @Nonnull
    @Override
    public String getBgmTvSubjectPageUrlByAnimePageUrl(@Nonnull String animePageUrl) {
        AssertUtils.notBlank(animePageUrl, "animePageUrl");
        LOGGER.debug("starting get bgm tv subject page url by anime page url");
        byte[] bytes = restTemplate.getForObject(animePageUrl, byte[].class);
        if (bytes == null) {
            throw new MikanRequestException("not found anime page url response data, "
                + "for anime page url: " + animePageUrl);
        }
        String content = new String(bytes, StandardCharsets.UTF_8);
        Document document = Jsoup.parse(content);
        Elements elements = document.select("#sk-container .bangumi-info");
        Element targetElement = null;
        for (Element element : elements) {
            List<TextNode> textNodes = element.textNodes();
            if (textNodes.isEmpty()) {
                continue;
            }
            TextNode textNode = textNodes.get(0);
            String val = textNode.text();
            if (val.contains("Bangumi")) {
                targetElement = element.selectFirst("a");
            }
        }

        if (targetElement == null) {
            throw new MikanRequestException(
                "not found element, for anime page url: " + animePageUrl);
        }
        LOGGER.debug("completed get bgm tv subject page url by anime page url");
        return targetElement.attr("href");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        OptionEntity optionEntity =
            optionService.findOptionValueByCategoryAndKey(OptionCategory.MIKAN,
                OptionMikan.ENABLE_PROXY.name());
        boolean enableHttpProxy = false;
        if (optionEntity != null
            && Boolean.TRUE.toString().equalsIgnoreCase(optionEntity.getValue())) {
            enableHttpProxy = true;
        }
        if (enableHttpProxy) {
            String httpProxyHost = optionService.getOptionNetworkHttpProxyHost();
            String httpProxyPort = optionService.getOptionNetworkHttpProxyPort();
            setRestTemplate(
                RestTemplateUtils.buildHttpProxyRestTemplate(httpProxyHost, httpProxyPort));
        }
    }
}
