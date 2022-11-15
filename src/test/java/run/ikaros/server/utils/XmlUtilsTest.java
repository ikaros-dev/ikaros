package run.ikaros.server.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;
import run.ikaros.server.tripartite.mikan.model.MikanRssItem;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class XmlUtilsTest {

    @Test
    void parseMikanRssXmlFile() throws FileNotFoundException {
        final String cacheRssCacheXmlFileResourceClassPath = ResourceUtils.CLASSPATH_URL_PREFIX + "mikan"
            + File.separator + "issue85unreadableCode.rss.xml";
        File cacheRssCacheXmlFile = ResourceUtils.getFile(cacheRssCacheXmlFileResourceClassPath);
        List<MikanRssItem> mikanRssItemList =
            XmlUtils.parseMikanRssXmlFile(cacheRssCacheXmlFile.getAbsolutePath());
        assertThat(mikanRssItemList).isNotEmpty();
    }
}