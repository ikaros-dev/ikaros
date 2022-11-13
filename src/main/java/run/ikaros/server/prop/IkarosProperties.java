package run.ikaros.server.prop;

import run.ikaros.server.utils.StringUtils;
import run.ikaros.server.constants.EnvConst;
import run.ikaros.server.utils.SystemVarUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author guohao
 * @date 2022/10/23
 */
@Component
@ConfigurationProperties("ikaros")
public class IkarosProperties {
    private String env;
    private String logLevel;
    private String serverPort;
    private String qbittorrentBaseUrl;

    public IkarosProperties() {
    }

    public boolean envIsDev() {
        return EnvConst.DEV.equalsIgnoreCase(env);
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


    public String getEnv() {
        return env;
    }

    public IkarosProperties setEnv(String env) {
        this.env = env;
        return this;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public IkarosProperties setLogLevel(String logLevel) {
        this.logLevel = logLevel;
        return this;
    }

    public String getServerPort() {
        return serverPort;
    }

    public IkarosProperties setServerPort(String serverPort) {
        this.serverPort = serverPort;
        return this;
    }

    public String getQbittorrentBaseUrl() {
        return qbittorrentBaseUrl;
    }

    public IkarosProperties setQbittorrentBaseUrl(String qbittorrentBaseUrl) {
        this.qbittorrentBaseUrl = qbittorrentBaseUrl;
        return this;
    }
}
