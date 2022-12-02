package run.ikaros.server.model.dto;

import lombok.Data;

@Data
public class OptionNetworkDTO {
    private String proxyHttpHost;
    private Integer proxyHttpPort;
    private Integer connectTimeout;
    private Integer readTimeout;
}
