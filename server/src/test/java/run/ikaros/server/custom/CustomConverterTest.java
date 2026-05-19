package run.ikaros.server.custom;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.api.custom.GroupVersionKind;
import run.ikaros.server.store.entity.CustomEntity;
import run.ikaros.server.store.entity.CustomMetadataEntity;

class CustomConverterTest {

    @Test
    void convertToConvertsCustomToCustomDto() {
        String title = "demo-custom-001";
        DemoCustom demoCustom = new DemoCustom()
            .setTitle(title)
            .setFlag(Boolean.TRUE)
            .setHead((byte) 1)
            .setNumber(42L)
            .setTime(LocalDateTime.of(2025, 1, 15, 10, 30))
            .setHeaderMap("headerMap".getBytes(StandardCharsets.UTF_8))
            .setUser(new DemoCustom.User().setUsername("alice"));

        CustomDto customDto = CustomConverter.convertTo(demoCustom);
        CustomEntity entity = customDto.customEntity();

        assertThat(entity).isNotNull();
        assertThat(entity.getGroup()).isEqualTo(DemoCustom.GROUP);
        assertThat(entity.getVersion()).isEqualTo(DemoCustom.VERSION);
        assertThat(entity.getKind()).isEqualTo(DemoCustom.KIND);
        assertThat(entity.getName()).isEqualTo(title);
        // id should be null before insert
        assertThat(entity.getId()).isNull();

        List<CustomMetadataEntity> metadataEntities = customDto.customMetadataEntityList();
        // All fields except @Name field become metadata entries
        int expectedMetadataCount =
            DemoCustom.class.getDeclaredFields().length - 1;
        assertThat(metadataEntities).hasSize(expectedMetadataCount);

        // Each metadata entry should have a non-null key and value
        for (CustomMetadataEntity meta : metadataEntities) {
            assertThat(meta.getKey()).isNotBlank();
            assertThat(meta.getValue()).isNotNull();
            assertThat(meta.getId()).isNull();
            assertThat(meta.getCustomId()).isNull();
        }
    }

    @Test
    void convertToRoundTripWithConvertFrom() {
        String title = "round-trip-title";
        byte[] headerBytes = "binary-data".getBytes(StandardCharsets.UTF_8);
        LocalDateTime now = LocalDateTime.now();

        DemoCustom original = new DemoCustom()
            .setTitle(title)
            .setFlag(Boolean.FALSE)
            .setHead((byte) 9)
            .setNumber(123L)
            .setTime(now)
            .setHeaderMap(headerBytes)
            .setUser(new DemoCustom.User().setUsername("bob"));

        CustomDto dto = CustomConverter.convertTo(original);

        // Assign an id and customId so convertFrom can filter metadata correctly
        UUID customId = UUID.randomUUID();
        dto.customEntity().setId(customId);
        for (CustomMetadataEntity meta : dto.customMetadataEntityList()) {
            meta.setCustomId(customId);
        }

        DemoCustom restored = CustomConverter.convertFrom(DemoCustom.class, dto);

        assertThat(restored).isNotNull();
        assertThat(restored.getTitle()).isEqualTo(title);
        assertThat(restored.getFlag()).isEqualTo(Boolean.FALSE);
        assertThat(restored.getHead()).isEqualTo((byte) 9);
        assertThat(restored.getNumber()).isEqualTo(123L);
        assertThat(restored.getTime()).isEqualTo(now);
        assertThat(restored.getHeaderMap()).isEqualTo(headerBytes);
        assertThat(restored.getUser()).isNotNull();
        assertThat(restored.getUser().getUsername()).isEqualTo("bob");
    }

    @Test
    void convertFromWithNoMetadataRestoresNameOnly() {
        String title = "name-only-title";
        DemoOnlyNameCustom original = new DemoOnlyNameCustom().setTitle(title);

        CustomDto dto = CustomConverter.convertTo(original);
        UUID customId = UUID.randomUUID();
        dto.customEntity().setId(customId);

        DemoOnlyNameCustom restored =
            CustomConverter.convertFrom(DemoOnlyNameCustom.class, dto);

        assertThat(restored).isNotNull();
        assertThat(restored.getTitle()).isEqualTo(title);
    }

    @Test
    void convertFromIgnoresMetadataWithMismatchedCustomId() {
        String title = "mismatch-id-title";
        DemoCustom demoCustom = new DemoCustom()
            .setTitle(title)
            .setNumber(100L);

        CustomDto dto = CustomConverter.convertTo(demoCustom);
        UUID correctId = UUID.randomUUID();
        dto.customEntity().setId(correctId);

        // Set all metadata entries to have a WRONG customId
        for (CustomMetadataEntity meta : dto.customMetadataEntityList()) {
            meta.setCustomId(UUID.randomUUID());
        }

        DemoCustom restored = CustomConverter.convertFrom(DemoCustom.class, dto);

        assertThat(restored).isNotNull();
        assertThat(restored.getTitle()).isEqualTo(title);
        // Fields that were metadata should NOT be restored because customId didn't match
        assertThat(restored.getNumber()).isNull();
        assertThat(restored.getFlag()).isNull();
        assertThat(restored.getTime()).isNull();
    }

    @Test
    void getNameFieldValueReturnsNullForNullInput() {
        assertThat(CustomConverter.getNameFieldValue(null)).isNull();
    }

    @Test
    void getNameFieldValueExtractsNameFieldFromCustomObject() {
        String title = "my-custom-name";
        DemoCustom demoCustom = new DemoCustom().setTitle(title);

        String nameValue = CustomConverter.getNameFieldValue(demoCustom);

        assertThat(nameValue).isEqualTo(title);
    }

    @Test
    void gvkReturnsGroupVersionKindForAnnotatedClass() {
        GroupVersionKind gvk = CustomConverter.gvk(DemoCustom.class);

        assertThat(gvk).isNotNull();
        assertThat(gvk.group()).isEqualTo(DemoCustom.GROUP);
        assertThat(gvk.version()).isEqualTo(DemoCustom.VERSION);
        assertThat(gvk.kind()).isEqualTo(DemoCustom.KIND);
    }

    @Test
    void gvkReturnsEmptyValuesForNonAnnotatedClass() {
        GroupVersionKind gvk = CustomConverter.gvk(String.class);

        assertThat(gvk).isNotNull();
        assertThat(gvk.group()).isEmpty();
        assertThat(gvk.version()).isEmpty();
        assertThat(gvk.kind()).isEmpty();
    }
}
