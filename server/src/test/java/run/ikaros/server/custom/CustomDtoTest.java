package run.ikaros.server.custom;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.server.store.entity.CustomEntity;
import run.ikaros.server.store.entity.CustomMetadataEntity;

class CustomDtoTest {

    @Test
    void customEntityReturnsEntity() {
        UUID id = UUID.randomUUID();
        CustomEntity entity = CustomEntity.builder()
            .id(id)
            .group("test.group")
            .version("v1")
            .kind("TestKind")
            .name("test-name")
            .build();

        CustomDto dto = new CustomDto(entity, List.of());

        assertThat(dto.customEntity()).isSameAs(entity);
        assertThat(dto.customEntity().getId()).isEqualTo(id);
        assertThat(dto.customEntity().getGroup()).isEqualTo("test.group");
        assertThat(dto.customEntity().getVersion()).isEqualTo("v1");
        assertThat(dto.customEntity().getKind()).isEqualTo("TestKind");
        assertThat(dto.customEntity().getName()).isEqualTo("test-name");
    }

    @Test
    void customMetadataEntityListReturnsList() {
        UUID customId = UUID.randomUUID();
        CustomEntity entity = CustomEntity.builder()
            .id(customId)
            .group("g")
            .version("v")
            .kind("k")
            .name("n")
            .build();

        CustomMetadataEntity meta1 = CustomMetadataEntity.builder()
            .id(UUID.randomUUID())
            .customId(customId)
            .key("key1")
            .value("val1".getBytes())
            .build();
        CustomMetadataEntity meta2 = CustomMetadataEntity.builder()
            .id(UUID.randomUUID())
            .customId(customId)
            .key("key2")
            .value("val2".getBytes())
            .build();
        List<CustomMetadataEntity> metadataList = List.of(meta1, meta2);

        CustomDto dto = new CustomDto(entity, metadataList);

        assertThat(dto.customMetadataEntityList()).isSameAs(metadataList);
        assertThat(dto.customMetadataEntityList()).hasSize(2);
        assertThat(dto.customMetadataEntityList().get(0).getKey()).isEqualTo("key1");
        assertThat(dto.customMetadataEntityList().get(1).getKey()).isEqualTo("key2");
    }

    @Test
    void customMetadataEntityListCanBeNull() {
        CustomEntity entity = CustomEntity.builder()
            .id(UUID.randomUUID())
            .group("g")
            .version("v")
            .kind("k")
            .name("n")
            .build();

        CustomDto dto = new CustomDto(entity, null);

        assertThat(dto.customMetadataEntityList()).isNull();
    }

    @Test
    void equalsReturnsTrueForSameContent() {
        UUID id = UUID.randomUUID();
        UUID metaId = UUID.randomUUID();
        CustomEntity entity = CustomEntity.builder()
            .id(id).group("g").version("v").kind("k").name("n")
            .build();
        CustomMetadataEntity meta = CustomMetadataEntity.builder()
            .id(metaId).customId(id).key("k").value("v".getBytes())
            .build();

        CustomDto dto1 = new CustomDto(entity, new ArrayList<>(List.of(meta)));
        CustomDto dto2 = new CustomDto(entity, new ArrayList<>(List.of(meta)));

        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
    }

    @Test
    void equalsReturnsFalseForDifferentContent() {
        CustomEntity entity1 = CustomEntity.builder()
            .id(UUID.randomUUID()).group("g1").version("v1").kind("k1").name("n1")
            .build();
        CustomEntity entity2 = CustomEntity.builder()
            .id(UUID.randomUUID()).group("g2").version("v2").kind("k2").name("n2")
            .build();

        CustomDto dto1 = new CustomDto(entity1, List.of());
        CustomDto dto2 = new CustomDto(entity2, List.of());

        assertThat(dto1).isNotEqualTo(dto2);
    }

    @Test
    void equalsReturnsFalseForDifferentMetadataList() {
        UUID id = UUID.randomUUID();
        CustomEntity entity = CustomEntity.builder()
            .id(id).group("g").version("v").kind("k").name("n")
            .build();

        CustomMetadataEntity meta = CustomMetadataEntity.builder()
            .id(UUID.randomUUID()).customId(id).key("k").value("v".getBytes())
            .build();

        CustomDto dto1 = new CustomDto(entity, new ArrayList<>(List.of(meta)));
        CustomDto dto2 = new CustomDto(entity, new ArrayList<>());

        assertThat(dto1).isNotEqualTo(dto2);
    }

    @Test
    void equalsReturnsFalseForNullAndOtherType() {
        CustomEntity entity = CustomEntity.builder()
            .id(UUID.randomUUID()).group("g").version("v").kind("k").name("n")
            .build();
        CustomDto dto = new CustomDto(entity, List.of());

        assertThat(dto).isNotEqualTo(null);
        assertThat(dto).isNotEqualTo("a string");
    }
}
