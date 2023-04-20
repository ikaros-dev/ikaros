package run.ikaros.server.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "ikaros.security")
public class SecurityProperties {

    private final Initializer initializer = new Initializer();

    @Data
    public static class Initializer {
        private boolean disabled;

        /**
         * default master username is tomoki.
         *
         * @see <a href="https://bgm.tv/character/10699">桜井智樹(さくらい ともき)</a>
         */
        private String masterUsername = "tomoki";
        private String masterNickname = "桜井智樹";
        private String masterPassword;
    }

}
