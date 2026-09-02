package run.ikaros.storage;

public record DeliveryGrantRequest(Integer ttlSeconds, Long rangeStart, Long rangeEnd) {}
