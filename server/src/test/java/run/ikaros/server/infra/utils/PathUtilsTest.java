package run.ikaros.server.infra.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PathUtilsTest {

    @Test
    void isAbsoluteUri_httpUrl_returnsTrue() {
        assertTrue(PathUtils.isAbsoluteUri("http://example.com/file.txt"));
    }

    @Test
    void isAbsoluteUri_httpsUrl_returnsTrue() {
        assertTrue(PathUtils.isAbsoluteUri("https://example.com/file.txt"));
    }

    @Test
    void isAbsoluteUri_relativePath_returnsFalse() {
        assertFalse(PathUtils.isAbsoluteUri("/relative/path/file.txt"));
    }

    @Test
    void isAbsoluteUri_emptyString_returnsFalse() {
        assertFalse(PathUtils.isAbsoluteUri(""));
    }

    @Test
    void isAbsoluteUri_null_returnsFalse() {
        assertFalse(PathUtils.isAbsoluteUri(null));
    }

    @Test
    void isAbsoluteUri_blankString_returnsFalse() {
        assertFalse(PathUtils.isAbsoluteUri("   "));
    }

    @Test
    void combinePath_singleSegment_returnsPath() {
        assertThat(PathUtils.combinePath("a", "b", "c")).isEqualTo("/a/b/c");
    }

    @Test
    void combinePath_withLeadingSlash_handlesCorrectly() {
        assertThat(PathUtils.combinePath("/a", "/b")).isEqualTo("/a/b");
    }

    @Test
    void combinePath_withTrailingSlash_removesTrailing() {
        assertThat(PathUtils.combinePath("a/", "b/")).isEqualTo("/a/b");
    }

    @Test
    void combinePath_emptySegments_skipsEmpty() {
        assertThat(PathUtils.combinePath("a", "", "b")).isEqualTo("/a/b");
    }

    @Test
    void combinePath_nullSegments_skipsNull() {
        assertThat(PathUtils.combinePath("a", null, "b")).isEqualTo("/a/b");
    }

    @Test
    void combinePath_noArgs_returnsEmpty() {
        assertThat(PathUtils.combinePath()).isEmpty();
    }

    @Test
    void appendPathSeparatorIfMissing_endsWithSlash_unchanged() {
        assertThat(PathUtils.appendPathSeparatorIfMissing("path/")).isEqualTo("path/");
    }

    @Test
    void appendPathSeparatorIfMissing_noSlash_appends() {
        assertThat(PathUtils.appendPathSeparatorIfMissing("path")).isEqualTo("path/");
    }

    @Test
    void appendPathSeparatorIfMissing_empty_returnsSlash() {
        assertThat(PathUtils.appendPathSeparatorIfMissing("")).isEqualTo("/");
    }

    @Test
    void appendPathSeparatorIfMissing_null_returnsNull() {
        assertThat(PathUtils.appendPathSeparatorIfMissing(null)).isNull();
    }

    @Test
    void simplifyPathPattern_removesRegexPlaceholder() {
        assertThat(PathUtils.simplifyPathPattern("/{year:\\d{4}}/{month:\\d{2}}"))
            .isEqualTo("/{year}/{month}");
    }

    @Test
    void simplifyPathPattern_simplePath_unchanged() {
        assertThat(PathUtils.simplifyPathPattern("/a/b/c")).isEqualTo("/a/b/c");
    }

    @Test
    void simplifyPathPattern_empty_returnsEmpty() {
        assertThat(PathUtils.simplifyPathPattern("")).isEmpty();
    }

    @Test
    void simplifyPathPattern_blank_returnsEmpty() {
        assertThat(PathUtils.simplifyPathPattern("   ")).isEmpty();
    }

    @Test
    void simplifyPathPattern_withoutColon_unchanged() {
        assertThat(PathUtils.simplifyPathPattern("/{slug}")).isEqualTo("/{slug}");
    }

    @Test
    void isAbsoluteUri_invalidUri_returnsFalse() {
        assertFalse(PathUtils.isAbsoluteUri("\\invalid\\path"));
    }
}
