package run.ikaros.storage.api;

public record DeliveryLeaseRequest(String deliveryGrant, Integer ttlSeconds) {}
