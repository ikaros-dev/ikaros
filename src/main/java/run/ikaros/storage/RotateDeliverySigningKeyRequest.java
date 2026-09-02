package run.ikaros.storage;

public record RotateDeliverySigningKeyRequest(String credentialRef, Boolean emergency) {}
