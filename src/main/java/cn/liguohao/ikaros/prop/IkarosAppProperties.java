package cn.liguohao.ikaros.prop;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author li-guohao
 */
@Component
@ConfigurationProperties("ikaros.app")
public class IkarosAppProperties {

    private String test;

    public String test() {
        return test;
    }

    public IkarosAppProperties setTest(String test) {
        this.test = test;
        return this;
    }
}
