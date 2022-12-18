package run.ikaros.server.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import run.ikaros.server.tripartite.mikan.model.MikanRssItem;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DomTest {
    public static void main(String[] args)
        throws IOException, SAXException, ParserConfigurationException {
        File file = new File("C:\\Users\\li-guohao\\Documents\\MyBangumi.rss.xml");
        List<MikanRssItem> mikanRssItemList = new ArrayList<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(file);
        Element element = document.getDocumentElement();
        NodeList childNodes = element.getFirstChild().getNextSibling().getChildNodes();
        for (int j = 0; j < childNodes.getLength(); j++) {
            Node item1 = childNodes.item(j);
            String nodeName = item1.getNodeName();
            System.out.println(nodeName);
            if ("item".equalsIgnoreCase(nodeName)) {
                MikanRssItem mikanRssItem = handlerItemNode(item1);
                mikanRssItemList.add(mikanRssItem);
            }
        }

        mikanRssItemList.forEach(System.out::println);
    }


    private static MikanRssItem handlerItemNode(Node item) {
        MikanRssItem mikanRssItem = new MikanRssItem();

        NodeList childNodes = item.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            String nodeName = node.getNodeName();

            if ("link".equalsIgnoreCase(nodeName)) {
                mikanRssItem.setEpisodePageUrl(node.getTextContent());
            }
            if ("title".equalsIgnoreCase(nodeName)) {
                mikanRssItem.setTitle(node.getTextContent());
            }
            if ("torrent".equalsIgnoreCase(nodeName)) {
                mikanRssItem.setTorrentUrl(node.getFirstChild().getNextSibling().getTextContent());
            }
        }
        return mikanRssItem;
    }
}
