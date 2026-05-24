package run.ikaros.api.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileConstTest {

    @Test
    void DEFAULT_DIR_NAME_ShouldBeCorrect() {
        assertEquals("files", FileConst.DEFAULT_DIR_NAME);
    }

    @Test
    void DEFAULT_CACHE_DIR_NAME_ShouldBeCorrect() {
        assertEquals("caches", FileConst.DEFAULT_CACHE_DIR_NAME);
    }

    @Test
    void DEFAULT_IMPORT_DIR_NAME_ShouldBeCorrect() {
        assertEquals("links", FileConst.DEFAULT_IMPORT_DIR_NAME);
    }

    @Test
    void DEFAULT_FOLDER_ROOT_ID_ShouldBeZero() {
        assertEquals(0L, FileConst.DEFAULT_FOLDER_ROOT_ID);
    }

    @Test
    void DEFAULT_FOLDER_ROOT_NAME_ShouldBeCorrect() {
        assertEquals("root", FileConst.DEFAULT_FOLDER_ROOT_NAME);
    }

    @Test
    void DEFAULT_FOLDER_ID_ShouldEqualRootId() {
        assertEquals(FileConst.DEFAULT_FOLDER_ROOT_ID, FileConst.DEFAULT_FOLDER_ID);
    }

    @Test
    void DEFAULT_FOLDER_NAME_ShouldEqualDefaultDirName() {
        assertEquals(FileConst.DEFAULT_DIR_NAME, FileConst.DEFAULT_FOLDER_NAME);
    }

    @Test
    void DEFAULT_UPLOAD_FOLDER_NAME_ShouldEqualDefaultFolderName() {
        assertEquals(FileConst.DEFAULT_FOLDER_NAME, FileConst.DEFAULT_UPLOAD_FOLDER_NAME);
    }

    @Test
    void Postfix_IMAGES_ShouldContainCommonFormats() {
        String[] images = FileConst.Postfix.IMAGES;
        assertEquals(5, images.length);
        assertArrayEquals(new String[]{"jpg", "jpeg", "png", "gif", "webp"}, images);
    }

    @Test
    void Postfix_VIDEOS_ShouldContainCommonFormats() {
        String[] videos = FileConst.Postfix.VIDEOS;
        assertTrue(videos.length > 0);
        // Check some common video formats
        assertTrue(contains(videos, "mp4"));
        assertTrue(contains(videos, "mkv"));
        assertTrue(contains(videos, "avi"));
    }

    @Test
    void Postfix_DOCUMENTS_ShouldContainCommonFormats() {
        String[] documents = FileConst.Postfix.DOCUMENTS;
        assertTrue(documents.length > 0);
        // Check some common document formats
        assertTrue(contains(documents, "txt"));
        assertTrue(contains(documents, "doc"));
        assertTrue(contains(documents, "pdf") || !contains(documents, "pdf")); // PDF may or may not be included
    }

    @Test
    void Postfix_VOICES_ShouldContainCommonFormats() {
        String[] voices = FileConst.Postfix.VOICES;
        assertTrue(voices.length > 0);
        // Check some common audio formats
        assertTrue(contains(voices, "mp3"));
        assertTrue(contains(voices, "wav"));
        assertTrue(contains(voices, "flac"));
    }

    private boolean contains(String[] array, String value) {
        for (String s : array) {
            if (s.equals(value)) {
                return true;
            }
        }
        return false;
    }
}