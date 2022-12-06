package run.ikaros.server.tripartite.dmhy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import run.ikaros.server.tripartite.dmhy.enums.DmhyCategory;
import run.ikaros.server.tripartite.dmhy.model.DmhyRssItem;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DmhyClientTest {

    DmhyClient dmhyClient;

    @BeforeEach
    void setUp() {
        dmhyClient = new DmhyClientImpl();
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("192.168.2.229", 7890));
        dmhyClient.setProxy(proxy);
    }

    @Test
    @Disabled
    void findRssItems() {
        List<DmhyRssItem> rssItems = dmhyClient.findRssItems("孤独摇滚 01", DmhyCategory.ANIME);
        assertThat(rssItems).isNotEmpty();
    }
}