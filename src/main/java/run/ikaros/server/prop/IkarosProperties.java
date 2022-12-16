package run.ikaros.server.prop;

import lombok.Data;
import run.ikaros.server.utils.StringUtils;
import run.ikaros.server.constants.EnvConst;
import run.ikaros.server.utils.SystemVarUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author guohao
 * @date 2022/10/23
 */
@Data
@Component
@ConfigurationProperties("ikaros")
public class IkarosProperties {
    private String env;
    private String logLevel;
    private String serverPort;
    private String qbittorrentBaseUrl;
    private String appUrlPrefix;

    public IkarosProperties() {
    }

    public boolean envIsDev() {
        return EnvConst.DEV.equalsIgnoreCase(env);
    }

    public boolean envIsLocal() {
        return EnvConst.LOCAL.equalsIgnoreCase(env);
    }

    public boolean envIsPro() {
        return EnvConst.PRO.equalsIgnoreCase(env);
    }

    /**
     * @return http://ip:port
     */
    public String getServerHttpBaseUrl() {
        return "http://"
            + SystemVarUtils.getIPAddress()
            + (StringUtils.isBlank(serverPort) ? "" : ":" + serverPort);
    }

    public String getLocalhostHttpBaseUrl() {
        return "http://localhost"
            + (StringUtils.isBlank(serverPort) ? "" : ":" + serverPort);
    }


}
