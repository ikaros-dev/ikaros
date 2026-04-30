package run.ikaros.api.constant;

public interface OpenApiConst {
    String CORE_GROUP = "core.ikaros.run";
    String PLUGIN_GROUP = "plugin.ikaros.run";
    String CORE_VERSION = "v1";
    String ATT_STREAM_ENDPOINT_PREFIX =
        "/api/" + OpenApiConst.CORE_VERSION + "/attachment/stream/id";
}
