package run.ikaros.server.store.convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import run.ikaros.api.store.enums.AttachmentType;

class String2EnumConverterTest {

    @Test
    void constructor_withEnumClass_succeeds() {
        String2EnumConverter<AttachmentType> converter =
            new String2EnumConverterTestHelper();
    }

    @Test
    void convert_validEnumName_returnsEnumValue() {
        String2EnumConverter<AttachmentType> converter =
            new String2EnumConverter<>(AttachmentType.class);
        assertEquals(AttachmentType.File, converter.convert("File"));
    }

    @Test
    void convert_null_returnsNull() {
        String2EnumConverter<AttachmentType> converter =
            new String2EnumConverter<>(AttachmentType.class);
        assertNull(converter.convert(null));
    }

    @Test
    void convert_emptyString_returnsNull() {
        String2EnumConverter<AttachmentType> converter =
            new String2EnumConverter<>(AttachmentType.class);
        assertNull(converter.convert(""));
    }

    @Test
    void convert_invalidName_returnsNull() {
        String2EnumConverter<AttachmentType> converter =
            new String2EnumConverter<>(AttachmentType.class);
        assertNull(converter.convert("NONEXISTENT"));
    }

    private static class String2EnumConverterTestHelper
        extends String2EnumConverter<AttachmentType> {
        String2EnumConverterTestHelper() {
            super(AttachmentType.class);
        }
    }
}
