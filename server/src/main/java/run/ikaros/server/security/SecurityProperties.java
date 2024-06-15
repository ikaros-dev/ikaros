package run.ikaros.server.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "ikaros.security")
public class SecurityProperties {

    private final Initializer initializer = new Initializer();
    private String jwtSecretKey = """
        平和な日常ってやつは、一瞬にして壊されるんだ！
        自分がどうしたいか、自分がどうなりたいか、それが一番大事なんだ！
        私はあなたのエンジェロイド。あなたの命令をなんでも聞きます。
        どんなに辛いことがあっても、笑顔で乗り越えるのが俺の流儀だ！
        君と出会えたこと、それが私の奇跡です。
        """;
    private Long jwtExpirationTime = (long) (2 * 60 * 60 * 1000); // 2h

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
