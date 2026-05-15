package run.ikaros.server.infra.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PathUtilsTest {

    @Test
    void isAbsoluteUri_httpUrl() {
        assertTrue(PathUtils.isAbsoluteUri("http://example.com"));
    }

    @Test
    void isAbsoluteUri_httpsUrl() {
        assertTrue(PathUtils.isAbsoluteUri("https://example.com/path"));
    }

    @Test
    void isAbsoluteUri_relativePath() {
        assertFalse(PathUtils.isAbsoluteUri("/api/v1/users"));
    }

    @Test
    void isAbsoluteUri_nullInput() {
        assertFalse(PathUtils.isAbsoluteUri(null));
    }

    @Test
    void isAbsoluteUri_emptyInput() {
        assertFalse(PathUtils.isAbsoluteUri(""));
    }

    @Test
    void isAbsoluteUri_blankInput() {
        assertFalse(PathUtils.isAbsoluteUri("   "));
    }

    @Test
    void isAbsoluteUri_invalidUri() {
        assertFalse(PathUtils.isAbsoluteUri("http://[invalid"));
    }

    @Test
    void combinePath_twoSegments() {
        assertEquals("/api/v1", PathUtils.combinePath("api", "v1"));
    }

    @Test
    void combinePath_alreadyHasSlash() {
        assertEquals("/api/v1", PathUtils.combinePath("/api", "/v1"));
    }

    @Test
    void combinePath_trailingSlash() {
        assertEquals("/api/v1", PathUtils.combinePath("/api/", "/v1/"));
    }

    @Test
    void combinePath_nullSegment() {
        assertEquals("/api/v1", PathUtils.combinePath("/api", null, "/v1"));
    }

    @Test
    void combinePath_singleSegment() {
        assertEquals("/api", PathUtils.combinePath("api"));
    }

    @Test
    void combinePath_multipleSegments() {
        assertEquals("/api/v1/users", PathUtils.combinePath("api", "v1", "users"));
    }

    @Test
    void appendPathSeparatorIfMissing_noTrailingSlash() {
        assertEquals("hello/", PathUtils.appendPathSeparatorIfMissing("hello"));
    }

    @Test
    void appendPathSeparatorIfMissing_hasTrailingSlash() {
        assertEquals("hello/", PathUtils.appendPathSeparatorIfMissing("hello/"));
    }

    @Test
    void appendPathSeparatorIfMissing_nullInput() {
        assertEquals(null, PathUtils.appendPathSeparatorIfMissing(null));
    }

    @Test
    void simplifyPathPattern_withRegex() {
        assertEquals("/archives/{year}/{month}",
            PathUtils.simplifyPathPattern("/archives/{year:\\d{4}}/{month:\\d{2}}"));
    }

    @Test
    void simplifyPathPattern_withoutRegex() {
        assertEquals("/archives/{year}/{slug}",
            PathUtils.simplifyPathPattern("/archives/{year}/{slug}"));
    }

    @Test
    void simplifyPathPattern_emptyInput() {
        assertEquals("", PathUtils.simplifyPathPattern(""));
    }

    @Test
    void simplifyPathPattern_nullInput() {
        assertEquals("", PathUtils.simplifyPathPattern(null));
    }

    @Test
    void simplifyPathPattern_blankInput() {
        assertEquals("", PathUtils.simplifyPathPattern("   "));
    }
}
