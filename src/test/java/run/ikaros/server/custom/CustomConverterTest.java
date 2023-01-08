package run.ikaros.server.custom;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import run.ikaros.server.store.entity.CustomEntity;
import run.ikaros.server.store.entity.CustomMetadataEntity;

class CustomConverterTest {

    CustomConverter customConverter = new DelegateCustomConverter();

    @Test
    void convertTo() {
        String title = "demo custom -- 0001";
        DemoCustom demoCustom = new DemoCustom()
            .setFlag(Boolean.FALSE)
            .setHead((byte) 1)
            .setHeaderMap("headerMap".getBytes(StandardCharsets.UTF_8))
            .setNumber(-1L)
            .setTime(LocalDateTime.now())
            .setTitle(title)
            .setUser(new DemoCustom.User()
                .setUsername("username"));
        CustomDto customDto = customConverter.convertTo(demoCustom);
        CustomEntity customEntity = customDto.customEntity();
        assertThat(customEntity.getGroup()).isEqualTo(DemoCustom.GROUP);
        assertThat(customEntity.getVersion()).isEqualTo(DemoCustom.VERSION);
        assertThat(customEntity.getKind()).isEqualTo(DemoCustom.KIND);
        assertThat(customEntity.getName()).isEqualTo(title);

        List<CustomMetadataEntity> customMetadataEntities = customDto.customMetadataEntityList();
        assertThat(customMetadataEntities.size()).isEqualTo(
            DemoCustom.class.getDeclaredFields().length - 1);

    }

    @Test
    void convertFrom() {
    }
}