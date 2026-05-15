package run.ikaros.api.endpoint;


import run.ikaros.api.custom.GroupVersionKind;

public interface CustomEndpoint extends Endpoint {
    GroupVersionKind groupVersionKind();
}
