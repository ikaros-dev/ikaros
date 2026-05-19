package run.ikaros.server.store.convert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ConverterUtilsTest {

    private enum SampleEnum {
        VALUE_A, VALUE_B, VALUE_C
    }

    @Test
    void enum2StringConverter_constructWithEnumClass() {
        var converter = new Enum2StringConverter<>(SampleEnum.class);
        assertThat(converter).isNotNull();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void enum2StringConverter_constructWithNonEnumClass_throwsException() {
        assertThatThrownBy(() -> new Enum2StringConverter(String.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("'cls' must is enum.");
    }

    @Test
    void enum2StringConverter_convertNonNull_returnsName() {
        var converter = new Enum2StringConverter<>(SampleEnum.class);

        assertThat(converter.convert(SampleEnum.VALUE_A)).isEqualTo("VALUE_A");
        assertThat(converter.convert(SampleEnum.VALUE_B)).isEqualTo("VALUE_B");
    }

    @Test
    void enum2StringConverter_convertNull_returnsNull() {
        var converter = new Enum2StringConverter<>(SampleEnum.class);

        assertThat(converter.convert(null)).isNull();
    }

    @Test
    void string2EnumConverter_constructWithEnumClass() {
        var converter = new String2EnumConverter<>(SampleEnum.class);
        assertThat(converter).isNotNull();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void string2EnumConverter_constructWithNonEnumClass_throwsException() {
        assertThatThrownBy(() -> new String2EnumConverter(String.class))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("'cls' must is enum.");
    }

    @Test
    void string2EnumConverter_convertValidString_returnsEnum() {
        var converter = new String2EnumConverter<>(SampleEnum.class);

        assertThat(converter.convert("VALUE_A")).isEqualTo(SampleEnum.VALUE_A);
        assertThat(converter.convert("VALUE_B")).isEqualTo(SampleEnum.VALUE_B);
    }

    @Test
    void string2EnumConverter_convertNull_returnsNull() {
        var converter = new String2EnumConverter<>(SampleEnum.class);

        assertThat(converter.convert(null)).isNull();
    }

    @Test
    void string2EnumConverter_convertEmptyString_returnsNull() {
        var converter = new String2EnumConverter<>(SampleEnum.class);

        assertThat(converter.convert("")).isNull();
    }

    @Test
    void string2EnumConverter_convertInvalidString_returnsNull() {
        var converter = new String2EnumConverter<>(SampleEnum.class);

        assertThat(converter.convert("NONEXISTENT")).isNull();
    }

    @Test
    void roundTrip_enumToStringAndBack() {
        var enum2String = new Enum2StringConverter<>(SampleEnum.class);
        var string2Enum = new String2EnumConverter<>(SampleEnum.class);

        for (SampleEnum value : SampleEnum.values()) {
            String str = enum2String.convert(value);
            SampleEnum result = string2Enum.convert(str);
            assertThat(result).isEqualTo(value);
        }
    }
}
