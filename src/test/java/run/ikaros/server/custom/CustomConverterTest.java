package run.ikaros.server.custom;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import run.ikaros.server.store.entity.CustomEntity;
import run.ikaros.server.store.entity.CustomMetadataEntity;

class CustomConverterTest {

    @Test
    void convert() {
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
        CustomDto customDto = CustomConverter.convertTo(demoCustom);
        CustomEntity customEntity = customDto.customEntity();
        assertThat(customEntity.getGroup()).isEqualTo(DemoCustom.GROUP);
        assertThat(customEntity.getVersion()).isEqualTo(DemoCustom.VERSION);
        assertThat(customEntity.getKind()).isEqualTo(DemoCustom.KIND);
        assertThat(customEntity.getName()).isEqualTo(title);

        List<CustomMetadataEntity> customMetadataEntities = customDto.customMetadataEntityList();
        assertThat(customMetadataEntities.size()).isEqualTo(
            DemoCustom.class.getDeclaredFields().length - 1);

        DemoCustom demoCustom1 = CustomConverter.convertFrom(DemoCustom.class, customDto);
        assertThat(demoCustom1).isNotEqualTo(demoCustom);
        assertThat(demoCustom1.getHeaderMap()).isEqualTo(demoCustom.getHeaderMap());
        assertThat(demoCustom1.getTime()).isEqualTo(demoCustom.getTime());

    }

}