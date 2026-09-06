package run.ikaros.storage;

public record DeliveryLeaseRequest(String deliveryGrant, Integer ttlSeconds) {}
