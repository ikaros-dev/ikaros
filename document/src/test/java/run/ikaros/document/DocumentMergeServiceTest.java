package run.ikaros.document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DocumentMergeServiceTest {
    @Test
    void usesLocalContentWhenServerStillMatchesBase() {
        MergeWorkingCopyView result = DocumentMergeService.merge("base", "base", "local", "v1", 3);

        assertEquals("local", result.content());
        assertFalse(result.conflict());
        assertEquals(3, result.currentVersion());
    }

    @Test
    void marksConflictWhenBothSidesChanged() {
        MergeWorkingCopyView result = DocumentMergeService.merge("server", "base", "local", "v1", 4);

        assertTrue(result.conflict());
        assertTrue(result.content().contains("<<<<<<< LOCAL"));
        assertTrue(result.content().contains("local"));
        assertTrue(result.content().contains("server"));
    }
}
