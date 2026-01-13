package run.ikaros.server.store.convert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@Slf4j
@ReadingConverter
public class String2EnumConverter<E extends Enum<E>>
    implements Converter<String, E> {
    private final Class<E> cls;

    /**
     * Construct.
     *
     * @param cls enum class, such as [AttachmentType.class]
     */
    public String2EnumConverter(Class<E> cls) {
        if (!cls.isEnum()) {
            throw new IllegalArgumentException("'cls' must is enum.");
        }
        this.cls = cls;
    }

    @Override
    public E convert(String source) {
        if (source == null || "".equalsIgnoreCase(source)) {
            return null;
        }

        E type;
        try {
            type = Enum.valueOf(cls, source);
        } catch (IllegalArgumentException e) {
            log.warn("Convert String to enum[{}] fail for source=[{}].",
                cls.getCanonicalName(), source);
            return null;
        }
        return type;
    }
}
