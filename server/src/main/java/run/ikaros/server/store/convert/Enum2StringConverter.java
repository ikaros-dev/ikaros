package run.ikaros.server.store.convert;

import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
@NullUnmarked
public class Enum2StringConverter<E extends Enum<E>>
    implements Converter<E, String> {
    private final Class<E> cls;

    /**
     * Construct.
     *
     * @param cls enum class, such as [AttachmentType.class]
     */
    public Enum2StringConverter(Class<E> cls) {
        if (!cls.isEnum()) {
            throw new IllegalArgumentException("'cls' must is enum.");
        }
        this.cls = cls;
    }

    @Override
    public @Nullable String convert(@Nullable E source) {
        if (source == null) {
            return null;
        }
        return source.name();
    }

}
