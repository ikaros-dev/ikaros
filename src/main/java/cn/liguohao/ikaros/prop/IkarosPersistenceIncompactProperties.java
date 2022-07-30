package cn.liguohao.ikaros.prop;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author li-guohao
 */
@Component
@ConfigurationProperties("ikaros.persistence.incompact")
public class IkarosPersistenceIncompactProperties {

    private String fileDirPrefix;

    public String getFileDirPrefix() {
        return fileDirPrefix;
    }

    public IkarosPersistenceIncompactProperties setFileDirPrefix(String fileDirPrefix) {
        this.fileDirPrefix = fileDirPrefix;
        return this;
    }
}
