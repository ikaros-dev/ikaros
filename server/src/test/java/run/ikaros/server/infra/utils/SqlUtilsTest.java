package run.ikaros.server.infra.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SqlUtilsTest {

    @Test
    void escapeLikeSpecialChars_null_returnsNull() {
        assertThat(SqlUtils.escapeLikeSpecialChars(null)).isNull();
    }

    @Test
    void escapeLikeSpecialChars_empty_returnsEmpty() {
        assertThat(SqlUtils.escapeLikeSpecialChars("")).isEmpty();
    }

    @Test
    void escapeLikeSpecialChars_noSpecial_unchanged() {
        assertThat(SqlUtils.escapeLikeSpecialChars("hello")).isEqualTo("hello");
    }

    @Test
    void escapeLikeSpecialChars_escapePercent() {
        assertThat(SqlUtils.escapeLikeSpecialChars("50%")).isEqualTo("50\\%");
    }

    @Test
    void escapeLikeSpecialChars_escapeUnderscore() {
        assertThat(SqlUtils.escapeLikeSpecialChars("a_b")).isEqualTo("a\\_b");
    }

    @Test
    void escapeLikeSpecialChars_escapeBackslash() {
        assertThat(SqlUtils.escapeLikeSpecialChars("a\\b")).isEqualTo("a\\\\b");
    }

    @Test
    void escapeLikeSpecialChars_escapeExclamation() {
        assertThat(SqlUtils.escapeLikeSpecialChars("a!b")).isEqualTo("a\\!b");
    }

    @Test
    void escapeLikeSpecialChars_escapeDash() {
        assertThat(SqlUtils.escapeLikeSpecialChars("a-b")).isEqualTo("a\\-b");
    }

    @Test
    void escapeLikeSpecialChars_escapeSingleQuote() {
        assertThat(SqlUtils.escapeLikeSpecialChars("it's")).isEqualTo("it''s");
    }

    @Test
    void escapeLikeSpecialChars_escapeMultipleSpecial() {
        String result = SqlUtils.escapeLikeSpecialChars("100%_complete!test");
        assertThat(result).contains("\\%");
        assertThat(result).contains("\\_");
        assertThat(result).contains("\\!");
    }
}
