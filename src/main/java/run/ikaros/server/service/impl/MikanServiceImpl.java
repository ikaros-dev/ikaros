package run.ikaros.server.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.annotation.Nonnull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import run.ikaros.server.exceptions.MikanRequestException;
import run.ikaros.server.service.MikanService;
import run.ikaros.server.utils.AssertUtils;

/**
 * @author li-guohao
 */
@Service
public class MikanServiceImpl implements MikanService {
    private final RestTemplate restTemplate;

    public MikanServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Nonnull
    @Override
    public String getAnimePageUrlByEpisodePageUrl(@Nonnull String episodePageUrl) {
        AssertUtils.notBlank(episodePageUrl, "episodePageUrl");
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
        return BASE_URL + href;
    }

    @Nonnull
    @Override
    public String getBgmTvSubjectPageUrlByAnimePageUrl(@Nonnull String animePageUrl) {
        AssertUtils.notBlank(animePageUrl, "animePageUrl");
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
        return targetElement.attr("href");
    }
}
