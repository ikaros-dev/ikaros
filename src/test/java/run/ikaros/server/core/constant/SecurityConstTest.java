package run.ikaros.server.core.constant;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import run.ikaros.server.infra.constant.SecurityConst;

class SecurityConstTest {
    @Test
    void isAnonymousUser() {
        assertThat(SecurityConst.AnonymousUser.isAnonymousUser("others")).isFalse();
        assertThat(SecurityConst.AnonymousUser.isAnonymousUser("anonymousUser")).isTrue();
    }
}