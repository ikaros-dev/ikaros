package run.ikaros.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/** 验证 HTTP Registry、OpenAPI 和公共表示之间的收敛关系。 */
class PublicApiContractTest {
    private static final Path CONTRACT_ROOT = Path.of("docs", "00-product-baseline");
    private static final Path REGISTRY = CONTRACT_ROOT.resolve("contracts/P0-HTTP-Operation-Registry.yaml");
    private static final Pattern SNAKE_CASE = Pattern.compile("[a-z][a-z0-9]*(?:_[a-z0-9]+)*");
    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "patch", "delete", "head", "options");
    private static final Yaml YAML = new Yaml();

    @Test
    void registryAndOpenApiDescribeExactlyTheSameOperations() throws IOException {
        Map<String, Object> registry = load(REGISTRY);
        Map<String, RegisteredOperation> registered = readRegistry(registry);
        Map<String, ApiOperation> openApi = readOpenApiOperations(registry);

        Set<String> missingFromOpenApi = new HashSet<>(registered.keySet());
        missingFromOpenApi.removeAll(openApi.keySet());
        Set<String> missingFromRegistry = new HashSet<>(openApi.keySet());
        missingFromRegistry.removeAll(registered.keySet());
        assertTrue(missingFromOpenApi.isEmpty() && missingFromRegistry.isEmpty(),
            "Registry 与 OpenAPI 的 method/path 集合必须一致; missingFromOpenApi="
                + missingFromOpenApi + "; missingFromRegistry=" + missingFromRegistry);
        assertEquals(registered.size(), new HashSet<>(registered.values()).size(), "Registry operation 不得重复");
        assertEquals(openApi.size(), openApi.values().stream().map(ApiOperation::operationId).distinct().count(),
            "operationId 必须全局唯一");

        registered.forEach((key, expected) -> {
            ApiOperation actual = openApi.get(key);
            assertEquals(expected.operationId(), actual.operationId(), key + " operationId 不一致");
            assertEquals(expected.contractId(), actual.contractId(), key + " contractId 不一致");
            assertEquals(expected.source(), actual.source(), key + " source 不一致");
        });
    }

    @Test
    void publicContractUsesStableApiBoundaryAndSnakeCaseProperties() throws IOException {
        Map<String, Object> registry = load(REGISTRY);
        List<String> sources = stringList(registry, "sources");
        assertTrue(sources.size() >= 2, "Registry 至少要覆盖主 OpenAPI 与收敛补充规范");

        for (String source : sources) {
            Map<String, Object> document = load(CONTRACT_ROOT.resolve(source));
            assertEquals("3.1.0", document.get("openapi"), source + " 必须是 OpenAPI 3.1");
            List<Map<String, Object>> servers = maps(document, "servers");
            assertFalse(servers.isEmpty(), source + " 必须声明 API server");
            assertEquals("/api", servers.getFirst().get("url"), source + " 的 base path 必须是 /api");
            assertFalse(document.containsKey("/api/v2"), source + " 不得在 path 中重复暴露 /api/v2");
            assertSnakeCaseProperties(document.get("components"), source);
        }
    }

    @Test
    void criticalHttpSemanticsArePresentInThePublicContract() throws IOException {
        Map<String, Object> registry = load(REGISTRY);
        Map<String, ApiOperation> operations = readOpenApiOperations(registry);

        for (String operationId : List.of("createResource", "createStorageProvider", "createUser")) {
            ApiOperation operation = byOperationId(operations, operationId);
            assertTrue(hasParameterRef(operation.operation(), "#/components/parameters/IdempotencyKey"),
                operationId + " 必须声明 Idempotency-Key");
        }

        for (String operationId : List.of("updateResource", "archiveResource", "restoreResource", "trashResource")) {
            ApiOperation operation = byOperationId(operations, operationId);
            assertTrue(hasParameterRef(operation.operation(), "#/components/parameters/IfMatch"),
                operationId + " 必须声明 If-Match");
            assertTrue(operation.responses().containsKey("412"), operationId + " 必须声明 stale ETag 的 412 响应");
            assertTrue(operation.responses().containsKey("428"), operationId + " 必须声明缺少 If-Match 的 428 响应");
        }

        Map<String, Object> base = load(CONTRACT_ROOT.resolve("contracts/openapi-v2-p0.yaml"));
        Map<String, Object> problem = componentSchema(base, "Problem");
        assertTrue(stringList(problem, "required").contains("code"), "Problem 必须包含 machine-readable code");

        ApiOperation content = byOperationId(operations, "getAttachmentContent");
        assertTrue(hasParameterName(content.operation(), "Range"), "Attachment content 必须接受 Range");
        assertTrue(content.responses().containsKey("206"), "Attachment content 必须声明 206 Partial Content");
    }

    @Test
    void invalidRegistryMappingIsRejected() {
        Map<String, Object> invalid = new HashMap<>();
        invalid.put("operations", List.of(
            Map.of("method", "GET", "path", "/resources", "operation_id", "listResources",
                "contract_id", "resource.list-resources", "source", "contracts/openapi-v2-p0.yaml"),
            Map.of("method", "GET", "path", "/resources", "operation_id", "listResources",
                "contract_id", "resource.list-resources", "source", "contracts/openapi-v2-p0.yaml")));

        assertThrows(IllegalStateException.class, () -> readRegistry(invalid),
            "重复 method/path 必须阻止契约继续发布");
    }

    private static Map<String, Object> load(Path path) throws IOException {
        Object parsed = YAML.load(Files.readString(path));
        assertTrue(parsed instanceof Map<?, ?>, path + " 必须是 YAML object");
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) parsed;
        return result;
    }

    private static Map<String, RegisteredOperation> readRegistry(Map<String, Object> registry) {
        Map<String, RegisteredOperation> result = new HashMap<>();
        for (Map<String, Object> entry : maps(registry, "operations")) {
            RegisteredOperation operation = new RegisteredOperation(
                string(entry, "method"), string(entry, "path"), string(entry, "operation_id"),
                string(entry, "contract_id"), string(entry, "source"));
            String key = key(operation.method(), operation.path());
            if (result.put(key, operation) != null) {
                throw new IllegalStateException("duplicate registry operation: " + key);
            }
        }
        assertFalse(result.isEmpty(), "Registry 必须至少登记一个 operation");
        return result;
    }

    private static Map<String, ApiOperation> readOpenApiOperations(Map<String, Object> registry) throws IOException {
        Map<String, ApiOperation> result = new HashMap<>();
        for (String source : stringList(registry, "sources")) {
            Map<String, Object> document = load(CONTRACT_ROOT.resolve(source));
            for (Map.Entry<String, Object> pathEntry : map(document, "paths").entrySet()) {
                assertTrue(pathEntry.getValue() instanceof Map<?, ?>,
                    source + " " + pathEntry.getKey() + " must define a path item object");
                Map<String, Object> pathItem = map(pathEntry.getValue());
                for (Map.Entry<String, Object> methodEntry : pathItem.entrySet()) {
                    if (!HTTP_METHODS.contains(methodEntry.getKey())) {
                        continue;
                    }
                    assertTrue(methodEntry.getValue() instanceof Map<?, ?>,
                        source + " " + pathEntry.getKey() + " " + methodEntry.getKey()
                            + " must define an operation object");
                    Map<String, Object> operation = effectiveOperation(pathItem, methodEntry.getValue());
                    String key = key(methodEntry.getKey().toUpperCase(), pathEntry.getKey());
                    ApiOperation parsed = new ApiOperation(
                        methodEntry.getKey().toUpperCase(), pathEntry.getKey(), string(operation, "operationId"),
                        string(operation, "x-ikaros-contract-id"), source, operation, map(operation, "responses"));
                    if (result.put(key, parsed) != null) {
                        throw new IllegalStateException("duplicate OpenAPI operation: " + key);
                    }
                }
            }
        }
        assertFalse(result.isEmpty(), "OpenAPI 必须至少声明一个 operation");
        return result;
    }

    private static Map<String, Object> effectiveOperation(Map<String, Object> pathItem, Object operationValue) {
        Map<String, Object> operation = new HashMap<>(map(operationValue));
        List<Object> parameters = new ArrayList<>();
        if (pathItem.get("parameters") instanceof List<?> pathParameters) {
            parameters.addAll(pathParameters);
        }
        if (operation.get("parameters") instanceof List<?> operationParameters) {
            parameters.addAll(operationParameters);
        }
        if (!parameters.isEmpty()) {
            operation.put("parameters", parameters);
        }
        return operation;
    }

    private static ApiOperation byOperationId(Map<String, ApiOperation> operations, String operationId) {
        return operations.values().stream().filter(operation -> operation.operationId().equals(operationId)).findFirst()
            .orElseThrow(() -> new AssertionError("missing operation: " + operationId));
    }

    private static boolean hasParameterRef(Map<String, Object> operation, String reference) {
        return maps(operation, "parameters").stream().map(parameter -> parameter.get("$ref"))
            .filter(String.class::isInstance).map(String.class::cast)
            .anyMatch(value -> reference.equals(value) || value.endsWith(reference.substring(reference.indexOf('#'))));
    }

    private static boolean hasParameterName(Map<String, Object> operation, String name) {
        return maps(operation, "parameters").stream().anyMatch(parameter -> name.equals(parameter.get("name")));
    }

    private static Map<String, Object> componentSchema(Map<String, Object> document, String name) {
        Map<String, Object> schemas = map(map(document, "components"), "schemas");
        return map(schemas, name);
    }

    private static void assertSnakeCaseProperties(Object node, String source) {
        if (!(node instanceof Map<?, ?> map)) {
            return;
        }
        Object properties = map.get("properties");
        if (properties instanceof Map<?, ?> propertyMap) {
            for (Object property : propertyMap.keySet()) {
                assertTrue(property instanceof String && SNAKE_CASE.matcher((String) property).matches(),
                    source + " contains non-snake_case public property: " + property);
            }
        }
        map.values().forEach(value -> assertSnakeCaseProperties(value, source));
    }

    private static String key(String method, String path) {
        return method.toUpperCase() + " " + path;
    }

    private static String string(Map<String, Object> map, String key) {
        Object value = map.get(key);
        assertNotNull(value, "missing YAML field: " + key);
        return value.toString();
    }

    private static Map<String, Object> map(Object value) {
        assertTrue(value instanceof Map<?, ?>, "YAML value must be an object");
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) value;
        return result;
    }

    private static Map<String, Object> map(Map<String, Object> parent, String key) {
        return map(parent.get(key));
    }

    private static List<Map<String, Object>> maps(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        assertTrue(value instanceof List<?>, "YAML field must be a list: " + key);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            result.add(map(item));
        }
        return result;
    }

    private static List<String> stringList(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        assertTrue(value instanceof List<?>, "YAML field must be a list: " + key);
        return ((List<?>) value).stream().map(Object::toString).toList();
    }

    private record RegisteredOperation(String method, String path, String operationId, String contractId, String source) {
        private RegisteredOperation {
            method = method.toUpperCase();
        }
    }

    private record ApiOperation(String method, String path, String operationId, String contractId, String source,
                                Map<String, Object> operation, Map<String, Object> responses) {
    }
}
