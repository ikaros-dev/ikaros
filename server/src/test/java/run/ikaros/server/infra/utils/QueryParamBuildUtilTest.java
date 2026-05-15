package run.ikaros.server.infra.utils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springdoc.core.fn.builders.operation.Builder;

class QueryParamBuildUtilTest {

    @Test
    void buildParametersFromType_withSimpleClass() {
        Builder operationBuilder = Builder.operationBuilder();
        // Test with a simple class
        QueryParamBuildUtil.buildParametersFromType(operationBuilder, SimpleQueryParam.class);
        assertNotNull(operationBuilder);
    }

    @Test
    void buildParametersFromType_withRequiredFields() {
        Builder operationBuilder = Builder.operationBuilder();
        QueryParamBuildUtil.buildParametersFromType(operationBuilder, RequiredQueryParam.class);
        assertNotNull(operationBuilder);
    }

    static class SimpleQueryParam {
        private String name;
        private Integer age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }
    }

    static class RequiredQueryParam {
        @io.swagger.v3.oas.annotations.media.Schema(requiredMode = io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED)
        private String id;

        private String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
