package run.ikaros.server.security.authentication.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class JwtApplyParamTest {

    @Test
    void builderPattern() {
        JwtApplyParam param = JwtApplyParam.builder()
            .authType(JwtApplyParam.Type.USERNAME_PASSWORD)
            .username("testUser")
            .password("testPass")
            .phoneNum("13800138000")
            .email("test@example.com")
            .code("123456")
            .build();

        assertNotNull(param);
        assertEquals(JwtApplyParam.Type.USERNAME_PASSWORD, param.getAuthType());
        assertEquals("testUser", param.getUsername());
        assertEquals("testPass", param.getPassword());
        assertEquals("13800138000", param.getPhoneNum());
        assertEquals("test@example.com", param.getEmail());
        assertEquals("123456", param.getCode());
    }

    @Test
    void gettersAndSetters() {
        JwtApplyParam param = new JwtApplyParam();
        param.setAuthType(JwtApplyParam.Type.PHONE_NUMBER_PASSWORD);
        param.setUsername("user2");
        param.setPassword("pass2");
        param.setPhoneNum("13900139000");
        param.setEmail("user2@example.com");
        param.setCode("654321");

        assertEquals(JwtApplyParam.Type.PHONE_NUMBER_PASSWORD, param.getAuthType());
        assertEquals("user2", param.getUsername());
        assertEquals("pass2", param.getPassword());
        assertEquals("13900139000", param.getPhoneNum());
        assertEquals("user2@example.com", param.getEmail());
        assertEquals("654321", param.getCode());
    }

    @Test
    void chainSetters() {
        JwtApplyParam param = new JwtApplyParam();
        JwtApplyParam returned = param
            .setAuthType(JwtApplyParam.Type.EMAIL_CODE)
            .setUsername("chainUser")
            .setPassword("chainPass")
            .setPhoneNum("13700137000")
            .setEmail("chain@example.com")
            .setCode("999999");

        assertSame(param, returned);
        assertEquals(JwtApplyParam.Type.EMAIL_CODE, param.getAuthType());
        assertEquals("chainUser", param.getUsername());
        assertEquals("chainPass", param.getPassword());
        assertEquals("13700137000", param.getPhoneNum());
        assertEquals("chain@example.com", param.getEmail());
        assertEquals("999999", param.getCode());
    }

    @Test
    void noArgsConstructor() {
        JwtApplyParam param = new JwtApplyParam();
        assertNotNull(param);
        assertEquals(null, param.getAuthType());
        assertEquals(null, param.getUsername());
        assertEquals(null, param.getPassword());
        assertEquals(null, param.getPhoneNum());
        assertEquals(null, param.getEmail());
        assertEquals(null, param.getCode());
    }

    @Test
    void allArgsConstructor() {
        JwtApplyParam param = new JwtApplyParam(
            JwtApplyParam.Type.USERNAME_PASSWORD,
            "allArgUser",
            "allArgPass",
            "13600136000",
            "allarg@example.com",
            "111111"
        );

        assertEquals(JwtApplyParam.Type.USERNAME_PASSWORD, param.getAuthType());
        assertEquals("allArgUser", param.getUsername());
        assertEquals("allArgPass", param.getPassword());
        assertEquals("13600136000", param.getPhoneNum());
        assertEquals("allarg@example.com", param.getEmail());
        assertEquals("111111", param.getCode());
    }

    @Test
    void enumTypeValuesExist() {
        JwtApplyParam.Type[] types = JwtApplyParam.Type.values();
        assertNotNull(types);
        assertEquals(5, types.length);

        assertSame(JwtApplyParam.Type.USERNAME_PASSWORD,
            JwtApplyParam.Type.valueOf("USERNAME_PASSWORD"));
        assertSame(JwtApplyParam.Type.PHONE_NUMBER_PASSWORD,
            JwtApplyParam.Type.valueOf("PHONE_NUMBER_PASSWORD"));
        assertSame(JwtApplyParam.Type.PHONE_NUMBER_CODE,
            JwtApplyParam.Type.valueOf("PHONE_NUMBER_CODE"));
        assertSame(JwtApplyParam.Type.EMAIL_PASSWORD,
            JwtApplyParam.Type.valueOf("EMAIL_PASSWORD"));
        assertSame(JwtApplyParam.Type.EMAIL_CODE,
            JwtApplyParam.Type.valueOf("EMAIL_CODE"));
    }
}
