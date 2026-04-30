package run.ikaros.server.infra.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PathUtilsTest {

    @Test
    void shouldReturnFalseForBlankUri() {
        assertThat(PathUtils.isAbsoluteUri(null)).isFalse();
        assertThat(PathUtils.isAbsoluteUri("")).isFalse();
        assertThat(PathUtils.isAbsoluteUri("  ")).isFalse();
    }

    @Test
    void shouldReturnTrueForAbsoluteUri() {
        assertThat(PathUtils.isAbsoluteUri("https://example.com")).isTrue();
        assertThat(PathUtils.isAbsoluteUri("http://example.com")).isTrue();
        assertThat(PathUtils.isAbsoluteUri("ftp://files.example.com")).isTrue();
    }

    @Test
    void shouldReturnFalseForRelativeUri() {
        assertThat(PathUtils.isAbsoluteUri("/path/to/resource")).isFalse();
        assertThat(PathUtils.isAbsoluteUri("path/to/resource")).isFalse();
    }

    @Test
    void shouldReturnFalseForInvalidUri() {
        assertThat(PathUtils.isAbsoluteUri("://invalid")).isFalse();
    }

    @Test
    void shouldCombinePaths() {
        assertThat(PathUtils.combinePath("api", "v1", "users"))
            .isEqualTo("/api/v1/users");
    }

    @Test
    void shouldCombinePathsWithLeadingSlash() {
        assertThat(PathUtils.combinePath("/api", "/v1", "/users"))
            .isEqualTo("/api/v1/users");
    }

    @Test
    void shouldCombinePathsWithTrailingSlash() {
        assertThat(PathUtils.combinePath("api/", "v1/", "users/"))
            .isEqualTo("/api/v1/users");
    }

    @Test
    void shouldCombinePathsWithNull() {
        assertThat(PathUtils.combinePath("api", null, "users"))
            .isEqualTo("/api/users");
    }

    @Test
    void shouldCombineEmptyPaths() {
        assertThat(PathUtils.combinePath()).isEmpty();
    }

    @Test
    void shouldAppendPathSeparatorIfMissing() {
        assertThat(PathUtils.appendPathSeparatorIfMissing("hello"))
            .isEqualTo("hello/");
        assertThat(PathUtils.appendPathSeparatorIfMissing("hello/"))
            .isEqualTo("hello/");
        assertThat(PathUtils.appendPathSeparatorIfMissing(null)).isNull();
    }

    @Test
    void shouldSimplifyPathPattern() {
        assertThat(PathUtils.simplifyPathPattern("/{year:\\d{4}}/{month:\\d{2}}"))
            .isEqualTo("/{year}/{month}");
    }

    @Test
    void shouldSimplifyComplexPathPattern() {
        assertThat(PathUtils.simplifyPathPattern("/archives/{year:\\d{4}}/{month:\\d{2}}"))
            .isEqualTo("/archives/{year}/{month}");
    }

    @Test
    void shouldSimplifyPathPatternWithMixedParts() {
        assertThat(PathUtils.simplifyPathPattern("/archives/{year:\\d{4}}/{slug}"))
            .isEqualTo("/archives/{year}/{slug}");
    }

    @Test
    void shouldReturnEmptyForBlankPattern() {
        assertThat(PathUtils.simplifyPathPattern(null)).isEmpty();
        assertThat(PathUtils.simplifyPathPattern("")).isEmpty();
        assertThat(PathUtils.simplifyPathPattern("  ")).isEmpty();
    }

    @Test
    void shouldSimplifyPathPatternWithoutRegex() {
        assertThat(PathUtils.simplifyPathPattern("/{year}/{month}"))
            .isEqualTo("/{year}/{month}");
    }
}
