package run.ikaros.server.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import run.ikaros.server.exceptions.RssOperateException;
import run.ikaros.server.exceptions.RuntimeIkarosException;
import run.ikaros.server.tripartite.mikan.model.MikanRssItem;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public static String generateJellyfinTvShowNfoXml(@Nonnull String filePath, String plot,
                                                       String title,
                                                       String originaltitle, String bangumiid) {
        AssertUtils.notBlank(filePath, "filePath");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        try {
            db = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeIkarosException("new document builder fail", e);
        }
        Document document = db.newDocument();
        // 不显示standalone="no"
        document.setXmlStandalone(true);
        Element tvshowElement = document.createElement("tvshow");
        document.appendChild(tvshowElement);

        Element plotElement = document.createElement("plot");
        plotElement.setTextContent(plot);
        tvshowElement.appendChild(plotElement);

        Element lockdataElement = document.createElement("lockdata");
        lockdataElement.setTextContent("false");
        tvshowElement.appendChild(lockdataElement);

        Element titleElement = document.createElement("title");
        titleElement.setTextContent(title);
        tvshowElement.appendChild(titleElement);

        Element originaltitleElement = document.createElement("originaltitle");
        originaltitleElement.setTextContent(originaltitle);
        tvshowElement.appendChild(originaltitleElement);

        Element bangumiidElement = document.createElement("bangumiid");
        bangumiidElement.setTextContent(bangumiid);
        tvshowElement.appendChild(bangumiidElement);

        File file = new File(filePath);
        File dir = file.getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            TransformerFactory tff = TransformerFactory.newInstance();
            Transformer tf = tff.newTransformer();
            // 输出内容是否使用换行
            tf.setOutputProperty(OutputKeys.INDENT, "yes");
            // 创建xml文件并写入内容
            tf.transform(new DOMSource(document), new StreamResult(file));
            LOGGER.info("generate jellyfin tv show nfo xml file success, filePath: {}", filePath);
        } catch (TransformerException transformerException) {
            LOGGER.warn("generate jellyfin tv show nfo xml file fail", transformerException);
        }
        return filePath;
    }

    public static String generateJellyfinEpisodeNfoXml(@Nonnull String filePath, String plot,
                                                       String title, String season,
                                                       String episode, String bangumiid) {
        AssertUtils.notBlank(filePath, "filePath");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        try {
            db = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeIkarosException("new document builder fail", e);
        }
        Document document = db.newDocument();
        // 不显示standalone="no"
        document.setXmlStandalone(true);
        Element tvshowElement = document.createElement("episodedetails");
        document.appendChild(tvshowElement);

        Element plotElement = document.createElement("plot");
        plotElement.setTextContent(plot);
        tvshowElement.appendChild(plotElement);

        Element lockdataElement = document.createElement("lockdata");
        lockdataElement.setTextContent("false");
        tvshowElement.appendChild(lockdataElement);

        Element titleElement = document.createElement("title");
        titleElement.setTextContent(title);
        tvshowElement.appendChild(titleElement);

        Element seasonElement = document.createElement("season");
        seasonElement.setTextContent(season);
        tvshowElement.appendChild(seasonElement);

        Element episodeElement = document.createElement("episode");
        episodeElement.setTextContent(episode);
        tvshowElement.appendChild(episodeElement);

        Element bangumiidElement = document.createElement("bangumiid");
        bangumiidElement.setTextContent(bangumiid);
        tvshowElement.appendChild(bangumiidElement);

        File file = new File(filePath);
        File dir = file.getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            TransformerFactory tff = TransformerFactory.newInstance();
            Transformer tf = tff.newTransformer();
            // 输出内容是否使用换行
            tf.setOutputProperty(OutputKeys.INDENT, "yes");
            // 创建xml文件并写入内容
            tf.transform(new DOMSource(document), new StreamResult(file));
            LOGGER.info("generate jellyfin episode nfo xml file success, filePath: {}", filePath);
        } catch (TransformerException transformerException) {
            LOGGER.warn("generate jellyfin episode nfo xml file fail", transformerException);
        }
        return filePath;
    }
}
