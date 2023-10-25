package run.ikaros.server.custom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import run.ikaros.api.custom.Custom;
import run.ikaros.api.custom.Name;
import run.ikaros.api.infra.exception.custom.CustomConvertException;
import run.ikaros.server.store.entity.CustomEntity;
import run.ikaros.server.store.entity.CustomMetadataEntity;
import run.ikaros.server.test.TestConst;
import run.ikaros.server.test.reflect.MemberMatcher;

class CustomConverterTest {

    @Test
    void convertSuccess() {
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

    @Test
    void covertCustomField2MetadataEntity() throws NoSuchFieldException, IOException {
        DemoCustom demoCustom = new DemoCustom()
            .setTitle("test-title")
            .setNumber(9L);

        ObjectMapper om = new ObjectMapper();
        Field numberField = MemberMatcher.field(DemoCustom.class, "number");

        try {
            CustomConverter.covertCustomFieldToMetadataEntity(null, numberField, om);
            fail(TestConst.PROCESS_SHOULD_NOT_RUN_TO_THIS);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("'custom' must not null");
        }

        try {
            CustomConverter.covertCustomFieldToMetadataEntity(demoCustom, null, om);
            fail(TestConst.PROCESS_SHOULD_NOT_RUN_TO_THIS);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("'field' must not null");
        }


        Field titleField = MemberMatcher.field(DemoCustom.class, "title");
        try {
            CustomConverter.covertCustomFieldToMetadataEntity(demoCustom, titleField, om);
            fail(TestConst.PROCESS_SHOULD_NOT_RUN_TO_THIS);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).startsWith(
                "@Name field can not convert CustomMetadataEntity instance");
        }


        CustomMetadataEntity customMetadataEntity =
            CustomConverter.covertCustomFieldToMetadataEntity(demoCustom, numberField, om);
        assertThat(om.readValue(customMetadataEntity.getValue(), Long.class))
            .isEqualTo(9L);

        customMetadataEntity =
            CustomConverter.covertCustomFieldToMetadataEntity(demoCustom, numberField, null);
        assertThat(om.readValue(customMetadataEntity.getValue(), Long.class))
            .isEqualTo(9L);

        om = Mockito.spy(om);
        Mockito.when(om.writeValueAsBytes(9L))
            .thenThrow(new NullPointerException("mock exception"));

        try {
            CustomConverter.covertCustomFieldToMetadataEntity(demoCustom, numberField, om);
            fail(TestConst.PROCESS_SHOULD_NOT_RUN_TO_THIS);
        } catch (CustomConvertException e) {
            assertThat(e.getMessage()).contains(
                "convert custom field to metadata entity fail for class: ");
        }
    }


    @Test
    void getNameFieldValue() {
        assertThat(CustomConverter.getNameFieldValue(null)).isNull();

        TestCustom1 testCustom1 = new TestCustom1()
            .setName("test-custom-1")
            .setTitle("test-title-1");

        try {
            CustomConverter.getNameFieldValue(testCustom1);
            fail(TestConst.PROCESS_SHOULD_NOT_RUN_TO_THIS);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo(
                "@run.ikaros.api.custom.Custom mark class "
                    + "must has one field that type is String and"
                    + " mark by @run.ikaros.api.custom.Name");
        }

        TestCustom2 testCustom2 = new TestCustom2();
        try {
            CustomConverter.getNameFieldValue(testCustom2);
            fail(TestConst.PROCESS_SHOULD_NOT_RUN_TO_THIS);
        } catch (CustomConvertException e) {
            assertThat(e.getMessage()).contains("get custom name filed value fail for name");
        }

        TestCustom2 testCustom21 = CustomConverter.convertFrom(TestCustom2.class,
            CustomConverter.convertTo(testCustom2.setTitle("test-custom-2")));
        assertThat(testCustom21.getTitle()).isEqualTo("test-custom-2");

        // CustomConverter.getNameFieldValue(testCustom2);
    }

    @Custom(group = "test.ikaros.run", version = "v1", kind = "TestCustom1",
        singular = "test", plural = "tests")
    static class TestCustom1 {
        @Name
        private String title;
        @Name
        private String name;

        public String getTitle() {
            return title;
        }

        public TestCustom1 setTitle(String title) {
            this.title = title;
            return this;
        }

        public String getName() {
            return name;
        }

        public TestCustom1 setName(String name) {
            this.name = name;
            return this;
        }
    }


    @Custom(group = "test.ikaros.run", version = "v1", kind = "TestCustom2",
        singular = "test", plural = "tests")
    static class TestCustom2 {
        @Name
        private String title;

        public String getTitle() {
            return title;
        }

        public TestCustom2 setTitle(String title) {
            this.title = title;
            return this;
        }
    }
}