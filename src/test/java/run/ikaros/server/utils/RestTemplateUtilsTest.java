package run.ikaros.server.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RestTemplateUtilsTest {

    @Test
    void testProxyConnect() {
        String host = "192.168.2.229";
        Integer port = 7890;
        boolean success = RestTemplateUtils.testProxyConnect(host, port, null, null);
        assertThat(success).isTrue();
    }
}