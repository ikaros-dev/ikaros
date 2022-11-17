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
        final String cacheRssCacheXmlFileResourceClassPath =
            ResourceUtils.CLASSPATH_URL_PREFIX + "mikan"
                + File.separator + "issue85unreadableCode.rss.xml";
        File cacheRssCacheXmlFile = ResourceUtils.getFile(cacheRssCacheXmlFileResourceClassPath);
        List<MikanRssItem> mikanRssItemList =
            XmlUtils.parseMikanRssXmlFile(cacheRssCacheXmlFile.getAbsolutePath());
        assertThat(mikanRssItemList).isNotEmpty();
    }

    @Test
    void generateJellyfinTvShowNfoXml() {
        String filePath =
            SystemVarUtils.getOsCacheDirPath() + File.separator + "tvshow.nfo";

        XmlUtils.generateJellyfinTvShowNfoXml(filePath, "默认的描述", "默认标题", "原始标题", "366695");

        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
    }
}