package run.ikaros.server.infra.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SqlUtilsTest {

    @Test
    void shouldReturnNullForNullInput() {
        assertThat(SqlUtils.escapeLikeSpecialChars(null)).isNull();
    }

    @Test
    void shouldEscapeBackslash() {
        assertThat(SqlUtils.escapeLikeSpecialChars("test\\path"))
            .isEqualTo("test\\\\path");
    }

    @Test
    void shouldEscapePercent() {
        assertThat(SqlUtils.escapeLikeSpecialChars("100%"))
            .isEqualTo("100\\%");
    }

    @Test
    void shouldEscapeUnderscore() {
        assertThat(SqlUtils.escapeLikeSpecialChars("test_name"))
            .isEqualTo("test\\_name");
    }

    @Test
    void shouldEscapeSquareBrackets() {
        assertThat(SqlUtils.escapeLikeSpecialChars("[test]"))
            .isEqualTo("\\[test\\]");
    }

    @Test
    void shouldEscapeHyphen() {
        assertThat(SqlUtils.escapeLikeSpecialChars("test-name"))
            .isEqualTo("test\\-name");
    }

    @Test
    void shouldEscapeExclamation() {
        assertThat(SqlUtils.escapeLikeSpecialChars("test!name"))
            .isEqualTo("test\\!name");
    }

    @Test
    void shouldEscapeSingleQuote() {
        assertThat(SqlUtils.escapeLikeSpecialChars("it's"))
            .isEqualTo("it''s");
    }

    @Test
    void shouldEscapeBacktick() {
        assertThat(SqlUtils.escapeLikeSpecialChars("`test`"))
            .isEqualTo("\\`test\\`");
    }

    @Test
    void shouldEscapeDoubleQuote() {
        assertThat(SqlUtils.escapeLikeSpecialChars("\"test\""))
            .isEqualTo("\\\"test\\\"");
    }

    @Test
    void shouldEscapeAsterisk() {
        assertThat(SqlUtils.escapeLikeSpecialChars("test*name"))
            .isEqualTo("test\\*name");
    }

    @Test
    void shouldEscapeParentheses() {
        assertThat(SqlUtils.escapeLikeSpecialChars("test(name)"))
            .isEqualTo("test\\(name\\)");
    }

    @Test
    void shouldEscapeCurlyBraces() {
        assertThat(SqlUtils.escapeLikeSpecialChars("test{name}"))
            .isEqualTo("test\\{name\\}");
    }

    @Test
    void shouldEscapeAngleBrackets() {
        assertThat(SqlUtils.escapeLikeSpecialChars("test<name>"))
            .isEqualTo("test\\<name\\>");
    }

    @Test
    void shouldEscapeSpecialChars() {
        assertThat(SqlUtils.escapeLikeSpecialChars("#&|^~$?+;:@/=."))
            .isEqualTo("\\#\\&\\|\\^\\~\\$\\?\\+\\;\\:\\@\\/\\=\\.");
    }

    @Test
    void shouldEscapeSpace() {
        assertThat(SqlUtils.escapeLikeSpecialChars("test name"))
            .isEqualTo("test\\ name");
    }

    @Test
    void shouldHandleEmptyString() {
        assertThat(SqlUtils.escapeLikeSpecialChars("")).isEmpty();
    }

    @Test
    void shouldHandleStringWithoutSpecialChars() {
        assertThat(SqlUtils.escapeLikeSpecialChars("hello")).isEqualTo("hello");
    }

    @Test
    void shouldEscapeMultipleSpecialChars() {
        assertThat(SqlUtils.escapeLikeSpecialChars("test%_name"))
            .isEqualTo("test\\%\\_name");
    }
}
