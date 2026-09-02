package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;

/** Delivery Grant 对外稳定响应，不携带内部 token 字段。 */
public record DeliveryGrantContractView(
    UUID grantId,
    UUID attachmentId,
    UUID leaseId,
    UUID deliveryProviderId,
    String method,
    String url,
    Instant expiresAt,
    boolean rangeSupported,
    String contentType,
    long contentLength,
    DeliveryGrantRevocationLevel revocationMode
) {}
