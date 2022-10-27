package run.ikaros.server.config;

import org.springframework.context.annotation.Configuration;

/**
 * @author li-guohao
 */
@Configuration
public class SSLConfig {
    static {
        System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2,SSLv3");
    }
}
