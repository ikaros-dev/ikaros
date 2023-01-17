package run.ikaros.server.custom.scheme;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import java.util.Map;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import run.ikaros.server.custom.Custom;
import run.ikaros.server.custom.GroupVersionKind;
import run.ikaros.server.custom.exception.CustomException;

/**
 * This class represents scheme of a Custom.
 *
 * @author li-guohao
 */
public record CustomScheme(Class<?> type,
                           GroupVersionKind groupVersionKind,
                           String plural,
                           String singular,
                           ObjectNode openApiSchema) {
    /**
     * Construct a CustomSchema.
     *
     * @param type             is Custom type.
     * @param groupVersionKind is GroupVersionKind of Extension.
     * @param plural           is plural name of Extension.
     * @param singular         is singular name of Extension.
     * @param openApiSchema    is JSON schema of Extension.
     */
    public CustomScheme {
        Assert.notNull(type, "Type of Custom must not be null");
        Assert.notNull(groupVersionKind, "GroupVersionKind of Extension must not be null");
        Assert.hasText(plural, "Plural name of Extension must not be blank");
        Assert.hasText(singular, "Singular name of Extension must not be blank");
        Assert.notNull(openApiSchema, "Json Schema must not be null");
    }

    /**
     * Builds Scheme from type with @GVK annotation.
     *
     * @param type is Extension type with GVK annotation.
     * @return Scheme definition.
     * @throws CustomException when the type has not annotated @GVK.
     */
    public static CustomScheme buildFromType(Class<?> type) {
        // concrete scheme from annotation
        var custom = getCustomAnnotationFromType(type);

        // generate OpenAPI schema
        var resolvedSchema = ModelConverters.getInstance().readAllAsResolvedSchema(type);
        var mapper = Json.mapper();
        var schema = (ObjectNode) mapper.valueToTree(resolvedSchema.schema);
        // for schema validation.
        schema.set("components",
            mapper.valueToTree(Map.of("schemas", resolvedSchema.referencedSchemas)));

        return new CustomScheme(type,
            new GroupVersionKind(custom.group(), custom.version(), custom.kind()),
            custom.plural(),
            custom.singular(),
            schema);
    }

    /**
     * Gets {@link Custom} annotation from Custom type.
     *
     * @param type is Custom type with {@link Custom} annotation
     * @return {@link Custom} annotation
     * @throws CustomException when the type has not annotated {@link Custom}
     */
    @NonNull
    public static Custom getCustomAnnotationFromType(@NonNull Class<?> type) {
        var gvk = type.getAnnotation(Custom.class);
        Assert.notNull(gvk,
            "Missing annotation " + Custom.class.getName() + " on type " + type.getName());
        return gvk;
    }
}
