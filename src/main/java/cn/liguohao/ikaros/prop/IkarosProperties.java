package cn.liguohao.ikaros.prop;

import cn.liguohao.ikaros.common.Strings;
import cn.liguohao.ikaros.common.constants.EnvConstants;
import cn.liguohao.ikaros.common.kit.SystemVarKit;
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

    public IkarosProperties() {
    }

    public boolean envIsDev() {
        return EnvConstants.DEV.equalsIgnoreCase(env);
    }

    public boolean envIsPro() {
        return EnvConstants.PRO.equalsIgnoreCase(env);
    }

    /**
     * @return http://ip:port
     */
    public String getServerHttpBaseUrl() {
        return "http://"
            + SystemVarKit.getIPAddress()
            + (Strings.isBlank(serverPort) ? "" : ":" + serverPort);
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
}
