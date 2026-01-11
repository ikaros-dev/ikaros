package run.ikaros.server.store.convert;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class Enum2StringConverter<E extends Enum<E>>
    implements Converter<E, String> {

    @Override
    public String convert(E source) {
        if (source == null) {
            return null;
        }
        return source.name();
    }

}
