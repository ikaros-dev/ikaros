package run.ikaros.server.store.convert;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import run.ikaros.api.store.enums.AttachmentDriverType;

@WritingConverter
public class AttachmentDriverType2StringConverter
    implements Converter<AttachmentDriverType, String> {

    @Override
    public String convert(AttachmentDriverType source) {
        if (source == null) {
            return null;
        }
        return source.name();
    }

}
