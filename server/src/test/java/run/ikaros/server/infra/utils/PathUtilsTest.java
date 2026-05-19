package run.ikaros.server.infra.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PathUtilsTest {

    @Test
    void isAbsoluteUriWithHttpsUrl() {
        assertTrue(PathUtils.isAbsoluteUri("https://example.com"));
    }

    @Test
    void isAbsoluteUriWithRelativePath() {
        assertFalse(PathUtils.isAbsoluteUri("/api/test"));
    }

    @Test
    void isAbsoluteUriWithNull() {
        assertFalse(PathUtils.isAbsoluteUri(null));
    }

    @Test
    void combinePathMultipleSegments() {
        assertEquals("/api/v1/test", PathUtils.combinePath("api", "v1", "test"));
    }

    @Test
    void combinePathSingleSegment() {
        assertEquals("/single", PathUtils.combinePath("single"));
    }

    @Test
    void appendPathSeparatorIfMissing() {
        assertEquals("/path/", PathUtils.appendPathSeparatorIfMissing("/path"));
    }

    @Test
    void appendPathSeparatorIfMissingAlreadyPresent() {
        assertEquals("/path/", PathUtils.appendPathSeparatorIfMissing("/path/"));
    }

    @Test
    void simplifyPathPatternWithRegex() {
        assertEquals("/{year}", PathUtils.simplifyPathPattern("{year:\\d{4}}"));
    }

    @Test
    void simplifyPathPatternWithoutRegex() {
        assertEquals("/{id}", PathUtils.simplifyPathPattern("{id}"));
    }
}
