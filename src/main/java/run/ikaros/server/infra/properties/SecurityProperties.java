package run.ikaros.server.infra.properties;

import lombok.Data;

@Data
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
