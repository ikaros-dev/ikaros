package run.ikaros.api.infra.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class StringUtilsTest {

    @Test
    void upperCaseFirstShouldCapitalizeFirstLetter() {
        assertThat(StringUtils.upperCaseFirst("hello")).isEqualTo("Hello");
        assertThat(StringUtils.upperCaseFirst("HELLO")).isEqualTo("HELLO");
        assertThat(StringUtils.upperCaseFirst("h")).isEqualTo("H");
    }

    @Test
    void isBlankShouldReturnTrueForNullOrBlank() {
        assertThat(StringUtils.isBlank(null)).isTrue();
        assertThat(StringUtils.isBlank("")).isTrue();
        assertThat(StringUtils.isBlank("   ")).isTrue();
        assertThat(StringUtils.isBlank("\t")).isTrue();
        assertThat(StringUtils.isBlank("\n")).isTrue();
    }

    @Test
    void isBlankShouldReturnFalseForNonBlank() {
        assertThat(StringUtils.isBlank("hello")).isFalse();
        assertThat(StringUtils.isBlank(" hello ")).isFalse();
        assertThat(StringUtils.isBlank("a")).isFalse();
    }

    @Test
    void isEmptyShouldReturnTrueForNullOrEmpty() {
        assertThat(StringUtils.isEmpty(null)).isTrue();
        assertThat(StringUtils.isEmpty("")).isTrue();
    }

    @Test
    void isEmptyShouldReturnFalseForNonEmpty() {
        assertThat(StringUtils.isEmpty(" ")).isFalse();
        assertThat(StringUtils.isEmpty("hello")).isFalse();
    }

    @Test
    void isNotBlankShouldBeOppositeOfIsBlank() {
        assertThat(StringUtils.isNotBlank(null)).isFalse();
        assertThat(StringUtils.isNotBlank("")).isFalse();
        assertThat(StringUtils.isNotBlank("hello")).isTrue();
    }

    @Test
    void addLikeCharShouldWrapWithPercent() {
        assertThat(StringUtils.addLikeChar("test")).isEqualTo("%test%");
        assertThat(StringUtils.addLikeChar("")).isEqualTo("%%");
    }

    @Test
    void generateRandomStrShouldReturnCorrectLength() {
        assertThat(StringUtils.generateRandomStr(10)).hasSize(10);
        assertThat(StringUtils.generateRandomStr(1)).hasSize(1);
        assertThat(StringUtils.generateRandomStr(100)).hasSize(100);
    }

    @Test
    void generateRandomStrShouldThrowForNonPositiveLength() {
        assertThatThrownBy(() -> StringUtils.generateRandomStr(0))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StringUtils.generateRandomStr(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generateRandomStrShouldReturnUniqueValues() {
        String s1 = StringUtils.generateRandomStr(20);
        String s2 = StringUtils.generateRandomStr(20);
        assertThat(s1).isNotEqualTo(s2);
    }

    @Test
    void isBase64EncodedShouldReturnTrueForValidBase64() {
        // "test" in base64 is "dGVzdA=="
        assertThat(StringUtils.isBase64Encoded("dGVzdA==")).isTrue();
        assertThat(StringUtils.isBase64Encoded("SGVsbG8=")).isTrue();
        assertThat(StringUtils.isBase64Encoded("QUJDRA==")).isTrue();
    }

    @Test
    void isBase64EncodedShouldReturnFalseForInvalidBase64() {
        assertThat(StringUtils.isBase64Encoded("not base64!")).isFalse();
        assertThat(StringUtils.isBase64Encoded("hello")).isFalse();
    }

    @Test
    void snakeToCamelShouldConvertSnakeToCamel() {
        assertThat(StringUtils.snakeToCamel("user_id")).isEqualTo("userId");
        assertThat(StringUtils.snakeToCamel("subject_id")).isEqualTo("subjectId");
        assertThat(StringUtils.snakeToCamel("create_time")).isEqualTo("createTime");
    }

    @Test
    void snakeToCamelShouldHandleUpperCaseInput() {
        assertThat(StringUtils.snakeToCamel("USER_ID")).isEqualTo("userId");
        assertThat(StringUtils.snakeToCamel("SUBJECT_ID")).isEqualTo("subjectId");
    }

    @Test
    void snakeToCamelShouldReturnInputWithoutUnderscore() {
        assertThat(StringUtils.snakeToCamel("userid")).isEqualTo("userid");
        assertThat(StringUtils.snakeToCamel(null)).isNull();
    }
}
