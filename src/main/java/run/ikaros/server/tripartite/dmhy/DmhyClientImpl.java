package run.ikaros.server.tripartite.dmhy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import run.ikaros.server.constants.IkarosConst;
import run.ikaros.server.core.service.OptionService;
import run.ikaros.server.exceptions.RuntimeIkarosException;
import run.ikaros.server.model.dto.OptionNetworkDTO;
import run.ikaros.server.tripartite.dmhy.enums.DmhyCategory;
import run.ikaros.server.tripartite.dmhy.model.DmhyRssItem;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.RestTemplateUtils;
import run.ikaros.server.utils.StringUtils;
import run.ikaros.server.utils.XmlUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class DmhyClientImpl implements DmhyClient, InitializingBean {
    private final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    private RestTemplate restTemplate = RestTemplateUtils.buildRestTemplate();
    private static final HttpHeaders headers = new HttpHeaders();
    private Proxy proxy;

    private final OptionService optionService;

    static {
        headers.set(HttpHeaders.USER_AGENT, IkarosConst.REST_TEMPLATE_USER_AGENT);
    }

    public DmhyClientImpl(OptionService optionService) {
        this.optionService = optionService;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
        this.restTemplate = RestTemplateUtils.buildProxyRestTemplate(proxy);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        OptionNetworkDTO optionNetworkDTO = optionService.getOptionNetworkDTO();
        String proxyHttpHost = optionNetworkDTO.getProxyHttpHost();
        Integer proxyHttpPort = optionNetworkDTO.getProxyHttpPort();
        if (StringUtils.isNotBlank(proxyHttpHost) && proxyHttpPort != null) {
            Proxy proxy =
                new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHttpHost, proxyHttpPort));
            setProxy(proxy);
        }
    }

    @Override
    public List<DmhyRssItem> findRssItems(@Nonnull String keyword,
                                          @Nullable DmhyCategory category) {
        AssertUtils.notBlank(keyword, "keyword");
        log.debug("find rss items with keyword={} and category={}", keyword, category);
        List<DmhyRssItem> dmhyRssItemList = new ArrayList<>();

        UriComponentsBuilder uriComponentsBuilder =
            UriComponentsBuilder.fromHttpUrl(Api.RSS_BAS_URL)
                .queryParam("keyword", keyword);
        if (category != null) {
            uriComponentsBuilder.queryParam("sort_id", category.getCode());
        }

        File cacheXmlFile =
            new File(XmlUtils.downloadRssXmlFile(uriComponentsBuilder.toUriString(), proxy));
        try {
            // ResponseEntity<Resource> entity =
            //     restTemplate.getForEntity(uriComponentsBuilder.toUriString(), Resource.class);
            // InputStream inputStream = entity.getBody().getInputStream();

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(cacheXmlFile);
            Element element = document.getDocumentElement();
            Node firstChild = element.getFirstChild();
            if (firstChild.getNodeName().contains("#text")) {
                firstChild = firstChild.getNextSibling();
            }
            NodeList childNodes = firstChild.getChildNodes();
            for (int j = 0; j < childNodes.getLength(); j++) {
                Node item = childNodes.item(j);
                String nodeName = item.getNodeName();
                // LOGGER.debug("parse channel item name: {}", nodeName);
                if ("item".equalsIgnoreCase(nodeName)) {
                    dmhyRssItemList.add(handlerItemNode(item));
                }
            }
        } catch (Exception exception) {
            throw new RuntimeIkarosException("find dmhy rss items fail", exception);
        } finally {
            if (cacheXmlFile.exists()) {
                cacheXmlFile.delete();
            }
        }

        // 动漫花园，分类是动画的话，全集也会被查询出来，所以这里做下过滤
        if (category != null) {
            dmhyRssItemList = dmhyRssItemList.stream()
                .filter(dmhyRssItem -> category.equals(dmhyRssItem.getCategory()))
                .toList();
        }

        log.debug("end find rss items with keyword={} and category={}, find size={}",
            keyword, category, dmhyRssItemList.size());
        return dmhyRssItemList;
    }

    /**
     * <pre>
     *     <![CDATA[ 動畫 ]]>  ==>  [ 動畫 ]
     * </pre>
     */
    private String removeCDATA(String str) {
        return str.replace("<![CDATA", "")
            .replace("]>", "").trim();
    }

    private DmhyRssItem handlerItemNode(Node item) {
        DmhyRssItem dmhyRssItem = new DmhyRssItem();

        NodeList childNodes = item.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            String nodeName = node.getNodeName();
            String textContent = node.getTextContent();
            if (textContent.contains("![CDATA")) {
                textContent = removeCDATA(textContent);
            }

            if ("title".equalsIgnoreCase(nodeName)) {
                dmhyRssItem.setTitle(textContent);
            }
            if ("link".equalsIgnoreCase(nodeName)) {
                dmhyRssItem.setLink(textContent);
            }
            if ("pubDate".equalsIgnoreCase(nodeName)) {
                dmhyRssItem.setPubDate(textContent);
            }
            if ("description".equalsIgnoreCase(nodeName)) {
                dmhyRssItem.setDescription(textContent);
            }
            if ("enclosure".equalsIgnoreCase(nodeName)) {
                String url = node.getAttributes().getNamedItem("url").getNodeValue();
                dmhyRssItem.setMagnetUrl(url);
            }
            if ("author".equalsIgnoreCase(nodeName)) {
                dmhyRssItem.setAuthor(textContent);
            }
            if ("category".equalsIgnoreCase(nodeName)) {
                if (textContent.contains("動畫")) {
                    dmhyRssItem.setCategory(DmhyCategory.ANIME);
                }
                if (textContent.contains("季度全集")) {
                    dmhyRssItem.setCategory(DmhyCategory.SEASON_COLLECTION);
                }
                if (textContent.contains("其他")) {
                    dmhyRssItem.setCategory(DmhyCategory.SEASON_COLLECTION);
                }
            }
        }
        return dmhyRssItem;
    }

}
