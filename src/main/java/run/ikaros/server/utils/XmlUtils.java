package run.ikaros.server.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import run.ikaros.server.exceptions.RssOperateException;
import run.ikaros.server.tripartite.mikan.model.MikanRssItem;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class XmlUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(XmlUtils.class);

    public static List<MikanRssItem> parseMikanRssXmlFile(@Nonnull String filePath) {
        AssertUtils.notBlank(filePath, "filePath");
        List<MikanRssItem> mikanRssItemList = new ArrayList<>();

        try {
            File cacheFile = new File(filePath);
            if (!cacheFile.exists()) {
                LOGGER.warn("cache file not exist, will return directly, file path:{}", filePath);
                return mikanRssItemList;
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(cacheFile);
            Element element = document.getDocumentElement();
            Node firstChild = element.getFirstChild();
            if (firstChild.getNodeName().contains("#text")) {
                firstChild = firstChild.getNextSibling();
            }
            NodeList childNodes = firstChild.getChildNodes();
            for (int j = 0; j < childNodes.getLength(); j++) {
                Node item1 = childNodes.item(j);
                String nodeName = item1.getNodeName();
                // LOGGER.debug("parse channel item name: {}", nodeName);
                if ("item".equalsIgnoreCase(nodeName)) {
                    MikanRssItem mikanRssItem = handlerItemNode(item1);
                    mikanRssItemList.add(mikanRssItem);
                }
            }
        } catch (IOException | SAXException | ParserConfigurationException exception) {
            throw new RssOperateException("parse xml file fail", exception);
        }
        return mikanRssItemList;
    }

    private static MikanRssItem handlerItemNode(Node item) {
        MikanRssItem mikanRssItem = new MikanRssItem();

        NodeList childNodes = item.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            String nodeName = node.getNodeName();
            String textContent = node.getTextContent();

            if ("link".equalsIgnoreCase(nodeName)) {
                mikanRssItem.setEpisodePageUrl(textContent);
            }
            if ("title".equalsIgnoreCase(nodeName)) {
                mikanRssItem.setTitle(textContent);
            }
            // if ("torrent".equalsIgnoreCase(nodeName)) {
            //  mikanRssItem.setTorrentUrl(node.getFirstChild().getNextSibling().getTextContent());
            // }
            if ("enclosure".equalsIgnoreCase(nodeName)) {
                String url = node.getAttributes().getNamedItem("url").getNodeValue();
                mikanRssItem.setTorrentUrl(url);
            }
        }
        return mikanRssItem;
    }
}
