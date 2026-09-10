package run.ikaros.operations.notification;

public record NotificationPreferenceView(boolean taskSuccessEnabled, boolean taskFailureEnabled) {
    static NotificationPreferenceView from(NotificationPreferenceEntity value) {
        return new NotificationPreferenceView(value.taskSuccessEnabled(), value.taskFailureEnabled());
    }
}
