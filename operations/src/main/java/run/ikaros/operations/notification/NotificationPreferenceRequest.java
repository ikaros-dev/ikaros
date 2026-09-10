package run.ikaros.operations.notification;

public record NotificationPreferenceRequest(boolean taskSuccessEnabled, boolean taskFailureEnabled) { }
