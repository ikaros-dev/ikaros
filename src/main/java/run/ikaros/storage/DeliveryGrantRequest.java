package run.ikaros.storage;

import java.util.UUID;

public record DeliveryGrantRequest(Integer ttlSeconds, Long rangeStart, Long rangeEnd,
                                   DeliveryIntent intent, UUID existingLeaseId, Boolean requireRange) {
    public DeliveryGrantRequest(Integer ttlSeconds, Long rangeStart, Long rangeEnd) {
        this(ttlSeconds, rangeStart, rangeEnd, DeliveryIntent.DOWNLOAD, null, true);
    }
}
