package run.ikaros.server.store.convert;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import run.ikaros.api.store.enums.AttachmentRelationType;

@Slf4j
@ReadingConverter
public class AttachmentRelationType2EnumConverter
    implements Converter<String, AttachmentRelationType> {


    @Override
    public AttachmentRelationType convert(String source) {
        if (source == null || "".equalsIgnoreCase(source)) {
            return null;
        }
        Set<String> types = Arrays.stream(AttachmentRelationType.values()).map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());
        if (!types.contains(source)) {
            return null;
        }
        AttachmentRelationType type;
        try {
            type = AttachmentRelationType.valueOf(source);
        } catch (IllegalArgumentException e) {
            log.warn("Convert String to enum[AttachmentRelationType] fail for source=[{}].",
                source);
            return null;
        }
        return type;
    }
}
