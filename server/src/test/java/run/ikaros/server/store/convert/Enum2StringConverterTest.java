package run.ikaros.server.store.convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import run.ikaros.api.store.enums.AttachmentType;

class Enum2StringConverterTest {

    @Test
    void constructor_withEnumClass_succeeds() {
        Enum2StringConverter<AttachmentType> converter =
            new Enum2EnumConverterTestHelper();
    }

    @Test
    void convert_enumValue_returnsName() {
        Enum2StringConverter<AttachmentType> converter =
            new Enum2StringConverter<>(AttachmentType.class);
        assertEquals("File", converter.convert(AttachmentType.File));
    }

    @Test
    void convert_null_returnsNull() {
        Enum2StringConverter<AttachmentType> converter =
            new Enum2StringConverter<>(AttachmentType.class);
        assertNull(converter.convert(null));
    }

    private static class Enum2EnumConverterTestHelper extends Enum2StringConverter<AttachmentType> {
        Enum2EnumConverterTestHelper() {
            super(AttachmentType.class);
        }
    }
}
